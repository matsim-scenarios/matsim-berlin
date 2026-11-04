library(tidyverse)
library(ggokabeito)

# read data
elasticities_share <- read_csv(file="C:/Users/Simon/Desktop/wd/2026-11-02/elasticities_bike_network_paper_qsim_experiments.csv") %>%
  mutate(
    elasticity = elasticity %>%
      na_if("#DIV/0!") %>%
      replace_na("0")
  ) %>% 
  mutate(elasticity = as.numeric(elasticity),
         `bike speed [km/h]` = as.numeric(`bike speed [km/h]`)) %>% 
  # mutate(
  #   case = case_when(
  #     str_detect(run, "base-case-ctd") ~ "base",
  #     str_detect(run, "bike-teleported") ~ "teleported",
  #     str_detect(run, "bike-in-qsim") ~ paste0("qsim", str_extract(run, "(?<=pce-)[0-9.]+")),
  #     TRUE ~ "Other"
  #   )
  # ) %>%
  mutate(case = factor(case, levels = unique(case)))
# filter(`speed factor`!=0.95) %>%
# filter(`speed factor`!=1.05)

# elasticities_share_base_no_routes <- read_csv(file="D:/runs-svn/matsim-berlin/v6.4_bike_network_study/bike_speed_elasticities_share_bike_network_paper_with_base_no_routes.csv") %>%
#   mutate(
#     elasticity = elasticity %>%
#       na_if("#DIV/0!") %>%
#       replace_na("0")
#   ) %>%
#   mutate(elasticity = as.numeric(elasticity)) %>%
#   mutate(
#     case = case_when(
#       str_detect(run, "base-case-ctd") ~ "base",
#       str_detect(run, "bike-teleported") ~ "teleported",
#       str_detect(run, "bike-in-qsim") ~ paste0("qsim", str_extract(run, "(?<=pce-)[0-9.]+")),
#       TRUE ~ "Other"
#     )
#   ) %>%
#   mutate(case = factor(case, levels = unique(case))) %>%
#   filter(`speed factor`!=0.95) %>%
#   filter(`speed factor`!=1.05)


# elasticities_share_same_jar <- read_csv(file="C:/Users/Simon/Desktop/wd/2026-10-19/bike_speed_elasticities_share_bike_network_paper_additional_teleported_runs_same_jar.csv") %>%
#   mutate(
#     elasticity = elasticity %>%
#       na_if("#DIV/0!") %>%
#       replace_na("0")
#   ) %>%
#   mutate(elasticity = as.numeric(elasticity)) %>%
#   mutate(
#     case = case_when(
#       str_detect(run, "base-case-ctd") ~ "base",
#       str_detect(run, "bike-teleported") ~ "teleported",
#       str_detect(run, "bike-in-qsim") ~ paste0("qsim", str_extract(run, "(?<=pce-)[0-9.]+")),
#       TRUE ~ "Other"
#     )
#   ) %>%
#   mutate(case = factor(case, levels = unique(case)))

elasticities_no_0 <- elasticities_share %>% 
  filter(elasticity != 0)

# elasticities_base_no_routes_no_0 <- elasticities_share_base_no_routes %>%
#   filter(elasticity != 0)

# elasticities_teleported <- elasticities_share %>%
#   filter(case=="teleported")

shares_plot <- ggplot(
  elasticities_share,
  aes(
    x = `speed factor`,
    y = `bike share`,
    color = case
  )
)+
  geom_line(linewidth = 2) +
  geom_point(size = 3)
shares_plot

# shares_plot_same_jar <- ggplot(
#   elasticities_share_same_jar,
#   aes(
#     x = `speed factor`,
#     y = `bike share`,
#     color = case
#   )
# )+
#   geom_line(linewidth = 2) +
#   geom_point(size = 3)
# shares_plot_same_jar




elasticities_plot <- ggplot(
  elasticities_no_0,
  aes(
    x = `bike speed [km/h]`,
    y = elasticity,
    color = case,
    group = case
  )
) +
  # geom_vline(
  #   xintercept = 10.3,
  #   linetype = "longdash",
  #   linewidth = 1
  # ) +
  geom_vline(
    xintercept = 10.7,
    linetype = "dashed",
    linewidth = 1
  ) +
  geom_line(linewidth = 1) +
  geom_point(size = 3) +
  scale_color_okabe_ito(order = c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)) +
  labs(
    x = "bicycle speed [km/h]",
    y = "bicycle speed elasticity",
    title="elasticitites qsim experiments"
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

elasticities_plot_agimo_poster <- ggplot(
  elasticities_no_0 %>% filter(case!="teleported", case!="qsim-0.0-pcu-threshold--0.1", `speed factor`!=0.95, `speed factor`!=1.05) %>%
    mutate(case = case_when(
      case=="base-w-speed-2.98-no-routes" ~ "fully teleported",
      case=="base" ~ "routed + teleported",
      case=="qsim-0.0" ~ "routed + qsim (pce=0.0)",
      case=="qsim-0.01" ~ "routed + qsim (pce=0.01)",
      case=="qsim-0.1" ~ "routed + qsim (pce=0.1)",
      case=="qsim-0.2" ~ "routed + qsim (pce=0.2)",
      case=="qsim-0.2-separate-bike-network" ~ "routed + qsim (pce=0.2; separate network)",
      case=="qsim-0.3" ~ "routed + qsim (pce=0.3)")
    ),
  aes(
    x = `bike speed [km/h]`,
    y = elasticity,
    color = case,
    group = case
  )
) +
  # geom_vline(
  #   xintercept = 10.3,
  #   linetype = "longdash",
  #   linewidth = 1
  # ) +
  geom_vline(
    xintercept = 10.7,
    linetype = "dashed",
    linewidth = 1
  ) +
  geom_line(linewidth = 1) +
  geom_point(size = 3) +
  scale_color_okabe_ito(order = c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)) +
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

elasticities_plot_agimo_poster

# ggsave("bicycle_speed_elasticities_plot.pdf", elasticities_plot, dpi = 500, w = 9, h = 9)
ggsave("bicycle_speed_elasticities_plot_qsim_experiments.pdf", elasticities_plot, dpi = 500, w = 9, h = 9)
ggsave("bicycle_speed_elasticities_plot_agimo_poster.pdf", elasticities_plot_agimo_poster, dpi = 500, w = 9, h = 9)