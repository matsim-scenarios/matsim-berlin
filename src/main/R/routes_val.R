
library(tidyverse)

setwd("~/Development/matsim-scenarios/matsim-berlin")

files <- fs::dir_ls(path = ".", glob = "routes-validation-api-*.csv")
m05 <- read_csv("05-eval.csv") %>%
    mutate(speed=3.6*dist/travel_time) %>%
    mutate(api="MATSim @ 0.5") %>%
    mutate(hour=21) %>%
    mutate(detour=dist/beeline_dist)


m09 <- read_csv("09-eval.csv") %>%
  mutate(speed=3.6*dist/travel_time) %>%
  mutate(api="MATSim @ 0.9") %>%
  mutate(hour=21)
  
mopt <- read_csv("network-opt-eval.csv") %>%
  mutate(speed=3.6*dist/travel_time) %>%
  mutate(api="MATSim @ opt") %>%
  mutate(hour=21)

val <- read_csv("routes-validation.csv")

df <- read_csv(files)

df <- df %>%
    filter(api != "Google" & api != "here") %>%
    mutate(api=ifelse(api=="woosmap", "here", api)) %>%
    mutate(speed=3.6*dist/travel_time) %>%
    mutate(across(where(is.numeric), ~na_if(., Inf))) %>%
    drop_na()

aggr <- df %>% 
  group_by(hour, api) %>% 
  summarise(speed=mean(speed))

ggplot(aggr, aes(x=hour, y=speed, color=api)) +
    labs(title = "Avg. speed per hour") +
    geom_line(size=1.5)

std <- read_csv("routes-std.csv") %>%
    filter(hour!=22)

ggplot(filter(bind_rows(df, ms), hour==21), aes(x=api, y=speed, fill=api)) + 
  labs(title = "Speed at 21:00") + 
  geom_violin(trim = T) +
  geom_boxplot(width=0.1, fill="white")

ggplot(filter(df, hour==8), aes(x=api, y=speed, fill=api)) + 
  labs(title = "Avg. speed at 08:00") + 
  geom_violin(trim = T) +
  geom_boxplot(width=0.1, fill="white")

ggplot(filter(bind_rows(df, ms), hour==21), aes(x=dist, y=travel_time, color=api)) +
  labs(title = "Travel time vs. distance at 21:00") +
  geom_point(size=0.3) +
  xlim(2000, 20000) +
  ylim(200, 2000)

aggr <- std %>% group_by(hour) %>%
                summarise(std=mean(std), min=mean(min), max=mean(max), mean=mean(mean))

ggplot(aggr, aes(x=hour, y=std)) +
  geom_line()


ggplot(aggr, aes(x=hour)) +
  ylab("km/h") +
  geom_line(mapping = aes(y=min)) + 
  geom_line(mapping = aes(y=mean)) + 
  geom_line(mapping = aes(y=max))

#########


trips <- read_csv("src/main/python/table-trips.csv") %>%
      mutate(speed=gis_length / (duration/60)) %>%
      mutate(across(where(is.numeric), ~na_if(., Inf))) %>%
      drop_na()


trips %>% 
  group_by(main_mode) %>% 
  summarise(speed=mean(speed))

######

capacity <- read_csv("dtv_links_capacity.csv")
sum(capacity$is_valid, na.rm = TRUE) / length(capacity$is_valid)


ft <- read_csv("input/sumo.net-edges.csv.gz")

######

freight <- read_csv("count_error_by_hour.csv")
freight_h <- read_csv("count_comparison_by_hour.csv") %>% group_by(hour) %>%
              summarise(observed=mean(observed_traffic_volume), simulated=mean(simulated_traffic_volume))

ggplot(freight, aes(x=hour)) +
    geom_line(aes(y=mean_bias))

ggplot(freight_h, aes(x=hour)) + 
    labs(title = "Observed vs. simulated freight counts") +
    ylab("Mean vehicles /h") +
    geom_line(aes(y=observed, color="observed"), size=2) +
    geom_line(aes(y=simulated, color="simulated"), size=2)
