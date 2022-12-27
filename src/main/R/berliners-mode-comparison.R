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


tollType <- "distance" #use 'area', 'distance' or 'cordon'
area <- "berlin" #use 'berlin' or 'hundekopf'


root<- "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/roadpricing/"
#### BERLINERS
berliners <- read.table(file=paste(root, "output-berlin-area/b-a-0/b-a-0.output_persons.csv.gz", sep =""),
                        header = TRUE,
                        sep = ";") %>% 
  filter(subpopulation == "person") %>% 
  filter(home.activity.zone == "berlin")



#tolls <- c("0", "5", "10", "25", "50")
#plots <- c()
#
#for(toll in tolls){
#  run <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
#    filter(person %in% berliners$person)
#  plot <- plotModalSplitPieChart(run)
#  append(plots, plot)
#}


toll <- "0.0"
  run0 <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
    filter(person %in% berliners$person)
  plot0 <- plotModalSplitPieChart(run0) + 
    ggtitle(str_c(area, "-", tollType, "-", toll))
  

toll <- "0.25"
run1 <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
  filter(person %in% berliners$person)
plot1 <- plotModalSplitPieChart(run1) + 
  ggtitle(str_c(area, "-", tollType, "-", toll))


toll <- "1.0"
run2 <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
  filter(person %in% berliners$person)
plot2 <- plotModalSplitPieChart(run2) + 
  ggtitle(str_c(area, "-", tollType, "-", toll))


toll <- "5.0"
run3 <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
  filter(person %in% berliners$person) 
plot3 <- plotModalSplitPieChart(run3) + 
  ggtitle(str_c(area, "-", tollType, "-", toll))


grid.arrange(plot0,plot1,plot2,plot3,
             ncol=2,
             nrow = 2,
             top = str_c(area, ": ", tollType, " roadpricing") )



  
  
  
  
  
  
  
  
  

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

###### 10 * DAILY RESIDENTIAL PARKING FEE = 0.28 €
plot0.28_l0 <- plotModalSplitPieChart(p0.28) + 
  ggtitle('non-residential: 0€/Std')

plot0.28_l1 <- plotModalSplitPieChart(p0.28_l1.0) + ggtitle('non-residential: 1€/Std')
plot0.28_l2 <- plotModalSplitPieChart(p0.28_l2.0) + ggtitle('non-residential: 2€/Std')
plot0.28_l4 <- plotModalSplitPieChart(p0.28_l4.0) + ggtitle('non-residential: 4€/Std')
plot0.28_l10 <- plotModalSplitPieChart(p0.28_l10.0) + ggtitle('non-residential: 10€/Std')
plot0.28_l20 <- plotModalSplitPieChart(p0.28_l20.0) + ggtitle('non-residential: 20€/Std')

grid.arrange(plot0.28_l0,
             plot0.28_l1,
             plot0.28_l2,
             plot0.28_l4,
             plot0.28_l10,
             plot0.28_l20,
             ncol=3, nrow = 2, top = "Residential parking cost 0.28 €/Tag")

###### 100 * DAILY RESIDENTIAL PARKING FEE = 2.80 €
plot2.8_l0 <- plotModalSplitPieChart(p2.8) + 
  ggtitle('non-residential: 0€/Std')

plot2.8_l1 <- plotModalSplitPieChart(p2.8_l1.0) + ggtitle('non-residential: 1€/Std')
plot2.8_l2 <- plotModalSplitPieChart(p2.8_l2.0) + ggtitle('non-residential: 2€/Std')
plot2.8_l4 <- plotModalSplitPieChart(p2.8_l4.0) + ggtitle('non-residential: 4€/Std')
plot2.8_l10 <- plotModalSplitPieChart(p2.8_l10.0) + ggtitle('non-residential: 10€/Std')
plot2.8_l20 <- plotModalSplitPieChart(p2.8_l20.0) + ggtitle('non-residential: 20€/Std')

grid.arrange(plot2.8_l0,
             plot2.8_l1,
             plot2.8_l2,
             plot2.8_l4,
             plot2.8_l10,
             plot2.8_l20,
             ncol=3, nrow = 2, top = "Residential parking cost 2.80 €/Tag")

###### 1000 * DAILY RESIDENTIAL PARKING FEE = 28.0 €

plot28.0_l0 <- plotModalSplitPieChart(p28.0) + 
  ggtitle('non-residential: 0€/Std')

plot28.0_l1 <- plotModalSplitPieChart(p28.0_l1.0) + ggtitle('non-residential: 1€/Std')
plot28.0_l2 <- plotModalSplitPieChart(p28.0_l2.0) + ggtitle('non-residential: 2€/Std')
plot28.0_l4 <- plotModalSplitPieChart(p28.0_l4.0) + ggtitle('non-residential: 4€/Std')
plot28.0_l10 <- plotModalSplitPieChart(p28.0_l10.0) + ggtitle('non-residential: 10€/Std')
plot28.0_l20 <- plotModalSplitPieChart(p28.0_l20.0) + ggtitle('non-residential: 20€/Std')

grid.arrange(plot28.0_l0,
             plot28.0_l1,
             plot28.0_l2,
             plot28.0_l4,
             plot28.0_l10,
             plot28.0_l20,
             ncol=3, nrow = 2, top = "Residential parking cost 28.00 €/Tag")
