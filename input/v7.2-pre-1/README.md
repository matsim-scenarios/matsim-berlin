# berlin-v7.2-pre-1

Run configs for the samples of the pre-release that are calibrated and published. They run the
scenario as it was published, reading every input over the network — no local pipeline run, no
checkout:

| config | sample | from |
| --- | --- | --- |
| `berlin-v7.2-pre-1-1pct.config.xml` | 1 % | ASC calibration trial 009 of ten |
| `berlin-v7.2-pre-1-3pct.config.xml` | 3 % | ASC calibration trial 010 of eleven |

Both are the run config the pipeline generated from the calibration result, with the calibrated
mode constants in them and the input paths pointing at

    shared-svn/matsim/scenarios/countries/de/berlin/berlin-v7.2-pre-1/output/

The population is the output population of the calibration trial the constants come from, as in
v7.1, where the configs read the output plans of the calibrated runs:
`asc-calib-1pct/runs/009/009.output_plans.xml.gz` and `asc-calib-3pct/runs/010/010.output_plans.xml.gz`.
The calibration chains its trials, each one starting from the output plans of an earlier one, so
these are the end of a 2000-iteration chain (1 %: initial plans → 001 → 003 → 005 → 009; 3 %:
initial plans → 001 → 003 → 006 → 010, 500 iterations each). The generated configs read
`berlin-v7.2-{1,3}pct.plans-initial.xml.gz` instead, the start of that chain, which the pipeline
derives from the cadyts run; that is the population the calibration starts from, not a calibrated
one, and runs with it start every activity exactly at its typical duration.

This is **not** a v7.2 release. The 10 % sample is not published yet, and the pre-release has
known issues; see the README of the published folder (commercial traffic generated at the
target sample size, cadyts near-inert at 1 %, sample specific mode constants).

## Running it

Main class `org.matsim.run.OpenBerlinScenarioWrapper`, program arguments:

    --config input/v7.2-pre-1/berlin-v7.2-pre-1-1pct.config.xml run

Environment variables:

| variable | needed for |
| --- | --- |
| `MATSIM_HTTP_USER`, `MATSIM_HTTP_PASSWORD` | **required** — the input files are on shared-svn, which needs a VSP login |
| `MATSIM_DECRYPTION_PASSWORD` | the emissions analysis only; the HBEFA files are encrypted, ask VSP for the password |

In an IDE, put them into the run configuration's environment; on the command line, export them
or pass them as `-D` system properties.

MATSim reads input files from URLs but does not authenticate, so without the first two every
input fails with HTTP 401. `HttpAuthentication` (in `org.matsim.run`) installs a JVM-wide
authenticator from those variables when the scenario starts, and does nothing when they are
unset — public URLs, as in the v7.0 and v7.1 configs, keep working without any credentials.
Once this folder is mirrored to public-svn, the URLs can be swapped and no login is needed.

Nothing is cached between runs, so each start downloads the inputs again: about 155 MB at 1 %
and about 420 MB at 3 %, most of it the output plans (every agent with its five plans and their
routes), and 22 MB the supply both samples share — network with pt, facilities, transit schedule,
transit vehicles, counts, vehicle types. To avoid the repeated
download, check the published folder out once

    svn co https://svn.vsp.tu-berlin.de/repos/shared-svn/matsim/scenarios/countries/de/berlin/berlin-v7.2-pre-1

and point the config at the local files instead — inside `output/` the paths are exactly the
ones in the config.

Both configs run 500 iterations, which is a cluster job rather than a laptop one: the 3 %
calibration trials took about 25 h each with 60 GB of heap. For a smoke test add
`--iterations 0`, which loads everything, simulates one day and writes the output directory;
that takes a few minutes at 1 %.
