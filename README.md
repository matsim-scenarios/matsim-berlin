# The MATSim Open Berlin Scenario

### About this project

This repository provides an open MATSim transport model for Berlin, provided by the [Transport Systems Planning and Transport Telematics group](https://www.vsp.tu-berlin.de) of [Technische Universität Berlin](http://www.tu-berlin.de).

### Simple things (without installing/running MATSim)

##### Movies

1. Go into the `scenarios` directory.  
1. Decide for a scenario that you find interesting (e.g. `berlin-v5.0-0.1pct-2018-06-18`) and go into that directory.
1. Inside there, look for an `output-*` directory that you find interesting and go into that directory.
1. Inside there, look for `movie-*` files.  You can't view them directly, but you there are various ways to download them, and you can view them then.  Try that.

##### Run VIA on output files

1. Do steps 1.-3. above.
1. Download `output_network.xml.gz` and `output_events.xml.gz`.  Best make sure that they do not uncompress, e.g. by "Download linked file as ...".
1. Get these files into VIA.  This can be achieved in various ways; one is to open VIA and then drag the files from a file browser into VIA.
1. Run VIA and enjoy.

### Cloning the repository and running MATSim

##### Initial stuff (needs to be done once)

1. Go to https://git-lfs.github.com and download the git lfs command line extension.
1. Type `git lfs install`.
1. Type `git clone https://github.com/matsim-vsp/matsim-berlin.git`

(We strongly advise using git from the command line, since in our experience the git lfs extension, which we really need, works by far best from the command line.)

This will result in a new `matsim-berlin` directory.  Memorize where you have put it.  You can move it, as a whole, to some other place.

##### Update your local clone of the repository.

1. Go into the `matsim-berlin` directory.
1. Type `git pull`

This will update your repository to the newest version.

##### Run MATSim

1. There should be a file directly in the `fem` directory with name approximately as `matsim-berlin-0.5.0-SNAPSHOT-jar-with-dependencies.jar`.
1. Double-click on that file (in a file system browser).  A simple GUI should open.
1. In the GUI, click on the "Choose" button for configuration file.  Navigate to one of the `scenario` directories and load one of the configuration files.
1. Increase memory in the GUI.
1. Press the "Start MATSim" button.  This should run MATSim.
1. "Open" the output directory.  You can drag files into VIA as was already done above.
1. "Edit..." (in the GUI) the config file.  Re-run MATSim.
