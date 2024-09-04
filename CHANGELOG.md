# Changelog

All notable changes to this project will be documented in this file. 

### 6.4 (WIP)
- Improved facility locations
  - OSM tag filtering has been improved to reduce number of wrongly assigned facility types

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
