# The MATSim Open Berlin Scenario

### About this project

This repository provides an open MATSim transport model for Berlin, provided by the [Transport Systems Planning and Transport Telematics group](https://www.vsp.tu-berlin.de) of [Technische Universität Berlin](http://www.tu-berlin.de).

Currently, there are two versions of the MATSim Open Berlin model:

#### 10pct scenario (scenarios/berlin-v5.2-10pct)

This scenario contains a 10pct sample of the Greater Berlin population; road capacities are accordingly reduced. The scenario is calibrated taking into consideration the traffic counts, modal split and mode-specific trip distance distributions.

#### 1pct scenario (scenarios/berlin-v5.2-1pct)

This scenario contains a 1pct sample of the Greater Berlin population; road capacities are accordingly reduced. This scenario was not (!) calibrated and should only be used for testing purposes or pre-studies!

### Simple things (without installing/running MATSim)

##### Movies

Go to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/output-berlin-v5.2-10pct/ and look for `movie-*` files.  You can't view them directly, but you there are various ways to download them, and you can view them then.  Try that.

1. Go into the `scenarios` directory.  
1. Decide for a scenario that you find interesting (e.g. `berlin-v5.0-0.1pct-2018-06-18`) and go into that directory.
1. Inside there, look for an `output-*` directory that you find interesting and go into that directory.
1. Inside there, look for `movie-*` files.  You can't view them directly, but you there are various ways to download them, and you can view them then.  Try that.

##### Run VIA on output files

1. Get VIA from https://www.simunto.com/via/.
1. For the 10pct scenario, go to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/output-berlin-v5.2-10pct/. For the 1pct scenario, go to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/.
1. Download `output_network.xml.gz` and `output_events.xml.gz`.  Best make sure that they do not uncompress, e.g. by "Download linked file as ...".
1. Get these files into VIA.  This can be achieved in various ways; one is to open VIA and then drag the files from a file browser into VIA.
1. Run VIA and enjoy.

### Downloading the repository

1. Click on `Clone or download` and then on `Download ZIP`.
1. Unzip the repository.

### Cloning the repository

##### Initial stuff (needs to be done once)

1. Install git for the command line.
1. Type `git clone https://github.com/matsim-vsp/matsim-berlin.git` in the command line.

(Or use your IDE, e.g. Eclipse, IntelliJ, to clone the repository.)

This will result in a new `matsim-berlin` directory.  Memorize where you have put it.  You can move it, as a whole, to some other place.

##### Update your local clone of the repository.

1. Go into the `matsim-berlin` directory.
1. Type `git pull`

(Or use your IDE, e.g. Eclipse, IntelliJ, to update the repository.)

This will update your repository to the newest version.

### Run the MATSim Berlin scenario
(Requires either cloning or downloading the repository.)

#### ... using a runnable jar file (coming soon!)
1. There should be a file directly in the `matsim-berlin` directory with name approximately as `matsim-berlin-5.2-SNAPSHOT-jar-with-dependencies.jar`.
1. Double-click on that file (in a file system browser).  A simple GUI should open.
1. In the GUI, click on the "Choose" button for configuration file.  Navigate to one of the `scenario` directories and load one of the configuration files.
1. Increase memory in the GUI.
1. Press the "Start MATSim" button.  This should run MATSim.
1. "Open" the output directory.  You can drag files into VIA as was already done above.
1. "Edit..." (in the GUI) the config file.  Re-run MATSim.

#### ... using an IDE, e.g. Eclipse, IntelliJ
1. Set up the project in your IDE.
1. Make sure the project is configured as maven project.
1. Run the JAVA class `src/main/java/org/matsim/run/RunBerlinScenario.java`.
1. "Open" the output directory.  You can drag files into VIA as was already done above.
1. Edit the config file or adjust the run class. Re-run MATSim.

### More information

For more information about MATSim, see here: https://www.matsim.org/.