# Changelog

All notable changes to this project will be documented in this file. 

### 7.2 (unreleased)
- Plan selection uses **ChangeExpBeta** (the MATSim default) again; the pseudo-random trip error term is disabled

### 7.0 (2025-11)
- Major update to scoring function and parameters
  - The scoring now uses **tasteVariations** which assumes that each agent has a random preference towards each transport mode
  - Parameters of the scoring function and taste distributions have been estimated based on the survey data SrV2018
  - This approach uses **PseudoRandomScoringModule** to generate explicit random utility terms together with the **BestScore** plan selector to be consistent with random utility maximization theory
  - More details of this approach are described in the dissertation _From Data to Simulation: Advanced Methods for Agent-Based Transport Modeling and Mode Choice Calibration_
  
- Network: For links where traffic counts are available, the capacity is set to the daily maximum

### 6.4 (2025-01)
- Improved facility locations
  - OSM tag filtering has been improved to reduce number of wrongly assigned facility types
- Update work location assignment within Berlin
  - Work locations now use weighted sampling during location choice
  - The weight is based on an attraction factor and zone specific probabilities determined from survey data
- Updated GTFS schedule
  - The schedule is now based on the 2024-11-19
  - Note that there are major changes how stops and links between them are created
    - Stops having the same gtfs parent_id and route types are merged together, allowing agents to find better PT connections
    - The PT network is created with loop links (on each PT stop) instead of duplicating stops, which also improves connections
    - The Berlin Ringbahn is manually adjusted so that each train drives multiple loops the whole day
- New income calculation
  - In previous versions income was used directly as household income from the survey data
  - Now, the income is calculated as personal equivalent income, which is the household income divided by equivalent household size
    - See https://en.wikipedia.org/wiki/Equivalisation
  - Corresponding attributes have been added to the population file
- Bike mode updated and recalibrated
  - Bike is now routed on the network, which results in more realistic travel distances
  - Bikes are not simulated on the network, and no link events generated yet. This will likely be added in future versions.
  - The road network includes bike infrastructure and corresponding attributes
  - The bike infrastructure is not fully complete yet, and has to be carefully evaluated first for bike centric studies
  - The avg. bike speed has been set to match SrV2018 survey data (~10.3 km/h)
- New dashboards 
  - PT Transit viewer
  - Emissions
  - Noise 

### 6.3 (2024-07)
- Include additional trip analysis and updated dashboard
  - Mode share is now analyzed by age, income, employment, economic_status
- Updated population, which now include reference modes for certain persons
 - The reference modes represent modes that a person has actually used in reality
 - Allows to evaluate the quality of the model in terms of mode choice
- ReplanningAnnealer module is now activated by default

### 6.2 (2024-06)
- Updated network and gtfs schedule
    - Network is now based on late 2022 osm data
    - PT schedule is based in mid 2023, earlier data of this feed was not available
    - The network conversion now uses a SUMO converter, a microscopic representation of the network will be available as well
    - Free speed of the network has been calibrated as described in the paper "Road network free flow speed estimation using microscopic simulation and point-to-point travel times"
- Walking speed increased to match reference data
- Recalibration of mode constants (ASCs)
- Updated commercial traffic, especially the trip duration should be more realistic
- Added a run class for DRT scenarios
- Please note that the 6.3 version is released nearly at the same time
    - Because of that 6.2 was not calibrated to the same accuracy
    - Rather use the newest version, which also includes all features of 6.2

### 6.1.1 (2024-06-11)

- Fix ASCs in tbe 6.1 config
  - The calibrated ASCs have not been correctly copied to the 6.1 config, this is now fixed.
- All input files remain the same
  -  The existing output remains the same as well, because this run was using the correct ASC values

### 6.1 (2024-04-17)

- Trips from survey data are filtered slightly different
  - Previously, trips that did not have the flag `E_WEG_GUELTIG`, have been ignored.
  - However, this flag is not considered in SrV reports, or the Berlin website that reports the modal share.
  - To be more consistent with these reports, these trips are now considered as well.
  - This leads to slightly different mode shares compared to the previous version.

- Commercial traffic was updated to include recent changes
  - Temporal distribution was updated according to survey data
  - Separated car and freight/truck mode

### 6.0 (2024-02-20)

Initial release of the updated MATSim Open Berlin scenario. 
For more information, see the paper (tba.)
