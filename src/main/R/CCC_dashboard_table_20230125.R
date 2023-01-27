## loading packages
library(tidyr)
library(tidyverse)
library(lubridate)
library(viridis)
library(dplyr)
library(utils) 
#########################################################################################################
#########################################################################################################
# options(error=function() { traceback(2); if(!interactive()) quit("no", status = 1, runLast = FALSE) })
print("########################################################################################")
print("########################################################################################")
#########################################################################################################
#########################################################################################################
## expanding grid with measures

frame <- expand.grid(OePNV = c("carbonised","dekarbonisiert"),
                  kiezblocks = c("nein","stark"),
                  Fahrrad = c("nein","stark"), 
                  Autoverbot = c("nein","fossil","all"),
                  MautHundekopf = c("keine","nurFossil","alle","autofrei"),
                  MautAussenbezirke = c("keine","nurFossil","alle","autofrei"),
                  DRT = c("nein","nurAussenbezirke","ganzeStadt"),
                  ParkraumbewirtschaftungHundekopf = c("keine","Besucher_teuer_Anwohner_preiswert","Besucher_teuer_Anwohner_teuer"),
                  ParkraumbewirtschaftungAussenbezirke = c("keine","Besucher_teuer_Anwohner_preiswert","Besucher_teuer_Anwohner_teuer")
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
######################################################################################################### Dekarbonsierung OePNV

massnahme <- "OePNV"
auspraegung <- "dekarbonisiert"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.01,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.01,measures$"Kosten")

# no consequences on moving/non-moving traffic:
#measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic",measures$"traffic")
#measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking",measures$"parking")

########################################################################################################## Ausbau Radinfrastruktur

massnahme <- "Fahrrad"
auspraegung <- "stark"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.05,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

########################################################################################################## Superblocks/Kiezblocks

massnahme <- "kiezblocks"
auspraegung <- "ja"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" + 0.01,measures$"CO2")

#kaum Kosten:
#measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.25,measures$"Kosten")

measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" + 0.25,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" + 0.25,measures$"parking")

########################################################################################################## DRT

massnahme <- "DRT"
auspraegung <- "nurAussenbezirke"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

auspraegung <- "ganzeStadt"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

########################################################################################################## MautHundekopf

####PAVE
### in PAVE hatten wir als Zusatzmaßnahme zu DRT die variablen PKW-Kosten von 0,20 €/km auf 0,40 €/km und auf 0,60 €/km erhöht (also so etwas wie globale Distanzmaut)
### DRT wurde hier sowohl als Taxi im Hundekopf und zusätzlich als Pooling Berlin-weit angeboten,
### s. auch https://vsp.berlin/pave/3-combined/T200P100-000-p3-10 und S.215ff im PAVE Bericht (VSP-WP 21-30)
##
## CO2:      0,20€/km -> ~ -50%, 0,40€/km -> -75%
## traffic:  0,20€/km -> ~ -35% FzgKm, 0,40€/km -> -55% FzgKm
## Kosten:   3,5 bis 4 Millionen Euro Einnahmen (+) am Tag
## parking:  0,20€/km -> ~ -50% car modal split, 0,40€/km -> -75% car modal split (VSP WP 20-03 does not explicitly confirm but points in the same direction (only cares about nr of drt rides and shift from cars))


massnahme <- "MautHundekopf"
auspraegung <- "nurFossil"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" + 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" + 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" + 0.05,measures$"parking")

auspraegung <- "alle"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

auspraegung <- "autofrei"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

########################################################################################################## ParkraumbewirtschaftungHundekopf

massnahme <- "MautAussenbezirke"
auspraegung <- "nurFossil"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" + 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" + 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" + 0.05,measures$"parking")

auspraegung <- "alle"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

auspraegung <- "autofrei"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

########################################################################################################## ParkraumbewirtschaftungHundekopf

massnahme <- "ParkraumbewirtschaftungHundekopf"
auspraegung <- "Besucher_teuer_Anwohner_preiswert"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

auspraegung <- "Besucher_teuer_Anwohner_teuer"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

########################################################################################################## ParkraumbewirtschaftungAussenbezirke

massnahme <- "ParkraumbewirtschaftungAussenbezirke"
auspraegung <- "Besucher_teuer_Anwohner_preiswert"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

auspraegung <- "Besucher_teuer_Anwohner_teuer"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

##########################################################################################################
### number format "x.yz"
#options(digits = 3) 

### writing CSV file ## PATH FOR OUTPUT
write.csv(measures, "CCC_dashboard.csv", row.names=FALSE)
system("head -11 CCC_dashboard.csv")
