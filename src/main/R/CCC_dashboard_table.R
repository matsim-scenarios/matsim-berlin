## loading packages
library(tidyr)
library(tidyverse)
library(lubridate)
library(viridis)
library(ggsci)
library(sf)
library(dplyr)
library(ggplot2)
library(matsim)
library(purrr)
library(gridExtra)
library(networkD3)
library(utils) 
#########################################################################################################
#########################################################################################################
## expanding grid with measures

frame <- expand.grid(pt = c("carbonised","decarbonised"),
                  kiezblocks = c("nein","stark"),
                  cycling = c("nein","stark"), 
                  DRT = c("nein","extra","Berlin-wide"),
                  car_price = c("nein","fossil","all"))

## adding output values: CO2, Kosten, Menge fließender Verkehr, Menge stehender Verkehr
## value in relation (1.00 = 100%)
CO2 = c(1.00)
Kosten = c(1.00)
traffic = c(1.00)
parking = c(1.00)
measures <- cbind(frame,CO2,Kosten,traffic,parking)

#########################################################################################################
## calculating CO2
measures$"CO2" <- ifelse(measures$"pt"=="carbonised",measures$"CO2" + 0.00,measures$"CO2")
measures$"CO2" <- ifelse(dmeasures$"pt"=="decarbonised",measures$"CO2" - 0.12,measures$"CO2")
measures$"CO2" <- ifelse(measures$"cycling"=="nein",measures$"CO2" + 0.05,measures$"CO2")
measures$"CO2" <- ifelse(measures$"cycling"=="stark",measures$"CO2" - 0.05,measures$"CO2")
measures$"CO2" <- ifelse(measures$"kiezblocks"=="nein",measures$"CO2" + 0.01,measures$"CO2")
measures$"CO2" <- ifelse(dmeasures$"kiezblocks"=="stark",measures$"CO2" - 0.01,measures$"CO2")
measures$"CO2" <- ifelse(measures$"DRT"=="nein",measures$"CO2" + 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"DRT"=="extra",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"DRT"=="Berlin-wide",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"car_price"=="nein",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"car_price"=="fossil",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"car_price"=="all",measures$"CO2" - 0.02,measures$"CO2")

#########################################################################################################
## calculating Kosten
measures$"Kosten" <- ifelse(measures$"pt"=="carbonised",measures$"Kosten" + 0.00,measures$"Kosten")
measures$"Kosten" <- ifelse(dmeasures$"pt"=="decarbonised",measures$"Kosten" - 0.25,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"cycling"=="nein",measures$"Kosten" + 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"cycling"=="stark",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"kiezblocks"=="nein",measures$"Kosten" + 0.25,measures$"Kosten")
measures$"Kosten" <- ifelse(dmeasures$"kiezblocks"=="stark",measures$"Kosten" - 0.25,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"DRT"=="nein",measures$"Kosten" + 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"DRT"=="extra",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"DRT"=="Berlin-wide",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"car_price"=="nein",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"car_price"=="fossil",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"car_price"=="all",measures$"Kosten" - 0.05,measures$"Kosten")

#########################################################################################################
## calculating amount of traffic
measures$"traffic" <- ifelse(measures$"pt"=="carbonised",measures$"traffic" + 0.00,measures$"traffic")
measures$"traffic" <- ifelse(dmeasures$"pt"=="decarbonised",measures$"traffic" - 0.25,measures$"traffic")
measures$"traffic" <- ifelse(measures$"cycling"=="nein",measures$"traffic" + 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"cycling"=="stark",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"kiezblocks"=="nein",measures$"traffic" + 0.25,measures$"traffic")
measures$"traffic" <- ifelse(dmeasures$"kiezblocks"=="stark",measures$"traffic" - 0.25,measures$"traffic")
measures$"traffic" <- ifelse(measures$"DRT"=="nein",measures$"traffic" + 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"DRT"=="extra",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"DRT"=="Berlin-wide",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"car_price"=="nein",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"car_price"=="fossil",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"car_price"=="all",measures$"traffic" - 0.05,measures$"traffic")

#########################################################################################################
## calculating parking
measures$"parking" <- ifelse(measures$"pt"=="carbonised",measures$"parking" + 0.00,measures$"parking")
measures$"parking" <- ifelse(dmeasures$"pt"=="decarbonised",measures$"parking" - 0.25,measures$"parking")
measures$"parking" <- ifelse(measures$"cycling"=="nein",measures$"parking" + 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"cycling"=="stark",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"kiezblocks"=="nein",measures$"parking" + 0.25,measures$"parking")
measures$"parking" <- ifelse(dmeasures$"kiezblocks"=="stark",measures$"parking" - 0.25,measures$"parking")
measures$"parking" <- ifelse(measures$"DRT"=="nein",measures$"parking" + 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"DRT"=="extra",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"DRT"=="Berlin-wide",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"car_price"=="nein",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"car_price"=="fossil",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"car_price"=="all",measures$"parking" - 0.05,measures$"parking")

#########################################################################################################
## number format "x.yz"
options(digits = 3) 
