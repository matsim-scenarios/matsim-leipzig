library(tidyverse)
library(dplyr)
library(lubridate)
library(gridExtra)
library(viridis)
library(ggsci)
library(sf)

#run ID
runID <- "umland_90_25pct"
ph <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/R_results/20220811/ph/"
pkm <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/R_results/20220811/pkm/"

#base
f <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_base/20220811/"
#90percent
#f <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario90/20220811/"
#95percent
#f <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario95/20220811/"
#99percent
#f <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario99/20220811/"

#legs <- read_delim("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_base/20220811/leipzig-flexa-25pct-scaledFleet-base_noDepot.output_legs.csv.gz",delim=";")
#legs <- read_delim("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario99/20220811/leipzig-flexa-25pct-scaledFleet-carfree99pct_noDepot.output_legs.csv.gz",delim=";")
#legs <- read_delim("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario95/20220811/leipzig-flexa-25pct-scaledFleet-carfree95pct_noDepot.output_legs.csv.gz",delim=";")
legs <- read_delim("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario90/20220811/leipzig-flexa-25pct-scaledFleet-carfree90pct_noDepot.output_legs.csv.gz",delim=";")

# contains a set of person Ids whose trips are to be considered.
shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Leipzig_puffer.shp", crs=25832)
shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Leipzig_stadt.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen90_update.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen95_update.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen99_update.shp", crs=25832)

personList <- read_delim(list.files(f, pattern = "*.output_persons.csv.gz", full.names = T, include.dirs = F) , delim = ";", trim_ws = T, 
                      col_types = cols(
                        person = col_character(),
                        good_type = col_integer()
                      )) %>%
  st_as_sf(coords = c("first_act_x", "first_act_y"), crs = 25832) %>%
  st_filter(shape)

#Leipzig
legs_filtered <- legs %>% 
  filter(person %in% personList$person)

legs_walk <- filter(legs_filtered, mode=="walk")
legs_car <- filter(legs_filtered, mode=="car")
legs_ride <- filter(legs_filtered, mode=="ride")
legs_pt <- filter(legs_filtered, mode=="pt")
legs_bike <- filter(legs_filtered, mode=="bike")
legs_drtN <- filter(legs_filtered, mode=="drtNorth")
legs_drtS <- filter(legs_filtered, mode=="drtSoutheast")

n_distinct(legs_bike$person)
n_distinct(legs_car$person)
n_distinct(legs_drtN$person)
n_distinct(legs_drtS$person)
n_distinct(legs_pt$person)
n_distinct(legs_ride$person)
n_distinct(legs_walk$person)

legs_modalDistances <- legs_filtered %>% 
  select(mode, distance) %>% 
  group_by(mode) %>% 
  summarise(totalDistance_km = sum(distance) / 1000) %>% 
  column_to_rownames(var = "mode")

legs_modalDistances <- as_tibble(t(legs_modalDistances)) 
#row.names(legs_modalDistances) -> "totalDistance_km"

fileName <- paste(pkm, runID, ".output_legs_km_per_mode", sep="")
print(paste("writing ouput to", fileName))
#write.csv2(legs_modalDistances, file = paste(fileName, ".csv", sep = ""), row.names = FALSE, quote = FALSE)
#write_tsv(legs_modalDistances, file = paste(fileName, ".tsv", sep = ""),  quote = FALSE)
#write_delim(legs_modalDistances, file = paste(fileName, ".tsv", sep = ""))
write.table(legs_modalDistances, file = paste(fileName, ".txt", sep = ""), sep = '\t', dec = '.', row.names = FALSE, quote = FALSE, col.names = TRUE)

legs_modalTravelTimes <- legs_filtered %>% 
  select(mode, trav_time) %>% 
  mutate(trav_time = hms(trav_time)) %>% 
  group_by(mode) %>%  
  summarise(totalTavelTime_h = sum(totalTavelTime_h = hour(trav_time) + minute(trav_time) /60 + second(trav_time) /3600)) %>% 
  column_to_rownames(var = "mode")

legs_modalTravelTimes <- as_tibble(t(legs_modalTravelTimes)) 
fileName <- paste(ph, runID, ".output_legs_ph_per_mode", sep="")
print(paste("writing ouput to", fileName))
write.table(legs_modalTravelTimes, file = paste(fileName, ".txt", sep = ""), sep = '\t', dec = '.', row.names = FALSE, quote = FALSE, col.names = TRUE)

