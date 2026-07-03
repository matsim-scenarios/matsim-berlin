library(apollo)
library(tidyverse)
library(dplyr)
library(patchwork)
library(networkD3)
library(sf)
library(stringr)
library(ggalluvial)
library(reshape2)

# ----------------------------------------------------------
# Load data
# ----------------------------------------------------------

persons <- read_delim(
  "/Users/gregorr/Documents/work/respos/shared-svn/projects/matsim-berlin/data/SrV/2018/converted/table-persons.csv",
  delim = ","
)

household <- read_delim(
  "/Users/gregorr/Documents/work/respos/shared-svn/projects/matsim-berlin/data/SrV/2018/converted/table-households.csv",
  delim = ","
)

# ----------------------------------------------------------
# Parking categories
# ----------------------------------------------------------

summary(household$car_parking)

table(household$car_parking, useNA = "ifany")

household %>%
  count(car_parking) %>%
  mutate(share = n / sum(n))

# ----------------------------------------------------------
# Berlin households only
# ----------------------------------------------------------

household_berlin <- household %>%
  filter(location == "Berlin")

table(household_berlin$car_parking, useNA = "ifany")

# ----------------------------------------------------------
# Parking type by economic status
# ----------------------------------------------------------

household_berlin %>%
  filter(car_parking != "na") %>%
  mutate(
    economic_status = factor(
      economic_status,
      levels = c("very_low", "low", "medium", "high", "very_high")
    )
  ) %>%
  count(economic_status, car_parking, wt = h_weight) %>%
  group_by(economic_status) %>%
  mutate(share = n / sum(n)) %>%
  ggplot(aes(x = economic_status, y = share, fill = car_parking)) +
  geom_col() +
  scale_y_continuous(labels = scales::percent) +
  labs(
    title = "Parking availability by economic status (Berlin)",
    x = "Economic status",
    y = "Share",
    fill = "Parking type"
  ) +
  theme_minimal() +
  coord_flip()

# ----------------------------------------------------------
# Private vs public parking by zone
# ----------------------------------------------------------

parking_zone_ratio <- household_berlin %>%
  filter(car_parking %in% c("private", "public")) %>%
  group_by(zone) %>%
  summarise(
    private_share = weighted.mean(car_parking == "private", h_weight),
    public_share = weighted.mean(car_parking == "public", h_weight),
    weighted_households = sum(h_weight),
    .groups = "drop"
  )

summary(parking_zone_ratio$private_share)

# ----------------------------------------------------------
# Distribution of private parking shares across zones
# ----------------------------------------------------------

ggplot(parking_zone_ratio, aes(private_share)) +
  geom_histogram(bins = 30) +
  scale_x_continuous(labels = scales::percent) +
  labs(
    title = "Distribution of private parking shares across zones",
    x = "Private parking share",
    y = "Number of zones"
  ) +
  theme_minimal()

# ----------------------------------------------------------
# Ranked zones
# ----------------------------------------------------------

parking_zone_ratio %>%
  arrange(private_share) %>%
  mutate(rank = row_number()) %>%
  ggplot(aes(rank, private_share)) +
  geom_line() +
  scale_y_continuous(labels = scales::percent) +
  labs(
    title = "Private parking share by zone",
    x = "Zone rank",
    y = "Private parking share"
  ) +
  theme_minimal()

# ----------------------------------------------------------
# Top 20 zones with highest private parking share
# ----------------------------------------------------------

top_private <- parking_zone_ratio %>%
  arrange(desc(private_share)) %>%
  select(zone, private_share, public_share, weighted_households) %>%
  head(20)

print(top_private)

# ----------------------------------------------------------
# Top 20 zones with lowest private parking share
# ----------------------------------------------------------

bottom_private <- parking_zone_ratio %>%
  arrange(private_share) %>%
  select(zone, private_share, public_share, weighted_households) %>%
  head(20)

print(bottom_private)

# ----------------------------------------------------------
# Berlin-wide average
# ----------------------------------------------------------

berlin_average <- household_berlin %>%
  filter(car_parking %in% c("private", "public")) %>%
  summarise(
    private_share = weighted.mean(car_parking == "private", h_weight),
    public_share = weighted.mean(car_parking == "public", h_weight)
  )

print(berlin_average)

# ----------------------------------------------------------
# Zones deviating from Berlin average
# ----------------------------------------------------------

berlin_private_share <- berlin_average$private_share

parking_zone_ratio <- parking_zone_ratio %>%
  mutate(
    deviation_from_berlin = private_share - berlin_private_share
  )

parking_zone_ratio %>%
  arrange(desc(abs(deviation_from_berlin))) %>%
  select(
    zone,
    private_share,
    public_share,
    deviation_from_berlin
  ) %>%
  head(20)

# ----------------------------------------------------------
# Distribution of deviations
# ----------------------------------------------------------

ggplot(
  parking_zone_ratio,
  aes(x = deviation_from_berlin)
) +
  geom_histogram(bins = 30) +
  scale_x_continuous(labels = scales::percent) +
  labs(
    title = "Deviation of zone private parking share from Berlin average",
    x = "Deviation from Berlin average",
    y = "Number of zones"
  ) +
  theme_minimal()