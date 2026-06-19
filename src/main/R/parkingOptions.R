library(apollo)
library(tidyverse)
library(dplyr)
library(patchwork)
library(networkD3)
library(sf) #=> geography
library(stringr)
library(ggalluvial)
library(reshape2)



persons <- read_delim("/Users/gregorr/Documents/work/respos/shared-svn/projects/matsim-berlin/data/SrV/2018/converted/table-persons.csv"
                      , delim = ",")


household <- read_delim("/Users/gregorr/Documents/work/respos/shared-svn/projects/matsim-berlin/data/SrV/2018/converted/table-households.csv",
                         delim = ",")


summary(household$car_parking)

table(household$car_parking, useNA = "ifany")

# Share of parking types
household %>%
  count(car_parking) %>%
  mutate(share = n / sum(n))


household %>%
  filter(car_parking != "na") %>%
  count(economic_status, car_parking, wt = h_weight) %>%
  group_by(economic_status) %>%
  mutate(share = n / sum(n)) %>%
  ggplot(aes(x = economic_status, y = share, fill = car_parking)) +
  geom_col() +
  scale_y_continuous(labels = scales::percent) +
  labs(
    x = "Economic status",
    y = "Share",
    fill = "Parking"
  ) +
  theme_minimal() +
  coord_flip()


household %>%
  filter(car_parking != "na") %>%
  count(region_type, car_parking, wt = h_weight) %>%
  group_by(region_type) %>%
  mutate(share = n / sum(n)) %>%
  ggplot(aes(x = region_type, y = share, fill = car_parking)) +
  geom_col() +
  scale_y_continuous(labels = scales::percent) +
  labs(
    x = "Region type",
    y = "Share",
    fill = "Parking"
  ) +
  theme_minimal()
