library(tidyverse)
library(matsim)
library(readr)

options(scipen = 99)

FILE_DIR <- "C:/Users/ACER/Desktop/Uni/VSP/Berlin_6_x/"

version1 <- "cadyts"
version2 <- "cadyts.02"

network <- loadNetwork(filename = paste0(FILE_DIR, "analysis/", version1, ".output_network.xml.gz"))
linkstats_old <- readLinkStats(runId = version1, file = paste0(FILE_DIR, "analysis/", version1, ".output_linkstats.csv.gz"), sampleSize = 0.25)
linkstat <- readLinkStats(runId = version2, file = paste0(FILE_DIR, "analysis/", version2, ".output_linkstats.csv.gz"), sampleSize = 0.25)

linkstat <- linkstat %>%
  mutate(linkId = as.character(linkId))
linkstats_old <- linkstats_old %>%
  mutate(linkId = as.character(linkId))

counts <- readCounts(file = paste0(FILE_DIR, "analysis/berlin-v6.0-counts-car-vmz.xml.gz")) %>%
  mutate(loc_id = as.character(loc_id))
str(network$links)

join <- mergeCountsAndLinks(counts = counts, network = network, linkStats = list(linkstat), aggr_to = "day") %>%
  filter(!is.na(volume))

createCountScatterPlot(joinedFrame = join)
ggsave(filename = paste0(FILE_DIR, "Berlin_cadyts_scatterplot.png"))

quality <- processDtvEstimationQuality(joinedFrame = join)

ggplot(quality, aes(estimation, share, fill = type)) +
  
  geom_col() +
  
  labs(y = "Share", x = "Quality category") +
  
  facet_grid(src ~ type) +
  
  theme_bw() +
  
  theme(legend.position = "none", axis.text.x = element_text(angle = 90))

ggsave(filename = paste0(FILE_DIR, "Berlin_v6.x_estimation_quality.png"))


### Comparsion of simulation link speeds and speed measurements by count stations

counts_viz <- readCounts(file = paste0(FILE_DIR, "count-files/berlin-v6.0.counts_car.xml"))

join_viz <- mergeCountsAndLinks(counts = counts_viz, network = network, linkStats = list(linkstat), networkModes = "car", aggr_to = "day")

sim_speeds <- read_csv(file = paste0(FILE_DIR, "/analysis/", version2, ".output_speeds.csv.gz"), col_types = c("c")) %>%
  mutate(key = paste0(linkId, "-", time + 3600))

real_speeds <- read_csv2(file = "C:/Users/ACER/Desktop/Uni/VSP/Berlin_6_x/count-files/berlin-v6.0.avg_speed.csv", col_types = c("c", "n", "d", "d"))%>%
  mutate(key = paste0(id, "-", hour * 3600))

join_with_speeds <- join_viz %>%
  filter(!is.na(volume)) %>%
  left_join(real_speeds, by = c("loc_id" = "id")) %>%
  arrange(loc_id, hour) %>%
  left_join(sim_speeds, by = "key") %>%
  filter(!is.na(hour)) %>%
  select(-c(ends_with(".y"), h, starts_with("time"), key, freight_avg_speed, linkId)) %>%
  mutate(sim_car_avg_speed_kmh = avg_speed * 3.6)

summary(join_with_speeds)

speeds_summary <- join_with_speeds %>%
  group_by(type, hour) %>%
  summarise(avg_speed_count = mean(car_avg_speed),
            avg_speed_sim = mean(sim_car_avg_speed_kmh)) %>%
  pivot_longer(cols = c(avg_speed_count, avg_speed_sim), names_to = "src", names_prefix = "avg_speed_", values_to = "avg_speed")

ggplot(speeds_summary, aes(hour, avg_speed, color = src)) +
  
  geom_line() +
  
  labs(y = "Average speed", color = "Source", title = "Comparsion of measured velocities in MATSim and count stations") +
  
  facet_grid(. ~ type) +
  
  theme_bw() +
  
  theme(legend.position = "bottom")

ggsave(filename = paste0(FILE_DIR, "Berlin_v6.x_speeds_comparsion.png"))
