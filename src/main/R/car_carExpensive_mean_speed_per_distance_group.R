library(tidyverse)
library(lubridate)

trips <- read_csv2(file="C:/Users/Simon/Desktop/wd/2026-09-07/berlin-v6.4-3pct-additional-car-mode-case-distance-cost--0.0003-sigma-3.0-3.0-mean-0.0.output_trips.csv.gz")

dist_levels <- c(
  "0.0 - 999.999999999999",
  "1000.0 - 1999.999999999999",
  "2000.0 - 4999.999999999999",
  "5000.0 - 9999.999999999998",
  "10000.0 - 20000.0",
  "20000.0 - 1.7976931348623157E308"
)

trips <- trips %>% 
  filter(!str_detect(person, "commercial")) %>% 
  filter(!str_detect(person, "goodsTraffic")) %>%
  filter(!str_detect(person, "freight")) %>%
  mutate(trav_time_s = as.numeric(seconds(trav_time))) %>% 
  mutate(speed_m_s = traveled_distance / trav_time_s) %>% 
  mutate(speed_km_h = speed_m_s * 3.6) %>% 
  mutate(distance_group = cut(traveled_distance,
                              breaks = c(0,
                                         999.999999999999,
                                         1999.999999999999,
                                         4999.999999999999,
                                         9999.999999999998,
                                         20000,
                                         Inf
                              ),
                              labels = dist_levels,
                              include.lowest = TRUE,
                              right = TRUE
  )
  )

car <- trips %>% 
  filter(main_mode=="car")

mean_speed_car <- mean(car$speed_km_h)

mean_speed_by_distance_car <- car %>%
  group_by(distance_group) %>%
  summarise(
    car_mean_speed_km_h = mean(speed_km_h, na.rm = TRUE),
    car_n_trips = n(),
    car_share_trips = n() / nrow(car),
    .groups = "drop"
  )

carExpensive <- trips %>% 
  filter(main_mode=="carExpensive")

mean_speed_carExpensive <- mean(carExpensive$speed_km_h)

mean_speed_by_distance_carExpensive <- carExpensive %>%
  group_by(distance_group) %>%
  summarise(
    carExpensive_mean_speed_km_h = mean(speed_km_h, na.rm = TRUE),
    carExpensive_n_trips = n(),
    carExpensive_share_trips = n() / nrow(carExpensive),
    .groups = "drop"
  )

mean_speed_by_distance <- inner_join(mean_speed_by_distance_car, mean_speed_by_distance_carExpensive, by="distance_group")
