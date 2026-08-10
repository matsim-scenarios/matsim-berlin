library(tidyverse)
library(lubridate)
library(readxl)

# read data and rename
bicycle_counts_2018 <- read_excel("../../runs-svn/matsim-berlin/v6.4_bike_network_study/gesamtdatei-stundenwerte.xlsx", sheet="Jahresdatei 2018") %>% 
  rename(date = 'Zählstelle        Inbetriebnahme',
         Jannowitzbruecke_N = '02-MI-JAN-N 01.04.2015',
         Jannowitzbruecke_S = '02-MI-JAN-S 01.04.2015',
         Invalidenstr_O = '03-MI-SAN-O 01.06.2015',
         Invalidenstr_W = '03-MI-SAN-W 01.06.2015',
         Oberbaumbruecke_O = '05-FK-OBB-O 01.06.2015',
         Oberbaumbruecke_W = '05-FK-OBB-W 01.06.2015',
         Frankfurter_Allee_O = '06-FK-FRA-O 01.06.2016',
         Frankfurter_Allee_W = '06-FK-FRA-W 01.06.2016',
         Berliner_Str_N = '10-PA-BER-N 01.05.2016',
         Berliner_Str_S = '10-PA-BER-S 01.05.2016',
         Schwedter_Steg = '12-PA-SCH 01.03.2012',
         Prinzregentenstr = '13-CW-PRI 01.04.2015',
         Klosterstr_N = '15-SP-KLO-N 01.06.2016',
         Klosterstr_S = '15-SP-KLO-S 01.06.2016',
         Breitenbachplatz_O = '17-SZ-BRE-O 01.05.2016',
         Breitenbachplatz_W = '17-SZ-BRE-W 01.05.2016',
         Yorckstr_O = '18-TS-YOR-O 01.04.2015',
         Yorckstr_W = '18-TS-YOR-W 01.04.2015',
         Monumentenst = '19-TS-MON 01.05.2015',
         Mariendorfer_Damm_N = '20-TS-MAR-N  01.05.2016',
         Mariendorfer_Damm_S = '20-TS-MAR-S 01.05.2016',
         Maybachufer = '21-NK-MAY 01.05.2016',
         Kaisersteg = '23-TK-KAI 01.05.2016',
         Alberichstr = '24-MH-ALB 01.07.2015',
         Paul_Paula_Uferweg = '26-LI-PUP 01.06.2015',
         Marktstr = '27-RE-MAR 01.05.2015')

head(bicycle_counts_2018)
names(bicycle_counts_2018)

# holidays
new_year <- interval(ymd("2018-01-01"), ymd("2018-01-01"))
karfreitag <- interval(ymd("2018-03-30"), ymd("2018-03-30"))
ostermontag <- interval(ymd("2018-04-02"), ymd("2018-04-02"))
tag_der_arbeit <- interval(ymd("2018-05-01"), ymd("2018-05-01"))
christi_himmelfahrt <- interval(ymd("2018-05-10"), ymd("2018-05-10"))
pfingstmontag <- interval(ymd("2018-05-21"), ymd("2018-05-21"))
deutsche_einheit <- interval(ymd("2018-10-03"), ymd("2018-10-03"))
weihnachten <- interval(ymd("2018-12-24"), ymd("2018-12-31"))

# filter out holidays, non typical weekdays
working_day_bicycle_counts_2018 <- bicycle_counts_2018 %>% 
  mutate(day=as.Date(date),
         weekday=wday(date, week_start = 1)) %>% 
  filter(! day %within% new_year,
         ! day %within% karfreitag,
         ! day %within% ostermontag,
         ! day %within% tag_der_arbeit,
         ! day %within% christi_himmelfahrt,
         ! day %within% pfingstmontag,
         ! day %within% deutsche_einheit,
         ! day %within% weihnachten) %>% 
  filter(weekday == 2 | weekday == 3 | weekday == 4)

# daily count sums
daily <- working_day_bicycle_counts_2018 %>% 
  group_by(day) %>% 
  summarise(
    across(where(is.numeric), ~ sum(.x, na.rm = TRUE)),
    .groups = "drop"
  ) %>% 
  select(-weekday)

# calc dtv and bring into correct format for station location data
dtv_bicycle_counts <- daily %>%
  summarise(
    across(where(is.numeric), ~ round(mean(.x, na.rm = TRUE)))
  )

dtv_bicycle_counts <- dtv_bicycle_counts %>% 
  pivot_longer(cols = names(dtv_bicycle_counts)) %>% 
  rename(station = 'name',
         dtv_2018 = 'value')

# read count location data
count_stations_location <- read_excel("../../runs-svn/matsim-berlin/v6.4_bike_network_study/gesamtdatei-stundenwerte.xlsx", sheet="Standortdaten") %>%
  rename(station = 'Zählstelle',
         description_direction = 'Beschreibung - Fahrtrichtung',
         x = 'Längengrad',
         y = 'Breitengrad')

names(count_stations_location)

station_lookup <- c('02-MI-JAN-N' = 'Jannowitzbruecke_N',
                    '02-MI-JAN-S' = 'Jannowitzbruecke_S',
                    '03-MI-SAN-O' = 'Invalidenstr_O',
                    '03-MI-SAN-W' = 'Invalidenstr_W',
                    '05-FK-OBB-O' = 'Oberbaumbruecke_O',
                    '05-FK-OBB-W' = 'Oberbaumbruecke_W',
                    '06-FK-FRA-O' = 'Frankfurter_Allee_O',
                    '06-FK-FRA-W' = 'Frankfurter_Allee_W',
                    '10-PA-BER-N' = 'Berliner_Str_N',
                    '10-PA-BER-S' = 'Berliner_Str_S',
                    '12-PA-SCH' = 'Schwedter_Steg',
                    '13-CW-PRI' = 'Prinzregentenstr',
                    '15-SP-KLO-N' = 'Klosterstr_N',
                    '15-SP-KLO-S' = 'Klosterstr_S',
                    '17-SK-BRE-O' = 'Breitenbachplatz_O',
                    '17-SK-BRE-W' = 'Breitenbachplatz_W',
                    '18-TS-YOR-O' = 'Yorckstr_O',
                    '18-TS-YOR-W' = 'Yorckstr_W',
                    '19-TS-MON' = 'Monumentenst',
                    '20-TS-MAR-N' = 'Mariendorfer_Damm_N',
                    '20-TS-MAR-S' = 'Mariendorfer_Damm_S',
                    '21-NK-MAY' = 'Maybachufer',
                    '23-TK-KAI' = 'Kaisersteg',
                    '24-MH-ALB' = 'Alberichstr',
                    '26-LI-PUP' = 'Paul_Paula_Uferweg',
                    '27-RE-MAR' = 'Marktstr')

count_stations_location$station <- station_lookup[count_stations_location$station]

# some stations did not exist in 2018, drop them
count_stations_location_2018 <- count_stations_location %>% 
  filter(!is.na(station))

# manual station to matsim link assignment
station_to_links <- c("909402342",
                      "29383966",
                      paste0("42104389#1,", "1119248953#1"),
                      paste0("248010194,", "1119248951"),
                      "127734801",
                      "909409602",
                      paste0("310172796#0,", "1123298492#1"),
                      "6263126#0",
                      "172439811#0",
                      "4631949",
                      NA,
                      paste0("814840750#1,", "-814840750#1"),
                      paste0("31697122#0,", "703182870#2,", "-824819958"),
                      paste0("333500494#0,", "824819958"),
                      "4405253#0",
                      "4405254#0",
                      paste0("-25178767#2,", "863543061"),
                      "25178766",
                      paste0("-527065428#0,", "337967751#4"),
                      "1101002048#3",
                      "881575686",
                      paste0("-845476767,", "249453223#2"),
                      NA,
                      paste0("-514540368#1,", "514540368#1"),
                      NA,
                      paste0("4610040,", "-4610040")
)

dtv_bicycle_counts_with_locations <- dtv_bicycle_counts %>% 
  inner_join(y=count_stations_location_2018, by='station') %>% 
  mutate(links = station_to_links) %>% 
  filter(!is.na(links))

write.csv(dtv_bicycle_counts_with_locations, file="dtv_typical_weekday_bicycle_counts_2018_wgs84.csv", quote=FALSE, row.names=FALSE)
