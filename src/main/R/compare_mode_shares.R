library(tidyverse)

mode_share <- read.csv(
  "//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-teleported-only/output-berlin-v6.4-3pct-bike-teleported-speed-factor-0.5-no-routes/analysis/population/mode_share.csv"
) %>%
  group_by(main_mode) %>%
  summarise(share = sum(share, na.rm = TRUE), .groups = "drop") %>%
  mutate(dataset = "mode_share")

mode_share_ref <- read.csv(
  "//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/calib-pce-0.3-correct-bike-tt/runs/007/analysis/resources/mode_share_ref.csv"
) %>%
  group_by(main_mode) %>%
  summarise(share = sum(share, na.rm = TRUE), .groups = "drop") %>%
  mutate(dataset = "mode_share_ref")

comparison <- bind_rows(mode_share, mode_share_ref)

ggplot(comparison, aes(x = main_mode, y = share, fill = dataset)) +
  geom_col(position = position_dodge(width = 0.9)) +
  geom_text(
    aes(label = sprintf("%.4f", share)),
    position = position_dodge(width = 0.9),
    angle = 270,
    vjust = 0.5,
    hjust = 0.5
  ) +
  labs(
    x = "Main mode",
    y = "Summed share",
    fill = "Dataset"
  ) +
  theme_minimal()

bike_only <- mode_share %>% 
  filter(main_mode=="bike")
print(bike_only$share)

