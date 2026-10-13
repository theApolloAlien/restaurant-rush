# Submission folder · SCMA Group 3

Everything for the NTULearn upload (brief §6.1), plus the editable files the team works on.

```
submission-folder/
├── SCMA_Grp3/                     ← exactly what goes into SCMA_Grp3.zip
│   ├── README.md                  repository URL, stage comparisons, video timestamps
│   ├── source-code/restaurant-rush/   Java source with build and run instructions
│   ├── report/Restaurant-Rush-Report.pdf
│   ├── stage-snapshots/           stage-1, stage-2, stage-3 as tagged (added to the zip by the script)
│   ├── video/                     the recording, or VIDEO_LINK.txt with a download link
│   └── declarations/              signed course forms (kept out of git)
├── editable/                      working files, not part of the zip
│   ├── Restaurant-Rush-Report.docx    editable Word version of the report
│   ├── diagrams/*.drawio          class diagram and the six sequence diagram parts for draw.io
│   └── video-script/              4-member script (Markdown and Word, one part per page)
└── prepare_submission.py          refreshes the copies and builds the zip
```

## How each §6.1 item is covered

| Item (brief §6.1) | Where |
|---|---|
| Source code with build and run instructions | `SCMA_Grp3/source-code/restaurant-rush/` (see its `README.md`: `./run.sh`, `./test.sh`, `run.bat`) |
| Stage snapshots (brief §5, GitHub evidence) | GitHub tags, plus `SCMA_Grp3/stage-snapshots/` in the zip so the stages can be checked offline |
| README with repository URL, stage comparisons and video timestamps | `SCMA_Grp3/README.md` |
| PDF report | `SCMA_Grp3/report/Restaurant-Rush-Report.pdf` |
| Video, or an accessible download link | `SCMA_Grp3/video/` (still to record) |
| Signed declarations | `SCMA_Grp3/declarations/` (each member to sign) |

## Video

The script is in `editable/video-script/`. It splits the seven §6.2 segments as follows:

| Presenter | Segments | Planned start |
|---|---|---|
| CCDS POLOYAPOY RON JOSHUA ABES (Apollo) | 1 Launch · 2 Manager choices | 00:00 |
| CCDS LEONG XUAN HAO | 3 READY → SERVED → payment · 4 VIP and Critic | 04:00 |
| CCDS VICTORIA HAUGAA | 5 Stage 3 features · 6 Victory and defeat | 08:00 |
| CCDS LEE SHENG KAI | 7 Stage tags and diagrams · closing | 12:00 |

## Editing the report or diagrams

- **Word:** edit `editable/Restaurant-Rush-Report.docx` directly. When Word asks whether to update fields, choose
  Yes so the contents page fills in. To get a PDF from your edited copy, use File › Save As › PDF.
- **draw.io:** open the `.drawio` files in [app.diagrams.net](https://app.diagrams.net), the draw.io desktop app,
  or the VS Code draw.io extension. Every class, lifeline and message is a normal shape you can move and edit.
  Export with File › Export as › PNG or SVG.
- The PDF in `SCMA_Grp3/report/` is generated from `docs/report/report.html`. If you edit the Word file
  instead, export it to PDF and replace that file before zipping.

## Before uploading

1. Record the video. Put the file in `SCMA_Grp3/video/`, or paste a link into `VIDEO_LINK.txt`.
2. Put the real start times in the **Video timestamps** table in the repository `README.md`, and replace the
   video placeholder at the top of it.
3. Each member signs the course forms. Put them in `SCMA_Grp3/declarations/`.
4. Run the prepare script from the repository root:

   ```bash
   python3 submission-folder/prepare_submission.py
   ```

   It refreshes the copies and writes `submission-folder/SCMA_Grp3.zip`. While the video or declarations are
   missing it writes `SCMA_Grp3-DRAFT.zip` instead and lists what is left.
5. Upload `SCMA_Grp3.zip` through the course assignment link.
