
# Scenario Generation

This document provides a brief overview of the scenario generation process and how to use it.

Most of these steps are also automated in the `Makefile` in the root directory of the repository.

## Prerequisites

### Downloading Population data

See package `org.matsim.prepare.download` for downloading population data from regionalstatistik.de for Brandenburg and [Kommunalatlas](https://instantatlas.statistik-berlin-brandenburg.de/instantatlas/interaktivekarten/kommunalatlas/atlas.html)

The Makefile references the downloaded files in the svn directory. (https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/matsim-germany/regionalstatistik/)
Re-downloading is normally not necessary.


### Install Make

The process is automated using the `Makefile` in the root directory of the repository. This requires `make` to be installed on your system.


## Running scenario generation

The first step prepares the network and population plans needed for (counts) calibration:
This step also prepares the commercial traffic and can take longer if it is not already prepared.
    
```bash
make prepare-calibration
```

TODO: Add more details on the calibration process


```bash
make prepare-initial
```


```bash
make prepare
```


### Mode calibration

