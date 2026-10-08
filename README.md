# Restaurant Rush

A turn-based restaurant management game for the command line (SC2002 group assignment).
You are the manager: each turn you decide what the Waiter and the Chef do, and the
Cashier collects payment on their own.

**This snapshot is Stage 1: basic operation** (Regular customers, menu, Waiter, Chef,
Cashier, the full turn sequence, victory and defeat).

## Build and run

Needs JDK 17 or newer and nothing else (no network, no build tool).

```bash
./run.sh                                         # build and play
./run.sh --script scripts/stage1-victory.txt     # replay the victory run
./run.sh --plain                                 # no colour, ASCII only
./test.sh                                        # rule checks
```

On Windows use `run.bat` and `test.bat`.

See [`docs/TEST_NOTES.md`](docs/TEST_NOTES.md) for the reproducible victory and defeat runs.
