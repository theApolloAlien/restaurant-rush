# Video script (about 15 minutes)

Every member presents at least one segment. Record the terminal at a readable size
(≥ 16 pt font, a dark theme, a window at least 100 columns wide) with clear audio. Each
segment lists the exact commands to run, so takes can be repeated.

> **Tip:** to type commands live while following a script, open the script file in a
> second window and type each line. To show a whole run quickly, use
> `./run.sh --script <file>` and scroll back through the output.

---

## 1. The game launching from the submitted project · ~1:30 · `[Member 1]`

```bash
unzip LABGroup_AssignmentGrp.zip && cd restaurant-rush
java -version          # any JDK 17+
./run.sh
```

* Show the title screen, then choose `2` (How to play): commands, guest types, targets.
* Choose `1` to start and point at each part of the dashboard: turn track, revenue
  against $45, paid against 4, average satisfaction (N/A until someone pays), queue,
  tables, kitchen, staff tasks.
* **Say:** "The UI only reads the game and sends commands. Every rule is in the
  domain objects, for example `Order.serve` decides whether a dish can be served."

## 2. Meaningful manager choices and a constrained decision · ~2:30 · `[Member 2]`

```bash
git checkout stage-1 && ./run.sh      # type the inputs from scripts/stage1-victory.txt
```

* Turn 1: type `order 1` (rejected: no table yet) and `dance` (unknown command), then
  `seat 1 1`. Point out "Nothing was used up. Choose again."
* Play through to turn 10. Dev arrives exactly when Chloe's Pasta is READY, and there is
  only one Waiter. Explain the trade-off: serving Chloe stops that satisfaction falling
  and frees the table sooner, while Dev queues one turn (−8).
* **Say:** "This is why the order of priorities matters. In our replays, a manager who
  always seats first and lets the Chef wait loses the game (report §5)."

## 3. READY → SERVED → payment · ~2:00 · `[Member 1]`

Continue the same run (turns 8–12):

* Turn 8: `order 3` + `cook 3` (Pasta 1/2). Turn 9: try `serve 3` (rejected: still
  cooking), `wait`, then `cook 3`: READY. The Waiter has already acted, so it is served
  next turn at the earliest.
* Turn 10: `serve 3` (SERVED). Turn 11: "finishes eating and is ready to pay". Turn 12:
  the Cashier collects $18.00, satisfaction 76 recorded, table 1 freed.
* **Say:** the eating and payment rules are in `Customer.finishEating` and
  `Cashier.collectPayment` (oldest first, only customers who became ready on an earlier turn).

## 4. VIP and Critic behaviour · ~2:00 · `[Member 3]`

```bash
git checkout stage-2 && ./run.sh --script scripts/stage2-victory.txt
```

Scroll to turns 4–9 and 13:

* The Critic (#2 Ben) orders **Pasta**, although guest #2 would get a Burger by rotation,
  because a Critic always orders the most elaborate dish. They lose **12** per turn.
* Turn 7: the VIP arrives while the Critic's Pasta is READY. Serve the Critic first: the
  VIP loses only **5** per turn.
* Turn 9: Critic's review, "3/5 stars". Turn 13: the VIP pays **$16.20** (10% off).
* Open `VIPCustomer.java` and `CriticCustomer.java` and show the overridden methods.
* **Say:** "`Game` did not change between stage 1 and stage 2. Run
  `git diff stage-1 stage-2 -- src/rush/control/Game.java` and the output is empty."

## 5. Stage 3 features · ~2:30 · `[Member 4]`

```bash
git checkout stage-3 && ./run.sh      # inputs from scripts/stage3-victory.txt
```

* Turn 1: the Host seats Ada, then the Waiter takes the order and the Chef cooks it, all in one turn.
* Turn 4: the Critic now orders the **Salad + Pasta Combo** ($24.30 = 90% of $27.00, 3 units).
* Turn 7: answer `y` to "happy hour now?". Show the result line ("2 turns left"), then
  `order 3` → **$12.96** = $18.00 × 0.9 (VIP) × 0.8 (Happy Hour).
* Turn 8: the header shows `HAPPY HOUR ON 1 turn left (last turn)` and both waiting guests
  recover +15.
* **Say:** "`PricingPolicy` applies the customer's discount first, then each
  `PriceModifier`; Happy Hour is just one modifier."

## 6. One victory and one defeat · ~2:00 · `[Member 4 or 5]`

```bash
./run.sh --script scripts/stage3-victory.txt     # VICTORY: 5 paid, $92.16, 82.6
./run.sh --script scripts/stage3-defeat.txt      # DEFEAT: 3 paid, average 57.0
```

* Scroll to each result screen and read out the target lines.
* Explain the difference: the defeat run seats first, never cooks an order on the turn
  it is taken, ignores the Host, and starts Happy Hour too late (turn 14).

## 7. Stage tags and diagrams · ~2:30 · `[Member 5]`

```bash
git log --oneline --decorate
git diff stage-1 stage-2 --stat
git diff stage-2 stage-3 --stat
./test.sh
```

* Show that the tags exist and the diffs are small: two new customer classes plus one
  registration change, then the Host, combos and Happy Hour.
* Open the PDF report: the class diagram (notes on encapsulation, polymorphism and
  composition) and the two sequence-diagram pages (loops, `opt`, `alt`, Happy Hour pricing).
* Close with one design trade-off from §4.4 and one reflection from §9.

---

After recording, put each segment's start time into the README table under
**Video timestamps**.
