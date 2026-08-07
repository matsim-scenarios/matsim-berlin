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

# calc dtv
dtv_bicycle_counts <- daily %>%
  summarise(
    across(where(is.numeric), ~ round(mean(.x, na.rm = TRUE)))
  )

write.csv(dtv_bicycle_counts, file="dtv_typical_weekday_bicycle_counts_2018.csv", quote=FALSE, row.names=FALSE)
