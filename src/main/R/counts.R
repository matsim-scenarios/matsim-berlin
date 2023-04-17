library(tidyverse)
library(matsim)
library(readr)

options(scipen = 99)

FILE_DIR <- "C:/Users/ACER/Desktop/Uni/VSP/Berlin_6_x/"

version1 <- "cadyts"
version2 <- "cadyts.02"

network <- loadNetwork(filename = paste0(FILE_DIR, "analysis/", version1, ".output_network.xml.gz"))
linkstats_old <- readLinkStats(runId = version1, file = paste0(FILE_DIR, "analysis/", version1, ".output_linkstats.csv.gz"), sampleSize = 0.25)
linkstat <- readLinkStats(runId = version2, file = paste0(FILE_DIR, "analysis/", version2, ".output_linkstats.csv.gz"), sampleSize = 0.25)

linkstat <- linkstat %>%
  mutate(linkId = as.character(linkId))
linkstats_old <- linkstats_old %>%
  mutate(linkId = as.character(linkId))

counts <- readCounts(file = paste0(FILE_DIR, "analysis/berlin-v6.0-counts-car-vmz.xml.gz")) %>%
  mutate(loc_id = as.character(loc_id))
str(network$links)

join <- mergeCountsAndLinks(counts = counts, network = network, linkStats = list(linkstats_old, linkstat), aggr_to = "day") %>%
  filter(!is.na(volume))

createCountScatterPlot(joinedFrame = join)
ggsave(filename = paste0(FILE_DIR, "Berlin_cadyts_scatterplot.png"))

quality <- processDtvEstimationQuality(joinedFrame = join)

ggplot(quality, aes(estimation, share, fill = type)) +
  
  geom_col() +
  
  labs(y = "Share", x = "Quality category") +
  
  facet_grid(src ~ type) +
  
  theme_bw() +
  
  theme(legend.position = "none", axis.text.x = element_text(angle = 90))

ggsave(filename = paste0(FILE_DIR, "Berlin_v6.x_estimation_quality.png"))
