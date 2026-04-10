# parking_osm.R
# Converted from parking_osm.Rmd to a clean standalone R script

# ----------------------------------------------------------
# Setup
# ----------------------------------------------------------

# install.packages("osmdata")
library(tidyverse)
library(sf)
library(osmdata)
library(tmap)
library(matsim)
library(units)

# ----------------------------------------------------------
# Reading Inputs
# ----------------------------------------------------------

# MATSim network
input_network_filename <- "berlin-v6.4-network-with-pt.xml.gz"

if (!file.exists(input_network_filename)) {
  download.file(
    "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/berlin-v6.4-network-with-pt.xml.gz",
    input_network_filename
  )
}
network_berlin <- matsim::read_network(input_network_filename)

# Berlin district boundaries
berlin_districts_shp_filename <- "bezirksgrenzen.shp.zip"
if (!file.exists(berlin_districts_shp_filename)) {
  download.file(
    "https://tsb-opendata.s3.eu-central-1.amazonaws.com/bezirksgrenzen/bezirksgrenzen.shp.zip",
    berlin_districts_shp_filename
  )
}

berlin_districts_shp <- st_read(berlin_districts_shp_filename) %>% st_transform(25832)

# ----------------------------------------------------------
# Question 1: Parking spots per meter of MATSim link
# ----------------------------------------------------------

nodes <- network_berlin$nodes %>%
  st_as_sf(coords = c("x", "y"), crs = 25832)

##filter only for links in Berlin
nodes_in_berlin <- nodes %>% st_filter(berlin_districts_shp)
nodes_in_berlin_ids <- nodes_in_berlin %>% pull(id)

links_in_berlin <- network_berlin$links %>%
  filter(from %in% nodes_in_berlin_ids | to %in% nodes_in_berlin_ids)

##total link length of berlin
total_link_length_berlin <- sum(links_in_berlin$length)
total_parking_spots_berlin <- 1276312  # from Berlin official estimate

# ----------------------------------------------------------
# Links < 50 km/h --> filter out motorways
# ----------------------------------------------------------
links_in_berlin_filtered <- links_in_berlin %>%
  filter(allowed_speed < 13.89)

# ----------------------------------------------------------
# Inside / Outside Hundekopf (Ring)
# ----------------------------------------------------------

# IMPORTANT: Add or load your "hundekopf" geometry here
hundekopf <- st_read("/Users/gregorr/Documents/work/respos/git/matsim-berlin/input/v6.4/hundekopf-shp/hundekopf-carBanArea-25832.shp") %>% st_transform(25832)

##filter out links in hundekopf
nodes_in_hundekopf_ids <- nodes %>% st_filter(hundekopf) %>% pull(id)

links_in_hundekopf_filtered <- links_in_berlin_filtered %>%
  filter(from %in% nodes_in_hundekopf_ids | to %in% nodes_in_hundekopf_ids)

links_outside_hundekopf_filtered <- links_in_berlin_filtered %>%
  filter(!id %in% links_in_hundekopf_filtered$id)

##where does the 230000 come from? --> 
inside_hundekopf_stats <- links_in_hundekopf_filtered %>%
  summarise(sum_link_distlink_length = sum(length)) %>%
  mutate(
    total_parking_spots = 230000,
    spots_per_meter = total_parking_spots / sum_link_distlink_length,
    meters_per_spot = sum_link_distlink_length / total_parking_spots
  )

outside_hundekopf_stats <- links_outside_hundekopf_filtered %>%
  summarise(sum_link_distlink_length = sum(length)) %>%
  mutate(
    total_parking_spots = 1276312 - 230000,
    spots_per_meter = total_parking_spots / sum_link_distlink_length,
    meters_per_spot = sum_link_distlink_length / total_parking_spots
  )

onstreet_parking <- links_in_berlin_filtered %>%
  mutate(
    spots_per_meter = case_when(
      id %in% links_in_hundekopf_filtered$id ~ inside_hundekopf_stats$spots_per_meter,
      id %in% links_outside_hundekopf_filtered$id ~ outside_hundekopf_stats$spots_per_meter,
      TRUE ~ NA
    )
  ) %>%
  transmute(id = id, onstreet_spots = round(spots_per_meter * length, 0))

onstreet_summary <- onstreet_parking %>%
  mutate(area = case_when(
    id %in% links_in_hundekopf_filtered$id ~ "inside_hundekopf",
    id %in% links_outside_hundekopf_filtered$id ~ "outside_hundekopf",
    TRUE ~ "unknown"
  )) %>%
  group_by(area) %>%
  summarise(total_spots = sum(onstreet_spots, na.rm = TRUE))


sum(links_in_hundekopf_filtered$length)
# ----------------------------------------------------------
# Question 2: Off-street parking (OSM)
# ----------------------------------------------------------

#amenity <- opq(bbox = 'Berlin, Germany') %>%
#  add_osm_feature(key = "amenity", value = "parking") %>%
#  osmdata_sf()


#amenity <- saveRDS(amenity, "/Users/gregorr/Documents/work/Paper/heartParking/data/amenity_parking_berlin.rds")

amenity <- read_rds("/Users/gregorr/Documents/work/Paper/heartParking/data/amenity_parking_berlin.rds")


# this function no longer exsist 
#tmap_options(check.and.fix = TRUE)

amenity_polygon <- amenity$osm_polygons %>%
  bind_rows(amenity$osm_multipolygons) %>%
  st_transform(25832) %>%
  filter(st_is_valid(.)) %>%
  st_filter(berlin_districts_shp)

amenity_lines <- amenity$osm_lines %>%
  bind_rows(amenity$osm_multilines) %>%
  st_transform(25832) %>%
  filter(st_is_valid(.)) %>%
  st_filter(berlin_districts_shp)

amenity_points <- amenity$osm_points %>%
  st_transform(25832) %>%
  filter(st_is_valid(.)) %>%
  st_filter(berlin_districts_shp)


# Filter out on-street parking
amenity_polygon_filtered <- amenity_polygon %>%
  filter(!parking %in% c("street_side", "lane", "on_kerb", "half_on_kerb"))


# # --- POINTS ---
# points_clean <- amenity_points %>%
#   select(-any_of("fid"))
# 
# st_write(
#   points_clean,
#   "/Users/gregorr/Documents/work/Paper/heartParking/data/amenity.gpkg",
#   layer = "points",
#   delete_dsn = TRUE
# )
# 
# # --- POLYGONS ---
# polygons_clean <- bind_rows(
#   amenity_lines,
#   amenity_polygon_filtered
# ) %>%
#   select(-any_of("fid"))
# 
# st_write(
#   polygons_clean,
#   "/Users/gregorr/Documents/work/Paper/heartParking/data/amenity.gpkg",
#   layer = "polygons",
#   append = TRUE
# )



# ----------------------------------------------------------
# Buildings = parking
# ----------------------------------------------------------

## load buildings
#building <- opq(bbox = "Berlin, Germany") %>%
#  add_osm_feature(key = "building", value = c("garages", "parking")) %>%
#  osmdata_sf()

##save raw data set extracted above
#saveRDS(building, "/Users/gregorr/Documents/work/Paper/heartParking/data/buildings_parking_berlin.rds")

building <- readRDS("/Users/gregorr/Documents/work/Paper/heartParking/data/buildings_parking_berlin.rds")

buildings_polygons <- bind_rows(building$osm_polygons, building$osm_multipolygons) %>%
  st_transform(25832) %>%
  filter(st_is_valid(.)) %>%
  st_filter(berlin_districts_shp)

buildings_points <- building$osm_points %>%
  st_transform(25832) %>%
  filter(st_is_valid(.)) %>%
  st_filter(berlin_districts_shp)

buildings_filt <- buildings_polygons %>% st_filter(amenity_polygon_filtered)

{{# --- Buildings polygons ---
st_write(
  buildings_polygons,
  "/Users/gregorr/Documents/work/Paper/heartParking/data/buildings_polygons.gpkg",
  layer = "buildings_polygons",
  append = TRUE
)

# --- Buildings points ---
st_write(
  buildings_points,
  "/Users/gregorr/Documents/work/Paper/heartParking/data/buildings.gpkg",
  layer = "buildings_points",
  append = TRUE
)}}

# --- Filtered buildings (intersecting parking areas) ---
st_write(
  buildings_filt,
  "/Users/gregorr/Documents/work/Paper/heartParking/data/buildings_filt.gpkg",
  layer = "buildings_filtered",
  append = TRUE
)


# ----------------------------------------------------------
# Capacity model for OSM amenities
# ----------------------------------------------------------

max_area <- 1500
max_capacity <- 100

amenity_polygon_filtered_area <- amenity_polygon_filtered %>%
  select(osm_id, parking, capacity, levels = "building:levels") %>%
  mutate(
    area = st_area(amenity_polygon_filtered),
    capacity = as.numeric(capacity),
    levels = as.numeric(levels),
    area_mod = as.numeric(case_when(
      is.na(levels) ~ area,
      TRUE ~ area * levels
    ))
  )

linear_model <- amenity_polygon_filtered_area %>%
  st_drop_geometry() %>%
  filter(area_mod < max_area & capacity < max_capacity) %>%
  lm(capacity ~ area_mod, data = .)

amenity_polygon_filtered_area$capacity_fitted <-
  predict(linear_model, newdata = amenity_polygon_filtered_area)

amenity_polygon_filtered_area <- amenity_polygon_filtered_area %>%
  mutate(capacity_final = case_when(
    is.na(capacity) ~ capacity_fitted,
    TRUE ~ capacity
  ))

##plotModel
# model summary
model_summary <- summary(linear_model)

# coefficients
a <- coef(linear_model)[2]  # slope
b <- coef(linear_model)[1]  # intercept

# R²
r2 <- model_summary$r.squared

label_text <- paste0(
  "y = ", round(a, 3), "x + ", round(b, 3),
  "\nR² = ", round(r2, 3)
)

ggplot(linear_model, aes(x = area_mod, y = capacity)) +
  geom_point(alpha = 0.4) +
  geom_smooth(method = "lm", se = FALSE) +
  annotate(
    "text",
    x = Inf, y = Inf,
    label = label_text,
    hjust = 1.1, vjust = 1.5,
    size = 5
  ) +
  labs(
    title = "Capacity vs. Area of Parking Facilities",
    x = "Adjusted area in square meter",
    y = "Capacity"
  ) +
  theme_minimal()



#Attach off-street parking

from_nodes <- network_berlin$links %>%
  select(id, x = x.from, y = y.from) %>%
  filter(!str_starts(id, "pt_")) %>%
  st_as_sf(coords = c("x", "y"), crs = 25832)

to_nodes <- network_berlin$links %>%
  select(id, x = x.to, y = y.to) %>%
  filter(!str_starts(id, "pt_")) %>%
  st_as_sf(coords = c("x", "y"), crs = 25832)

links <- rbind(from_nodes, to_nodes) %>%
  group_by(id) %>%
  summarise(geometry = st_combine(geometry)) %>%
  st_cast("LINESTRING")


##find closest links

offstreet_parking <- amenity_polygon_filtered_area %>%
  mutate(closest_link = links[st_nearest_feature(amenity_polygon_filtered_area, links),] %>% pull(id)) %>%
  st_drop_geometry() %>%
  group_by(closest_link) %>%
  summarise(offstreet_spots = round(sum(capacity_final), 0)) %>%
  rename(id = closest_link)



# ----------------------------------------------------------
# Write output CSV
# ----------------------------------------------------------

#we only use the offstreet parking 
on_and_offstreet_parking_per_link <- full_join(onstreet_parking, offstreet_parking)

print(paste0(
  "Onstreet: ", sum(onstreet_parking$onstreet_spots, na.rm = TRUE),
  " | Offstreet: ", sum(offstreet_parking$offstreet_spots, na.rm = TRUE)
))

write_csv(
  on_and_offstreet_parking_per_link,
  file = "/Users/gregorr/Documents/work/Paper/heartParking/data/offStreet_parking_per_link.csv"
)

View(offstreet_parking)