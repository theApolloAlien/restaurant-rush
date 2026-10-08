"""
Generates docs/diagrams/class-diagram.svg, the final (Stage 3) UML class diagram.

The layout is placed by hand so the diagram stays readable when printed; every
class, member and relationship below mirrors the Java code in src/rush.
Run:  python3 docs/diagrams/class_diagram.py
"""
from html import escape
from pathlib import Path

W, H = 2280, 1960
LINE = 15          # member line height
CHAR = 6.65        # width of one 11px monospace character
PAD = 9

PACKAGES = {
    # name: (x, y, w, h, fill, stroke, label colour)
    "rush.ui  «boundary»": (20, 20, 420, 1045, "#eef4ff", "#9db7e8", "#3a5ea8"),
    "rush.control  «control»": (470, 20, 1160, 520, "#fff4ec", "#f0b48f", "#b6522b"),
    "rush.policy  «control»": (1650, 20, 610, 520, "#fff9e6", "#e8cf7d", "#9a7414"),
    "rush.staff  «control»": (470, 560, 1790, 505, "#effaf1", "#9fd3a9", "#2f7d43"),
    "rush.model  «entity»": (20, 1085, 2240, 855, "#f7f0fb", "#c9a9df", "#7240a0"),
}

# name: (x, y, package, stereotype, attributes, operations)
# Member markup: leading '*' = abstract (italic), leading '_' = static (underlined).
CLASSES = {
    # ---------------------------------------------------------------- boundary
    "ConsoleUI": (40, 60, "ui", None,
                  ["-term : Terminal", "-dashboard : Dashboard", "-parser : CommandParser", "-edition : String"],
                  ["+run() : void", "+play() : void", "-offerHappyHour(Game) : void",
                   "-instruct(Game, DirectedStaff) : void"]),
    "Dashboard": (40, 285, "ui", None,
                  ["-term : Terminal", "-t : Theme"],
                  ["+turnHeader(Game, String) : void", "+board(Game) : void",
                   "+staffPrompt(Game, DirectedStaff) : void", "+actionResult(ActionResult) : void",
                   "+events(List<GameEvent>) : void", "+endOfTurn(Game, List<GameEvent>) : void",
                   "+finalResult(Game, GameResult) : void"]),
    "CommandParser": (40, 545, "ui", None,
                      [],
                      ["+parse(line : String) : Input", "_+usage(Verb) : String"]),
    "Terminal": (40, 640, "ui", None,
                 ["-in : BufferedReader", "-out : PrintStream", "-echoInput : boolean"],
                 ["+prompt(text : String) : Optional<String>", "+println(line : String) : void"]),

    # ----------------------------------------------------------------- control
    "Game": (485, 60, "control", None,
             ["-turn : int", "-turnInProgress : boolean", "-staffHaveActed : boolean"],
             ["+beginTurn() : List<GameEvent>", "+startHappyHour() : ActionResult",
              "+canStartHappyHour() : boolean", "+direct(DirectedStaff, Command) : ActionResult",
              "+options(DirectedStaff) : List<Command>", "+endTurn() : List<GameEvent>",
              "+isOver() : boolean", "+result() : GameResult", "+scoreboard() : Scoreboard",
              "-context() : TurnContext"]),
    "GameConfig": (915, 60, "control", None,
                   ["-maxTurns : int", "-tableCount : int"],
                   ["_+base() : GameConfig", "+hireDirectedStaff() : List<DirectedStaff>",
                    "+hireCashier() : Cashier"]),
    "ArrivalSchedule": (915, 255, "control", None,
                        ["-arrivals : List<Arrival>"],
                        ["+at(int, String, CustomerFactory) : ArrivalSchedule",
                         "+arrivingOn(turn : int) : Optional<Customer>"]),
    "GameEvent": (915, 400, "control", "record",
                  ["+kind : Kind", "+message : String"], []),
    "Objective": (1320, 60, "control", "record",
                  ["+minPaidCustomers : int", "+minRevenue : Money", "+minAverageSatisfaction : int"],
                  ["_+base() : Objective", "+evaluate(Scoreboard) : GameResult"]),
    "GameResult": (1320, 235, "control", "record",
                   ["+targets : List<Target>", "+score : Scoreboard"],
                   ["+isVictory() : boolean"]),
    "Scoreboard": (1320, 360, "control", "record",
                   ["+servedCount : int", "+paidCount : int", "+departures : int", "+revenue : Money",
                    "+averageSatisfaction : OptionalDouble"],
                   ["+averageText() : String"]),

    # ------------------------------------------------------------------ policy
    "PricingPolicy": (1675, 60, "policy", None,
                      ["-modifiers : List<PriceModifier>"],
                      ["+quote(Customer, MenuItem, turn : int) : Money"]),
    "PriceModifier": (1675, 190, "policy", "interface",
                      [],
                      ["*+adjust(price : Money, turn : int) : Money"]),
    "HappyHour": (1675, 300, "policy", None,
                  ["_+DURATION_TURNS : int = 2", "_+DISCOUNT_PERCENT : int = 20", "_+RECOVERY : int = 15",
                   "-startTurn : int"],
                  ["+isAvailable() : boolean", "+activate(turn : int) : void",
                   "+turnsRemaining(turn : int) : int", "+adjust(Money, int) : Money",
                   "+refresh(Restaurant, int) : List<Customer>"]),

    # ------------------------------------------------------------------- staff
    "StaffMember": (485, 600, "staff", "abstract",
                    ["-name : String", "-tasksPerTurn : int", "-tasksUsed : int"],
                    ["*+role() : String", "+title() : String", "+startTurn() : void",
                     "+hasTaskLeft() : boolean", "#useTask() : void"]),
    "DirectedStaff": (485, 820, "staff", "abstract",
                      [],
                      ["+perform(Command, TurnContext) : ActionResult", "+options(TurnContext) : List<Command>",
                       "*+duties() : Set<Verb>", "*#carryOut(Command, TurnContext) : String",
                       "*#usefulCommands(TurnContext) : List<Command>",
                       "_#seatCustomer(String, Command, TurnContext) : String"]),
    "Cashier": (700, 600, "staff", None,
                ["_-OLDEST_FIRST : Comparator<Customer>"],
                ["+nextToPay(TurnContext) : Optional<Customer>",
                 "+collectPayment(TurnContext) : Optional<Payment>"]),
    "Waiter": (900, 720, "staff", None,
               [],
               ["+duties() : Set<Verb>", "#carryOut(Command, TurnContext) : String",
                "-takeOrder(Customer, TurnContext) : String", "-serve(Customer, TurnContext) : String"]),
    "Chef": (900, 845, "staff", None, [], ["+duties() : Set<Verb>", "#carryOut(Command, TurnContext) : String"]),
    "Host": (900, 940, "staff", None, [], ["+duties() : Set<Verb>", "#carryOut(Command, TurnContext) : String"]),
    "Command": (1250, 600, "staff", "record",
                ["+verb : Verb", "+customerId : Integer", "+tableNo : Integer"],
                ["_+seat(int, int) : Command", "_+waitTurn() : Command"]),
    "Verb": (1500, 600, "staff", "enumeration",
             ["SEAT", "ORDER", "SERVE", "COOK", "WAIT"], []),
    "ActionResult": (1250, 770, "staff", "record",
                     ["+succeeded : boolean", "+message : String"], []),
    "TurnContext": (1250, 900, "staff", "record",
                    ["+turn : int", "+restaurant : Restaurant", "+pricing : PricingPolicy"], []),

    # ------------------------------------------------------------------- model
    "Restaurant": (40, 1130, "model", None,
                   ["-tables : List<Table>", "-queue : List<Customer>", "-guests : List<Customer>",
                    "-orders : List<Order>", "-revenue : Money"],
                   ["+admit(Customer) : void", "+seat(Customer, Table) : void",
                    "+placeOrder(Customer, MenuItem, Money, int) : Order", "+collectPayment(Customer) : Money",
                    "+depart(Customer) : void", "+awaitingService() : List<Customer>",
                    "+averagePaidSatisfaction() : OptionalDouble"]),
    "Table": (40, 1470, "model", None,
              ["-number : int", "-occupant : Customer"],
              ["+isFree() : boolean", "~assign(Customer) : void", "~release() : void"]),
    "Customer": (560, 1430, "model", "abstract",
                 ["-id : int", "-name : String", "-satisfaction : int", "-status : CustomerStatus",
                  "-servedTurn : int", "-readyToPayTurn : int"],
                 ["*+type() : String", "*+satisfactionLossPerTurn() : int", "+chooseFrom(Menu) : MenuItem",
                  "+billFor(menuPrice : Money) : Money", "+remarkOnPaying() : String",
                  "+loseSatisfaction() : int", "+recover(points : int) : int", "+finishEating(turn : int) : boolean",
                  "+canPayOn(turn : int) : boolean", "~seatAt(Table) : void", "~placeOrder(Order) : void",
                  "~startEating(turn : int) : void", "~pay() : void", "~leave() : void"]),
    "CustomerStatus": (40, 1660, "model", "enumeration",
                       ["WAITING", "SEATED", "ORDERED", "EATING", "READY_TO_PAY", "PAID", "LEFT"], []),
    "RegularCustomer": (900, 1470, "model", None,
                        ["_+SATISFACTION_LOSS : int = 8"],
                        ["+type() : String", "+satisfactionLossPerTurn() : int"]),
    "VIPCustomer": (900, 1585, "model", None,
                    ["_+SATISFACTION_LOSS : int = 5", "_+DISCOUNT_PERCENT : int = 10"],
                    ["+satisfactionLossPerTurn() : int", "+billFor(menuPrice : Money) : Money"]),
    "CriticCustomer": (900, 1715, "model", None,
                       ["_+SATISFACTION_LOSS : int = 12"],
                       ["+satisfactionLossPerTurn() : int", "+chooseFrom(Menu) : MenuItem",
                        "+remarkOnPaying() : String"]),
    "Order": (1290, 1430, "model", None,
              ["-price : Money", "-placedTurn : int", "-unitsDone : int", "-status : OrderStatus",
               "-readyTurn : int"],
              ["+prepareUnit(turn : int) : void", "+serve(turn : int) : void", "+describe() : String",
               "~markPaid() : void", "~cancel() : void"]),
    "OrderStatus": (1290, 1670, "model", "enumeration",
                    ["IN_PREPARATION", "READY", "SERVED", "PAID", "CANCELLED"], []),
    "Menu": (1650, 1130, "model", None,
             ["-items : List<MenuItem>"],
             ["+atPosition(position : int) : MenuItem"]),
    "MenuItem": (1650, 1270, "model", "abstract",
                 ["-name : String"],
                 ["*+price() : Money", "*+prepUnits() : int"]),
    "Dish": (1650, 1440, "model", None,
             ["-price : Money", "-prepUnits : int"],
             ["+price() : Money", "+prepUnits() : int"]),
    "ComboMeal": (1920, 1440, "model", None,
                  ["_+PRICE_PERCENT : int = 90"],
                  ["+price() : Money", "+prepUnits() : int", "+components() : List<Dish>"]),
    "Money": (1650, 1650, "model", None,
              ["-cents : long"],
              ["_+ofCents(long) : Money", "+plus(Money) : Money", "+percent(int) : Money",
               "+isAtLeast(Money) : boolean"]),
    "RuleViolation": (2010, 1130, "model", "exception",
                      [], ["+RuleViolation(message : String)"]),
}

HEADER_FILL = {"ui": "#3a5ea8", "control": "#c8603a", "policy": "#b68a1c", "staff": "#2f7d43", "model": "#7240a0"}

# (from, to, kind, from_mult, to_mult, label, from_side, to_side, via)
# kinds: inherit, realize, compose, aggregate, assoc, depend.  The diamond / triangle sits at 'to' for
# inherit/realize and at 'from' for compose/aggregate.
EDGES = [
    ("ConsoleUI", "Dashboard", "compose", "1", "1", None, ("bottom", .3), ("top", .3), None),
    ("ConsoleUI", "Game", "depend", None, None, "drives", ("right", .35), ("left", .12), None),
    ("Dashboard", "Game", "depend", None, None, "reads", ("right", .1), ("left", .55), None),
    ("ConsoleUI", "CommandParser", "compose", "1", "1", None, ("left", .9), ("left", .4), [(30, None), (30, None)]),
    ("ConsoleUI", "Terminal", "assoc", None, "1", None, ("left", .6), ("left", .3), [(25, None), (25, None)]),

    ("Game", "GameConfig", "assoc", None, "1", "config", ("right", .1), ("left", .2), None),
    ("GameConfig", "ArrivalSchedule", "compose", "1", "1", None, ("bottom", .3), ("top", .3), None),
    ("GameConfig", "Objective", "compose", "1", "1", None, ("right", .3), ("left", .3), None),
    ("Game", "GameEvent", "depend", None, None, "creates", ("right", .9), ("left", .5), None),
    ("Objective", "GameResult", "depend", None, None, "creates", ("bottom", .5), ("top", .5), None),
    ("GameResult", "Scoreboard", "compose", "1", "1", None, ("bottom", .5), ("top", .5), None),
    ("Game", "PricingPolicy", "compose", "1", "1", "pricing", ("top", .8), ("top", .5),
     [(None, 42), (None, 42)]),
    ("Game", "HappyHour", "compose", "1", "1", "happyHour", ("right", .55), ("left", .55),
     [(1645, None), (1645, None)]),
    ("Game", "DirectedStaff", "compose", "1", "3 (Host, Waiter, Chef)", None, ("bottom", .602), ("top", .536), None),
    ("Game", "Cashier", "compose", "1", "1", None, ("bottom", .849), ("top", .178), None),
    ("Game", "Restaurant", "compose", "1", "1", None, ("left", .92), ("top", .55),
     [(455, None), (455, 1105), (None, 1105)]),
    ("GameConfig", "Menu", "assoc", None, "1", "menu", ("bottom", .9), ("top", .7),
     [(None, 218), (1637, None), (1637, 1105), (None, 1105)]),

    ("DirectedStaff", "StaffMember", "inherit", None, None, None, ("top", .245), ("bottom", .5), None),
    ("Cashier", "StaffMember", "inherit", None, None, None, ("left", .5), ("right", .25), None),
    ("Waiter", "DirectedStaff", "inherit", None, None, None, ("left", .5), ("right", .1), None),
    ("Chef", "DirectedStaff", "inherit", None, None, None, ("left", .5), ("right", .45), None),
    ("Host", "DirectedStaff", "inherit", None, None, None, ("left", .5), ("right", .9), None),
    ("Waiter", "Command", "depend", None, None, "performs", ("right", .3), ("left", .9), None),
    ("Waiter", "ActionResult", "depend", None, None, "returns", ("right", .7), ("left", .5), None),
    ("Command", "Verb", "assoc", None, "1", None, ("right", .3), ("left", .3), None),
    ("Host", "TurnContext", "depend", None, None, "uses", ("right", .4), ("left", .6), None),
    ("TurnContext", "PricingPolicy", "assoc", None, "1", "pricing", ("right", .7), ("right", .5),
     [(2030, None), (2030, None)]),

    ("PricingPolicy", "PriceModifier", "aggregate", "1", "0..*", "modifiers", ("bottom", .2), ("top", .2), None),
    ("HappyHour", "PriceModifier", "realize", None, None, None, ("top", .2), ("bottom", .2), None),

    ("Restaurant", "Table", "compose", "1", "2", "tables", ("bottom", .2), ("top", .2), None),
    ("Restaurant", "Customer", "aggregate", "1", "0..*", "queue, guests", ("right", .55), ("top", .3), None),
    ("Restaurant", "Order", "compose", "1", "0..*", "orders", ("right", .3), ("top", .5), None),
    ("Restaurant", "Menu", "assoc", None, "1", "menu", ("right", .04), ("left", .126), None),
    ("Table", "Customer", "assoc", None, "0..1", "occupant", ("right", .2), ("left", .2), None),
    ("Customer", "CustomerStatus", "assoc", None, "1", None, ("left", .85), ("right", .5), None),
    ("RegularCustomer", "Customer", "inherit", None, None, None, ("left", .3), ("right", .2), None),
    ("VIPCustomer", "Customer", "inherit", None, None, None, ("left", .5), ("right", .5), None),
    ("CriticCustomer", "Customer", "inherit", None, None, None, ("left", .5), ("right", .85), None),
    ("Customer", "Order", "assoc", "1", "0..1", "order", ("right", .02), ("left", .03), None),
    ("Order", "MenuItem", "assoc", None, "1", "item", ("right", .15), ("left", .7), None),
    ("Order", "OrderStatus", "assoc", None, "1", None, ("bottom", .5), ("top", .5), None),
    ("Menu", "MenuItem", "aggregate", "1", "1..*", "items", ("bottom", .5), ("top", .5), None),
    ("Dish", "MenuItem", "inherit", None, None, None, ("top", .5), ("bottom", .5), None),
    ("ComboMeal", "MenuItem", "inherit", None, None, None, ("top", .3), ("right", .5), None),
    ("ComboMeal", "Dish", "aggregate", "1", "2", "components", ("left", .7), ("right", .7), None),
]

NOTES = [
    # (x, y, w, title, lines, target, (side, frac))
    (290, 1545, 222, "POLYMORPHISM",
     ["Game never asks a customer's", "type. Loss rate, food choice and", "bill are dispatched to the",
      "subclass (Stage 2)."], "Customer", ("left", .4)),
    (690, 1272, 310, "ENCAPSULATION + COMPOSITION",
     ["Restaurant owns its tables and queue and is the", "only object that moves a customer between",
      "queue, table and door. Lists are read-only."], "Restaurant", ("right", .85)),
    (485, 980, 330, "TEMPLATE METHOD",
     ["perform() checks the task budget and duty, then", "calls carryOut(). A broken rule throws",
      "RuleViolation, so the task is not used up."], "DirectedStaff", ("bottom", .4)),
    (1930, 1575, 310, "COMPOSITE",
     ["A ComboMeal is a MenuItem made of two", "Dishes: price = 90% of their sum,",
      "units = sum. Callers never tell them apart."], "ComboMeal", ("bottom", .5)),
    (1990, 300, 250, "STRATEGY (Stage 3)",
     ["New pricing rules plug in as", "PriceModifiers. VIP discount", "first, then Happy Hour."],
     "HappyHour", ("right", .3)),
]


def box_size(name, stereo, attrs, ops):
    head = 34 if stereo else 24
    lines = [name] + attrs + ops
    width = max(len(x.lstrip("*_")) for x in lines) * CHAR + 2 * PAD
    width = max(width, len(name) * 8 + 2 * PAD, 120)
    height = head + (len(attrs) * LINE + 8 if attrs else 6) + (len(ops) * LINE + 8 if ops else 6)
    return round(width), height


GEOM = {}
for cname, (x, y, pkg, stereo, attrs, ops) in CLASSES.items():
    w, h = box_size(cname, stereo, attrs, ops)
    GEOM[cname] = (x, y, w, h)


def anchor(cname, side, frac):
    x, y, w, h = GEOM[cname]
    return {"top": (x + w * frac, y), "bottom": (x + w * frac, y + h),
            "left": (x, y + h * frac), "right": (x + w, y + h * frac)}[side]


def route(edge):
    src, dst, kind, fm, tm, label, (fs, ff), (ts, tf), via = edge
    a = anchor(src, fs, ff)
    b = anchor(dst, ts, tf)
    pts = [a]
    if via:
        cur = a
        for vx, vy in via:
            nxt = (vx if vx is not None else cur[0], vy if vy is not None else cur[1])
            pts.append(nxt)
            cur = nxt
        last = pts[-1]
        # close with one orthogonal elbow towards b
        if ts in ("top", "bottom"):
            pts.append((b[0], last[1]))
        else:
            pts.append((last[0], b[1]))
    else:
        vertical_a = fs in ("top", "bottom")
        vertical_b = ts in ("top", "bottom")
        if vertical_a and vertical_b:
            my = (a[1] + b[1]) / 2
            pts += [(a[0], my), (b[0], my)]
        elif not vertical_a and not vertical_b:
            mx = (a[0] + b[0]) / 2
            pts += [(mx, a[1]), (mx, b[1])]
        elif vertical_a:
            pts.append((a[0], b[1]))
        else:
            pts.append((b[0], a[1]))
    pts.append(b)
    # drop duplicate consecutive points
    clean = [pts[0]]
    for p in pts[1:]:
        if abs(p[0] - clean[-1][0]) > .5 or abs(p[1] - clean[-1][1]) > .5:
            clean.append(p)
    return clean


def text(x, y, s, size=11, weight="normal", fill="#1f2937", anchor_="start", style="", family="mono", deco=""):
    fam = "'SF Mono','JetBrains Mono',Menlo,Consolas,monospace" if family == "mono" else \
        "'Inter','Helvetica Neue',Helvetica,Arial,sans-serif"
    extra = f' font-style="{style}"' if style else ""
    extra += f' text-decoration="{deco}"' if deco else ""
    return (f'<text x="{x:.1f}" y="{y:.1f}" font-family="{fam}" font-size="{size}" font-weight="{weight}" '
            f'fill="{fill}" text-anchor="{anchor_}"{extra}>{escape(s)}</text>')


def member(x, y, raw):
    italic = raw.startswith("*")
    static = raw.startswith("_")
    s = raw.lstrip("*_")
    return text(x, y, s, style="italic" if italic else "", deco="underline" if static else "")


def draw_class(cname):
    x, y, w, h = GEOM[cname]
    _, _, pkg, stereo, attrs, ops = CLASSES[cname]
    color = HEADER_FILL[pkg]
    head = 34 if stereo else 24
    out = [f'<g class="cls"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="5" fill="#ffffff" '
           f'stroke="{color}" stroke-width="1.3" filter="url(#shadow)"/>',
           f'<path d="M{x},{y + 5} a5,5 0 0 1 5,-5 h{w - 10} a5,5 0 0 1 5,5 v{head - 5} h{-w} z" fill="{color}"/>']
    ty = y + 16
    if stereo:
        out.append(text(x + w / 2, y + 13, f"«{stereo}»", size=10, fill="#ffffffcc", anchor_="middle",
                        family="sans"))
        ty = y + 27
    out.append(text(x + w / 2, ty, cname, size=12.5, weight="700", fill="#ffffff", anchor_="middle",
                    family="sans", style="italic" if stereo in ("abstract", "interface") else ""))
    cy = y + head
    if attrs:
        for i, a in enumerate(attrs):
            out.append(member(x + PAD, cy + 15 + i * LINE, a))
        cy += len(attrs) * LINE + 8
    else:
        cy += 6
    out.append(f'<line x1="{x}" y1="{cy}" x2="{x + w}" y2="{cy}" stroke="{color}" stroke-opacity=".45"/>')
    for i, o in enumerate(ops):
        out.append(member(x + PAD, cy + 15 + i * LINE, o))
    out.append("</g>")
    return "\n".join(out)


def draw_edge(edge):
    src, dst, kind, fm, tm, label, *_ = edge
    pts = route(edge)
    dashed = kind in ("realize", "depend")
    d = "M" + " L".join(f"{px:.1f},{py:.1f}" for px, py in pts)
    marker_end = {"inherit": "url(#tri)", "realize": "url(#tri)", "assoc": "url(#arrow)",
                  "depend": "url(#arrow)"}.get(kind, "")
    marker_start = {"compose": "url(#diamondFilled)", "aggregate": "url(#diamond)"}.get(kind, "")
    stroke = "#4b5563" if kind not in ("depend",) else "#6b7280"
    attrs = f'stroke="{stroke}" stroke-width="1.15" fill="none"'
    if dashed:
        attrs += ' stroke-dasharray="5 4"'
    if marker_end:
        attrs += f' marker-end="{marker_end}"'
    if marker_start:
        attrs += f' marker-start="{marker_start}"'
    out = [f'<path d="{d}" {attrs}/>']

    def mult(p, q, s):
        # place a multiplicity label just beside the end point p, on the segment towards q
        dx, dy = q[0] - p[0], q[1] - p[1]
        if abs(dx) > abs(dy):
            ox = 10 if dx > 0 else -10
            return text(p[0] + ox, p[1] - 5, s, size=10.5, fill="#111827", weight="600",
                        anchor_="start" if dx > 0 else "end", family="sans")
        oy = 14 if dy > 0 else -6
        return text(p[0] + 6, p[1] + oy, s, size=10.5, fill="#111827", weight="600", family="sans")

    if fm:
        out.append(mult(pts[0], pts[1], fm))
    if tm:
        out.append(mult(pts[-1], pts[-2], tm))
    if label:
        # label on the longest segment
        best = max(range(len(pts) - 1), key=lambda i: abs(pts[i + 1][0] - pts[i][0]) + abs(pts[i + 1][1] - pts[i][1]))
        (x1, y1), (x2, y2) = pts[best], pts[best + 1]
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        lw = len(label) * 5.6 + 8
        if abs(x2 - x1) >= abs(y2 - y1):
            out.append(f'<rect x="{mx - lw / 2:.1f}" y="{my - 15:.1f}" width="{lw:.1f}" height="13" rx="3" '
                       f'fill="#ffffff" fill-opacity=".85"/>')
            out.append(text(mx, my - 5, label, size=10, fill="#4b5563", anchor_="middle", family="sans",
                            style="italic"))
        else:
            out.append(f'<rect x="{mx + 4:.1f}" y="{my - 9:.1f}" width="{lw:.1f}" height="13" rx="3" '
                       f'fill="#ffffff" fill-opacity=".85"/>')
            out.append(text(mx + 8, my + 1, label, size=10, fill="#4b5563", family="sans", style="italic"))
    return "\n".join(out)


def draw_note(note):
    x, y, w, title, lines, target, (side, frac) = note
    h = 26 + len(lines) * 14
    fold = 10
    out = [f'<path d="M{x},{y} h{w - fold} l{fold},{fold} v{h - fold} h{-w} z" fill="#fffbe6" '
           f'stroke="#d9b74a" stroke-width="1"/>',
           f'<path d="M{x + w - fold},{y} v{fold} h{fold}" fill="none" stroke="#d9b74a"/>',
           text(x + 9, y + 16, title, size=10.5, weight="700", fill="#8a6a00", family="sans")]
    for i, line in enumerate(lines):
        out.append(text(x + 9, y + 31 + i * 14, line, size=10.5, fill="#4b4b3c", family="sans"))
    tx, ty = anchor(target, side, frac)
    cx = min(max(tx, x), x + w)
    cy = min(max(ty, y), y + h)
    out.append(f'<line x1="{cx:.1f}" y1="{cy:.1f}" x2="{tx:.1f}" y2="{ty:.1f}" stroke="#d9b74a" '
               f'stroke-dasharray="3 3"/>')
    return "\n".join(out)


def legend():
    x, y = 1650, 1880
    items = [("inherit", "generalisation"), ("realize", "realisation"), ("compose", "composition"),
             ("aggregate", "aggregation"), ("assoc", "association"), ("depend", "dependency")]
    out = []
    for i, (kind, name) in enumerate(items):
        col, row = i % 3, i // 3
        lx, ly = x + col * 195, y + row * 22
        dashed = ' stroke-dasharray="5 4"' if kind in ("realize", "depend") else ""
        m_end = {"inherit": "url(#tri)", "realize": "url(#tri)", "assoc": "url(#arrow)",
                 "depend": "url(#arrow)"}.get(kind)
        m_start = {"compose": "url(#diamondFilled)", "aggregate": "url(#diamond)"}.get(kind)
        mk = (f' marker-end="{m_end}"' if m_end else "") + (f' marker-start="{m_start}"' if m_start else "")
        out.append(f'<path d="M{lx},{ly} h52" stroke="#4b5563" stroke-width="1.15" fill="none"{dashed}{mk}/>')
        out.append(text(lx + 62, ly + 4, name, size=10.5, fill="#374151", family="sans"))
    out.append(text(x, y - 18, "Visibility: + public   - private   # protected   ~ package.   "
                                "Italic = abstract, underlined = static.", size=10, fill="#6b7280", family="sans"))
    return "\n".join(out)


def build():
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">',
             """<defs>
  <filter id="shadow" x="-5%" y="-5%" width="110%" height="115%">
    <feDropShadow dx="0" dy="1.5" stdDeviation="1.6" flood-color="#000" flood-opacity=".12"/>
  </filter>
  <marker id="tri" viewBox="0 0 14 14" refX="13" refY="7" markerWidth="14" markerHeight="14" orient="auto-start-reverse" markerUnits="userSpaceOnUse">
    <path d="M1,1 L13,7 L1,13 z" fill="#ffffff" stroke="#4b5563" stroke-width="1.2"/></marker>
  <marker id="arrow" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="11" markerHeight="11" orient="auto-start-reverse" markerUnits="userSpaceOnUse">
    <path d="M1,1 L11,6 L1,11" fill="none" stroke="#4b5563" stroke-width="1.3"/></marker>
  <marker id="diamond" viewBox="0 0 20 12" refX="1" refY="6" markerWidth="20" markerHeight="12" orient="auto-start-reverse" markerUnits="userSpaceOnUse">
    <path d="M1,6 L10,1 L19,6 L10,11 z" fill="#ffffff" stroke="#4b5563" stroke-width="1.2"/></marker>
  <marker id="diamondFilled" viewBox="0 0 20 12" refX="1" refY="6" markerWidth="20" markerHeight="12" orient="auto-start-reverse" markerUnits="userSpaceOnUse">
    <path d="M1,6 L10,1 L19,6 L10,11 z" fill="#4b5563" stroke="#4b5563" stroke-width="1.2"/></marker>
</defs>""",
             f'<rect width="{W}" height="{H}" fill="#ffffff"/>']
    for name, (x, y, w, h, fill, stroke, ink) in PACKAGES.items():
        parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="14" fill="{fill}" stroke="{stroke}" '
                     f'stroke-width="1.2"/>')
        parts.append(text(x + w - 14, y + h - 12, name, size=13, weight="700", fill=ink, family="sans",
                          anchor_="end"))
    for e in EDGES:
        parts.append(draw_edge(e))
    for n in NOTES:
        parts.append(draw_note(n))
    for c in CLASSES:
        parts.append(draw_class(c))
    parts.append(legend())
    parts.append("</svg>")
    out = Path(__file__).with_name("class-diagram.svg")
    out.write_text("\n".join(parts), encoding="utf-8")
    print("wrote", out)
    for c, (x, y, w, h) in GEOM.items():
        print(f"  {c:16s} x={x:5} y={y:5} w={w:4} h={h:4}  right={x + w:5} bottom={y + h:5}")


if __name__ == "__main__":
    build()
