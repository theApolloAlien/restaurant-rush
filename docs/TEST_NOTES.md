# Test notes

Every run below is fully reproducible: the base setting has no randomness, and each
script in `scripts/` holds exactly the manager's inputs, one per line. Replay any of
them with

```bash
./run.sh --script scripts/<name>.txt          # coloured
./run.sh --script scripts/<name>.txt --plain  # plain text, as saved in docs/transcripts/
```

The automated rule checks (`./test.sh`) also replay these scripts through the real
console and assert the outcome.

## Stage 1: Regular customers only

Arrivals: #1 Ada, #2 Ben, #3 Chloe, #4 Dev, #5 Elif, #6 Farid (all Regular) at turns
1, 4, 7, 10, 13, 16. They order by rotating through the menu: Salad, Burger, Pasta,
Salad, Burger, Pasta.

### Victory run: `scripts/stage1-victory.txt`

Strategy: serve READY dishes first, then take orders, then seat. The Chef always
cooks an order the same turn it is taken.

| Turn | Waiter | Chef | Notes |
|---:|---|---|---|
| 1 | `order 1` ✘, `dance` ✘, `seat 1 1` | wait | Two invalid inputs are explained and do not use the task |
| 2 | order 1 (Salad $9.00) | cook 1 → READY | |
| 3 | serve 1 | wait | Ada locked at 84 |
| 4 | seat 2 2 | wait | Ada finishes eating, READY_TO_PAY |
| 5 | order 2 (Burger $15.00) | cook 2 → READY | Cashier: Ada pays $9.00 |
| 6 | serve 2 | wait | |
| 7 | seat 3 1 | wait | |
| 8 | order 3 (Pasta $18.00) | cook 3 (1/2) | Cashier: Ben pays $15.00 |
| 9 | `serve 3` ✘ (still cooking), wait | cook 3 → READY | |
| 10 | serve 3 | `cook 3` ✘, wait | **Constrained choice:** Dev arrives but the Waiter serves Chloe first |
| 11 | seat 4 2 | wait | |
| 12 | order 4 (Salad) | cook 4 → READY | Cashier: Chloe pays $18.00 |
| 13 | serve 4 | wait | Elif arrives and waits in the queue |
| 14 | seat 5 1 | wait | |
| 15 | order 5 (Burger) | cook 5 → READY | Cashier: Dev pays $9.00 |
| 16 | serve 5 | wait | Farid arrives |
| 17 | seat 6 2 | wait | |
| 18 | order 6 (Pasta) | cook 6 (1/2) | Cashier: Elif pays $15.00 |

**Result: VICTORY.** 5 paid, revenue **$66.00**, average satisfaction **79.2**
(84, 84, 76, 76, 76), 0 unhappy departures. Transcript:
[`transcripts/stage1-victory.txt`](transcripts/stage1-victory.txt).

### Defeat run: `scripts/stage1-defeat.txt`

Strategy of a rookie manager: the Waiter always **seats first**, then serves, then
takes orders, and the Chef **never cooks an order on the turn it was taken**.
Every customer ends up waiting one or two turns longer, so the pipeline slips.

**Result: DEFEAT.** 3 paid (needs 4), revenue **$42.00** (needs $45.00), average
satisfaction 60.0 (just meets 60). Transcript:
[`transcripts/stage1-defeat.txt`](transcripts/stage1-defeat.txt).

### How choices change the result (Stage 1)

We replayed the base setting with every fixed priority order for the Waiter, with
and without the Chef cooking an order on the turn it is taken:

| Waiter priority | Chef cooks same turn | Paid | Revenue | Avg sat. | Result |
|---|:-:|:-:|:-:|:-:|---|
| serve › order › seat | yes | 5 | $66.00 | 79.2 | Victory |
| serve › order › seat | no | 4 | $51.00 | 66.0 | Victory |
| order › serve › seat | no | 4 | $51.00 | 64.0 | Victory |
| seat › order › serve | yes | 4 | $51.00 | 76.0 | Victory |
| seat › order › serve | no | 4 | $51.00 | 60.0 | Victory (only just) |
| seat › serve › order | no | 3 | $42.00 | 60.0 | **Defeat** |

Serving before seating finishes customers sooner, which both frees tables and stops
their satisfaction falling. Letting the Chef start an order the moment it is taken
saves a whole turn per dish.
