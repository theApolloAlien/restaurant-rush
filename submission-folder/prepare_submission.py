"""
Refreshes submission-folder/ from the repository and builds the zip for NTULearn.

  python3 submission-folder/prepare_submission.py

What it does
  1. copies the current source code, README and PDF report into SCMA_Grp3/ (the zip contents)
  2. copies the editable files (report .docx, draw.io diagrams) into editable/
  3. turns the video script into a Word file, one member's part per page
  4. zips SCMA_Grp3/ as SCMA_Grp3.zip, or as SCMA_Grp3-DRAFT.zip while the video or the signed
     declarations are still missing. The zip also gets stage-snapshots/, each stage tag exactly as
     committed (git archive), so the stages can be checked without GitHub

Run python3 docs/report/build.py first if the report or diagrams changed.
"""
import io
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile
import time
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
STAGES = ["stage-1", "stage-2", "stage-3"]
SNAPSHOT_NOTE = """Stage snapshots: SCMA Group 3, Restaurant Rush

Each folder is the repository exactly as committed at that git tag (made with `git archive <tag>`),
so the three stages can be built and compared without GitHub. The final code is in
source-code/restaurant-rush/.

Build and replay a stage from inside its folder:

  ./build.sh && ./run.sh --script scripts/stage1-victory.txt      (macOS, Linux)
  javac --release 17 -encoding UTF-8 -sourcepath src -d out src/rush/Main.java
  java -cp out rush.Main --script scripts/stage1-victory.txt     (any shell, including Windows)

Compare two stages with any diff tool, for example: diff -r stage-1/src stage-2/src
"""


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
            "> below are relative to that folder. The PDF report is in `report/`, and `stage-snapshots/` holds\n"
            "> the code exactly as tagged at `stage-1`, `stage-2` and `stage-3`.\n\n")
    (PACK / "README.md").write_text(note + readme, encoding="utf-8")
    copy(ROOT / "docs/report/Restaurant-Rush-Report.pdf", PACK / "report" / "Restaurant-Rush-Report.pdf")
    print(f"  refreshed {PACK.relative_to(ROOT)}/ (source code, README, PDF report)")


def refresh_editable():
    copy(ROOT / "docs/report/Restaurant-Rush-Report.docx", EDIT / "Restaurant-Rush-Report.docx")
    current = sorted((ROOT / "docs/diagrams").glob("*.drawio"))
    for old in (EDIT / "diagrams").glob("*.drawio"):
        if old.name not in {f.name for f in current}:
            old.unlink()                     # a diagram that was renamed or removed in docs/diagrams
    for f in current:
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


def add_stage_snapshots(z):
    """Adds every stage tag, exactly as committed, under stage-snapshots/."""
    entries = []
    for tag in STAGES:
        try:
            archive = subprocess.run(["git", "archive", "--format=tar", tag], cwd=ROOT, capture_output=True,
                                     check=True).stdout
        except (OSError, subprocess.CalledProcessError) as e:
            print(f"  ! could not read tag {tag} ({e}); stage snapshots left out of the zip")
            return
        with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
            for member in tar.getmembers():
                if member.isfile():
                    entries.append((f"{GROUP}/stage-snapshots/{tag}/{member.name}", member.mtime, member.mode,
                                    tar.extractfile(member).read()))
    entries.append((f"{GROUP}/stage-snapshots/README.txt", time.time(), 0o644, SNAPSHOT_NOTE.encode("utf-8")))
    for name, mtime, mode, data in entries:
        info = zipfile.ZipInfo(name, date_time=time.localtime(mtime)[:6])
        info.external_attr = (0o100000 | (mode & 0o777)) << 16
        info.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(info, data)
    print(f"  added stage-snapshots/ ({', '.join(STAGES)}, from git)")


def build_zip(missing):
    name = f"{GROUP}-DRAFT.zip" if missing else f"{GROUP}.zip"
    target = HERE / name
    for old in HERE.glob(f"{GROUP}*.zip"):
        old.unlink()
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(PACK.rglob("*")):
            if f.is_file() and f.name != ".DS_Store":
                z.write(f, f.relative_to(HERE))
        add_stage_snapshots(z)
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
