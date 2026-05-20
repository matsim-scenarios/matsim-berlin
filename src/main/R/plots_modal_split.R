library(tidyverse)

setwd("D:/public-svn/matsim/scenarios/countries/de/berlin/projects/mobilityToGrid/3pct/")

mode_share_hedonism_path <- list.files(path=paste0(getwd(), "/output-berlin-v6.4-3pct-motorized-hedonism-scenario", "/analysis/population/"), pattern="*mode_share.csv*", full.names = TRUE)
mode_share_hedonism <- read.csv(file=mode_share_hedonism_path)

mode_share_hedonism <- mode_share_hedonism %>%
  mutate(
    main_mode = if_else(
      str_starts(main_mode, "pt_w_"),
      "pt",
      main_mode
    )
  ) %>%
  group_by(main_mode) %>%
  summarise(share = sum(share, na.rm = TRUE)) %>%
  mutate()

mode_share_multimodal_path <- list.files(path=paste0(getwd(), "/output-berlin-v6.4-3pct-multimodal-mass-scenario", "/analysis/population/"), pattern="*mode_share.csv*", full.names = TRUE)
mode_share_multimodal <- read.csv(file=mode_share_multimodal_path)

mode_share_multimodal <- mode_share_multimodal %>%
  mutate(
    main_mode = if_else(
      str_starts(main_mode, "pt_w_"),
      "pt",
      main_mode
    )
  ) %>%
  group_by(main_mode) %>%
  summarise(share = sum(share, na.rm = TRUE))

mode_share_stagnation_path <- list.files(path=paste0(getwd(), "/output-berlin-v6.4-3pct-stagnation-scenario", "/analysis/population/"), pattern="*mode_share.csv*", full.names = TRUE)
mode_share_stagnation <- read.csv(file=mode_share_stagnation_path)

mode_share_stagnation <- mode_share_stagnation %>%
  group_by(main_mode) %>%
  summarise(share = sum(share, na.rm = TRUE))

mode_share_base_ctd_path <- list.files(path=paste0(getwd(), "/output-berlin-v6.4-3pct-base-case-ctd-3-3", "/analysis/population/"), pattern="*mode_share.csv*", full.names = TRUE)
mode_share_base_ctd <- read.csv(file=mode_share_base_ctd_path)

mode_share_base_ctd <- mode_share_base_ctd %>%
  group_by(main_mode) %>%
  summarise(share = sum(share, na.rm = TRUE))

################################################### PLOTS ########################################################################################

mode_colors <- c(
  "bike"      = "#009E73",  # bluish green
  "car"       = "#D55E00",  # vermillion
  "pt"        = "#0072B2",  # blue
  "ride"      = "#CC79A7",  # reddish purple
  "walk"      = "#E69F00",  # orange
  "eScooter"  = "#56B4E9",  # sky blue
  "drt"       = "#F0E442"   # yellow
)

plot_hedonism <- ggplot(mode_share_hedonism,
       aes(x = main_mode,
           y = share,
           fill = main_mode)) +
  geom_col(width = 0.7) +
  geom_text(
    aes(label = scales::percent(share, accuracy = 0.01)),
    vjust = -0.3,
    size = 6
  ) +
  scale_fill_manual(values = mode_colors) +
  scale_y_continuous(
    labels = scales::percent_format(),
    limits = c(0, 0.4)
  ) +
  labs(
    x = "Main mode",
    y = "Share",
    title = "Motorized Hedonism Scenario"
  ) +
  theme_minimal(base_size = 14) +
  theme(
    axis.text = element_text(size = 25),
    axis.title = element_text(size = 25),
    plot.title = element_text(size = 25, face = "bold"),
    legend.position = "none"
  )

plot_multimodal <- ggplot(mode_share_multimodal,
                        aes(x = main_mode,
                            y = share,
                            fill = main_mode)) +
  geom_col(width = 0.7) +
  geom_text(
    aes(label = scales::percent(share, accuracy = 0.01)),
    vjust = -0.3,
    size = 6
  ) +
  scale_fill_manual(values = mode_colors) +
  scale_y_continuous(
    labels = scales::percent_format(),
    limits = c(0, 0.4)
  ) +
  labs(
    x = "Main mode",
    y = "Share",
    title = "Multimodal Mass Scenario"
  ) +
  theme_minimal(base_size = 14) +
  theme(
    axis.text = element_text(size = 25),
    axis.title = element_text(size = 25),
    plot.title = element_text(size = 25, face = "bold"),
    legend.position = "none"
  )

plot_stagnation <- ggplot(mode_share_stagnation,
                          aes(x = main_mode,
                              y = share,
                              fill = main_mode)) +
  geom_col(width = 0.7) +
  geom_text(
    aes(label = scales::percent(share, accuracy = 0.01)),
    vjust = -0.3,
    size = 6
  ) +
  scale_fill_manual(values = mode_colors) +
  scale_y_continuous(
    labels = scales::percent_format(),
    limits = c(0, 0.4)
  ) +
  labs(
    x = "Main mode",
    y = "Share",
    title = "Stagnation Scenario"
  ) +
  theme_minimal(base_size = 14) +
  theme(
    axis.text = element_text(size = 25),
    axis.title = element_text(size = 25),
    plot.title = element_text(size = 25, face = "bold"),
    legend.position = "none"
  )

plot_base_ctd <- ggplot(mode_share_base_ctd,
                          aes(x = main_mode,
                              y = share,
                              fill = main_mode)) +
  geom_col(width = 0.7) +
  geom_text(
    aes(label = scales::percent(share, accuracy = 0.01)),
    vjust = -0.3,
    size = 6
  ) +
  scale_fill_manual(values = mode_colors) +
  scale_y_continuous(
    labels = scales::percent_format(),
    limits = c(0, 0.4)
  ) +
  labs(
    x = "Main mode",
    y = "Share",
    title = "Base Case"
  ) +
  theme_minimal(base_size = 14) +
  theme(
    axis.text = element_text(size = 25),
    axis.title = element_text(size = 25),
    plot.title = element_text(size = 25, face = "bold"),
    legend.position = "none"
  )

plot_hedonism
plot_multimodal
plot_stagnation
plot_base_ctd

setwd("C:/Users/Simon/Desktop/wd/2026-05-18/")
ggsave("modal_split_hedonism.png", plot_hedonism, dpi = 500, w = 9, h = 9)
ggsave("modal_split_multimodal.png", plot_multimodal, dpi = 500, w = 9, h = 9)
ggsave("modal_split_stagnation.png", plot_stagnation, dpi = 500, w = 9, h = 9)
ggsave("modal_split_base_ctd.png", plot_base_ctd, dpi = 500, w = 9, h = 9)

