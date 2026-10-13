draw.io diagrams: Restaurant Rush, SCMA Group 3

Import these seven files into draw.io, in this order. Each file holds one diagram.

  01-class-diagram.drawio                    UML class diagram (final design, Stage 3)
  02-sequence-part-1-arrival-seating.drawio  Sequence diagram part 1: turn 7, arrival, Happy Hour, seating  (messages 1-26)
  03-sequence-part-2-order-pricing.drawio    Sequence diagram part 2: turn 7, ordering, pricing, cooking    (messages 27-54)
  04-sequence-part-3-ready.drawio            Sequence diagram part 3: turn 8, the order becomes READY       (messages 55-69)
  05-sequence-part-4-served.drawio           Sequence diagram part 4: turn 9, the order is SERVED           (messages 70-82)
  06-sequence-part-5-eating-payment.drawio   Sequence diagram part 5: turns 10-11, eating and payment       (messages 83-106)
  07-sequence-part-6-result.drawio           Sequence diagram part 6: after turn 18, the result check       (messages 107-117)

Files 02 to 07 are one sequence diagram split into six parts so it stays readable. The message numbers carry on
from one part to the next, and report section 3 refers to them.

How to open: go to https://app.diagrams.net and use File > Open from > Device (or drag the file onto the page),
or use the draw.io desktop app or the VS Code draw.io extension.

Use the files in this folder. The same diagrams also appear in docs/diagrams/ (regenerated every time
docs/report/build.py runs) and inside SCMA_Grp3/source-code/ (part of the zip), so edits there would be lost.

prepare_submission.py will not overwrite a file in this folder that you have edited. The PDF report is built
from the generated diagrams, so if you change one here, export it (File > Export as > PNG) and use that picture
in the Word report.
