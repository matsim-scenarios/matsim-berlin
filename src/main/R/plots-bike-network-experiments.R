library(tidyverse)
library(ggokabeito)

###################################### tt per dist group plots #############################################################################################################

# read data from pt fare cases
tt_per_dist_group_base <- read_csv(file="//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/base-case-ctd/output-berlin-v6.4-3pct-base-case-ctd/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "base")
tt_per_dist_group_teleported <- read_csv(file="//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-teleported-only/output-berlin-v6.4-3pct-bike-teleported/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "teleported")
tt_per_dist_group_qsim0.0 <- read_csv(file="//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.0/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.0")
tt_per_dist_group_qsim0.01 <- read_csv(file="//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.01/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.01")
tt_per_dist_group_qsim0.1 <- read_csv(file="//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.1/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.1")
tt_per_dist_group_qsim0.2 <- read_csv(file="//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.2/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.2")
tt_per_dist_group_qsim0.3 <- read_csv(file="//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.3/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.3")

distGroups <- unique(tt_per_dist_group_base$dist_group)

dist_levels <- c(
  "0.0 - 999.999999999999",
  "1000.0 - 1999.999999999999",
  "2000.0 - 4999.999999999999",
  "5000.0 - 9999.999999999998",
  "10000.0 - 20000.0",
  "20000.0 - 1.7976931348623157E308"
)

# Combine datasets
combined <- bind_rows(tt_per_dist_group_base, tt_per_dist_group_teleported, tt_per_dist_group_qsim0.0, tt_per_dist_group_qsim0.01, tt_per_dist_group_qsim0.1, tt_per_dist_group_qsim0.2, tt_per_dist_group_qsim0.3) %>%
  mutate(dist_group = factor(dist_group, levels = dist_levels)) %>%
  mutate(case = factor(case, levels = unique(case))) %>%
  mutate(mean_tt_min = round(mean_tt_s / 60, digits=2)) %>% 
  mutate(
    dist_group = recode(
      dist_group,
      "0.0 - 999.999999999999" = "0 - 0.99",
      "1000.0 - 1999.999999999999" = "1.0 - 1.99",
      "2000.0 - 4999.999999999999" = "2.0 - 4.99",
      "5000.0 - 9999.999999999998" = "5.0 - 9.99",
      "10000.0 - 20000.0" = "10.0 - 20.0",
      "20000.0 - 1.7976931348623157E308" = "> 20.0"
    )
  )
# combined_0_fare <- bind_rows(tt_per_dist_group_qsim0.2, tt_per_dist_group_qsim0.3, income_groups_0_fare_3, income_groups_0_fare_4, income_groups_0_fare_5, income_groups_general) %>%
#   mutate(incomeGroup = factor(incomeGroup, levels = distGroups)) %>%
#   mutate(case = factor(case, levels = unique(case)))

combined_car <- combined %>%
  filter(mode == "bike")

# plot tt per dist group bike
tt_per_dist_group_car_plot <- ggplot(combined_car, aes(x = dist_group, y = mean_tt_min, fill = case)) +
  geom_col(
    data = combined_car,
    position = position_dodge(width = 0.8), width = 0.7
  ) +
  # geom_col(
  #   data = subset(combined_bike, case == "All agents"),
  #   alpha = 0.4, width = 0.95, show.legend = TRUE
  # ) +
  scale_fill_okabe_ito(order = c(1,2,3,4,5,6,7)) +
  scale_y_continuous(
    breaks = seq(0, max(combined_car$mean_tt_min, na.rm = TRUE), by = 50)
  ) +
  labs(x = "distance group [km]", y = "mean travel time [min]") +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5, size = 20),
    axis.title = element_text(size = 21),
    axis.text = element_text(size = 20),
    axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1),
    legend.title = element_text(size = 20),
    legend.text = element_text(size = 19),
    plot.margin = margin(5, 5, 5, 5)
  )
tt_per_dist_group_car_plot
ggsave("tt_per_dist_group_bike_plot.pdf", tt_per_dist_group_car_plot, dpi = 500, w = 9, h = 9)

combined_car <- combined %>%
  filter(mode == "car")

# plot tt per dist group car
tt_per_dist_group_car_plot <- ggplot(combined_car, aes(x = dist_group, y = mean_tt_min, fill = case)) +
  geom_col(
    data = combined_car,
    position = position_dodge(width = 0.8), width = 0.7
  ) +
  # geom_col(
  #   data = subset(combined_bike, case == "All agents"),
  #   alpha = 0.4, width = 0.95, show.legend = TRUE
  # ) +
  scale_fill_okabe_ito(order = c(1,2,3,4,5,6,7)) +
  scale_y_continuous(
    breaks = seq(0, max(combined_car$mean_tt_min, na.rm = TRUE), by = 25)
  ) +
  labs(x = "distance group [km]", y = "mean travel time [min]") +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5, size = 20),
    axis.title = element_text(size = 26),
    axis.text = element_text(size = 25),
    axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1),
    legend.title = element_text(size = 20),
    legend.text = element_text(size = 19),
    plot.margin = margin(5, 5, 5, 5),
    legend.position = "none"
  )
tt_per_dist_group_car_plot
ggsave("tt_per_dist_group_car_plot.pdf", tt_per_dist_group_car_plot, dpi = 500, w = 9, h = 9)
