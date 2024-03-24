# Changelog

All notable changes to this project will be documented in this file. 


### 6.1 (2024-03-26)

- Trips from survey data are filtered slightly different
  - Previously trips that did not have the flag `E_WEG_GUELTIG`, where filtered out.
  - However, this flag is not considered in SrV reports, or the Berlin website that reports the modal share.
  - To be more in line with these reports, these trips are now not filtered as well.
  - This leads to slightly different mode shares from the previous version.

- Commercial traffic was updated to include recent changes
  - Temporal distribution was updated according to survey data
  - Separated car and freight/truck mode

### 6.0 (2024-02-20)

Initial release of the updated MATSim Open Berlin scenario. 
For more information, see the paper (tba.)
