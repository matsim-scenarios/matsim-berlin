library(tidyverse)

# -----------------------------------------------------------------------------
# Paths
# -----------------------------------------------------------------------------

path <- "/Users/gregorr/Volumes/math-cluster/matsim-berlin/v6.4-parking/experiments/baseBellocheRegionalTotals0iters/analysis/parking"

links_berlin_path <- "/Users/gregorr/Documents/work/Paper/heartParking/linksInBerlin.csv"
links_hundekopf_path <- "/Users/gregorr/Documents/work/Paper/heartParking/linksInHundekopf.csv"

output_path <- file.path(path, "r-analysis")
dir.create(output_path, showWarnings = FALSE)


# -----------------------------------------------------------------------------
# Load parking outputs
# -----------------------------------------------------------------------------

# ParkingAnalysis writes these files with a semicolon separator.
parking_search_times <- read_delim(
  file.path(path, "parking_search_times.csv"),
  delim = ";"
)

parking_search_times_per_link <- read_delim(
  file.path(path, "parking_search_times_per_link.csv"),
  delim = ";"
)

parking_search_times_per_person <- read_delim(
  file.path(path, "parking_search_times_per_person.csv"),
  delim = ";"
)

parking_search_times_time_of_day <- read_delim(
  file.path(path, "parking_search_times_time_of_day.csv"),
  delim = ";"
)

# Convert seconds to minutes once after loading the data.
parking_search_times <- parking_search_times %>%
  mutate(search_time_min = search_time / 60)


# -----------------------------------------------------------------------------
# Overall parking search times
# -----------------------------------------------------------------------------

overall_summary <- parking_search_times %>%
  summarise(
    observations = n(),
    mean_min = mean(search_time_min, na.rm = TRUE),
    median_min = median(search_time_min, na.rm = TRUE),
    sd_min = sd(search_time_min, na.rm = TRUE),
    total_hours = sum(search_time_min, na.rm = TRUE) / 60
  )

print(overall_summary)
write_csv(overall_summary, file.path(output_path, "overall_summary.csv"))


# Full distribution
ggplot(parking_search_times, aes(x = search_time_min)) +
  geom_histogram(bins = 50, color = "black", fill = "grey70") +
  labs(
    title = "Distribution of Parking Search Times",
    x = "Search time [minutes]",
    y = "Frequency"
  ) +
  theme_minimal(base_size = 14)

ggsave(file.path(output_path, "parking_search_times.png"), width = 8, height = 5)


# Distribution up to the 95th percentile
cutoff_95 <- quantile(parking_search_times$search_time_min, 0.95, na.rm = TRUE)

parking_search_times_95 <- parking_search_times %>%
  filter(search_time_min <= cutoff_95)

ggplot(parking_search_times_95, aes(x = search_time_min)) +
  geom_histogram(bins = 30, color = "black", fill = "grey70") +
  labs(
    title = "Distribution of Parking Search Times: All Agents",
    subtitle = "Values truncated at the 95th percentile",
    x = "Search time [minutes]",
    y = "Frequency"
  ) +
  theme_minimal(base_size = 14)

ggsave(file.path(output_path, "parking_search_times_95_percent.png"), width = 8, height = 5)


# The CSV contains person IDs but not the MATSim subpopulation attribute.
# Regular persons in the Berlin scenario use IDs beginning with "berlin_".
parking_search_times_person <- parking_search_times_per_person %>%
  mutate(
    subpopulation = str_extract(person_id, "^[^_]+"),
    search_time_min = search_time / 60
  ) %>%
  filter(subpopulation == "berlin")

cutoff_person_95 <- quantile(
  parking_search_times_person$search_time_min,
  0.95,
  na.rm = TRUE
)

parking_search_times_person_95 <- parking_search_times_person %>%
  filter(search_time_min <= cutoff_person_95)

ggplot(parking_search_times_person_95, aes(x = search_time_min)) +
  geom_histogram(bins = 30, color = "black", fill = "grey70") +
  labs(
    title = "Distribution of Parking Search Times: Person Subpopulation",
    subtitle = "Values truncated at the 95th percentile",
    x = "Search time [minutes]",
    y = "Frequency"
  ) +
  theme_minimal(base_size = 14)

ggsave(
  file.path(output_path, "parking_search_times_person_95_percent.png"),
  width = 8,
  height = 5
)


# Distribution up to 12 minutes
parking_search_times_12 <- parking_search_times %>%
  filter(search_time_min <= 12)

ggplot(parking_search_times_12, aes(x = search_time_min)) +
  geom_histogram(aes(y = after_stat(density)), bins = 25,
                 color = "black", fill = "grey70") +
  geom_density(linewidth = 1) +
  labs(
    title = "Distribution of Parking Search Times",
    subtitle = "Values truncated at 12 minutes",
    x = "Search time [minutes]",
    y = "Density"
  ) +
  theme_minimal(base_size = 14)

ggsave(file.path(output_path, "parking_search_times_12_minutes.png"), width = 8, height = 5)


# -----------------------------------------------------------------------------
# Parking search times per person and subpopulation
# -----------------------------------------------------------------------------

person_summary <- parking_search_times_per_person %>%
  mutate(subpopulation = str_extract(person_id, "^[^_]+")) %>%
  group_by(subpopulation) %>%
  summarise(
    persons = n_distinct(person_id),
    observations = n(),
    total_hours = sum(search_time, na.rm = TRUE) / 3600,
    mean_min = mean(search_time, na.rm = TRUE) / 60,
    median_min = median(search_time, na.rm = TRUE) / 60,
    sd_min = sd(search_time, na.rm = TRUE) / 60,
    .groups = "drop"
  ) %>%
  arrange(desc(total_hours))

print(person_summary)
write_csv(person_summary, file.path(output_path, "subpopulation_summary.csv"))


# -----------------------------------------------------------------------------
# Mean parking search time per link
# -----------------------------------------------------------------------------

mean_per_link <- parking_search_times_per_link %>%
  group_by(link_id) %>%
  summarise(
    mean_search_time_min = mean(search_time, na.rm = TRUE) / 60,
    observations = n(),
    .groups = "drop"
  )

write_csv(mean_per_link, file.path(output_path, "mean_search_time_per_link.csv"))


# -----------------------------------------------------------------------------
# Comparison of Berlin and Hundekopf
# -----------------------------------------------------------------------------

# Only linkId is required from these files.
links_in_berlin <- read_csv(links_berlin_path, col_select = linkId)
links_in_hundekopf <- read_csv(links_hundekopf_path, col_select = linkId)

parking_berlin <- links_in_berlin %>%
  inner_join(mean_per_link, by = c("linkId" = "link_id"))

parking_hundekopf <- links_in_hundekopf %>%
  inner_join(mean_per_link, by = c("linkId" = "link_id"))

region_summary <- bind_rows(
  parking_berlin %>% mutate(region = "Berlin"),
  parking_hundekopf %>% mutate(region = "Hundekopf")
) %>%
  group_by(region) %>%
  summarise(
    links = n(),
    mean_min = mean(mean_search_time_min, na.rm = TRUE),
    median_min = median(mean_search_time_min, na.rm = TRUE),
    sd_min = sd(mean_search_time_min, na.rm = TRUE),
    .groups = "drop"
  )

print(region_summary)
write_csv(region_summary, file.path(output_path, "region_summary.csv"))


ggplot(parking_berlin, aes(x = mean_search_time_min)) +
  geom_density(linewidth = 1) +
  labs(
    title = "Mean Parking Search Time per Link in Berlin",
    x = "Mean search time [minutes]",
    y = "Density"
  ) +
  theme_minimal(base_size = 14)

ggsave(file.path(output_path, "mean_search_time_per_link_berlin.png"), width = 8, height = 5)


parking_berlin_12 <- parking_berlin %>%
  filter(mean_search_time_min <= 12)

ggplot(parking_berlin_12, aes(x = mean_search_time_min)) +
  geom_density(linewidth = 1) +
  labs(
    title = "Mean Parking Search Time per Link in Berlin",
    subtitle = "Values truncated at 12 minutes",
    x = "Mean search time [minutes]",
    y = "Density"
  ) +
  theme_minimal(base_size = 14)

ggsave(file.path(output_path, "mean_search_time_per_link_berlin_12_minutes.png"),
       width = 8, height = 5)


# -----------------------------------------------------------------------------
# Parking search time by time of day
# -----------------------------------------------------------------------------

search_time_by_hour <- parking_search_times_time_of_day %>%
  mutate(hour = floor(time_of_day / 3600)) %>%
  group_by(hour) %>%
  summarise(
    observations = n(),
    mean_min = mean(search_time, na.rm = TRUE) / 60,
    median_min = median(search_time, na.rm = TRUE) / 60,
    .groups = "drop"
  )

write_csv(search_time_by_hour, file.path(output_path, "search_time_by_hour.csv"))

ggplot(search_time_by_hour, aes(x = hour)) +
  geom_line(aes(y = mean_min, color = "Mean"), linewidth = 1) +
  geom_line(aes(y = median_min, color = "Median"), linewidth = 1) +
  labs(
    title = "Parking Search Time by Time of Day",
    x = "Hour",
    y = "Search time [minutes]",
    color = NULL
  ) +
  theme_minimal(base_size = 14)

ggsave(file.path(output_path, "parking_search_time_by_hour.png"), width = 8, height = 5)
