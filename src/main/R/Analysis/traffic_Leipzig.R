library(tidyverse)
library(dplyr)
library(lubridate)
library(gridExtra)
library(viridis)
library(ggsci)
library(sf)



# contains a set of links Ids which are to be considered.
shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Leipzig_puffer.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Leipzig_stadt.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen90_update.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen95_update.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen99_update.shp", crs=25832)

linkList <- read_delim(list.files("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/networks/leipzig-flexa-25pct-scaledFleet-baseCase_noDepot.output_network.xml.gz", full.names = T, include.dirs = F) , delim = ";", trim_ws = T, 
                         col_types = cols(
                           person = col_character(),
                           good_type = col_integer()
                         )) %>%
  st_as_sf(coords = c("x", "y"), crs = 25832) %>%
  st_filter(shape)




#Leipzig
links_filtered <- network %>% 
  filter(links %in% linkList$link)

traffic_base <- read_table("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/simwrapper/dashboard/simwrapper/05_traffic/base/traffic_output_base.csv")
mean_base_cong = mean(traffic_base$congestion_index)
print(mean_base_cong)
median_base_cong = median(traffic_base$congestion_index)
print(median_base_cong)

mean_base_speed = mean(traffic_base$average_daily_speed)
print(mean_base_speed)
median_base_speed = median(traffic_base$average_daily_speed)
print(median_base_speed)

traffic_99 <- read_table("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/simwrapper/dashboard/simwrapper/05_traffic/99/traffic_output_99.csv")
mean_99_cong = mean(traffic_99$congestion_index)
print(mean_99_cong)
median_99_cong = median(traffic_99$congestion_index)
print(median_99_cong)

mean_99_speed = mean(traffic_99$average_daily_speed)
print(mean_99_speed)
median_99_speed = median(traffic_99$average_daily_speed)
print(median_99_speed)

traffic_95 <- read_table("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/simwrapper/dashboard/simwrapper/05_traffic/95/traffic_output_95.csv")
mean_95_cong = mean(traffic_95$congestion_index)
print(mean_95_cong)
median_95_cong = median(traffic_95$congestion_index)
print(median_95_cong)

mean_95_speed = mean(traffic_95$average_daily_speed)
print(mean_95_speed)
median_95_speed = median(traffic_95$average_daily_speed)
print(median_95_speed)

traffic_90 <- read_table("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/simwrapper/dashboard/simwrapper/05_traffic/90/traffic_output_90.csv")
mean_90_cong = mean(traffic_90$congestion_index)
print(mean_90_cong)
median_90_cong = median(traffic_90$congestion_index)
print(median_90_cong)

mean_90_speed = mean(traffic_90$average_daily_speed)
print(mean_90_speed)
median_90_speed = median(traffic_90$average_daily_speed)
print(median_90_speed)

traffic_base_u <- read_table("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/R_results/traffic/traffic_output_base_umland.csv")
mean_base_cong_u = mean(traffic_base_u$congestion_index)
print(mean_base_cong)
median_base_cong_u = median(traffic_base_u$congestion_index)
print(median_base_cong)

mean_base_speed_u = mean(traffic_base_u$average_daily_speed)
print(mean_base_speed_u)
median_base_speed_u = median(traffic_base_u$average_daily_speed)
print(median_base_speed_u)



