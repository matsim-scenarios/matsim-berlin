library(tidyverse)
library(ggokabeito)

###################################### tt per dist group plots #############################################################################################################

# read data
tt_per_dist_group_base <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-base-case-ctd/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "base")
tt_per_dist_group_teleported <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-teleported/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "teleported")
tt_per_dist_group_qsim0.0 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.0/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.0")
tt_per_dist_group_qsim0.01 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.01/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.01")
tt_per_dist_group_qsim0.1 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.1/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.1")
tt_per_dist_group_qsim0.2 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.2/mean_tt_per_distance_bin.csv") %>%
  mutate(case = "qsim0.2")
tt_per_dist_group_qsim0.3 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.3/mean_tt_per_distance_bin.csv") %>%
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

combined_bike <- combined %>%
  filter(mode == "bike")

# plot tt per dist group bike
avg_cycling_speed_per_road_type_plot <- ggplot(combined_bike, aes(x = dist_group, y = mean_tt_min, fill = case)) +
  geom_col(
    data = combined_bike,
    position = position_dodge(width = 0.8), width = 0.7
  ) +
  # geom_col(
  #   data = subset(combined_bike, case == "All agents"),
  #   alpha = 0.4, width = 0.95, show.legend = TRUE
  # ) +
  scale_fill_okabe_ito(order = c(1,2,3,4,5,6,7)) +
  scale_y_continuous(
    breaks = seq(0, max(combined_bike$mean_tt_min, na.rm = TRUE), by = 50)
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
avg_cycling_speed_per_road_type_plot
ggsave("tt_per_dist_group_bike_plot.pdf", avg_cycling_speed_per_road_type_plot, dpi = 500, w = 9, h = 9)

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

############################################ dist group distr per case plot ##################################################

# read data
modal_dist_groups_base <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-base-case-ctd/analysis/population/mode_share_per_dist.csv") %>%
  mutate(case = "base")
modal_dist_groups_teleported <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-teleported/analysis/population/mode_share_per_dist.csv") %>%
  mutate(case = "teleported")
modal_dist_groups_qsim0.0 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.0/analysis/population/mode_share_per_dist.csv") %>%
  mutate(case = "qsim0.0")
modal_dist_groups_qsim0.01 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.01/analysis/population/mode_share_per_dist.csv") %>%
  mutate(case = "qsim0.01")
modal_dist_groups_qsim0.1 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.1/analysis/population/mode_share_per_dist.csv") %>%
  mutate(case = "qsim0.1")
modal_dist_groups_qsim0.2 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.2/analysis/population/mode_share_per_dist.csv") %>%
  mutate(case = "qsim0.2")
modal_dist_groups_qsim0.3 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.3/analysis/population/mode_share_per_dist.csv") %>%
  mutate(case = "qsim0.3")
modal_dist_groups_ref <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-base-case-ctd/analysis/resources/mode_share_per_dist_ref.csv") %>%
  mutate(case = "ref")

dist_levels_modal <- c(
  "0 - 1000",
  "1000 - 2000",
  "2000 - 5000",
  "5000 - 10000",
  "10000 - 20000",
  "20000+"
)

# Combine datasets
combined_modal <- bind_rows(modal_dist_groups_base, modal_dist_groups_teleported, modal_dist_groups_qsim0.0, modal_dist_groups_qsim0.01, modal_dist_groups_qsim0.1, modal_dist_groups_qsim0.2, modal_dist_groups_qsim0.3, modal_dist_groups_ref) %>%
  mutate(dist_group = factor(dist_group, levels = dist_levels_modal)) %>%
  mutate(case = factor(case, levels = unique(case))) %>%
  mutate(
    dist_group = recode(
      dist_group,
      "0 - 1000" = "0 - 0.99",
      "1000 - 2000" = "1.0 - 1.99",
      "2000 - 5000" = "2.0 - 4.99",
      "5000 - 10000" = "5.0 - 9.99",
      "10000 - 20000" = "10.0 - 20.0",
      "20000+" = "> 20.0"
    )
  ) %>% 
  select(-mean_dist)

combined_modal_bike <- combined_modal %>%
  filter(main_mode == "bike")

# plot modal distances bike
modal_dist_group_bike_plot <- ggplot(combined_modal_bike, aes(x = dist_group, y = share, fill = case)) +
  geom_col(
    data = combined_modal_bike,
    position = position_dodge(width = 0.8), width = 0.7
  ) +
  # geom_col(
  #   data = subset(combined_modal_bike, case == "All agents"),
  #   alpha = 0.4, width = 0.95, show.legend = TRUE
  # ) +
  scale_fill_okabe_ito(order = c(1,2,3,4,5,6,7,8)) +
  # scale_y_continuous(
  #   breaks = seq(0, max(combined_modal_bike$share, na.rm = TRUE), by = 0.5)
  # ) +
  labs(x = "distance group [km]", y = "share") +
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
modal_dist_group_bike_plot
ggsave("modal_dist_group_bike_plot.pdf", modal_dist_group_bike_plot, dpi = 500, w = 9, h = 9)

combined_modal_car <- combined_modal %>%
  filter(main_mode == "car")

# plot modal distances car
modal_dist_group_car_plot <- ggplot(combined_modal_car, aes(x = dist_group, y = share, fill = case)) +
  geom_col(
    data = combined_modal_car,
    position = position_dodge(width = 0.8), width = 0.7
  ) +
  # geom_col(
  #   data = subset(combined_modal_car, case == "All agents"),
  #   alpha = 0.4, width = 0.95, show.legend = TRUE
  # ) +
  scale_fill_okabe_ito(order = c(1,2,3,4,5,6,7,8)) +
  # scale_y_continuous(
  #   breaks = seq(0, max(combined_modal_car$share, na.rm = TRUE), by = 0.5)
  # ) +
  labs(x = "distance group [km]", y = "share") +
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
modal_dist_group_car_plot
ggsave("modal_dist_group_car_plot.pdf", modal_dist_group_car_plot, dpi = 500, w = 9, h = 9)

############################################ bicycle speed elasticities plot ##################################################

# read data
elasticities_share <- read_csv(file="C:/Users/Simon/Desktop/wd/2026-08-24/bike_speed_elasticities_share_bike_network_paper.csv") %>% 
  mutate(
    elasticity = elasticity %>%
      na_if("#DIV/0!") %>%
      replace_na("0")
    ) %>% 
  mutate(elasticity = as.numeric(elasticity)) %>% 
  mutate(
    case = case_when(
      str_detect(run, "base-case-ctd") ~ "base",
      str_detect(run, "bike-teleported") ~ "teleported",
      str_detect(run, "bike-in-qsim") ~ paste0("qsim", str_extract(run, "(?<=pce-)[0-9.]+")),
      TRUE ~ "Other"
    )
  ) %>% 
  mutate(case = factor(case, levels = unique(case))) %>% 
  filter(elasticity != 0)

elasticities_plot <- ggplot(
  elasticities_share,
  aes(
    x = `bike speed [km/h]`,
    y = elasticity,
    color = case,
    group = case
  )
) +
  geom_vline(
    xintercept = 10.3,
    linetype = "longdash",
    linewidth = 1
  ) +
  geom_vline(
    xintercept = 10.7,
    linetype = "dashed",
    linewidth = 1
  ) +
  geom_line(linewidth = 2) +
  geom_point(size = 3) +
  scale_color_okabe_ito(order = c(1, 2, 3, 4, 5, 6, 7)) +
  labs(
    x = "bicycle speed [km/h]",
    y = "bicycle speed elasticity"
  ) +
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

elasticities_plot
ggsave("bicycle_speed_elasticities_plot.pdf", elasticities_plot, dpi = 500, w = 9, h = 9)

############################################ avg bicycle network speed per link type ##################################################
avg_speed_per_road_type_qsim0.0 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.0/analysis/traffic/traffic_stats_by_road_type_daily_bike.csv") %>%
  mutate(case = "qsim0.0") %>% 
  rename(avg_speed_m_s = 'Avg. Speed [km/h]',
         road_type = 'Road Type')
avg_speed_per_road_type_qsim0.01 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.01/analysis/traffic/traffic_stats_by_road_type_daily_bike.csv") %>%
  mutate(case = "qsim0.01") %>% 
  rename(avg_speed_m_s = 'Avg. Speed [km/h]',
         road_type = 'Road Type')
avg_speed_per_road_type_qsim0.1 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.1/analysis/traffic/traffic_stats_by_road_type_daily_bike.csv") %>%
  mutate(case = "qsim0.1") %>% 
  rename(avg_speed_m_s = 'Avg. Speed [km/h]',
         road_type = 'Road Type')
avg_speed_per_road_type_qsim0.2 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.2/analysis/traffic/traffic_stats_by_road_type_daily_bike.csv") %>%
  mutate(case = "qsim0.2") %>% 
  rename(avg_speed_m_s = 'Avg. Speed [km/h]',
         road_type = 'Road Type')
avg_speed_per_road_type_qsim0.3 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.3/analysis/traffic/traffic_stats_by_road_type_daily_bike.csv") %>%
  mutate(case = "qsim0.3") %>% 
  rename(avg_speed_m_s = 'Avg. Speed [km/h]',
         road_type = 'Road Type')

road_type_levels <- c(
  "primary",
  "primary_link",
  "secondary",
  "secondary_link",
  "tertiary",
  "residential",
  "living_street",
  "service",
  "path",
  "cycleway",
  "unclassified",
  "all"
)

combined_avg_speed_per_road_type <- bind_rows(avg_speed_per_road_type_qsim0.0, avg_speed_per_road_type_qsim0.01, avg_speed_per_road_type_qsim0.1, avg_speed_per_road_type_qsim0.2, avg_speed_per_road_type_qsim0.3) %>%
  mutate(road_type = factor(road_type, levels = road_type_levels)) %>%
  mutate(case = factor(case, levels = unique(case))) %>%
  mutate(avg_speed_km_h = round(avg_speed_m_s * 3.6, digits=2))

avg_cycling_speed_per_road_type_plot <- ggplot(combined_avg_speed_per_road_type, aes(x = road_type, y = avg_speed_km_h, fill = case)) +
  geom_col(
    data = combined_avg_speed_per_road_type,
    position = position_dodge(width = 0.8), width = 0.7
  ) +
  scale_fill_okabe_ito(order = c(3,4,5,6,7)) +
  scale_y_continuous(
    # limits = c(5, 11),
    breaks = seq(5, max(combined_avg_speed_per_road_type$avg_speed_km_h, na.rm = TRUE), by = 1)
  ) +
  coord_cartesian(ylim = c(7, 11)) +
  labs(x = "road type", y = "avg. cycling speed [km/h]") +
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
avg_cycling_speed_per_road_type_plot
ggsave("avg_cycling_speed_per_road_type_plot.pdf", avg_cycling_speed_per_road_type_plot, dpi = 500, w = 9, h = 9)

############################################ counts vs matsim bike volumes ##################################################
# read data
count_data <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/dtv_typical_weekday_bicycle_counts_2018_wgs84.csv")

volumes_qsim0.0 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.0/analysis/traffic/traffic_stats_by_link_daily_bike.csv")
volumes_qsim0.01 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.01/analysis/traffic/traffic_stats_by_link_daily_bike.csv") 
volumes_qsim0.1 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.1/analysis/traffic/traffic_stats_by_link_daily_bike.csv") 
volumes_qsim0.2 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.2/analysis/traffic/traffic_stats_by_link_daily_bike.csv") 
volumes_qsim0.3 <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/output-berlin-v6.4-3pct-bike-in-qsim-pce-0.3/analysis/traffic/traffic_stats_by_link_daily_bike.csv") 

get_bike_volumes <- function(count_data, volumes, case) {
  volume_col <- case
  
  count_data %>%
    rowwise() %>%
    mutate( !!volume_col := sum(
      volumes$vol_bike[
        volumes$link_id %in% unlist(strsplit(links, ";\\s*"))
      ], na.rm = TRUE
    )
    ) %>%
    ungroup()
  }

count_data <- get_bike_volumes(count_data, volumes_qsim0.0, "qsim0.0")
count_data <- get_bike_volumes(count_data, volumes_qsim0.01, "qsim0.01")
count_data <- get_bike_volumes(count_data, volumes_qsim0.1, "qsim0.1")
count_data <- get_bike_volumes(count_data, volumes_qsim0.2, "qsim0.2")
count_data <- get_bike_volumes(count_data, volumes_qsim0.3, "qsim0.3")

count_data <- count_data %>% 
  mutate(ratio_0.0 = dtv_2018 / qsim0.0,
         ratio_0.01 = dtv_2018 / qsim0.01,
         ratio_0.1 = dtv_2018 / qsim0.1,
         ratio_0.2 = dtv_2018 / qsim0.2,
         ratio_0.3 = dtv_2018 / qsim0.3)

filtered <- count_data %>% 
  filter(abs(ratio_0.0 - ratio_0.01) >= 0.5) 

qsim0.0_less_volume_than_qsim0.3 <- count_data %>% 
  filter(qsim0.0 < qsim0.3)

head(count_data)
  
count_plot_data <- count_data %>%
  select(station, dtv_2018, starts_with("qsim"), highway_types) %>% 
  pivot_longer(
    cols = starts_with("qsim"),
    names_to = "case",
    values_to = "simulation"
  )

counts_plot <- ggplot(count_plot_data,
                      aes(x = dtv_2018,
                          y = simulation,
                          color = case,
                          group = case)) +
  geom_abline(slope = 1, intercept = 0, size=1) +
  geom_point(size = 3) +
  scale_x_continuous(limits = c(0, 15000)) +
  scale_y_continuous(limits = c(0, 15000)) +
  scale_color_okabe_ito(order = c(3, 4, 5, 6, 7)) +
  labs(
    x = "bicycle count dtv 2018",
    y = "simulation"
  ) +
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

counts_plot
ggsave("bicycle_counts_to_volumes_plot.pdf", counts_plot, dpi = 500, w = 9, h = 9)

count_highway_types <- c("primary", "secondary", "tertiary", "cycleway", "path", "residential")

for (type in count_highway_types) {
  count_plot_data_type <- count_plot_data %>%
    filter(str_detect(highway_types, type))

  counts_plot_type <- ggplot(count_plot_data_type,
                                aes(x = dtv_2018,
                                    y = simulation,
                                    color = case,
                                    group = case)) +
    geom_abline(slope = 1, intercept = 0, size=1) +
    geom_point(size = 3) +
    scale_x_continuous(limits = c(0, 15000)) +
    scale_y_continuous(limits = c(0, 15000)) +
    scale_color_okabe_ito(order = c(3, 4, 5, 6, 7)) +
    labs(
      # title = paste0(type, ", n=", n_distinct(count_plot_data_type$station)),
      x = "bicycle count dtv 2018",
      y = "simulation"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(hjust = 0.5, size = 20),
      axis.title = element_text(size = 21),
      axis.text = element_text(size = 20),
      axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1),
      legend.title = element_text(size = 20),
      legend.text = element_text(size = 19),
      plot.margin = margin(5, 5, 5, 5))
  
  print(paste(type, ": n=", n_distinct(count_plot_data_type$station)))

  print(counts_plot_type)
  ggsave(paste0("bicycle_counts_to_volumes_plot_", type, ".pdf"), counts_plot_type, dpi = 500, w = 9, h = 9)
}


counts_performance <- count_plot_data %>%
  group_by(case) %>%
  summarise(
    MAE = mean(abs(simulation - dtv_2018), na.rm = TRUE),
    NMAE = mean(
      abs(simulation - dtv_2018),
      na.rm = TRUE
    ) / mean(
      dtv_2018,
      na.rm = TRUE
    ),
    RMSE = sqrt(
      mean((simulation - dtv_2018)^2, na.rm = TRUE)
    ),
    MBE = mean(
      simulation - dtv_2018,
      na.rm = TRUE
    ),
    MAPE = mean(
      abs((simulation - dtv_2018) / dtv_2018),
      na.rm = TRUE
    ),
    .groups = "drop"
  )

write.csv(counts_performance, file="error_measure_bicycle_counts_sim.csv", quote=FALSE, row.names=FALSE)
