# Tools

Development scripts; none of them ship in the jar.

- `run_debloat_review.py` (`run-debloat-review.sh`): a fresh world with a server and a client, for looking around.
- `bench/profile_bench.py`: generates benchmark windows with one mod profile and reports the time per chunk.
  `bench/jfr*.py` read its flight recordings; `bench/worlddiff.py` compares two generated worlds.
- `origen_overrides.py`, `ru_nether_decoration.py`: regenerate data files under `src/main/resources`.
- `deadcode/`: the dead-code cutter used for the debloat.

The review and bench scripts copy a local server and client from `run/debloat-template/` (`launch.json`, `server/`,
`client/`), which is not in the repository; `launch.json` holds the machine's own paths and launch command.
