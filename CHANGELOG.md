# Changelog

All notable changes to this project will be documented in this file. 

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
