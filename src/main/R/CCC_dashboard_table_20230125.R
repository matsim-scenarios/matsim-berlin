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

mautFossil="mautFossil"

frame <- expand.grid(OePNV = c("base","dekarbonisiert"),
                  kiezblocks = c("base","stark"),
                  Fahrrad = c("base","stark"), 
                  fahrenderVerkehr = c("base",mautFossil,"MautFuerAlle","zeroEmissionsZone","autofrei"),
                  #fahrenderVerkehrAussenbezirke = c("base",mautFossil,"MautFuerAlle","zeroEmissionsZone","autofrei"),
                  DRT = c("base","nurAussenbezirke","ganzeStadt"),
                  ParkraumHundekopf = c("base","Besucher_teuer_Anwohner_preiswert","Besucher_teuer_Anwohner_teuer"),
                  ParkraumAussenbezirke = c("base","Besucher_teuer_Anwohner_preiswert","Besucher_teuer_Anwohner_teuer")
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

############################################
############################################
massnahme <- "Fahrrad"
auspraegung <- "stark"

# Annahme: Fahrrad nimmt 10% vom Autoverkehr weg.  Also z.B. 30% auf 27%.

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"*0.9,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten",measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic"*0.9,measures$"traffic")

measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking"*0.95,measures$"parking")
# Annahme: Jede zweite Person schafft ihr Auto ab.

########################################################################################################## Superblocks/Kiezblocks

massnahme <- "kiezblocks"
auspraegung <- "stark"

# Gleiche Annahme wie bei Fahrrad

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"*0.9,measures$"CO2")

#measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.25,measures$"Kosten")
# Annahme: kaum Kosten

measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic"*0.9,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking"*0.95,measures$"parking")
# Annahme: Jede zweite Person schafft ihr Auto ab.

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

########################################################################################################## fahrenderVerkehrHundekopf

####PAVE
### in PAVE hatten wir als Zusatzmaßnahme zu DRT die variablen PKW-Kosten von 0,20 €/km auf 0,40 €/km und auf 0,60 €/km erhöht (also so etwas wie globale Distanzmaut)
### DRT wurde hier sowohl als Taxi im Hundekopf und zusätzlich als Pooling Berlin-weit angeboten,
### s. auch https://vsp.berlin/pave/3-combined/T200P100-000-p3-10 und S.215ff im PAVE Bericht (VSP-WP 21-30)
##
## CO2:      0,20€/km -> ~ -50%, 0,40€/km -> -75%
## traffic:  0,20€/km -> ~ -35% FzgKm, 0,40€/km -> -55% FzgKm
## Kosten:   3,5 bis 4 Millionen Euro Einnahmen (+) am Tag
## parking:  0,20€/km -> ~ -50% car modal split, 0,40€/km -> -75% car modal split (VSP WP 20-03 does not explicitly confirm but points in the same direction (only cares about nr of drt rides and shift from cars))


############################################
############################################

massnahme <- "fahrenderVerkehr"

# --------------------------------------------

auspraegung <- "MautFuerAlle"
# 20ct/km

traffRed=0.5

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"*traffRed,measures$"CO2")

measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 4,measures$"Kosten")
# Was sind die bisherigen Kosten?  Das geht nicht in Prozent, oder??

measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic"*traffRed,measures$"traffic")
# DRT müsste irgendwie separat dazu kommen.

measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking"*traffRed,measures$"parking")
# (Auto-Abschaffung analog CO2-Reduktion)

# --------------------------------------------

auspraegung <- mautFossil

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"*0.5,measures$"CO2")
# ähnliche Wirkung auf wie "Maut für alle".  Wirkt intuitiv richtig, aber warum?

measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 2,measures$"Kosten")
# Eine Hälfte zahlt Maut, die andere wechselt auf nicht-fossiles Auto.

measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic"*0.75,measures$"traffic")

measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking"*0.25,measures$"parking")

# --------------------------------------------

auspraegung <- "zeroEmissionsZone"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"*0.01,measures$"CO2")

measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.01 ,measures$"Kosten")
# Schilder, Durchsetzung, etc.

measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic",measures$"traffic")

measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking",measures$"parking")

# --------------------------------------------

auspraegung <- "autofrei"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"*0.01,measures$"CO2")
measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten",measures$"Kosten")
measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic"*0.01,measures$"traffic")
measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking"*0.01,measures$"parking")

############################################
#############################################
#massnahme <- "fahrenderVerkehrHundekopf"
#auspraegung <- mautFossil

#measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"/2,measures$"CO2")
## 

#measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.05,measures$"Kosten")
#measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" + 0.05,measures$"traffic")
#measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" + 0.05,measures$"parking")

#auspraegung <- "MautFuerAlle"

#measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
#measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
#measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
#measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

#auspraegung <- "autofrei"

#measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
#measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
#measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
#measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

#############################################
#############################################
#massnahme <- "fahrenderVerkehrAussenbezirke"
#auspraegung <- mautFossil

#measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" + 0.02,measures$"CO2")
#measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.05,measures$"Kosten")
#measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" + 0.05,measures$"traffic")
#measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" + 0.05,measures$"parking")

#auspraegung <- "MautFuerAlle"

#measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
#measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
#measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
#measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

#auspraegung <- "autofrei"

#measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2" - 0.02,measures$"CO2")
#measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" - 0.05,measures$"Kosten")
#measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic" - 0.05,measures$"traffic")
#measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking" - 0.05,measures$"parking")

#############################################
############################################
massnahme <- "ParkraumHundekopf"
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

########################################################################################################## ParkraumAussenbezirke

massnahme <- "ParkraumAussenbezirke"
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

############################################
############################################
massnahme <- "OePNV"

# Alles oberhalb von hier sind Pkw-Emissionen.  Das ist so etwas wie 4
# mio t / yr.  Davon sind jetzt noch irgendwelche pct übrig.  Das multiplizieren wir jetzt mit 0.99, und tun dann noch 0.01 drauf oder auch nicht.

# ---

auspraegung <- "base"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"*0.99+0.01,measures$"CO2")

# ---
auspraegung <- "dekarbonisiert"

measures$"CO2" <- ifelse(measures[[massnahme]]==auspraegung,measures$"CO2"*0.99,measures$"CO2")

measures$"Kosten" <- ifelse(measures[[massnahme]]==auspraegung,measures$"Kosten" + 0.01,measures$"Kosten")

# no consequences on moving/non-moving traffic:
#measures$"traffic" <- ifelse(measures[[massnahme]]==auspraegung,measures$"traffic",measures$"traffic")
#measures$"parking" <- ifelse(measures[[massnahme]]==auspraegung,measures$"parking",measures$"parking")

############################################
##########################################################################################################
### number format "x.yz"
#options(digits = 1) 

### writing CSV file ## PATH FOR OUTPUT
write.csv(measures, "CCC_dashboard.csv", row.names=FALSE)