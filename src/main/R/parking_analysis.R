library(tidyverse)
library(matsim)

# -----------------------------------------------------------------------------
# Paths
# -----------------------------------------------------------------------------

path <- "/Users/gregorr/Volumes/math-cluster/matsim-berlin/v6.4-parking/experiments/baseBellocheParkingData0itersDetailedWithModeChoce/analysis/parking"

links_berlin_path <- "/Users/gregorr/Documents/work/Paper/heartParking/linksInBerlin.csv"
links_hundekopf_path <- "/Users/gregorr/Documents/work/Paper/heartParking/linksInHundekopf.csv"

# The run directory is two levels above analysis/parking. MATSim writes one
# output network into this directory, which provides the link geometries used
# for the maps below.
run_path <- dirname(dirname(path))
# First look in the run directory, where MATSim normally writes the network.
network_files <- list.files(
  run_path,
  pattern = "output_network\\.xml(\\.(gz|zst))?$",
  full.names = TRUE
)

# Some archived runs store the output network in a subdirectory. Search there
# only when no root-level output network was found.
if (length(network_files) == 0) {
  network_files <- list.files(
    run_path,
    pattern = "output_network\\.xml(\\.(gz|zst))?$",
    full.names = TRUE,
    recursive = TRUE
  )
}

if (length(network_files) == 0) {
  stop(
    "No MATSim output network was found below: ", run_path,
    "\nSet network_path manually to the run's output network file."
  )
}

# Prefer Zstandard, followed by gzip, if several copies of the network exist.
zstd_network_files <- network_files[str_ends(network_files, "\\.xml\\.zst$")]
gzip_network_files <- network_files[str_ends(network_files, "\\.xml\\.gz$")]

if (length(zstd_network_files) > 0) {
  network_path <- sort(zstd_network_files)[[1]]
} else if (length(gzip_network_files) > 0) {
  network_path <- sort(gzip_network_files)[[1]]
} else {
  network_path <- sort(network_files)[[1]]
}

message("Using network: ", network_path)

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

total_parking_search_time_per_link <- read_delim(
  file.path(path, "total_parking_search_time_per_link.csv"),
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


# -----------------------------------------------------------------------------
# Maps of parking search time per link
# -----------------------------------------------------------------------------

# matsim::read_network() can read XML and gzip files, but not Zstandard files.
# For a .zst network, temporarily decompress it and remove the XML afterwards.
read_output_network <- function(network_path) {
  if (!str_ends(network_path, "\\.zst$")) {
    return(matsim::read_network(network_path))
  }

  zstd <- Sys.which("zstd")
  if (zstd == "") {
    stop("The zstd command is required to read: ", network_path)
  }

  temporary_network <- tempfile(fileext = ".xml")
  on.exit(unlink(temporary_network), add = TRUE)

  status <- system2(
    zstd,
    args = c("--decompress", "--stdout", shQuote(network_path)),
    stdout = temporary_network
  )

  if (status != 0) {
    stop("Could not decompress network: ", network_path)
  }

  matsim::read_network(temporary_network)
}

# The MATSim R package adds the coordinates of both end nodes to every link, so
# the network can be drawn directly with geom_segment().
network <- read_output_network(network_path)

network_links <- network$links %>%
  transmute(
    link_id = as.character(id),
    x_from = x.from,
    y_from = y.from,
    x_to = x.to,
    y_to = y.to
  )

# Limit the maps to Berlin. Links without a recorded parking-search event form
# the grey background, while links with observations are coloured.
network_berlin <- network_links %>%
  semi_join(links_in_berlin, by = c("link_id" = "linkId"))

mean_search_time_map_data <- network_berlin %>%
  inner_join(mean_per_link, by = "link_id")

mean_map_limit <- quantile(
  mean_search_time_map_data$mean_search_time_min,
  0.99,
  na.rm = TRUE
)

mean_search_time_map <- ggplot() +
  geom_segment(
    data = network_berlin,
    aes(x = x_from, y = y_from, xend = x_to, yend = y_to),
    color = "grey85",
    linewidth = 0.08
  ) +
  geom_segment(
    data = mean_search_time_map_data,
    aes(
      x = x_from,
      y = y_from,
      xend = x_to,
      yend = y_to,
      color = mean_search_time_min
    ),
    linewidth = 0.35
  ) +
  scale_color_viridis_c(
    option = "magma",
    trans = "sqrt",
    limits = c(0, mean_map_limit),
    oob = scales::squish,
    name = "Mean search time\n[minutes]"
  ) +
  coord_equal() +
  labs(
    title = "Mean Parking Search Time per Link",
    subtitle = paste(
      "Values above the 99th percentile are capped;",
      "links without observations are grey"
    )
  ) +
  theme_void(base_size = 12) +
  theme(
    legend.position = "right",
    plot.title = element_text(face = "bold")
  )

ggsave(
  file.path(output_path, "mean_parking_search_time_per_link_map.png"),
  plot = mean_search_time_map,
  width = 9,
  height = 8,
  dpi = 300,
  bg = "white"
)


# Total search time highlights links on which the largest overall parking-search
# burden accumulates. This can differ from the mean map when a link has many
# short search observations.
total_per_link <- total_parking_search_time_per_link %>%
  transmute(
    link_id = as.character(link_id),
    total_search_time_min = total_parking_search_time / 60
  )

total_search_time_map_data <- network_berlin %>%
  inner_join(total_per_link, by = "link_id")

total_map_limit <- quantile(
  total_search_time_map_data$total_search_time_min,
  0.99,
  na.rm = TRUE
)

total_search_time_map <- ggplot() +
  geom_segment(
    data = network_berlin,
    aes(x = x_from, y = y_from, xend = x_to, yend = y_to),
    color = "grey85",
    linewidth = 0.08
  ) +
  geom_segment(
    data = total_search_time_map_data,
    aes(
      x = x_from,
      y = y_from,
      xend = x_to,
      yend = y_to,
      color = total_search_time_min
    ),
    linewidth = 0.35
  ) +
  scale_color_viridis_c(
    option = "magma",
    trans = "sqrt",
    limits = c(0, total_map_limit),
    oob = scales::squish,
    name = "Total search time\n[minutes]"
  ) +
  coord_equal() +
  labs(
    title = "Total Parking Search Time per Link",
    subtitle = paste(
      "Values above the 99th percentile are capped;",
      "links without observations are grey"
    )
  ) +
  theme_void(base_size = 12) +
  theme(
    legend.position = "right",
    plot.title = element_text(face = "bold")
  )

ggsave(
  file.path(output_path, "total_parking_search_time_per_link_map.png"),
  plot = total_search_time_map,
  width = 9,
  height = 8,
  dpi = 300,
  bg = "white"
)


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

# The analysis output records the end of each completed parking search. The
# parking-search delay is determined when the search starts, so reconstruct the
# start time before aggregating. Simulation hours are not wrapped at 24.
# Searches that were still running when QSim ended are absent from this input.
if (nrow(parking_search_times_time_of_day) == 0) {
  stop("No completed parking searches were found.")
}

if (any(!is.finite(parking_search_times_time_of_day$time_of_day)) ||
    any(!is.finite(parking_search_times_time_of_day$search_time))) {
  stop("Parking-search times must be finite.")
}

if (any(parking_search_times_time_of_day$search_time < 0)) {
  stop("Parking-search durations must not be negative.")
}

parking_searches_with_timing <- parking_search_times_time_of_day %>%
  transmute(
    person_id,
    subpopulation = str_extract(person_id, "^[^_]+"),
    search_end_time = time_of_day,
    search_time,
    search_time_min = search_time / 60,
    search_start_time = search_end_time - search_time,
    search_start_hour = floor(search_start_time / 3600),
    search_end_hour = floor(search_end_time / 3600)
  )

if (any(parking_searches_with_timing$search_start_time < 0)) {
  stop("At least one parking search starts before simulation time zero.")
}

observed_search_time_by_start_hour <- parking_searches_with_timing %>%
  group_by(search_start_hour) %>%
  summarise(
    parking_searches = n(),
    total_search_time_hours = sum(search_time_min) / 60,
    mean_min = mean(search_time_min),
    median_min = median(search_time_min),
    sd_min = sd(search_time_min),
    p75_min = quantile(search_time_min, 0.75),
    p90_min = quantile(search_time_min, 0.90),
    p95_min = quantile(search_time_min, 0.95),
    maximum_min = max(search_time_min),
    share_over_1_min = mean(search_time_min > 1),
    share_over_5_min = mean(search_time_min > 5),
    share_over_10_min = mean(search_time_min > 10),
    .groups = "drop"
  )

# Insert hours without completed searches so the CSV shows these gaps
# explicitly. Their duration statistics remain empty.
search_time_by_start_hour <- tibble(
  search_start_hour = seq.int(
    0,
    max(parking_searches_with_timing$search_start_hour)
  )
) %>%
  left_join(observed_search_time_by_start_hour, by = "search_start_hour") %>%
  mutate(parking_searches = replace_na(parking_searches, 0L))

if (sum(search_time_by_start_hour$parking_searches) !=
    nrow(parking_searches_with_timing)) {
  stop("Hourly parking-search counts do not match the input.")
}

print(search_time_by_start_hour)
# Print searches beginning after midnight separately while preserving their
# original simulation hours.
print(search_time_by_start_hour %>% filter(search_start_hour >= 24))
write_csv(
  search_time_by_start_hour,
  file.path(output_path, "search_time_by_hour.csv")
)

# Aggregate the system-wide parking-search burden by subpopulation. These totals
# intentionally retain every completed search, including extreme values from
# the unbounded Belloche function.
total_search_time_by_start_hour_and_subpopulation <-
  parking_searches_with_timing %>%
  group_by(search_start_hour, subpopulation) %>%
  summarise(
    parking_searches = n(),
    total_search_time_hours = sum(search_time_min) / 60,
    .groups = "drop"
  )

write_csv(
  total_search_time_by_start_hour_and_subpopulation,
  file.path(output_path, "total_search_time_by_hour_and_subpopulation.csv")
)

simulation_hour_range <- range(search_time_by_start_hour$search_start_hour)
simulation_hour_breaks <- seq(
  floor(simulation_hour_range[[1]] / 4) * 4,
  ceiling(simulation_hour_range[[2]] / 4) * 4,
  by = 4
)

hourly_total_search_time_plot <-
  total_search_time_by_start_hour_and_subpopulation %>%
  mutate(
    subpopulation = factor(
      recode(
        subpopulation,
        berlin = "Berlin residents",
        bb = "Brandenburg residents",
        commercialPersonTraffic = "Commercial person traffic",
        goodsTraffic = "Goods traffic"
      ),
      levels = c(
        "Berlin residents",
        "Brandenburg residents",
        "Commercial person traffic",
        "Goods traffic"
      )
    )
  ) %>%
  ggplot(aes(
    x = search_start_hour,
    y = total_search_time_hours,
    fill = subpopulation
  )) +
  geom_col(width = 0.85) +
  geom_vline(xintercept = 24, color = "grey25", linetype = "dashed") +
  scale_x_continuous(
    breaks = simulation_hour_breaks,
    limits = c(-0.5, simulation_hour_range[[2]] + 0.5)
  ) +
  scale_fill_manual(
    values = c(
      "Berlin residents" = "#0072B2",
      "Brandenburg residents" = "#56B4E9",
      "Commercial person traffic" = "#E69F00",
      "Goods traffic" = "#D55E00"
    )
  ) +
  labs(
    title = "Total Parking Search Burden by Search Start Hour",
    subtitle = "Completed parking searches stacked by agent group",
    caption = "Dashed line marks simulation hour 24",
    x = "Simulation hour at start of parking search",
    y = "Total parking-search time [hours]",
    fill = NULL
  ) +
  theme_minimal(base_size = 14) +
  theme(
    legend.position = "bottom"
  ) +
  guides(fill = guide_legend(nrow = 2, byrow = TRUE))

spike_hours <- search_time_by_start_hour %>%
  filter(parking_searches > 0) %>%
  slice_max(mean_min, n = 8, with_ties = FALSE) %>%
  pull(search_start_hour)

parking_searches_with_timing %>%
  mutate(
    subpopulation = str_extract(person_id, "^[^_]+")
  ) %>%
  filter(search_start_hour %in% spike_hours) %>%
  group_by(search_start_hour) %>%
  mutate(
    contribution_to_hour =
      search_time_min / sum(search_time_min)
  ) %>%
  slice_max(
    search_time_min,
    n = 5,
    with_ties = FALSE
  ) %>%
  ungroup() %>%
  select(
    search_start_hour,
    person_id,
    subpopulation,
    search_time_min,
    search_end_hour,
    contribution_to_hour
  ) %>%
  arrange(search_start_hour, desc(search_time_min))



ggsave(
  file.path(output_path, "parking_search_time_by_hour.png"),
  plot = hourly_total_search_time_plot,
  width = 8,
  height = 5,
  dpi = 300,
  bg = "white"
)
