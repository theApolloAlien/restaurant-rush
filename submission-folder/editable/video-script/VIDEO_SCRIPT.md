# Restaurant Rush: video script

SCMA Group 3 · about 15 minutes · covers the seven segments in brief §6.2

| Start | Segment (brief §6.2) | Presenter | Length |
|---|---|---|---|
| 00:00 | Intro, then **1** The game launching from the submitted project | CCDS POLOYAPOY RON JOSHUA ABES (Apollo) | 2:00 |
| 02:00 | **2** Meaningful manager choices, including a decision where staff are constrained | Apollo | 2:00 |
| 04:00 | **3** A dish going READY → SERVED → payment | CCDS LEONG XUAN HAO | 2:00 |
| 06:00 | **4** A VIP or Critic behaviour (we show both) | Leong Xuan Hao | 2:00 |
| 08:00 | **5** A Stage 3 feature (we show the Host, a combo and Happy Hour) | CCDS VICTORIA HAUGAA | 2:30 |
| 10:30 | **6** One victory and one defeat | Victoria Haugaa | 1:30 |
| 12:00 | **7** The stage tag comparisons and the diagrams, then closing | CCDS LEE SHENG KAI | 3:00 |

The start times are targets. After editing, write the real times into the **Video timestamps** table in
`README.md`.

## Before recording

- Terminal: dark theme, font size 16 pt or larger, window at least 100 columns wide.
- Have a clone of the repository ready: `git clone https://github.com/theApolloAlien/restaurant-rush`.
- **After every `git checkout`, run `./build.sh` before `./run.sh`.** `run.sh` reuses an existing
  `restaurant-rush.jar`, and that file is not tracked by git, so without a rebuild you would be running the
  wrong stage.
- To show a replay slowly, pipe it into `less -R` and jump to a turn by typing `/TURN 8` and pressing Enter.
  Press `q` to quit.
- Each person records their own part (screen and voice) and we join the clips in order. Speak at a normal pace.
  The lines below are a guide, so say them in your own words.

---

## Part 1: Apollo (CCDS POLOYAPOY RON JOSHUA ABES)

**Segments 1 and 2 · 00:00 to 04:00**

### Intro and segment 1: launching the game (00:00, about 2 minutes)

**On screen:** the unzipped submission folder in a terminal.

```bash
unzip SCMA_Grp3.zip
cd SCMA_Grp3/source-code/restaurant-rush
java -version
./run.sh
```

**Say:**

> Hi, we're SCMA Group 3, and this is Restaurant Rush, a turn-based restaurant game that runs in the terminal.
> I'm Apollo, and I'll start by launching it from our submitted zip.
>
> All you need is Java 17 or newer. There are no libraries and no network access. `run.sh` compiles the game
> the first time and then starts it.

**On screen:** the title menu. Choose `2` (How to play), scroll through it, then go back and choose `1`.

> The help screen lists the commands, the customer types and the targets. To win, after 18 turns we need at
> least four paying customers, $45 in revenue, and an average satisfaction of 60 among the customers who paid.

**On screen:** the first dashboard. Point at each part as you mention it.

> Every turn starts with this dashboard. At the top are the turn, our revenue against $45, how many customers
> have paid, and the average satisfaction, which shows N/A until someone pays. Below that are the waiting queue,
> the two tables with each customer's satisfaction bar, the kitchen, and the tasks each staff member can do
> right now.
>
> One design point: the interface only displays the game and passes our commands on. Every rule lives in the
> domain objects. For example, whether a dish can be served is decided by `Order.serve`, not by the UI.

Type `quit` to leave the game, then `3` to close the menu.

### Segment 2: manager choices and a constrained decision (02:00, about 2 minutes)

**On screen:** switch to the git clone (the unzipped folder has no git history), check out Stage 1 and play
turn 1 live.

```bash
cd ~/restaurant-rush          # wherever you cloned the repository
git checkout stage-1 && ./build.sh && ./run.sh
```

Choose `1`. On turn 1, as the Waiter, type `order 1`, then `dance`, then `seat 1 1`. As the Chef, type `wait`.

> This is Stage 1, with Regular customers only. Ada has just arrived. If I try to take her order before she has
> a table, the game tells me why that isn't allowed, and the Waiter's task isn't used up. A command it doesn't
> know, like "dance", gets the same treatment. Now I'll seat her at table 1.

Type `quit`, then `3`, then replay the full Stage 1 victory run and jump to turn 10.

```bash
./run.sh --script scripts/stage1-victory.txt | less -R      # then type /TURN 10
```

> Now the interesting part. On turn 10, Dev walks in just as Chloe's Pasta is READY to serve. We only have
> one Waiter, who can do one task per turn, so we have to choose. If we serve Chloe, she stops losing
> satisfaction and frees her table sooner. If we seat Dev, Chloe's dish sits there. We served Chloe, and Dev
> waited one turn and lost 8 points.
>
> These choices add up. In our replays, a manager who always seats first and lets the Chef wait ends with only
> three paid customers and $42, which is a defeat, while serving first wins with five customers and $66. That
> comparison is in section 5 of our report. Xuan Hao will follow Chloe's Pasta all the way to payment.

---

## Part 2: Leong Xuan Hao (CCDS LEONG XUAN HAO)

**Segments 3 and 4 · 04:00 to 08:00**

### Segment 3: READY → SERVED → payment (04:00, about 2 minutes)

**On screen:** the same Stage 1 replay, starting from turn 8. If you record this part separately, check out
Stage 1 in the clone first.

```bash
git checkout stage-1 && ./build.sh
./run.sh --script scripts/stage1-victory.txt | less -R      # then type /TURN 8
```

> I'm Xuan Hao. I'll follow one order from the kitchen to the till.
>
> On turn 8 the Waiter takes Chloe's order. She chooses Pasta, which costs $18.00 and needs two preparation
> units. The Chef does the first unit in the same turn.

Jump to turn 9 (`/TURN 9`).

> On turn 9 we try to serve it, and the game rejects it because the Pasta is still cooking, one of two units
> done. Nothing is used up, so the Waiter waits instead. The Chef then finishes the second unit and the order
> becomes READY. The Waiter has already acted this turn, so the earliest it can be served is next turn.

Jump to turn 10, then to "End of turn 11" and "End of turn 12".

> On turn 10 the Waiter serves it and the order is SERVED. Chloe spends turn 11 eating, and at the end of that
> turn she's ready to pay. On turn 12 the Cashier collects $18.00, records her satisfaction of 76, and frees
> table 1.
>
> Each of these rules is in one class. `Order.serve` only accepts a dish that was READY before this turn.
> `Customer.finishEating` handles eating, and `Cashier.collectPayment` takes one payment per turn, oldest
> customer first, and only from someone who became ready on an earlier turn.

### Segment 4: VIP and Critic behaviour (06:00, about 2 minutes)

**On screen:**

```bash
git checkout stage-2 && ./build.sh
./run.sh --script scripts/stage2-victory.txt | less -R      # then type /TURN 4
```

> Stage 2 adds two new customer types. Ben, who arrives on turn 4, is a Critic. Going by the menu rotation,
> guest number 2 would order a Burger, but a Critic always orders the most elaborate dish, so Ben orders Pasta.
> Critics are also impatient: Ben loses 12 satisfaction per turn instead of 8.

Jump to turn 7.

> On turn 7 Chloe arrives, and this time she's a VIP. Ben's Pasta is READY, so we serve Ben first. That's the
> right call, because a VIP only loses 5 points per turn while waiting.

Jump to "End of turn 9", then to turn 13.

> When Ben pays on turn 9 he leaves a review, three out of five stars, "Good food, let down by the wait." On
> turn 13 Chloe pays $16.20, which is the $18 Pasta with her 10% VIP discount.

**On screen:** open `src/rush/model/VIPCustomer.java` and `CriticCustomer.java`, then run:

```bash
git diff stage-1 stage-2 -- src/rush/control/Game.java src/rush/staff
```

> Each type only overrides the methods that differ: the satisfaction loss, the food choice, and the bill. The
> game controller never checks a customer's type. You can see that here: between Stage 1 and Stage 2, `Game`
> and the staff classes didn't change at all, so this diff is empty. Victoria will now show Stage 3.

---

## Part 3: Victoria Haugaa (CCDS VICTORIA HAUGAA)

**Segments 5 and 6 · 08:00 to 12:00**

### Segment 5: Stage 3 features (08:00, about 2.5 minutes)

**On screen:** start Stage 3 and play turn 1 live.

```bash
git checkout stage-3 && ./build.sh && ./run.sh
```

Choose `1`. Answer `n` to Happy Hour. Host: `seat 1 1`. Waiter: `order 1`. Chef: `cook 1`.

> I'm Victoria, and this is Stage 3. There's a new staff member, Hugo the Host, who can seat one customer per
> turn on top of what the Waiter does. On turn 1, Hugo seats Ada, which leaves the Waiter free to take her
> order straight away, and the Chef cooks it, all in the same turn. Without a Host that takes two turns.

Type `quit`, then `3`, then replay the Stage 3 victory run.

```bash
./run.sh --script scripts/stage3-victory.txt | less -R      # then type /TURN 4
```

> Stage 3 also adds combo meals. On turn 4 Ben, our Critic, orders the Salad and Pasta Combo. A combo's price
> is 90% of its two dishes, so $27 becomes $24.30, and its preparation time is the sum of theirs, three units.

Jump to turn 7.

> On turn 7 we switch on Happy Hour, which can only be used once per game. It lasts for this turn and the next,
> and the game shows how many turns are left. Orders taken during it get 20% off, and customers who are still
> waiting recover 15 satisfaction at the start of each turn. Hugo seats Chloe, our VIP, and the Waiter takes
> her order. Her Pasta comes to $12.96: $18, minus the 10% VIP discount, minus 20% for Happy Hour. The price is
> locked when the order is taken.

Jump to turn 8.

> On turn 8 the header says one turn left, and both waiting guests perk up: Ben to 82 and Chloe back to 100.

**On screen:** briefly show `Host.java`, `ComboMeal.java` and `HappyHour.java`.

> In the code, the Host is just another `DirectedStaff` with one duty, seating. `ComboMeal` is a `MenuItem` made
> of two dishes, so orders and the Chef treat it like any other item. Happy Hour is a `PriceModifier` that the
> pricing policy applies after the customer's own discount.

### Segment 6: one victory and one defeat (10:30, about 1.5 minutes)

**On screen:**

```bash
./run.sh --script scripts/stage3-victory.txt
./run.sh --script scripts/stage3-defeat.txt
```

Scroll to each result screen and point at the three target lines.

> Here are two complete runs on the same base setting. The first is a victory: five paid customers, $92.16 in
> revenue, and an average satisfaction of 82.6.
>
> The second is a defeat: only three paid customers, $49.50, and an average of 57, below the target of 60. In
> this run the manager seats first, never cooks an order on the turn it's taken, ignores the Host, and only
> switches on Happy Hour on turn 14, when it's too late to help anyone who can still pay. Because there's no
> randomness, the only difference between the two runs is the manager's decisions. Sheng Kai will now cover how
> the stages developed and our diagrams.

---

## Part 4: Lee Sheng Kai (CCDS LEE SHENG KAI)

**Segment 7 and closing · 12:00 to 15:00**

### Segment 7: stage tags, comparisons and diagrams (12:00, about 2.5 minutes)

**On screen:**

```bash
git checkout main
git log --oneline --decorate
git diff stage-1 stage-2 --stat -- src
git diff stage-2 stage-3 --stat -- src
./test.sh
```

> I'm Sheng Kai. Each stage is a tagged commit on GitHub, stage-1, stage-2 and stage-3, and each one compiles,
> runs and passes its own rule checks.
>
> From Stage 1 to Stage 2 the source change is small: six files. Two are the new customer classes, plus a
> percentage helper in `Money`, two lines of registration in `GameConfig`, colours in the dashboard, and the
> edition label in `Main`.
>
> From Stage 2 to Stage 3 there's more: fourteen files. The new classes are `Host`, `Dish`, `ComboMeal`,
> `PriceModifier` and `HappyHour`. `MenuItem` became abstract, but its public methods stayed the same, so
> `Order`, `Chef` and `Waiter` didn't need any changes.

**On screen:** the README on GitHub, scrolled to "Stages and change analysis". Open one compare link.

> The README lists every new and changed file for both transitions, with the reason and a GitHub compare link.
> The test script runs 22 rule checks, including the worked example from the brief, and all of them pass.

**On screen:** the PDF report, section 2 (class diagram), then section 3 (the sequence diagram, parts 1 to 6).

> This is our final class diagram, which matches the Stage 3 code. The packages are layered from the UI down to
> the model. The notes point out encapsulation in `Restaurant` and `Money`, polymorphism in the customer types,
> and composition in `ComboMeal`.
>
> The sequence diagram is drawn in six parts, with message numbers that carry on across them. It follows
> Chloe, our VIP, from her arrival on turn 7 to her payment on turn 11, then the result check. It shows the loop over turns, the optional Happy Hour, the alternative where a serve is
> rejected, and the pricing step where VIP and Happy Hour combine into $12.96. That's the manager choice that
> changes the outcome.

### Closing (14:30, about 30 seconds)

> One trade-off we made: rules throw a `RuleViolation` with a message for the player. That keeps every rule in
> one place and gives clear error messages, at the cost of using exceptions for an expected case.
> If we did it again, we'd pass the pricing policy in through `GameConfig`, so a new policy would only need
> registering.
>
> Thanks for watching. That's Restaurant Rush by SCMA Group 3.
