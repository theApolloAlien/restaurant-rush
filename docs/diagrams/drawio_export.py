"""
Writes editable draw.io versions of the report diagrams:

  docs/diagrams/class-diagram.drawio               (from class_diagram.py, same layout as the SVG)
  docs/diagrams/sequence-1-arrival-order.drawio    (from the .mmd source)
  docs/diagrams/sequence-2-service-payment.drawio  (from the .mmd source)

Open them in draw.io / diagrams.net (desktop app, the web app, or the VS Code extension).
Every class, member, lifeline, message and fragment is a normal draw.io shape, so it can be moved
and edited by hand.  Run:  python3 docs/diagrams/drawio_export.py
"""
import re
import sys
from html import escape
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import class_diagram as cd  # noqa: E402


def xml(s):
    """Escape a value for an XML attribute."""
    return escape(str(s), quote=True)


class Doc:
    def __init__(self, name, width, height):
        self.name, self.width, self.height = name, width, height
        self.cells = []
        self.next_id = 2

    def new_id(self, prefix="c"):
        self.next_id += 1
        return f"{prefix}{self.next_id}"

    def vertex(self, value, style, x, y, w, h, parent="1", cid=None):
        cid = cid or self.new_id()
        self.cells.append(f'<mxCell id="{cid}" value="{xml(value)}" style="{style}" vertex="1" parent="{parent}">'
                          f'<mxGeometry x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" as="geometry"/>'
                          f'</mxCell>')
        return cid

    def edge(self, value, style, source=None, target=None, points=(), src_pt=None, dst_pt=None, cid=None):
        cid = cid or self.new_id("e")
        ends = (f' source="{source}"' if source else "") + (f' target="{target}"' if target else "")
        inner = ""
        if src_pt:
            inner += f'<mxPoint x="{src_pt[0]:.1f}" y="{src_pt[1]:.1f}" as="sourcePoint"/>'
        if dst_pt:
            inner += f'<mxPoint x="{dst_pt[0]:.1f}" y="{dst_pt[1]:.1f}" as="targetPoint"/>'
        if points:
            inner += '<Array as="points">' + "".join(f'<mxPoint x="{px:.1f}" y="{py:.1f}"/>' for px, py in points) \
                     + "</Array>"
        self.cells.append(f'<mxCell id="{cid}" value="{xml(value)}" style="{style}" edge="1" parent="1"{ends}>'
                          f'<mxGeometry relative="1" as="geometry">{inner}</mxGeometry></mxCell>')
        return cid

    def edge_label(self, edge_id, value, at, dx, dy, style):
        cid = self.new_id("l")
        self.cells.append(f'<mxCell id="{cid}" value="{xml(value)}" style="{style}" vertex="1" connectable="0" '
                          f'parent="{edge_id}"><mxGeometry x="{at}" relative="1" as="geometry">'
                          f'<mxPoint x="{dx:.1f}" y="{dy:.1f}" as="offset"/></mxGeometry></mxCell>')

    def write(self, path):
        body = "".join(self.cells)
        path.write_text(
            f'<mxfile host="drawio" agent="restaurant-rush drawio_export.py" type="device">'
            f'<diagram id="{xml(path.stem)}" name="{xml(self.name)}">'
            f'<mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" '
            f'arrows="1" fold="1" page="1" pageScale="1" pageWidth="{self.width}" pageHeight="{self.height}" '
            f'math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>{body}</root>'
            f'</mxGraphModel></diagram></mxfile>\n', encoding="utf-8")
        print("wrote", path.relative_to(HERE.parent.parent))


# ===================================================================================== class diagram

SANS = "fontFamily=Helvetica;"
MONO = "fontFamily=Courier New;"
EDGE_STYLE = {
    "inherit": "endArrow=block;endFill=0;endSize=14;",
    "realize": "endArrow=block;endFill=0;endSize=14;dashed=1;",
    "compose": "startArrow=diamondThin;startFill=1;startSize=18;endArrow=none;",
    "aggregate": "startArrow=diamondThin;startFill=0;startSize=18;endArrow=none;",
    "assoc": "endArrow=open;endSize=10;",
    "depend": "endArrow=open;endSize=10;dashed=1;",
}
SIDE = {"top": lambda f: (f, 0), "bottom": lambda f: (f, 1), "left": lambda f: (0, f), "right": lambda f: (1, f)}


def member_html(raw):
    s = escape(raw.lstrip("*_"))
    if raw.startswith("*"):
        return f"<i>{s}</i>"
    if raw.startswith("_"):
        return f"<u>{s}</u>"
    return s


def class_drawio():
    doc = Doc("Class diagram (Stage 3)", cd.W, cd.H)
    pkg_ids, pkg_origin = {}, {}
    for name, (x, y, w, h, fill, stroke, ink) in cd.PACKAGES.items():
        key = name.split()[0].split(".")[1]
        pkg_ids[key] = doc.vertex(
            escape(name), f"rounded=1;absoluteArcSize=1;arcSize=24;whiteSpace=wrap;html=1;fillColor={fill};"
                          f"strokeColor={stroke};fontColor={ink};fontStyle=1;fontSize=13;align=right;"
                          f"verticalAlign=bottom;spacingRight=12;spacingBottom=6;container=1;collapsible=0;"
                          f"recursiveResize=0;{SANS}", x, y, w, h)
        pkg_origin[key] = (x, y)

    class_ids = {}
    for cname, (x, y, pkg, stereo, attrs, ops) in cd.CLASSES.items():
        _, _, w, h = cd.GEOM[cname]
        color = cd.HEADER_FILL[pkg]
        head = 34 if stereo else 24
        italic = stereo in ("abstract", "interface")
        title = f"<i>{cname}</i>" if italic else cname
        label = (f'<span style="font-size:10px;font-weight:normal">«{stereo}»</span><br><b>{title}</b>'
                 if stereo else f"<b>{title}</b>")
        px, py = pkg_origin[pkg]
        attr_h = len(attrs) * cd.LINE + 8 if attrs else 6
        op_h = len(ops) * cd.LINE + 8 if ops else 6
        total = head + attr_h + 1 + op_h
        cid = doc.vertex(label, f"swimlane;html=1;fontStyle=0;childLayout=stackLayout;horizontal=1;startSize={head};"
                                f"horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;"
                                f"marginBottom=0;rounded=1;arcSize=6;fillColor={color};strokeColor={color};"
                                f"fontColor=#ffffff;swimlaneFillColor=#ffffff;fontSize=12.5;{SANS}",
                         x - px, y - py, w, total, parent=pkg_ids[pkg])
        class_ids[cname] = cid
        text = (f"text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=5;"
                f"spacingTop=-1;spacingRight=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];"
                f"portConstraint=eastwest;fontSize=11;{MONO}")
        doc.vertex("<br>".join(member_html(a) for a in attrs), text, 0, head, w, attr_h, parent=cid)
        doc.vertex("", f"line;strokeWidth=1;fillColor=none;strokeColor={color};opacity=50;align=left;"
                       f"verticalAlign=middle;spacingTop=-1;rotatable=0;labelPosition=right;points=[];"
                       f"portConstraint=eastwest;", 0, head + attr_h, w, 1, parent=cid)
        doc.vertex("<br>".join(member_html(o) for o in ops), text, 0, head + attr_h + 1, w, op_h, parent=cid)

    label_style = (f"edgeLabel;html=1;resizable=0;labelBackgroundColor=none;fontSize=10.5;fontStyle=1;"
                   f"fontColor=#111827;align=center;verticalAlign=middle;{SANS}")
    for e in cd.EDGES:
        src, dst, kind, fm, tm, label, (fs, ff), (ts, tf), _ = e
        pts = cd.route(e)
        ex, ey = SIDE[fs](ff)
        nx, ny = SIDE[ts](tf)
        stroke = "#6b7280" if kind == "depend" else "#4b5563"
        style = (f"{EDGE_STYLE[kind]}html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;strokeColor={stroke};"
                 f"strokeWidth=1.2;exitX={ex};exitY={ey};exitDx=0;exitDy=0;exitPerimeter=0;entryX={nx};entryY={ny};"
                 f"entryDx=0;entryDy=0;entryPerimeter=0;fontSize=10;fontStyle=2;fontColor=#4b5563;"
                 f"labelBackgroundColor=#ffffff;{SANS}")
        eid = doc.edge(label or "", style, class_ids[src], class_ids[dst], points=pts[1:-1])

        def offset(p, q):
            dx, dy = q[0] - p[0], q[1] - p[1]
            if abs(dx) > abs(dy):
                return (14 if dx > 0 else -14), -9
            return 12, (11 if dy > 0 else -11)

        if fm:
            doc.edge_label(eid, fm, -1, *offset(pts[0], pts[1]), label_style)
        if tm:
            ox, oy = offset(pts[-1], pts[-2])
            if len(tm) > 4:
                ox += (len(tm) * 2.6) if ox > 0 else 0
            doc.edge_label(eid, tm, 1, ox, oy, label_style)

    for x, y, w, title, lines, target, (side, frac) in cd.NOTES:
        h = 26 + len(lines) * 14
        nid = doc.vertex(f'<b style="color:#8a6a00">{escape(title)}</b><br>{escape(" ".join(lines))}',
                         f"shape=note;whiteSpace=wrap;html=1;size=10;fillColor=#fffbe6;strokeColor=#d9b74a;"
                         f"align=left;verticalAlign=top;spacingLeft=6;spacingRight=6;fontSize=10.5;"
                         f"fontColor=#4b4b3c;{SANS}", x, y, w, h)
        tx, ty = SIDE[side](frac)
        doc.edge("", f"endArrow=none;dashed=1;dashPattern=3 3;html=1;strokeColor=#d9b74a;entryX={tx};entryY={ty};"
                     f"entryDx=0;entryDy=0;entryPerimeter=0;", nid, class_ids[target])

    lx, ly = 1650, 1880
    doc.vertex("Visibility: + public &nbsp; - private &nbsp; # protected &nbsp; ~ package. "
               "Italic = abstract, underlined = static.",
               f"text;html=1;align=left;verticalAlign=middle;fontSize=10;fontColor=#6b7280;{SANS}",
               lx, ly - 30, 560, 16)
    legend = [("inherit", "generalisation"), ("realize", "realisation"), ("compose", "composition"),
              ("aggregate", "aggregation"), ("assoc", "association"), ("depend", "dependency")]
    for i, (kind, name) in enumerate(legend):
        x, y = lx + (i % 3) * 195, ly + (i // 3) * 22
        doc.edge("", f"{EDGE_STYLE[kind]}html=1;strokeColor=#4b5563;strokeWidth=1.2;", src_pt=(x, y),
                 dst_pt=(x + 52, y))
        doc.vertex(name, f"text;html=1;align=left;verticalAlign=middle;fontSize=10.5;fontColor=#374151;{SANS}",
                   x + 60, y - 8, 120, 16)
    doc.write(HERE / "class-diagram.drawio")


# ================================================================================== sequence diagrams

MSG = re.compile(r"^(\w+)\s*(-->>|->>)\s*([+-]?)(\w+)\s*:\s*(.*)$")
CHAR_W = 6.1      # average width of an 11px Helvetica character
ACT_W = 10


def tw(s):
    return len(s) * CHAR_W


def clean(s):
    return s.replace("#35;", "#").strip()


def guard(s):
    s = clean(s)
    return s if s.startswith("[") else f"[{s}]"


def parse(src):
    parts, events = [], []
    for raw in src.splitlines():
        line = raw.strip()
        if not line or line in ("sequenceDiagram", "autonumber"):
            continue
        m = re.match(r"^(actor|participant)\s+(\w+)\s+as\s+(.+)$", line)
        if m:
            parts.append((m.group(2), clean(m.group(3)), m.group(1) == "actor"))
            continue
        m = re.match(r"^Note over ([\w,\s]+):\s*(.*)$", line)
        if m:
            events.append(("note", [p.strip() for p in m.group(1).split(",")], clean(m.group(2))))
            continue
        m = re.match(r"^(loop|opt|alt)\s+(.*)$", line)
        if m:
            events.append(("open", m.group(1), guard(m.group(2))))
            continue
        m = re.match(r"^rect\s+rgb\((\d+),\s*(\d+),\s*(\d+)\)$", line)
        if m:
            events.append(("open", "rect", "#%02x%02x%02x" % tuple(int(v) for v in m.groups())))
            continue
        m = re.match(r"^else\s*(.*)$", line)
        if m:
            events.append(("else", guard(m.group(1))))
            continue
        if line == "end":
            events.append(("close",))
            continue
        m = MSG.match(line)
        if not m:
            raise SystemExit(f"cannot parse Mermaid line: {line}")
        a, arrow, act, b, text = m.groups()
        events.append(("msg", a, b, arrow == "-->>", act, clean(text)))
    return parts, events


def sequence_drawio(name, title):
    parts, events = parse((HERE / f"{name}.mmd").read_text())
    ids = [p[0] for p in parts]
    idx = {p: i for i, p in enumerate(ids)}
    box_w = [40 if actor else max(96, tw(label) + 22) for _, label, actor in parts]

    # horizontal layout: grow the gap between lifelines until every message label fits
    gaps = [max(110, (box_w[i] + box_w[i + 1]) / 2 + 24) for i in range(len(ids) - 1)]
    number = 0
    for ev in events:
        if ev[0] == "msg":
            number += 1
            i, j = idx[ev[1]], idx[ev[2]]
            need = tw(f"{number}  {ev[5]}") + 40
            if i == j:
                if i < len(gaps):
                    gaps[i] = max(gaps[i], need + 30)
                continue
            lo, hi = min(i, j), max(i, j)
            have = sum(gaps[lo:hi])
            if have < need:
                gaps[hi - 1] += need - have
        elif ev[0] == "note" and len(ev[1]) == 1:
            i = idx[ev[1][0]]
            half = min(tw(ev[2]) + 20, 230) / 2 + 12
            if i < len(gaps):
                gaps[i] = max(gaps[i], half + 60)
            if i > 0:
                gaps[i - 1] = max(gaps[i - 1], half + 60)
    cx = [60.0]
    for g in gaps:
        cx.append(cx[-1] + g)

    top, head = 20, 44
    y = top + head + 26
    active = {p: [] for p in ids}            # stack of activation start ys
    activations = []                         # (participant, depth, y0, y1)
    stack = []                               # open fragments: dict(kind, label, y0, elses, extent, depth)
    items = []                               # drawn after layout: (kind, data)
    number = 0

    def touch(x0, x1):
        for frag in stack:
            frag["x0"] = min(frag["x0"], x0)
            frag["x1"] = max(frag["x1"], x1)

    def act_edge(p, toward_right):
        depth = len(active[p])
        if depth == 0:
            return cx[idx[p]]
        off = ACT_W / 2 + (depth - 1) * 5
        return cx[idx[p]] + (off if toward_right else -off)

    for ev in events:
        kind = ev[0]
        if kind == "open":
            stack.append({"kind": ev[1], "label": ev[2], "y0": y, "elses": [], "x0": 1e9, "x1": -1e9})
            y += 12 if ev[1] == "rect" else 46
        elif kind == "else":
            stack[-1]["elses"].append((y + 2, ev[1]))
            y += 40
        elif kind == "close":
            frag = stack.pop()
            y += 8 if frag["kind"] == "rect" else 14
            frag["y1"] = y
            depth = len(stack)
            pad = 14 if frag["kind"] != "rect" else 22
            frag["x0"] -= pad
            frag["x1"] += pad
            if frag["kind"] != "rect":
                frag["x1"] = max(frag["x1"], frag["x0"] + max(170, tw(frag["label"]) + 70))
            items.append(("frag", frag, depth))
            touch(frag["x0"], frag["x1"])
            y += 12 if frag["kind"] == "rect" else 24
        elif kind == "note":
            ps = ev[1]
            if len(ps) == 1:
                w = min(max(tw(ev[2]) + 20, 90), 230)
                x0 = cx[idx[ps[0]]] - w / 2
            else:
                x0 = cx[idx[ps[0]]] - 40
                w = cx[idx[ps[-1]]] + 40 - x0
            lines = max(1, int(tw(ev[2]) / (w - 16)) + 1)
            h = 10 + lines * 14
            items.append(("note", (x0, y, w, h, ev[2])))
            touch(x0, x0 + w)
            y += h + 24
        else:
            _, a, b, reply, act, text = ev
            number += 1
            label = f"<b>{number}</b>&nbsp; {escape(text)}"
            ia, ib = idx[a], idx[b]
            if ia == ib:
                x = act_edge(a, True)
                items.append(("self", (x, y, label, reply)))
                touch(cx[ia] - 10, x + 50 + tw(text))
                y += 40
                continue
            right = ib > ia
            if act == "+":
                active[b].append(y)
            x1 = act_edge(a, right)
            x2 = act_edge(b, not right)
            items.append(("msg", (x1, x2, y, label, reply)))
            touch(min(x1, x2), max(x1, x2))
            if act == "-":
                start = active[a].pop()
                activations.append((a, len(active[a]), start, y))
            y += 30

    for p in ids:                       # close anything left open
        while active[p]:
            activations.append((p, len(active[p]) - 1, active[p].pop(), y))
    height = y + 30
    width = cx[-1] + max(box_w[-1] / 2, 140) + 40

    doc = Doc(title, int(width) + 40, int(height) + 40)
    # backgrounds first (rect blocks), so lifelines and messages sit on top
    for kind, data, *rest in items:
        if kind == "frag" and data["kind"] == "rect":
            doc.vertex("", f"rounded=0;whiteSpace=wrap;html=1;fillColor={data['label']};strokeColor=none;",
                       data["x0"], data["y0"], data["x1"] - data["x0"], data["y1"] - data["y0"])
    for i, (pid, label, actor) in enumerate(parts):
        if actor:
            style = ("shape=umlLifeline;participant=umlActor;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;"
                     "container=0;collapsible=0;recursiveResize=0;verticalAlign=top;spacingTop=36;outlineConnect=0;"
                     "labelBackgroundColor=#ffffff;size=44;fillColor=#c8603a;strokeColor=#a84e2d;fontSize=11;"
                     f"{SANS}")
        else:
            style = ("shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=0;"
                     "dropTarget=0;collapsible=0;recursiveResize=0;outlineConnect=0;portConstraint=eastwest;size=44;"
                     "fillColor=#c8603a;strokeColor=#a84e2d;fontColor=#ffffff;fontSize=11;fontStyle=1;"
                     f"{SANS}")
        doc.vertex(escape(label), style, cx[i] - box_w[i] / 2, top, box_w[i], height - top)
    for p, depth, y0, y1 in activations:
        x = cx[idx[p]] - ACT_W / 2 + depth * 5
        doc.vertex("", "html=1;points=[];perimeter=orthogonalPerimeter;outlineConnect=0;targetShapes=umlLifeline;"
                       "portConstraint=eastwest;fillColor=#ffe7d6;strokeColor=#c8603a;",
                   x, y0 - 4, ACT_W, max(y1 - y0 + 8, 14))
    for kind, data, *rest in items:
        if kind == "frag" and data["kind"] != "rect":
            fx, fy, fw, fh = data["x0"], data["y0"], data["x1"] - data["x0"], data["y1"] - data["y0"]
            doc.vertex(data["kind"], "shape=umlFrame;whiteSpace=wrap;html=1;pointerEvents=0;width=46;height=20;"
                                     "fillColor=#ffffff;strokeColor=#3a5ea8;fontColor=#3a5ea8;fontStyle=1;"
                                     f"fontSize=11;swimlaneFillColor=none;{SANS}", fx, fy, fw, fh)
            doc.vertex(escape(data["label"]), f"text;html=1;align=left;verticalAlign=middle;fontSize=11;"
                                              f"fontColor=#3a5ea8;fontStyle=2;{SANS}",
                       fx + 52, fy + 1, tw(data["label"]) + 20, 18)
            for ey, eg in data["elses"]:
                doc.edge("", "endArrow=none;dashed=1;html=1;strokeColor=#3a5ea8;", src_pt=(fx, ey),
                         dst_pt=(fx + fw, ey))
                doc.vertex(escape(eg), f"text;html=1;align=left;verticalAlign=middle;fontSize=11;"
                                       f"fontColor=#3a5ea8;fontStyle=2;{SANS}",
                           fx + 8, ey + 2, tw(eg) + 20, 18)
    msg_style = f"html=1;verticalAlign=bottom;fontSize=11;labelBackgroundColor=none;strokeColor=#374151;{SANS}"
    for kind, data, *rest in items:
        if kind == "msg":
            x1, x2, y, label, reply = data
            arrow = "dashed=1;endArrow=open;endSize=8;" if reply else "endArrow=block;endFill=1;endSize=7;"
            doc.edge(label, arrow + msg_style, src_pt=(x1, y), dst_pt=(x2, y))
        elif kind == "self":
            x, y, label, reply = data
            arrow = "dashed=1;endArrow=open;endSize=8;" if reply else "endArrow=block;endFill=1;endSize=7;"
            doc.edge(label, arrow + msg_style.replace("verticalAlign=bottom", "verticalAlign=middle") +
                     "align=left;spacingLeft=6;rounded=0;", src_pt=(x, y), dst_pt=(x, y + 16),
                     points=[(x + 34, y), (x + 34, y + 16)])
        elif kind == "note":
            x0, y0, w, h, text = data
            doc.vertex(escape(text), f"rounded=0;whiteSpace=wrap;html=1;fillColor=#fffbe6;strokeColor=#d9b74a;"
                                     f"fontSize=11;fontColor=#1f2937;{SANS}", x0, y0, w, h)
    doc.write(HERE / f"{name}.drawio")


if __name__ == "__main__":
    class_drawio()
    sequence_drawio("sequence-1-arrival-order", "Sequence diagram, part A (turn 7)")
    sequence_drawio("sequence-2-service-payment", "Sequence diagram, part B (turns 8-11 and result)")
