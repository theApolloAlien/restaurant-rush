# Restaurant Rush

A turn-based restaurant management game for the command line (SC2002 group assignment).
You are the manager: each turn you decide what the Waiter and the Chef do, and the
Cashier collects payment on their own.

**This snapshot is Stage 2: customer behaviour.** On top of Stage 1 (Regular customers,
menu, Waiter, Chef, Cashier, full turn sequence, victory and defeat) it adds VIP
customers (lose 5 per turn, 10% off) and Critics (lose 12 per turn, always order the
most elaborate dish).

## Build and run

Needs JDK 17 or newer and nothing else (no network, no build tool).

```bash
./run.sh                                         # build and play
./run.sh --script scripts/stage2-victory.txt     # replay the victory run
./run.sh --plain                                 # no colour, ASCII only
./test.sh                                        # rule checks
```

On Windows use `run.bat` and `test.bat`.

See [`docs/TEST_NOTES.md`](docs/TEST_NOTES.md) for the reproducible victory and defeat runs.
