library(tidyverse)
library(optparse)

option_list <- list(
  make_option(c("-r", "--runDir"), type="character", default=NULL,
              help="Path to run directory. Avoid using '\', use '/' instead.", metavar="character"),
  make_option(c("-p", "--petrolPct"), type = "double", default = 1,
              help = "Share of petrol vehicles"),
  make_option(c("-b", "--bevPct"), type = "double", default = 0,
              help = "Share of bev vehicles"),
  make_option(c("-s", "--syntheticFuelPct"), type = "double", default = 0,
              help = "Share of synthetic fuel vehicles"),
  make_option(c("-w", "--h2Pct"), type = "double", default = 0,
              help = "Share of h2 vehicles")
)

opt_parser <- OptionParser(option_list=option_list)
opt <- parse_args(opt_parser)

if (is.null(opt$runDir)) {
  print_help(opt_parser)
  stop("Error: --runDir is required", call.=FALSE)
}

run_dir <- opt$runDir
run_dir_fixed <- gsub("////", "/", run_dir)

petrol_pct <- opt$petrolPct
bev_pct <- opt$bevPct
synthetic_pct <- opt$syntheticFuelPct
h2_pct <- opt$h2Pct

# if you do not want to use opt_parse, comment out the above lines starting from option_list <- ...
# you have to define the params yourself
# run_dir_fixed <- "Y:/net/ils/matsim-lausitz/caseStudies/v2.0/pt-case-study/output-lausitz-pt-case_full_plans/"
# petrol_pct <- 0.05
# bev_pct <- 0.9
# synthetic_pct <- 0
# h2_pct <- 0.05

setwd(run_dir_fixed)
print(paste("Running analysis on run dir", getwd()))

car_only_petrol_path <- list.files(path=paste0(run_dir_fixed, "/analysis/emissions-emissions-private-car-petrol-else-none/"), pattern="*emissions_total.csv", full.names = TRUE)
car_only_bev_path <- list.files(path=paste0(run_dir_fixed, "/analysis/emissions-emissions-private-car-bev-else-none/"), pattern="*emissions_total.csv", full.names = TRUE)
freight_only_path <- list.files(path=paste0(run_dir_fixed, "/analysis/emissions-emissions-freight-only/"), pattern="*emissions_total.csv", full.names = TRUE)
drt_legs_path <- list.files(path=paste0(run_dir_fixed, "/"), pattern="*output_drt_legs_drt.csv", full.names = TRUE)

# percentages for different drive train types
# dummy var which always is 1
freight_pct <- 1

safe_read_pollutants_csv <- function(path, name) {
  if (length(path) == 0 || is.null(path) || is.na(path)) {
    message("")
    message(paste("No data found for drive train type with path: ", path, "Will use dummy dataframe with 0 values for dataframe", name))
    message("")
    return(tibble(Pollutant = character(), kg = numeric(), kg_weighted = numeric()))
  }
  read_csv(path) %>%
    mutate(kg = as.numeric(kg))
}

safe_read_drt_legs_csv <- function(path, name) {
  if (length(path) == 0 || is.null(path) || is.na(path)) {
    message("")
    message(paste("No data found for drt legs with path: ", "Will use dummy dataframe with 0 values for dataframe", name))
    message("")
    return(tibble(requestId = character(), travelDistance_m = numeric()))
  }
  read_csv2(path)
}

# read data
car_only_petrol <- safe_read_pollutants_csv(car_only_petrol_path, "car_only_petrol") %>%
  rowwise() %>% 
  mutate(kg_weighted = kg * petrol_pct)
car_only_bev <- safe_read_pollutants_csv(car_only_bev_path, "car_only_bev") %>%
  mutate(kg = as.numeric(kg)) %>% 
  rowwise() %>% 
  mutate(kg_weighted = kg * bev_pct)
freight_only <- safe_read_pollutants_csv(freight_only_path, "freight_only") %>%
  mutate(kg = as.numeric(kg)) %>%
  rowwise() %>% 
  mutate(kg_weighted = kg * freight_pct)
drt_legs <- safe_read_drt_legs_csv(drt_legs_path, "drt_legs")

# synthetic is same as petrol
car_only_synthetic <- car_only_petrol %>%
  rowwise() %>% 
  mutate(kg_weighted = kg * synthetic_pct)

# h2 is same as bev
car_only_h2 <- car_only_bev %>%
  rowwise() %>%
  mutate(kg_weighted = kg * h2_pct)
  

sum_pct <- petrol_pct + bev_pct + synthetic_pct + h2_pct

if (sum_pct != 1.0) {
  stop(paste("The given percentages for petrol, bev, synthetic and h2 sum up to", sum_pct, "but should sum up to 1.0. Aborting!"))
}

combined_pollutants <- bind_rows(
  car_only_petrol,
  car_only_bev,
  car_only_h2,
  car_only_synthetic,
  freight_only
) %>%
  group_by(Pollutant) %>%
  summarise(
    kg_weighted = sum(kg_weighted, na.rm = TRUE),
    .groups = "drop"
  )

# add potential emissions by drt vehicles
# we assume that drt vehicles are bev, so only non exhaust emissions are relevant
# according to an OECD report https://www.oecd.org/en/publications/non-exhaust-particulate-emissions-from-road-transport_4a4dc6ca-en/full-report/component-7.html?utm_source=chatgpt.com; tables 3.4 and 3.5:
# for BEVs with range of 300km PM2.5 non exhaust between 0.0115-0.0169 [g/v-km]: median: 0.0142 g/v-km
# for BEVs with range of 300km PM10 non exhaust between 0.0270-0.0276 [g/v-km]: median: 0.0273 g/v-km
# there seems to be only very small BC non exhaust for BEV, so we assume 0.0001 g/km
bev_pm_25_g_km <- 0.0142
bev_pm_10_g_km <- 0.0273
bev_bc_g_km <- 0.0001

# calc average non exhaust emissions for drt legs
drt_legs_emissions <- drt_legs %>% 
  select(requestId, travelDistance_m) %>% 
  mutate(travelDistance_km = travelDistance_m / 1000) %>% 
  mutate(pm_25_average_kg = (bev_pm_25_g_km * travelDistance_km) / 1000,
         pm_10_average_kg = (bev_pm_10_g_km * travelDistance_km) / 1000,
         bc_average_kg = (bev_bc_g_km * travelDistance_km) / 1000)

sum_drt_pm_25 <- sum(drt_legs_emissions$pm_25_average_kg)
sum_drt_pm_10 <- sum(drt_legs_emissions$pm_10_average_kg)
sum_drt_bc <- sum(drt_legs_emissions$bc_average_kg)

# function add drt pollutants to combined table
add_drt_to_pollutant <- function(df, pollutant_name, add_value) {
  df %>%
    mutate(
      kg_weighted = if_else(
        Pollutant == pollutant_name,
        kg_weighted + add_value,
        kg_weighted
      )
    )
}

# add drt pollutants to combined table
combined_pollutants_w_drt <- add_drt_to_pollutant(combined_pollutants, "PM2_5_non_exhaust", sum_drt_pm_25)
combined_pollutants_w_drt <- add_drt_to_pollutant(combined_pollutants_w_drt, "PM_non_exhaust", sum_drt_pm_10)
combined_pollutants_w_drt <- add_drt_to_pollutant(combined_pollutants_w_drt, "BC_non_exhaust", sum_drt_bc)

# save to csv
output_path <- paste0(getwd(), "/analysis/emissions-emissions/emissions_total_combined.csv")
write.csv(combined_pollutants_w_drt, output_path, quote=FALSE, row.names = FALSE)
print(paste("combined pollutants with respective shares for drive train types written to", output_path))

