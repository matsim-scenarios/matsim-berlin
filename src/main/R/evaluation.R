
library(tidyverse)
library(patchwork)
library(scales)
library(viridis)
library(ggsci)

# trip distance groups
levels = c("0 - 1000", "1000 - 2000", "2000 - 5000", "5000 - 10000", "10000 - 20000", "20000+")
breaks = c(0, 1000, 2000, 5000, 10000, 20000, Inf)
order_modes <- c("walk", "bike", "pt","ride","car")


##################
# Read survey data
##################

households <- read_csv("../python/table-households.csv")

persons_ref <- read_csv("../python/table-unscaled-persons.csv") %>%
          left_join(households) %>%
          filter(region_type == 1) %>%
          filter(present_on_day == 1) %>%
        #  filter(mobile_on_day == 1) %>%
          filter(reporting_day <= 4)


# TODO: mobile and non mobile calculation
# 93.3% should be mobile
# TODO: person with invalid trips can not be considered

trips_ref <- read_csv("../python/table-trips.csv") %>%
          left_join(households) %>%
          filter(region_type == 1) %>%
          filter(day_of_week <= 4) %>%
          filter(valid == TRUE) %>%
          mutate(mode=main_mode) %>%
          mutate(dist_group = cut(gis_length * 1000, breaks=breaks, labels=levels))


per_day <- weighted.mean(persons_ref$n_trips, persons_ref$p_weight)

# number of residents
population <- 3.645 * 1e6

ref <- trips_ref %>%
  group_by(dist_group, mode) %>%
  summarise(trips=sum(t_weight)) %>%
  mutate(mode = fct_relevel(mode, order_modes)) %>%
  mutate(source = "ref")


# Mode share by distance group
ref <- ref %>%
  mutate(share=trips / sum(ref$trips)) %>%
  mutate(scaled_trips=per_day * population * share)

# Average mode share
aggr_ref <- ref %>%
  group_by(mode) %>%
  summarise(share=sum(share)) %>%  # assume shares sum to 1
  mutate(mode=fct_relevel(mode, order_modes))

##################
# Read simulation data
##################

f <- "~/Volumes/math-cluster/matsim-berlin/calibration/output/cadyts"
sim_scale <- 100/25

persons <- read_delim(list.files(f, pattern = "*.output_persons.csv.gz", full.names = T, include.dirs = F), delim = ";", trim_ws = T, 
                      col_types = cols(
                        person = col_character()
                      )) %>%
  #right_join(homes) %>%
  #st_as_sf(coords = c("home_x", "home_y"), crs = 25832) %>%
  #        st_as_sf(coords = c("first_act_x", "first_act_y"), crs = 25832) %>%
  #st_filter(shape)
  filter(str_starts(person, "berlin"))


trips <- read_delim(list.files(f, pattern = "*.output_trips.csv.gz", full.names = T, include.dirs = F), delim = ";", trim_ws = T, 
                    col_types = cols(
                      person = col_character()
                    )) %>%
  mutate(main_mode = longest_distance_mode) %>%
  semi_join(persons) %>%
  mutate(dist_group = cut(traveled_distance, breaks=breaks, labels=levels, right = F)) %>%
  filter(main_mode!="freight")

sim <- trips %>%
  group_by(dist_group, main_mode) %>%
  summarise(trips=n()) %>%
  mutate(mode = fct_relevel(main_mode, order_modes)) %>%
  mutate(scaled_trips=sim_scale * trips) %>%
  mutate(source = "sim")

##################
# Total modal split
##################

aggr <- sim %>%
  group_by(mode) %>%
  summarise(share=sum(trips) / sum(sim$trips)) %>%
  mutate(mode=fct_relevel(mode, order_modes))

p1_aggr <- ggplot(data=aggr_ref, mapping =  aes(x=1, y=share, fill=mode)) +
  labs(subtitle = "Survey data") +
  geom_bar(position="fill", stat="identity") +
  coord_flip() +
  geom_text(aes(label=scales::percent(share, accuracy = 0.1)), angle=90, size=6, position=position_fill(vjust=0.5)) +
  scale_fill_locuszoom() +
  theme_void() +
  theme(legend.position="none")

p2_aggr <- ggplot(data=aggr, mapping =  aes(x=1, y=share, fill=mode)) +
  labs(subtitle = "Simulation") +
  geom_bar(position="fill", stat="identity") +
  coord_flip() +
  geom_text(aes(label=scales::percent(share, accuracy = 0.1)), angle=90, size=6, position=position_fill(vjust=0.5)) +
  scale_fill_locuszoom() +
  theme_void() +
  theme(legend.position = "bottom")

combined <- p1_aggr / p2_aggr
combined + plot_layout(guides = "auto")


############
# Distance groups
############

d_ref <- ref %>% group_by(dist_group) %>%
    summarise(share=sum(share)) %>%
    mutate(source = "SrV")

d_sim <- sim %>% group_by(dist_group) %>%
    summarise(share=sum(trips) / sum(sim$trips)) %>%
    mutate(source = "sim")

ggplot(data=bind_rows(list(d_ref, d_sim)), aes(x=dist_group, y=share, fill=source)) +
  geom_bar(stat = "identity", position=position_dodge())
