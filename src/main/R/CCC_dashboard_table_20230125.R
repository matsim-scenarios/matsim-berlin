## loading packages
library(tidyr)
library(tidyverse)
library(lubridate)
library(viridis)
library(dplyr)
library(utils) 
#########################################################################################################
#########################################################################################################
## expanding grid with measures

frame <- expand.grid(ÖPNV = c("carbonised","decarbonised"),
                  kiezblocks = c("nein","stark"),
                  Fahrrad = c("nein","stark"), 
                  Autoverbot = c("nein","fossil","all"),
                  Maut = c("niedrig","mittel","hoch"),
                  DRT = c("nein","extra","Berlin-wide"),
                  Parkraum = c("niedrig","mittel","hoch"),
                  Anwohner = c("niedrig","mittel","hoch")
                  )

## adding output values: CO2, Kosten, Menge fließender Verkehr, Menge stehender Verkehr
## value in relation (1.00 = 100%)
CO2 = c(1.00)
Kosten = c(1.00)
traffic = c(1.00)
parking = c(1.00)
measures <- cbind(frame,CO2,Kosten,traffic,parking)
#########################################################################################################
#########################################################################################################

## MASSNAHMEN
######################################################################################################### Dekarbonsierung ÖPNV

measures$"CO2" <- ifelse(measures$"ÖPNV"=="carbonised",measures$"CO2" + 0.00,measures$"CO2")
measures$"CO2" <- ifelse(measures$"ÖPNV"=="decarbonised",measures$"CO2" - 0.12,measures$"CO2")
measures$"Kosten" <- ifelse(measures$"ÖPNV"=="carbonised",measures$"Kosten" + 0.00,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"ÖPNV"=="decarbonised",measures$"Kosten" - 0.25,measures$"Kosten")
measures$"traffic" <- ifelse(measures$"ÖPNV"=="carbonised",measures$"traffic" + 0.00,measures$"traffic")
measures$"traffic" <- ifelse(measures$"ÖPNV"=="decarbonised",measures$"traffic" - 0.25,measures$"traffic")
measures$"parking" <- ifelse(measures$"ÖPNV"=="carbonised",measures$"parking" + 0.00,measures$"parking")
measures$"parking" <- ifelse(measures$"ÖPNV"=="decarbonised",measures$"parking" - 0.25,measures$"parking")

######################################################################################################### Ausbau Radinfrastruktur

measures$"CO2" <- ifelse(measures$"Fahrrad"=="nein",measures$"CO2" + 0.05,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Fahrrad"=="stark",measures$"CO2" - 0.05,measures$"CO2")
measures$"Kosten" <- ifelse(measures$"Fahrrad"=="nein",measures$"Kosten" + 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Fahrrad"=="stark",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures$"Fahrrad"=="nein",measures$"traffic" + 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Fahrrad"=="stark",measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures$"Fahrrad"=="nein",measures$"parking" + 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Fahrrad"=="stark",measures$"parking" - 0.05,measures$"parking")

######################################################################################################### Superblocks/Kiezblocks

measures$"CO2" <- ifelse(measures$"kiezblocks"=="nein",measures$"CO2" + 0.01,measures$"CO2")
measures$"CO2" <- ifelse(measures$"kiezblocks"=="stark",measures$"CO2" - 0.01,measures$"CO2")
measures$"Kosten" <- ifelse(measures$"kiezblocks"=="nein",measures$"Kosten" + 0.25,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"kiezblocks"=="stark",measures$"Kosten" - 0.25,measures$"Kosten")
measures$"traffic" <- ifelse(measures$"kiezblocks"=="nein",measures$"traffic" + 0.25,measures$"traffic")
measures$"traffic" <- ifelse(measures$"kiezblocks"=="stark",measures$"traffic" - 0.25,measures$"traffic")
measures$"parking" <- ifelse(measures$"kiezblocks"=="nein",measures$"parking" + 0.25,measures$"parking")
measures$"parking" <- ifelse(measures$"kiezblocks"=="stark",measures$"parking" - 0.25,measures$"parking")

######################################################################################################### Autoverbote

measures$"CO2" <- ifelse(measures$"Autoverbot"=="nein",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Autoverbot"=="fossil",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Autoverbot"=="all",measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures$"Autoverbot"=="nein",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Autoverbot"=="fossil",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Autoverbot"=="all",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures$"Autoverbot"=="nein",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Autoverbot"=="fossil",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Autoverbot"=="all",measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures$"Autoverbot"=="nein",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Autoverbot"=="fossil",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Autoverbot"=="all",measures$"parking" - 0.05,measures$"parking")

######################################################################################################### Maut

###PAVE
## in PAVE hatten wir als Zusatzmaßnahme zu DRT die variablen PKW-Kosten von 0,20 €/km auf 0,40 €/km und auf 0,60 €/km erhöht (also so etwas wie globale Distanzmaut)
## DRT wurde hier sowohl als Taxi im Hundekopf und zusätzlich als Pooling Berlin-weit angeboten,
## s. auch https://vsp.berlin/pave/3-combined/T200P100-000-p3-10 und S.215ff im PAVE Bericht (VSP-WP 21-30)
#
# CO2:      0,20€/km -> ~ -50%, 0,40€/km -> -75%
# traffic:  0,20€/km -> ~ -35% FzgKm, 0,40€/km -> -55% FzgKm
# Kosten:   3,5 bis 4 Millionen Euro Einnahmen (+) am Tag
# parking:  0,20€/km -> ~ -50% car modal split, 0,40€/km -> -75% car modal split (VSP WP 20-03 does not explicitly confirm but points in the same direction (only cares about nr of drt rides and shift from cars))

measures$"CO2" <- ifelse(measures$"Maut"=="nein",measures$"CO2" + 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Maut"=="extra",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Maut"=="Berlin-wide",measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures$"Maut"=="nein",measures$"Kosten" + 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Maut"=="extra",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Maut"=="Berlin-wide",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures$"Maut"=="nein",measures$"traffic" + 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Maut"=="extra",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Maut"=="Berlin-wide",measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures$"Maut"=="nein",measures$"parking" + 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Maut"=="extra",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Maut"=="Berlin-wide",measures$"parking" - 0.05,measures$"parking")

######################################################################################################### DRT

measures$"CO2" <- ifelse(measures$"DRT"=="nein",measures$"CO2" + 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"DRT"=="extra",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"DRT"=="Berlin-wide",measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures$"DRT"=="nein",measures$"Kosten" + 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"DRT"=="extra",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"DRT"=="Berlin-wide",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures$"DRT"=="nein",measures$"traffic" + 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"DRT"=="extra",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"DRT"=="Berlin-wide",measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures$"DRT"=="nein",measures$"parking" + 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"DRT"=="extra",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"DRT"=="Berlin-wide",measures$"parking" - 0.05,measures$"parking")

######################################################################################################### Parkraumbewirtschaftung

measures$"CO2" <- ifelse(measures$"Parkraum"=="niedrig",measures$"CO2" + 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Parkraum"=="mittel",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Parkraum"=="hoch",measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures$"Parkraum"=="niedrig",measures$"Kosten" + 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Parkraum"=="mittel",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Parkraum"=="hoch",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures$"Parkraum"=="niedrig",measures$"traffic" + 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Parkraum"=="mittel",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Parkraum"=="hoch",measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures$"Parkraum"=="niedrig",measures$"parking" + 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Parkraum"=="mittel",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Parkraum"=="Bhoch",measures$"parking" - 0.05,measures$"parking")

######################################################################################################### Anwohnerparkausweis

measures$"CO2" <- ifelse(measures$"Anwohner"=="niedrig",measures$"CO2" + 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Anwohner"=="mittel",measures$"CO2" - 0.02,measures$"CO2")
measures$"CO2" <- ifelse(measures$"Anwohner"=="hoch",measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures$"Anwohner"=="niedrig",measures$"Kosten" + 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Anwohner"=="mittel",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"Kosten" <- ifelse(measures$"Anwohner"=="hoch",measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures$"Anwohner"=="niedrig",measures$"traffic" + 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Anwohner"=="mittel",measures$"traffic" - 0.05,measures$"traffic")
measures$"traffic" <- ifelse(measures$"Anwohner"=="hoch",measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures$"Anwohner"=="niedrig",measures$"parking" + 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Anwohner"=="mittel",measures$"parking" - 0.05,measures$"parking")
measures$"parking" <- ifelse(measures$"Anwohner"=="Bhoch",measures$"parking" - 0.05,measures$"parking")

#########################################################################################################

#########################################################################################################
## number format "x.yz"
options(digits = 3) 

## writing CSV file ## PATH FOR OUTPUT
write.csv(measures, "/Users/mkreuschnervsp/Desktop/CCC_dashboard.csv", row.names=FALSE)

