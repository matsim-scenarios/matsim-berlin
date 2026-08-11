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
