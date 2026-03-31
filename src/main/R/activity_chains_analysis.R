library(tidyverse)
library(optparse)

# this script reads output activities from matsim run output and prints activity chains according to their frequency in the given population.

########################################## input params #########################################################################

option_list <- list(
  make_option(c("-r", "--runDir"), type="character", default=NULL,
              help="Path to drt run directory. Avoid using '/', use '/' instead.", metavar="character"))

opt_parser <- OptionParser(option_list=option_list)
opt <- parse_args(opt_parser)

run_dir <- opt$runDir
run_dir_fixed <- gsub("////", "/", run_dir)

# if you do not want to use opt_parse, comment out the above lines starting from option_list <- ...
# you have to define run_dir_fixed yourself
# run_dir_fixed <- "D:/public-svn/matsim/scenarios/countries/de/berlin/projects/mobilityToGrid/3pct/output-berlin-v6.4-3pct-base-case-ctd-3-3/"

setwd(run_dir_fixed)
print(paste("Running analysis on run dir", getwd()))

activities_path <- list.files(path=run_dir_fixed, pattern="output_activities\\.csv\\.gz$", full.names = TRUE)

############################################## get the data ##################################################################

activities <- read_csv2(file=activities_path)

############################################# analysis ####################################################

activities_persons <- activities %>% 
  filter(str_detect(person, "bb_|berlin_")) %>% 
  mutate(act_type_string_only = str_remove(activity_type, "_[^_]+$"))

activities_bb <- activities_persons %>% 
  filter(str_detect(person, "bb_"))

activities_berlin <- activities_persons %>% 
  filter(str_detect(person, "berlin_"))

activities_summary <- activities_persons %>%
  group_by(person) %>%
  summarise(
    act_chain = paste(act_type_string_only, collapse = "-"),
    .groups = "drop"
  ) %>% 
  count(act_chain) %>%
  mutate(share = n / sum(n)) %>% 
  arrange(desc(share))

activities_bb_summary <- activities_bb %>%
  group_by(person) %>%
  summarise(
    act_chain = paste(act_type_string_only, collapse = "-"),
    .groups = "drop"
  ) %>% 
  count(act_chain) %>%
  mutate(share = n / sum(n)) %>% 
  arrange(desc(share))

activities_berlin_summary <- activities_berlin %>%
  group_by(person) %>%
  summarise(
    act_chain = paste(act_type_string_only, collapse = "-"),
    .groups = "drop"
  ) %>% 
  count(act_chain) %>%
  mutate(share = n / sum(n)) %>% 
  arrange(desc(share))

# print to csv file for each summary df
write.csv(activities_summary, "act_chains_person_agents.csv", quote=FALSE, row.names = FALSE)
print(paste("activity chains for person agents and their frequency in the population were written to", getwd()))
write.csv(activities_bb_summary, "act_chains_person_agents_brandenburg.csv", quote=FALSE, row.names = FALSE)
print(paste("activity chains for brandenburg person agents and their frequency in the population were written to", getwd()))
write.csv(activities_berlin_summary, "act_chains_person_agents_berlin.csv", quote=FALSE, row.names = FALSE)
print(paste("activity chains for berlin person agents and their frequency in the population were written to", getwd()))

