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


tollType <- "cordon" #use 'area', 'distance' or 'cordon'
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


toll <- "0"
  run0 <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
    filter(person %in% berliners$person)
  plot0 <- plotModalSplitPieChart(run0) + 
    ggtitle(str_c(area, "-", tollType, "-", toll))
  

toll <- "2.5"
run1 <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
  filter(person %in% berliners$person)
plot1 <- plotModalSplitPieChart(run1) + 
  ggtitle(str_c(area, "-", tollType, "-", toll))


toll <- "5.0"
run2 <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
  filter(person %in% berliners$person)
plot2 <- plotModalSplitPieChart(run2) + 
  ggtitle(str_c(area, "-", tollType, "-", toll))


toll <- "7.5"
run3 <- readTripsTable(str_c(root, "output-", area, "-", tollType, "/", str_sub(area,1,1), "-", str_sub(tollType, 1, 1), "-", toll)) %>% 
  filter(person %in% berliners$person) 
plot3 <- plotModalSplitPieChart(run3) + 
  ggtitle(str_c(area, "-", tollType, "-", toll))


grid.arrange(plot0,plot1,plot2,plot3,
             ncol=2,
             nrow = 2,
             top = str_c(area, ": ", tollType, " roadpricing") )