library(gridExtra)
library(tidyverse)
library(lubridate)
library(viridis)
library(ggsci)
library(sf)

#base
f <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_base/20220811/"
#90percent
g <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario90/20220811/"
#95percent
h <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario95/20220811/"
#99percent
i <- "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/25pct_scenario99/20220811/"

sim_scale <- 10

# breaks in meter
breaks = c(0, 1000, 2000, 5000, 10000, 20000, Inf)

#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Leipzig_stadt.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen90_update.shp", crs=25832)
#shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen95_update.shp", crs=25832)
shape <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen99_update.shp", crs=25832)

#base
persons <- read_delim(list.files(f, pattern = "*.output_persons.csv.gz", full.names = T, include.dirs = F) , delim = ";", trim_ws = T, 
                      col_types = cols(
                        person = col_character(),
                        good_type = col_integer()
                      )) %>%
  st_as_sf(coords = c("first_act_x", "first_act_y"), crs = 25832) %>%
  st_filter(shape)

residents <- nrow(persons)

trips <- read_delim(list.files(f, "*.output_trips.csv.gz", full.names = T, include.dirs = F) , delim = ";", trim_ws = T, 
                    col_types = cols(
                      person = col_character()
                    )) %>%
  mutate(main_mode=longest_distance_mode) %>%
  filter(main_mode!="freight") %>%
  semi_join(persons) %>%
  mutate(dist_group = cut(traveled_distance, breaks=breaks, labels= c("0-1000","1000-2000","2000-5000","5000-10000","10000-20000",">20000"))) %>%
  filter(!is.na(dist_group))

sim <- trips %>%
  group_by(dist_group, main_mode) %>%
  summarise(trips=n()) %>%
  mutate(mode = fct_relevel(main_mode, "walk", "bike", "pt", "ride", "car","freight")) %>%
  mutate(scaled_trips=sim_scale * trips) %>%
  mutate(source = "sim")

sim <- sim %>%
  mutate(share=trips/sum(sim$trips))

write_csv(sim, "/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/carfree_scenario/R_results/20220811/modalsplit/Leipzig_99_25pct.csv")

