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

> Scripts are tied to the stage they were written for, because each stage changes
> the base setting (Stage 2 adds a Critic and a VIP; Stage 3 adds the Host, combos and
> Happy Hour). Replay `stage1-*` at tag `stage-1`, `stage2-*` at tag `stage-2`, and so on:
> `git checkout stage-1 && ./run.sh --script scripts/stage1-victory.txt`.

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

## Stage 2: VIP and Critic customers

Arrivals: #1 Ada (Regular, T1), **#2 Ben (Critic, T4)**, **#3 Chloe (VIP, T7)**,
#4 Dev, #5 Elif, #6 Farid (Regular, T10, T13, T16). Two of the six Regular arrivals
were replaced, so there are still six.

| Type | Loss per waiting turn | Bill | Special behaviour |
|---|:-:|---|---|
| Regular | 8 | full price | orders by rotating through the menu |
| VIP | 5 | 10% off (Pasta $18.00 → $16.20) | — |
| Critic | 12 | full price | **always orders the most elaborate dish** (most preparation units, dearer on a tie), so Ben orders Pasta where the rotation would give a Burger; leaves a 1–5 star review when paying |

### Victory run: `scripts/stage2-victory.txt`

Strategy: serve › order › seat, Chef cooks immediately.

* Turns 4–7: the Critic's Pasta needs two Chef turns while the Critic loses 12 per turn
  (100 → 88 → 76 → 64).
* **Turn 7 (constrained choice):** Chloe the VIP arrives just as the Critic's Pasta
  is READY. The single Waiter serves the Critic and leaves the VIP queuing, because
  the VIP loses only 5 per turn while the Critic loses 12.
* Turn 9: the Critic pays $18.00 and writes "3/5 stars: Good food, let down by the
  wait." Turn 13: the VIP pays **$16.20** (10% off $18.00).

**Result: VICTORY.** 5 paid, revenue **$67.20**, average satisfaction **76.0**
(84, 64, 80, 76, 76). Transcript: [`transcripts/stage2-victory.txt`](transcripts/stage2-victory.txt).

### Defeat run: `scripts/stage2-defeat.txt`

The same rookie strategy as Stage 1 (seat › serve › order, Chef never cooks on the
turn an order is taken). The Critic now waits so long that they pay at satisfaction 40
and post a 2-star review.

**Result: DEFEAT on all three targets.** 3 paid, revenue **$43.20**, average
satisfaction **59.3**. Transcript: [`transcripts/stage2-defeat.txt`](transcripts/stage2-defeat.txt).

## Stage 3: Host, ComboMeal and Happy Hour

Same arrivals as Stage 2. New in this stage:

* **Host Hugo** seats at most one queued customer per turn, in addition to the
  Waiter's task. The Host acts first, so the Waiter may take that customer's order in
  the same turn (and the Chef may start cooking it).
* **Combos** follow the three dishes on the menu board:
  Salad + Burger Combo ($21.60, 2 units) and Salad + Pasta Combo ($24.30, 3 units).
  **Combo rule:** customers still read the board in rotation by arrival number, so
  guests #4 and #5 order the two combos; the Critic orders the most elaborate item,
  which is now the 3-unit Salad + Pasta Combo.
* **Happy Hour** is offered at the start of every turn until it is used
  (`happy hour now? [y/N]`). It lasts that turn and the next; the header shows the
  turns remaining.
* Staff with nothing useful to do now wait automatically, so the manager is only
  asked when there is a real choice.

### Victory run: `scripts/stage3-victory.txt`

Strategy: take orders › serve › seat, Host seats every arrival, Happy Hour on turn 7.

* Turn 1: the Host seats Ada, the Waiter takes the order and the Chef finishes the
  Salad in the **same turn**, which was impossible before the Host existed.
* Turn 4: the Critic orders the **Salad + Pasta Combo ($24.30, 3 units)**.
* **Turn 7 (constrained choice):** the Critic's combo is READY, but the manager
  switches on **Happy Hour** and has the Waiter take the VIP's order first, so the
  VIP's Pasta is locked at **$18.00 × 0.9 × 0.8 = $12.96**. The Critic recovers +15
  (64 → 79). The header shows `HAPPY HOUR ON 1 turn left (last turn)` on turn 8, and
  both waiting guests recover again (Critic 67 → 82, VIP 95 → 100).
* Turn 8: the Critic is served at 82 and later writes "4/5 stars".

**Result: VICTORY.** 5 paid, revenue **$92.16**, average satisfaction **82.6**
(92, 82, 95, 68, 76). Transcript: [`transcripts/stage3-victory.txt`](transcripts/stage3-victory.txt).

### Defeat run: `scripts/stage3-defeat.txt`

The rookie strategy again (seat › serve › order, Chef never cooks on the turn an
order is taken), but the manager also **ignores the new Host** and switches Happy
Hour on **too late** (turn 14). The discount only reaches Dev's combo ($21.60 → $17.28),
who never gets to pay before closing.

**Result: DEFEAT.** 3 paid (needs 4), revenue $49.50, average satisfaction
**57.0** (needs 60). Transcript: [`transcripts/stage3-defeat.txt`](transcripts/stage3-defeat.txt).

### How choices change the result (Stage 3)

| Waiter priority | Chef same turn | Host used | Happy Hour | Paid | Revenue | Avg sat. | Result |
|---|:-:|:-:|:-:|:-:|:-:|:-:|---|
| order › serve › seat | yes | yes | turn 7 | 5 | $92.16 | 82.6 | Victory |
| order › serve › seat | yes | yes | never | 5 | $95.40 | 75.6 | Victory |
| serve › order › seat | yes | yes | turn 7 | 4 | $67.86 | 84.3 | Victory |
| order › serve › seat | yes | **no** | turn 7 | 4 | $67.86 | 74.0 | Victory |
| seat › serve › order | no | **yes** | turn 7 | 4 | $67.86 | 81.0 | **Victory** |
| seat › serve › order | no | **no** | turn 7 | 3 | $49.50 | 68.7 | Defeat |
| seat › serve › order | no | no | turn 14 | 3 | $49.50 | 57.0 | Defeat |

* With a Host, the best Waiter priority **changes**: taking the order first beats
  serving first (5 paid instead of 4), because the Host has already done the seating.
* Using the Host alone turns the rookie strategy from Defeat into Victory.
* Happy Hour trades revenue for satisfaction: on turn 7 it costs $3.24 of revenue but
  lifts the average from 75.6 to 82.6.
