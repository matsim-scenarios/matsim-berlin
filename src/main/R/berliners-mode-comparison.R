library(tidyverse)
library(dplyr)
library(matsim)
library(ggalluvial)
library(gridExtra)



distanceDistributionTable <- function(tripsTable){
  modes = levels(factor(tripsTable$main_mode))
  
  #This is a very bad way to do that, but I see no other way to get it done
  #Also filtering table into a new doesn't creates new objects in memory, so it works fast
  tripsTable_05km = tripsTable %>% filter(traveled_distance<=1000) %>% mutate(dist_cat = "0-1km")
  #tripsTable_1km = tripsTable %>% filter(traveled_distance>500 & traveled_distance<=1000  ) %>% mutate(dist_cat = "0.5-1km")
  tripsTable_2km = tripsTable %>% filter(traveled_distance>1000 & traveled_distance<=2000) %>% mutate(dist_cat = "1-2km")
  tripsTable_5km = tripsTable %>% filter(traveled_distance>2000 & traveled_distance<=5000) %>% mutate(dist_cat = "2-5km")
  tripsTable_10km = tripsTable %>% filter(traveled_distance>5000 & traveled_distance<=10*1000) %>% mutate(dist_cat = "5-10km")
  tripsTable_20km = tripsTable %>% filter(traveled_distance>10*1000 & traveled_distance<=20*1000) %>% mutate(dist_cat = "10-20km")
  tripsTable_50km = tripsTable %>% filter(traveled_distance>20*1000 & traveled_distance<=50*1000) %>% mutate(dist_cat = "20-50km")
  tripsTable_100km = tripsTable %>% filter(traveled_distance>50*1000 & traveled_distance<=100*1000) %>% mutate(dist_cat = "50-100km")
  tripsTable_100pluskm = tripsTable %>% filter(traveled_distance>100*1000) %>% mutate(dist_cat = "> 100km")
  
  tripsTable_result = rbind(tripsTable_05km,
                            #tripsTable_1km,
                            tripsTable_2km,
                            tripsTable_5km,
                            tripsTable_10km,
                            tripsTable_20km,
                            tripsTable_50km,
                            tripsTable_100km,
                            tripsTable_100pluskm)
  
  return(tripsTable_result)
}


compareModalDistanceDistribution <- function(tripsTable1,tripsTable2){

  distribution1 <- distanceDistributionTable(tripsTable1)
  distribution2 <- distanceDistributionTable(tripsTable2)
  
  tableWithCounts = distribution1 %>% count(main_mode,dist_cat) 
  tableWithCounts2 = distribution2 %>% count(main_mode,dist_cat)
  
  joined <- full_join(tableWithCounts, tableWithCounts2, by = c("main_mode", "dist_cat"))
  
  result <- joined %>% 
    replace_na( list(n.x = 0, n.y = 0) ) %>% 
    mutate(diff = n.y - n.x) %>% 
    select(main_mode, dist_cat, diff)
  
  plt = ggplot(result) +
    geom_col(aes(x = dist_cat,fill = main_mode, y = diff), position = position_dodge())+
    ggtitle("Difference in number of trips per travelling distance")
  
  return(plt)
}








##########################################################READ INPUT###########################################################

#### BERLINERS
berliners <- read.table(file="//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.0/res0.0.output_persons.csv.gz",
                        header = TRUE,
                        sep = ";") %>% 
  filter(subpopulation == "person") %>% 
  filter(home.activity.zone == "berlin")

##### BASE TRIPS
base <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.0") %>% 
  filter(person %in% berliners$person)
  

###### CURRENT DAILY RESIDENTIAL PARKING FEE = 0.028 €
p0.028 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.028") %>% 
  filter(person %in% berliners$person)

p0.028_l0.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.028-link0.0") %>% 
  filter(person %in% berliners$person)

p0.028_l1.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.028-link1.0") %>% 
  filter(person %in% berliners$person)

p0.028_l2.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.028-link2.0") %>% 
  filter(person %in% berliners$person)

p0.028_l4.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.028-link4.0") %>% 
  filter(person %in% berliners$person)

p0.028_l10.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.028-link10.0") %>% 
  filter(person %in% berliners$person)

p0.028_l20.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.028-link20.0") %>% 
  filter(person %in% berliners$person)





###### 10 * DAILY RESIDENTIAL PARKING FEE = 0.28 € ###################################################
p0.28 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.28") %>% 
  filter(person %in% berliners$person)

p0.28_l1.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.28-link1.0") %>% 
  filter(person %in% berliners$person)

p0.28_l2.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.28-link2.0") %>% 
  filter(person %in% berliners$person)

p0.28_l4.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.28-link4.0") %>% 
  filter(person %in% berliners$person)

p0.28_l10.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.28-link10.0") %>% 
  filter(person %in% berliners$person)

p0.28_l20.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res0.28-link20.0") %>% 
  filter(person %in% berliners$person)



###### 100 * DAILY RESIDENTIAL PARKING FEE = 2.8 €
p2.8 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res2.8") %>% 
  filter(person %in% berliners$person)

p2.8_l1.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res2.8-link1.0") %>% 
  filter(person %in% berliners$person)

p2.8_l2.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res2.8-link2.0") %>% 
  filter(person %in% berliners$person)

p2.8_l4.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res2.8-link4.0") %>% 
  filter(person %in% berliners$person)

p2.8_l10.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res2.8-link10.0") %>% 
  filter(person %in% berliners$person)

p2.8_l20.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res2.8-link20.0") %>% 
  filter(person %in% berliners$person)


###### 1000 * DAILY RESIDENTIAL PARKING FEE = 28 €
p28.0 <- readTripsTable("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/parkingCosts/output/res28.0") %>% 
  filter(person %in% berliners$person)




##########################################################PLOT###########################################################

plotAverageTravelWait(base)
plotTripsByDistance(base)
plotTripDistanceByMode(base)

###### CURRENT DAILY RESIDENTIAL PARKING FEE = 0.028 €
p1 <- plotModalShiftSankey(base, p0.028) +
  ggtitle('Aktuell: 0.028 €/Tag')

p12 <- plotModalSplitPieChart(p0.028) + 
  ggtitle('Aktuell: 0.028 €/Tag')

p13 <- compareModalDistanceDistribution(base, p0.028) + 
  ggtitle('Aktuell: 0.028 €/Tag')

###### 10 * DAILY RESIDENTIAL PARKING FEE = 0.28 €
p2 <- plotModalShiftSankey(base, p0.28) +
  ggtitle('10fach: 0.28 €/Tag')

p22 <- plotModalSplitPieChart(p0.28) + 
  ggtitle('10fach: 0.28 €/Tag')

p23 <- compareModalDistanceDistribution(base, p0.28) + 
  ggtitle('Aktuell: 0.28 €/Tag')

###### 100 * DAILY RESIDENTIAL PARKING FEE = 2.8 €
p3 <- plotModalShiftSankey(base, p2.8) +
  ggtitle('100fach: 2.8 €/Tag')

p32 <- plotModalSplitPieChart(p2.8) + 
  ggtitle('100fach: 2.8 €/Tag')

p33 <- compareModalDistanceDistribution(base, p2.8) + 
  ggtitle('Aktuell: 2.8 €/Tag')

###### 1000 * DAILY RESIDENTIAL PARKING FEE = 28 €
p4 <- plotModalShiftSankey(base, p28.0) +
  ggtitle('1000fach: 28 €/Tag')

p42 <- plotModalSplitPieChart(p28.0) + 
  ggtitle('1000fach: 28 €/Tag')

p43 <- compareModalDistanceDistribution(base, p28.0) + 
  ggtitle('Aktuell: 28 €/Tag')


######## COMBINED PLOT
grid.arrange(p1,p2,p3,p4, ncol=2, nrow = 2, top = "Einführung von Anwohnerparkkosten im Berlin-Modell")
grid.arrange(p12,p22,p32,p42, ncol=2, nrow = 2, top = "Einführung von Anwohnerparkkosten im Berlin-Modell")
grid.arrange(p13,p23,p33,p43, ncol=2, nrow = 2, top = "Einführung von Anwohnerparkkosten im Berlin-Modell")



###########################################
### COMPARE MODAL SPLIT ACROSS NON-RESIDENTIAL PARKING FEES

###### CURRENT DAILY RESIDENTIAL PARKING FEE = 0.028 €
plot0.028 <- plotModalSplitPieChart(p0.028)
  +  ggtitle('0 €/Std')
plot0.028_l0 <- plotModalSplitPieChart(p0.028_l1.0)
  +  ggtitle('0 €/Std')
plot0.028_l1 <- plotModalSplitPieChart(p0.028_l1.0)
  +  ggtitle('1 €/Std')
plot0.028_l2 <- plotModalSplitPieChart(p0.028_l2.0)
  +  ggtitle('2 €/Std')
plot0.028_l4 <- plotModalSplitPieChart(p0.028_l4.0)
  +  ggtitle('4 €/Std')
plot0.028_l10 <- plotModalSplitPieChart(p0.028_l10.0)
  +  ggtitle('10 €/Std')
plot0.028_l20 <- plotModalSplitPieChart(p0.028_l20.0)
  +  ggtitle('20 €/Std')

grid.arrange(plot0.028,
             plot0.028_l0,
             plot0.028_l1,
             plot0.028_l2,
             plot0.028_l4,
             plot0.028_l10,
             #plot0.028_l20,
             ncol=3, nrow = 2, top = "Residential parking cost 0.28 €/Tag")


###### 10 * DAILY RESIDENTIAL PARKING FEE = 0.28 €
plot0.28_l0 <- p22

plot0.28_l1 <- plotModalSplitPieChart(p0.28_l1.0)
plot0.28_l2 <- plotModalSplitPieChart(p0.28_l2.0)
plot0.28_l4 <- plotModalSplitPieChart(p0.28_l4.0)
plot0.28_l10 <- plotModalSplitPieChart(p0.28_l10.0)
plot0.28_l20 <- plotModalSplitPieChart(p0.28_l20.0)

grid.arrange(plot0.28_l0,
             plot0.28_l1,
             plot0.28_l2,
             plot0.28_l4,
             plot0.28_l10,
             plot0.28_l20,
             ncol=3, nrow = 2, top = "Residential parking cost 0.28 €/Tag")

