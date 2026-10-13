"""
Refreshes submission-folder/ from the repository and builds the zip for NTULearn.

  python3 submission-folder/prepare_submission.py

What it does
  1. copies the current source code, README and PDF report into SCMA_Grp3/ (the zip contents)
  2. copies the editable files (report .docx, draw.io diagrams) into editable/
  3. turns the video script into a Word file, one member's part per page
  4. zips SCMA_Grp3/ as SCMA_Grp3.zip, or as SCMA_Grp3-DRAFT.zip while the video or the signed
     declarations are still missing

Run python3 docs/report/build.py first if the report or diagrams changed.
"""
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
GROUP = "SCMA_Grp3"
PACK = HERE / GROUP
EDIT = HERE / "editable"

SOURCE_ITEMS = ["src", "test", "scripts", "build.sh", "run.sh", "test.sh", "run.bat", "test.bat", "README.md",
                "docs/TEST_NOTES.md", "docs/transcripts", "docs/diagrams"]
VIDEO_TYPES = {".mp4", ".mov", ".mkv", ".webm", ".avi", ".m4v"}
FORM_TYPES = {".pdf", ".docx", ".jpg", ".jpeg", ".png"}
PLACEHOLDER = "[paste the video link here]"


def copy(src, dst):
    if src.is_dir():
        shutil.copytree(src, dst, dirs_exist_ok=True,
                        ignore=shutil.ignore_patterns("__pycache__", "*.pyc", ".DS_Store"))
    else:
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)


def refresh_pack():
    code = PACK / "source-code" / "restaurant-rush"
    if code.exists():
        shutil.rmtree(code)
    for item in SOURCE_ITEMS:
        copy(ROOT / item, code / item)
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    note = ("> Copy of the repository README. The game itself is in `source-code/restaurant-rush/`, and the links\n"
            "> below are relative to that folder. The PDF report is in `report/`.\n\n")
    (PACK / "README.md").write_text(note + readme, encoding="utf-8")
    copy(ROOT / "docs/report/Restaurant-Rush-Report.pdf", PACK / "report" / "Restaurant-Rush-Report.pdf")
    print(f"  refreshed {PACK.relative_to(ROOT)}/ (source code, README, PDF report)")


def refresh_editable():
    copy(ROOT / "docs/report/Restaurant-Rush-Report.docx", EDIT / "Restaurant-Rush-Report.docx")
    for f in sorted((ROOT / "docs/diagrams").glob("*.drawio")):
        copy(f, EDIT / "diagrams" / f.name)
    print(f"  refreshed {EDIT.relative_to(ROOT)}/ (report .docx, draw.io diagrams)")


def video_script_docx():
    src = EDIT / "video-script" / "VIDEO_SCRIPT.md"
    text = src.read_text(encoding="utf-8")
    page_break = '\n```{=openxml}\n<w:p><w:r><w:br w:type="page"/></w:r></w:p>\n```\n\n'
    text = re.sub(r"\n---\n\n(## Part)", lambda m: "\n" + page_break + m.group(1), text)
    with tempfile.TemporaryDirectory() as tmp:
        md = Path(tmp) / "script.md"
        md.write_text(text, encoding="utf-8")
        out = src.with_suffix(".docx")
        try:
            subprocess.run(["pandoc", str(md), "-f", "gfm+raw_attribute", "-t", "docx", "-o", str(out)],
                           check=True)
        except (OSError, subprocess.CalledProcessError) as e:
            print(f"  ! could not run pandoc ({e}); VIDEO_SCRIPT.docx not updated")
            return
    print(f"  wrote {out.relative_to(ROOT)}")


def missing_items():
    missing = []
    video_dir = PACK / "video"
    has_file = any(f.suffix.lower() in VIDEO_TYPES for f in video_dir.glob("*"))
    link = (video_dir / "VIDEO_LINK.txt").read_text(encoding="utf-8") if (video_dir / "VIDEO_LINK.txt").exists() else ""
    if not has_file and PLACEHOLDER in link:
        missing.append("video: add the recording to video/ or paste a link into video/VIDEO_LINK.txt")
    forms = [f for f in (PACK / "declarations").glob("*") if f.suffix.lower() in FORM_TYPES]
    if not forms:
        missing.append("declarations: add the signed Declaration of Original Work and each member's "
                       "Declaration on Use of GAI Assistance to declarations/")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    if "[add the video link or file name here]" in readme:
        missing.append("README: replace the video placeholder and check the timestamps")
    return missing


def build_zip(missing):
    name = f"{GROUP}-DRAFT.zip" if missing else f"{GROUP}.zip"
    target = HERE / name
    for old in HERE.glob(f"{GROUP}*.zip"):
        old.unlink()
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(PACK.rglob("*")):
            if f.is_file() and f.name != ".DS_Store":
                z.write(f, f.relative_to(HERE))
    size = target.stat().st_size / 1_000_000
    print(f"  wrote {target.relative_to(ROOT)} ({size:.1f} MB)")
    return target


def main():
    print("Preparing the submission")
    refresh_pack()
    refresh_editable()
    video_script_docx()
    missing = missing_items()
    build_zip(missing)
    if missing:
        print("\nStill to do before submitting (the zip is marked DRAFT until these are done):")
        for item in missing:
            print("  -", item)
    else:
        print("\nAll items present. Upload the zip on NTULearn.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
