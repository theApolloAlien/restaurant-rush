# Restaurant Rush

A turn-based restaurant management game for the command line (SC2002 group assignment).
You are the manager: each turn you decide what the Waiter and the Chef do, and the
Cashier collects payment on their own.

**This snapshot is Stage 3: new role and menu policy.** On top of Stages 1 and 2
(Regular, VIP and Critic customers, Waiter, Chef, Cashier, the full turn sequence) it
adds a Host who seats one extra customer per turn, ComboMeals made of two dishes, and
a once-per-game Happy Hour.

## Build and run

Needs JDK 17 or newer and nothing else (no network, no build tool).

```bash
./run.sh                                         # build and play
./run.sh --script scripts/stage3-victory.txt     # replay the victory run
./run.sh --plain                                 # no colour, ASCII only
./test.sh                                        # rule checks
```

On Windows use `run.bat` and `test.bat`.

See [`docs/TEST_NOTES.md`](docs/TEST_NOTES.md) for the reproducible victory and defeat runs.
