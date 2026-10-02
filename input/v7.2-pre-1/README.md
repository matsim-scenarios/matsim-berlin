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

Nothing is cached between runs, so each start downloads the inputs again: about 35 MB at 1 %
and just under 60 MB at 3 %, of which 22 MB is the supply both samples share — network with pt,
facilities, transit schedule, transit vehicles, counts, vehicle types. To avoid the repeated
download, check the published folder out once

    svn co https://svn.vsp.tu-berlin.de/repos/shared-svn/matsim/scenarios/countries/de/berlin/berlin-v7.2-pre-1

and point the config at the local files instead — inside `output/` the file names are exactly
the ones in the config.

Both configs run 500 iterations, which is a cluster job rather than a laptop one: the 3 %
calibration trials took about 25 h each with 60 GB of heap. For a smoke test add
`--iterations 0`, which loads everything, simulates one day and writes the output directory;
that takes a few minutes at 1 %.
