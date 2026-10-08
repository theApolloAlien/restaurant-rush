"""
Builds the PDF report: docs/report/Restaurant-Rush-Report.pdf

  python3 docs/report/build.py

Steps
  1. regenerates the class diagram SVG (docs/diagrams/class_diagram.py)
  2. renders the Mermaid sequence diagrams to SVG (needs network for the Mermaid script;
     the committed SVGs are reused when offline)
  3. checks out each stage tag into a temporary folder, builds it and replays its scripts
     to capture real, coloured terminal output for the functional evidence
  4. fills report.html and prints it to PDF with headless Chrome / Chromium

Set CHROME=/path/to/chrome if Chrome is not in the default macOS location.
"""
import html
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
DIAGRAMS = ROOT / "docs" / "diagrams"
BUILD = HERE / "build"
CHROME = os.environ.get("CHROME", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")

sys.path.insert(0, str(HERE))
from ansi2html import convert  # noqa: E402

ANSI = re.compile(r"\x1b\[[0-9;]*m")
SEQUENCES = ["sequence-1-arrival-order", "sequence-2-service-payment"]

MERMAID_PAGE = """<!doctype html><meta charset=utf-8><body style="margin:0;background:#fff">
<pre id=src style=display:none>{src}</pre><div id=out></div>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>
<script>
mermaid.initialize({{startOnLoad:false, theme:'base', sequence:{{useMaxWidth:false, mirrorActors:true,
 messageFontSize:13, noteFontSize:12.5, actorFontSize:14, boxMargin:8}},
 themeVariables:{{fontFamily:'Helvetica Neue, Helvetica, Arial, sans-serif', primaryColor:'#fff4ec',
 primaryBorderColor:'#c8603a', primaryTextColor:'#1f2937', lineColor:'#4b5563', noteBkgColor:'#fffbe6',
 noteBorderColor:'#d9b74a', actorBkg:'#c8603a', actorTextColor:'#ffffff', actorBorder:'#a84e2d',
 signalColor:'#374151', signalTextColor:'#111827', labelBoxBkgColor:'#eef4ff', labelBoxBorderColor:'#3a5ea8',
 loopTextColor:'#3a5ea8', activationBkgColor:'#ffe7d6', activationBorderColor:'#c8603a',
 sequenceNumberColor:'#ffffff'}}}});
mermaid.render('{diagram_id}', document.getElementById('src').textContent)
  .then(r => {{ document.getElementById('out').innerHTML = r.svg }})
  .catch(e => {{ document.getElementById('out').textContent = 'ERR ' + e.message }});
</script>"""


def run(cmd, cwd=None, stdin=None):
    return subprocess.run(cmd, cwd=cwd, input=stdin, capture_output=True, text=True, check=True).stdout


def render_sequences():
    for name in SEQUENCES:
        page = BUILD / f"{name}.html"
        diagram_id = "seq" + name.split("-")[1]
        page.write_text(MERMAID_PAGE.format(src=html.escape((DIAGRAMS / f"{name}.mmd").read_text()),
                                            diagram_id=diagram_id))
        try:
            dom = run([CHROME, "--headless=new", "--disable-gpu", "--virtual-time-budget=20000",
                       "--dump-dom", page.as_uri()])
        except (OSError, subprocess.CalledProcessError) as e:
            print(f"  ! could not run Chrome for {name}: {e}; keeping existing SVG")
            continue
        m = re.search(r"<svg.*?</svg>", dom, re.S)
        text = re.sub(r"<[^>]+>", " ", re.sub(r"<style.*?</style>", "", m.group(0), flags=re.S)) if m else ""
        if not m or "Syntax error" in text:
            print(f"  ! Mermaid could not render {name} (offline?); keeping existing SVG")
            continue
        svg = m.group(0)
        if "xmlns=" not in svg[:300]:
            svg = svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" ', 1)
        (DIAGRAMS / f"{name}.svg").write_text(svg)
        print(f"  rendered {name}.svg")


def capture_runs():
    """Replays every stage's scripts at its own git tag, in colour."""
    runs = {}
    with tempfile.TemporaryDirectory() as tmp:
        for stage in (1, 2, 3):
            folder = Path(tmp) / f"stage-{stage}"
            folder.mkdir()
            archive = subprocess.run(["git", "archive", f"stage-{stage}"], cwd=ROOT, capture_output=True,
                                     check=True).stdout
            subprocess.run(["tar", "-x"], cwd=folder, input=archive, check=True)
            sources = [str(p) for p in folder.glob("src/**/*.java")]
            run(["javac", "--release", "17", "-encoding", "UTF-8", "-d", "out", *sources], cwd=folder)
            for kind in ("victory", "defeat"):
                runs[f"s{stage}{kind[0]}"] = run(["java", "-cp", "out", "rush.Main", "--script",
                                                   f"scripts/stage{stage}-{kind}.txt"], cwd=folder)
            tests = run(["sh", "test.sh"], cwd=folder)
            runs[f"tests{stage}"] = tests
            print(f"  stage-{stage}: replayed scripts, {ANSI.sub('', tests).strip().splitlines()[-1].strip()}")
    return runs


def excerpt(text, start, end=None, before=0, after=0, start_occurrence=1):
    """Lines from the one matching `start` up to (not including) the one matching `end`."""
    lines = text.split("\n")
    plain = [ANSI.sub("", line) for line in lines]
    hits = [i for i, line in enumerate(plain) if start in line]
    i = hits[start_occurrence - 1] - before
    if end is None:
        j = len(lines)
    else:
        j = next(k for k in range(i + 1 + before, len(lines)) if end in plain[k]) + after
    while i < j and not plain[i].strip():
        i += 1
    while j > i and (not plain[j - 1].strip() or plain[j - 1].strip()[0] in "╭+"):
        j -= 1
    return "\n".join(lines[i:j])


def terminal(text, caption):
    return (f'<figure class="term"><div class="term-bar"><span></span><span></span><span></span>'
            f'<em>{html.escape(caption)}</em></div><pre>{convert(text)}</pre></figure>')


def test_list(output):
    items = []
    for line in ANSI.sub("", output).splitlines():
        m = re.match(r"\s*(PASS|FAIL)\s+(.*)", line)
        if m:
            cls = "pass" if m.group(1) == "PASS" else "fail"
            items.append(f'<li class="{cls}"><b>{m.group(1)}</b> {html.escape(m.group(2))}</li>')
    summary = ANSI.sub("", output).strip().splitlines()[-1].strip()
    return f'<ul class="tests">{"".join(items)}</ul><p class="tests-summary">{html.escape(summary)}</p>'


def git_log():
    log = run(["git", "log", "--reverse", "--date=short", "--format=%h|%ad|%s|%D"], cwd=ROOT)
    rows = []
    for line in log.strip().splitlines():
        sha, date, subject, refs = line.split("|", 3)
        tag = ", ".join(t.replace("tag: ", "") for t in refs.split(", ") if t.startswith("tag:"))
        tag_html = f'<span class="tag">{html.escape(tag)}</span>' if tag else ""
        rows.append(f"<tr><td><code>{sha}</code></td><td>{date}</td><td>{html.escape(subject)}</td>"
                    f"<td>{tag_html}</td></tr>")
    return "\n".join(rows)


SECTIONS = ["1Overview", "2UMLclassdiagram", "3UMLsequencediagram", "4Designdiscussion",
            "5Howtheplayer", "6Developmentprocess", "7Functionalevidence", "8Ruleinterpretations",
            "Whatworked"]


def section_pages(pdf):
    """Page on which each numbered section starts (needs pypdf; otherwise the contents stay blank)."""
    try:
        import pypdf
    except ImportError:
        print("  (pypdf not installed: contents page numbers left blank)")
        return {}
    pages = {}
    for number, page in enumerate(pypdf.PdfReader(str(pdf)).pages, 1):
        if number <= 2:
            continue
        text = re.sub(r"\s+", "", page.extract_text() or "")[:160]
        for n, key in enumerate(SECTIONS, 1):
            if n not in pages and key in text:
                pages[n] = number
    return pages


def inline_svg(name):
    svg = (DIAGRAMS / name).read_text()
    return re.sub(r'\s(width|height)="[^"]*"', "", svg, count=2)


def main():
    BUILD.mkdir(exist_ok=True)
    print("1/4 class diagram")
    run([sys.executable, str(DIAGRAMS / "class_diagram.py")])
    print("2/4 sequence diagrams")
    render_sequences()
    print("3/4 replaying the tagged stages")
    r = capture_runs()

    shots = {
        "TERM_LAUNCH": terminal(excerpt(r["s1v"], "╦═╗", "Chef Carlo waits", after=1),
                                "Stage 1: launch, first dashboard and two rejected inputs (turn 1)"),
        "TERM_READY_SERVED": terminal(excerpt(excerpt(r["s1v"], "TURN 9 of 18", "TURN 10 of 18", before=1),
                                              "WAITER WENDY", None),
                                      "Stage 1, turn 9: serve rejected (still cooking), then the Chef makes it READY"),
        "TERM_TRADEOFF": terminal(excerpt(r["s1v"], "TURN 10 of 18", "TURN 11 of 18", before=1),
                                  "Stage 1, turn 10: Dev arrives as the Pasta is READY; one Waiter, two needs"),
        "TERM_PAYMENT": terminal(excerpt(r["s1v"], "End of turn 12", "TURN 13 of 18", before=0),
                                 "Stage 1: end of turn 12, the Cashier collects Chloe's Pasta"),
        "TERM_S1_VICTORY": terminal(excerpt(r["s1v"], "╦  ╦┬┌─┐┌┬┐┌─┐┬─┐┬ ┬", "Play the base setting"),
                                    "Stage 1 victory run: result screen"),
        "TERM_S1_DEFEAT": terminal(excerpt(r["s1d"], "╔╦╗┌─┐┌─┐┌─┐┌─┐┌┬┐", "Play the base setting"),
                                   "Stage 1 defeat run: result screen"),
        "TERM_CRITIC": terminal(excerpt(r["s2v"], "TURN 7 of 18", "End of turn 7", before=1, after=6),
                                "Stage 2, turn 7: the Critic's Pasta is READY just as the VIP arrives"),
        "TERM_REVIEW": terminal(excerpt(r["s2v"], "End of turn 9", "TURN 10 of 18"),
                                "Stage 2, end of turn 9: the Critic pays and posts a review"),
        "TERM_HAPPY": terminal(excerpt(r["s3v"], "TURN 7 of 18", "End of turn 7", before=1, after=7),
                               "Stage 3, turn 7: Happy Hour, the Host seats, the VIP order is $12.96"),
        "TERM_HAPPY2": terminal(excerpt(r["s3v"], "TURN 8 of 18", "WAITING QUEUE", before=1),
                                "Stage 3, turn 8: remaining duration and recovery"),
        "TERM_S3_VICTORY": terminal(excerpt(r["s3v"], "╦  ╦┬┌─┐┌┬┐┌─┐┬─┐┬ ┬", "Play the base setting"),
                                    "Stage 3 victory run: result screen"),
        "TERM_S3_DEFEAT": terminal(excerpt(r["s3d"], "╔╦╗┌─┐┌─┐┌─┐┌─┐┌┬┐", "Play the base setting"),
                                   "Stage 3 defeat run: result screen"),
        "TESTS_1": test_list(r["tests1"]),
        "TESTS_2": test_list(r["tests2"]),
        "TESTS_3": test_list(r["tests3"]),
        "GIT_LOG": git_log(),
        "CLASS_DIAGRAM": inline_svg("class-diagram.svg"),
        "SEQ_1": inline_svg("sequence-1-arrival-order.svg"),
        "SEQ_2": inline_svg("sequence-2-service-payment.svg"),
    }

    template = (HERE / "report.html").read_text(encoding="utf-8")
    for key, value in shots.items():
        template = template.replace("{{" + key + "}}", value)
    shutil.copy(HERE / "report.css", BUILD / "report.css")
    pdf = HERE / "Restaurant-Rush-Report.pdf"

    print("4/4 printing PDF")
    pages = {}
    for attempt in (1, 2):
        page = template
        for n in range(1, len(SECTIONS) + 1):
            page = page.replace("{{PAGE_%d}}" % n, str(pages.get(n, "")))
        missing = re.findall(r"\{\{[A-Z0-9_]+\}\}", page)
        if missing:
            raise SystemExit(f"unfilled placeholders: {missing}")
        filled = BUILD / "report.html"
        filled.write_text(page, encoding="utf-8")
        subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                        "--virtual-time-budget=5000", f"--print-to-pdf={pdf}", filled.as_uri()],
                       check=True, capture_output=True)
        found = section_pages(pdf)
        if not found or found == pages:
            break
        pages = found
    print(f"  wrote {pdf.relative_to(ROOT)}" + (f" (contents: {pages})" if pages else ""))


if __name__ == "__main__":
    main()
