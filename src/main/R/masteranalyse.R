#### reading shp files ####

region_shp_path <- "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/NaMAV/data/shapefiles/leipzig_region/Leipzig_puffer.shp"
city_shp_path <- "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/NaMAV/data/shapefiles/leipzig_stadt/Leipzig_stadt.shp"
RegionShape <- st_read(region_shp_path, crs=25832) #study area
CityShape <- st_read(city_shp_path, crs=25832) #city of Leipzig
print("#### Shapes geladen! ####")


BASEtripsTable <- readTripsTable(pathToMATSimOutputDirectory = base_trips_path)
SCENARIOtripsTable <- readTripsTable(pathToMATSimOutputDirectory = scenario_trips_path)
BASELegsTable <- read_delim(base_legs_path, delim= ";")
SCENARIOlegsTable <- read_delim(scenario_legs_path, delim= ";")

print("#### Trips und Legs fertig! ####")

#### reading trips/legs files ####
base_trips_region <- filterByRegion(BASEtripsTable,RegionShape,crs=25832,start.inshape = TRUE,end.inshape = TRUE)
base_trips_city <- filterByRegion(BASEtripsTable,CityShape,crs=25832,start.inshape = TRUE,end.inshape = TRUE)
scenario_trips_region <- filterByRegion(SCENARIOtripsTable,RegionShape,crs=25832,start.inshape = TRUE,end.inshape = TRUE)
scenario_trips_city <- filterByRegion(SCENARIOtripsTable,CityShape,crs=25832,start.inshape = TRUE,end.inshape = TRUE)

print("#### Trips gefiltert! ####")

base_legs_region <- filterByRegion(BASELegsTable,RegionShape,crs=25832,start.inshape = TRUE,end.inshape = TRUE)
base_legs_city <- filterByRegion(BASELegsTable,CityShape,crs=25832,start.inshape = TRUE,end.inshape = TRUE)
scenario_legs_region <- filterByRegion(SCENARIOlegsTable,RegionShape,crs=25832,start.inshape = TRUE,end.inshape = TRUE)
scenario_legs_city <- filterByRegion(SCENARIOlegsTable,CityShape,crs=25832,start.inshape = TRUE,end.inshape = TRUE)


print("#### Legs gefiltert! ####")
#### 0. Parameters ####

#BREAKING DIFFERENT DISTANCES IN M
breaks = c(0, 1000, 2000, 5000, 10000, 20000, Inf)

#NAMES OF THE CASES
cases <- c("base", "scenario")

#### #1.1 Average Distance - trips based####

if (x_average_traveled_distance_trips == 1){
  #calculation
  avg_trav_dist_trips_base_region <- mean(base_trips_region$traveled_distance)
  avg_trav_dist_trips_base_city <- mean(base_trips_city$traveled_distance)
  avg_trav_dist_trips_scenario_region <- mean(scenario_trips_region$traveled_distance)
  avg_trav_dist_trips_scenario_city <- mean(scenario_trips_city$traveled_distance)
  avg_eucl_dist_trips_base_region <- mean(base_trips_region$euclidean_distance)
  avg_eucl_dist_trips_base_city <- mean(base_trips_city$euclidean_distance)
  avg_eucl_dist_trips_scenario_region <- mean(scenario_trips_region$euclidean_distance)
  avg_eucl_dist_trips_scenario_city <- mean(scenario_trips_city$euclidean_distance)
  
  #join to one table
  avg_dist_trips_city <- c(avg_trav_dist_trips_base_city,avg_trav_dist_trips_scenario_city,
                      avg_eucl_dist_trips_base_city,avg_eucl_dist_trips_scenario_city)
  
  avg_dist_trips_city <- data.frame(cases, avg_dist_trips_city)
  avg_dist_trips_region <- c(avg_trav_dist_trips_base_region,avg_trav_dist_trips_scenario_region,
                           avg_eucl_dist_trips_base_region,avg_eucl_dist_trips_scenario_region)
  
  avg_dist_trips_region <- data.frame(cases, avg_dist_trips_region)
  #write table
  write.csv(avg_dist_trips_city, file = paste0(outputDirectoryGeneral,"/avg_dist_trips_city.csv"))
  write.csv(avg_dist_trips_region, file = paste0(outputDirectoryGeneral,"/avg_dist_trips_region.csv"))
}
#### #1.2 Average Distance - legs based#####

if (x_average_traveled_distance_legs == 1) {
  #calculation
  avg_trav_dist_legs_base_region <- mean(base_legs_region$distance)
  avg_trav_dist_legs_base_city <- mean(base_legs_city$distance)
  avg_trav_dist_legs_scenario_region <- mean(scenario_legs_region$distance)
  avg_trav_dist_legs_scenario_city <- mean(scenario_legs_city$distance)
  #join to one table
  avg_dist_legs_city <- c(avg_trav_dist_legs_base_city,avg_trav_dist_legs_scenario_city)
  
  avg_dist_legs_city <- data.frame(cases, avg_dist_legs_city)
  avg_dist_legs_region <- c(avg_trav_dist_legs_base_region,avg_trav_dist_legs_scenario_region)
  
  avg_dist_legs_region <- data.frame(cases, avg_dist_legs_region)
  #write table
  write.csv(avg_dist_legs, file = paste0(outputDirectoryGeneral,"/avg_dist_legs.csv"))
}

#### #1.3 Average Travel Time - trips based #####

if (x_average_time_trips == 1){
  #calculation
  avg_time_trips_base_city <- mean(base_trips_city$trav_time)
  avg_time_trips_scenario_city <- mean(scenario_trips_city$trav_time)
  avg_time_trips_base_region <- mean(base_trips_region$trav_time)
  avg_time_trips_scenario_region <- mean(scenario_trips_region$trav_time)
  #join to one table
  avg_time_trips <- c(avg_time_trips_base_city,avg_time_trips_scenario_city,
                     avg_time_trips_base_region,avg_time_trips_scenario_region)
  #write table
  avg_time_trips <- data.frame(cases, avg_time_trips)
  write.csv(avg_time_trips, file = paste0(outputDirectoryGeneral,"/avg_time_trips.csv"))
}
#### #1.4 Average Travel Time - legs based #####

if (x_average_time_legs == 1){
  avg_time_legs_base_city <- mean(base_legs_city$trav_time)
  avg_time_legs_scenario_city <- mean(scenario_legs_city$trav_time)
  avg_time_legs_base_region <- mean(base_legs_region$trav_time)
  avg_time_legs_scenario_region <- mean(scenario_legs_region$trav_time)
  
  
  avg_time_legs <- c(avg_time_legs_base,avg_time_legs_scenario,
                     avg_time_legs_base_region,avg_time_legs_scenario_region)
  avg_time_legs <- data.frame(cases, avg_time_legs)
  write.csv(avg_time_legs, file = paste0(outputDirectoryGeneral,"/avg_time_legs.csv"))}

#### #1.5 Average Speed ####

if (x_average_traveled_speed_trips == 1){
# x) function
avg_trav_distance <- function(x){
  x %>%
    select(main_mode, traveled_distance) %>% 
    group_by(main_mode) %>% 
    summarise(avg_trav_distance = mean(traveled_distance)) %>% 
    column_to_rownames(var = "main_mode")
}
avg_trav_time <- function(x){
  x %>%
    select(main_mode, trav_time) %>% 
    mutate(trav_time = hms(trav_time)) %>% 
    group_by(main_mode) %>%  
    summarise(avgTime_s = mean(hour(trav_time)*3600 + minute(trav_time) *60 + second(trav_time) )) %>% 
    column_to_rownames(var = "main_mode")
}  

avg_trav_dist_base_city <- avg_trav_distance(base_trips_city)
avg_trav_dist_base_region <- avg_trav_distance(base_trips_region)
avg_trav_dist_scenario_city <- avg_trav_distance(scenario_trips_city)
avg_trav_dist_scenario_region <- avg_trav_distance(scenario_trips_region)

avg_trav_time_base_city <- avg_trav_time(base_trips_city)
avg_trav_time_base_region <- avg_trav_time(base_trips_region)
avg_trav_time_scenario_city <- avg_trav_time(scenario_trips_city)
avg_trav_time_scenario_region <- avg_trav_time(scenario_trips_region)

avg_trav_speed_base_city = avg_trav_dist_base_city/avg_trav_time_base_city*3.6 #km/h
avg_trav_speed_base_region = avg_trav_dist_base_region/avg_trav_time_base_region*3.6 #km/h
avg_trav_speed_scenario_city = avg_trav_dist_scenario_city/avg_trav_time_scenario_city*3.6 #km/h
avg_trav_speed_scenario_region = avg_trav_dist_scenario_region/avg_trav_time_scenario_region*3.6 #km/h


avg_trav_speed <- data.frame(X = c("base_city","base_region","scenario_city","scenario_region"), 
                            TRAV = c(avg_trav_dist_base_city,avg_trav_dist_base_region,avg_trav_dist_scenario_city,avg_trav_dist_scenario_region),
                            TIME= c(avg_trav_time_base_city,avg_trav_time_base_region,avg_trav_time_scenario_city,avg_trav_time_scenario_region),
                            SPEED = c(avg_trav_speed_base_city,avg_trav_speed_base_region,avg_trav_speed_scenario_city,avg_trav_speed_scenario_region))

write.csv(avg_trav_speed, file = paste0(outputDirectoryGeneral,"/avg_trav_speed.csv"))
}

#### #1.6 Average Beeline Speed ####

if (x_average_beeline_speed_trips == 1){
# x) function
avg_beeline_distance <- function(x){
  x %>%
    select(main_mode, euclidean_distance) %>% 
    group_by(main_mode) %>% 
    summarise(avg_beeline_distance = mean(euclidean_distance)) %>% 
    column_to_rownames(var = "main_mode")
}

avg_trav_time <- function(x){
  x %>%
    select(main_mode, trav_time) %>% 
    mutate(trav_time = hms(trav_time)) %>% 
    group_by(main_mode) %>%  
    summarise(avgTime_s = mean(hour(trav_time)*3600 + minute(trav_time) *60 + second(trav_time) )) %>% 
    column_to_rownames(var = "main_mode")
}  
# average beeline distance and average travel time
avg_beeline_dist_base_city <- avg_beeline_distance(base_trips_city)
avg_beeline_dist_base_region <- avg_beeline_distance(base_trips_region)
avg_beeline_dist_scenario_city <- avg_beeline_distance(scenario_trips_city)
avg_beeline_dist_scenario_region <- avg_beeline_distance(scenario_trips_region)
avg_trav_time_base_city <- avg_trav_time(base_trips_city)
avg_trav_time_base_region <- avg_trav_time(base_trips_region)
avg_trav_time_scenario_city <- avg_trav_time(scenario_trips_city)
avg_trav_time_scenario_region <- avg_trav_time(scenario_trips_region)
# average beeline speed
avg_beeline_speed_base_city = avg_beeline_dist_base_city/avg_trav_time_base_city*3.6 #km/h
avg_beeline_speed_base_region = avg_beeline_dist_base_region/avg_trav_time_base_region*3.6
avg_beeline_speed_scenario_city = avg_beeline_dist_policy_city/avg_trav_time_policy_city*3.6
avg_beeline_speed_scenario_region = avg_beeline_dist_policy_region/avg_trav_time_policy_region*3.6

avg_beeline_speed <- data.frame(X = c("base_city","base_region","scenario_city","scenario_region"), 
                             TRAV = c(avg_beeline_dist_base_city,avg_beeline_dist_base_region,avg_beeline_dist_scenario_city,avg_beeline_dist_scenario_region),
                             TIME= c(avg_trav_time_base_city,avg_trav_time_base_region,avg_trav_time_scenario_city,avg_trav_time_scenario_region),
                             SPEED = c(avg_beeline_speed_base_city,avg_beeline_speed_base_region,avg_beeline_speed_scenario_city,avg_beeline_speed_scenario_region))

write.csv(avg_beeline_speed, file = paste0(outputDirectoryGeneral,"/avg_beeline_speed.csv"))
} 

#### #2.1 Personen KM - trips based ####
if (x_personen_km_trips==1){
  personen_km_trips <- function (x){
    x %>% 
      filter(main_mode!="freight") %>% 
      summarise(pers_km = (sum(traveled_distance)/length(unique(person)))/1000)
  }
  
  pkm_trips_base_city <- personen_km_trips(base_trips_city)
  pkm_trips_base_region <- personen_km_trips(base_trips_region)
  pkm_trips_scenario_city <- personen_km_trips(scenario_trips_city)
  pkm_trips_scenario_region <- personen_km_trips(scenario_trips_region)
  pkm_trips <- bind_rows("base_city" = pkm_trips_base_city,
                         "base_region" = pkm_trips_base_region,
                         "scenario_city" = pkm_trips_scenario_city,
                         "scenario_region" = pkm_trips_scenario_region,
                         .id = "case")
  
  write.csv(pkm_trips, file = paste0(outputDirectoryGeneral,"/pkm_trips.csv"))
}

#### #2.2 Personen KM - legs based ####
if (x_personen_km_legs == 1){
  personen_km_legs <- function (x){
    x %>% 
      summarise(pers_km = (sum(distance)/length(unique(person)))/1000) 
    
  }
  
  pkm_legs_base_city <- personen_km_legs(base_legs_city)
  pkm_legs_base_region <- personen_km_legs(base_legs_region)
  pkm_legs_scenario_city <- personen_km_legs(scenario_legs_city)
  pkm_legs_scenario_region <- personen_km_legs(scenario_legs_region)
  pkm_legs <- bind_rows("base_city" = pkm_legs_base_city,
                         "base_region" = pkm_legs_base_region,
                         "scenario_city" = pkm_legs_scenario_city,
                         "scenario_region" = pkm_legs_scenario_region,
                         .id = "case")
  
  write.csv(pkm_legs, file = paste0(outputDirectoryGeneral,"/pkm_legs.csv"))
}


#### #2.3 Personen Stunden - trips based ####
if (x_personen_h_trips == 1){
  personen_stunden_trips <- function (x){
    x %>% 
      filter(main_mode!="freight") %>% 
      summarise(personen_stunden_trips = (sum(trav_time)/length(unique(person))))
  }
  
  ph_trips_base_city <- personen_stunden_trips(base_trips_city)
  ph_trips_base_region <- personen_stunden_trips(base_trips_region)
  ph_trips_scenario_city <- personen_stunden_trips(scenario_trips_city)
  ph_trips_scenario_region <- personen_stunden_trips(scenario_trips_region)
  
  ph_trips <- bind_rows("base_city" = ph_trips_base_city,
                        "base_region" = ph_trips_base_region,
                        "scenario_city" = ph_trips_scenario_city,
                        "scenario_region" = ph_trips_scenario_region,
                        .id = "case")
  
  write.csv(ph_trips, file = paste0(outputDirectoryGeneral,"/ph_trips.csv"))
  
}

#### #2.4 Personen Stunden - legs based ####
if (x_personen_h_legs == 1){
  personen_stunden_legs <- function (x){
    x %>% 
      summarise(personen_stunden_legs = (sum(trav_time)/length(unique(person))))
  }
  
  ph_legs_base_city <- personen_stunden_legs(base_legs_city)
  ph_legs_base_region <- personen_stunden_legs(base_legs_region)
  ph_legs_scenario_city <- personen_stunden_legs(scenario_legs_city)
  ph_legs_scenario_region <- personen_stunden_legs(scenario_legs_region)
  
  ph_legs <- bind_rows("base_city" = ph_legs_base_city,
                       "base_region" = ph_legs_base_region,
                       "scenario_city" = ph_legs_scenario_city,
                       "scenario_region" = ph_legs_scenario_region,
                       .id = "case")
  
  write.csv(ph_legs, file = paste0(outputDirectoryGeneral,"/ph_legs.csv"))
  
}


#### #3.1 Modal Split - trips based - main mode (count) ####

if (x_ms_trips_count == 1){
  
  modal_split_trips_main_mode <- function(x){
    x %>%
      count(main_mode) %>%
      mutate(percent = 100*n/sum(n))
  }
  
  
  ms_main_mode_trips_baseCity <- modal_split_trips_main_mode(base_trips_city)
  ms_main_mode_trips_scenarioCity <- modal_split_trips_main_mode(scenario_trips_city)
  
  ms_main_mode_trips_city <- bind_rows("base" = ms_main_mode_trips_baseCity,
                                       "scenario" = ms_main_mode_trips_scenarioCity,
                                       .id = "case")
  
  write.csv(ms_main_mode_trips_city,file = paste0(outputDirectoryGeneral,"/ms_main_mode_trips_city.csv"))
  
  ms_main_mode_trips_baseRegion <- modal_split_trips_main_mode(base_trips_region)
  ms_main_mode_trips_scenarioRegion <- modal_split_trips_main_mode(scenario_trips_region)
  
  ms_main_mode_trips_region <- bind_rows("base" = ms_main_mode_trips_baseRegion,
                                       "scenario" = ms_main_mode_trips_scenarioRegion,
                                       .id = "case")
  
  write.csv(ms_main_mode_trips_region,file = paste0(outputDirectoryGeneral,"/ms_main_mode_trips_region.csv"))
}

#### #3.2 Modal Split - trips based - distance ####
if (x_ms_trips_distance == 1){
  modal_split_trips_distance <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise(distance = sum(traveled_distance)) %>%
      mutate(percent = round(100*distance/sum(distance),2))
  }
  
  ms_dist_trips_baseCity <- modal_split_trips_distance(base_trips_city)
  ms_dist_trips_scenarioCity <- modal_split_trips_distance(scenario_trips_city)
  
  ms_dist_trips_city <- bind_rows("base" = ms_dist_trips_baseCity,
                                  "scenario" = ms_dist_trips_scenarioCity,
                                  .id = "case")
  write.csv(ms_dist_trips_city,file = paste0(outputDirectoryGeneral,"/ms_dist_trips_city.csv"))
  
  ms_dist_trips_baseRegion <- modal_split_trips_distance(base_trips_region)
  ms_dist_trips_scenarioRegion<- modal_split_trips_distance(scenario_trips_region)
  
  ms_dist_trips_region <- bind_rows("base" = ms_dist_trips_baseRegion,
                                  "scenario" = ms_dist_trips_scenarioRegion,
                                  .id = "case")
  write.csv(ms_dist_trips_region,file = paste0(outputDirectoryGeneral,"/ms_dist_trips_region.csv"))
}


#### #3.3 Modal Split - legs based - main mode (count) ####
if (x_ms_legs_count == 1){
  modal_split_legs_mode <- function(x){
    x %>%
      count(mode) %>%
      mutate(percent = 100*n/sum(n))
  }
  
  ms_mode_legs_baseCity <- modal_split_legs_mode(base_legs_city)
  ms_mode_legs_scenarioCity <- modal_split_legs_mode(scenario_legs_city)
  
  
  ms_mode_legs_city <-  bind_rows("base" = ms_mode_legs_baseCity,
                                  "scenario" = ms_mode_legs_scenarioCity,
                                  .id = "case")
  write.csv(ms_mode_legs_city,file = paste0(outputDirectoryGeneral,"/ms_mode_legs_city.csv"))
  
  ms_mode_legs_baseRegion <- modal_split_legs_mode(base_legs_region)
  ms_mode_legs_scenarioRegion <- modal_split_legs_mode(scenario_legs_region)
  
  
  ms_mode_legs_region <-  bind_rows("base" = ms_mode_legs_baseRegion,
                                  "scenario" = ms_mode_legs_scenarioRegion,
                                  .id = "case")
  write.csv(ms_mode_legs_region,file = paste0(outputDirectoryGeneral,"/ms_mode_legs_region.csv"))
}

#### #3.4 Modal Split - legs based - distance ####
if (x_ms_legs_distance == 1){
  modal_split_legs_distance <- function(x){
    x %>%
      group_by(mode) %>%
      summarise(distance = sum(distance)) %>%
      mutate(percent = round(100*distance/sum(distance),2))
  }
  
  ms_dist_legs_baseCity <- modal_split_legs_distance(base_legs_city)
  ms_dist_legs_scenarioCity <- modal_split_legs_distance(scenario_legs_city)
  
  ms_dist_legs_city <- bind_rows("base"= ms_dist_legs_baseCity,
                                 "scenario" = ms_dist_legs_scenarioCity,
                                 .id = "case")
  write.csv(ms_dist_legs_city,file = paste0(outputDirectoryGeneral,"/ms_dist_legs_city.csv"))
  
  ms_dist_legs_baseRegion <- modal_split_legs_distance(base_legs_region)
  ms_dist_legs_scenarioRegion <- modal_split_legs_distance(scenario_legs_region)
  
  ms_dist_legs_region <- bind_rows("base"= ms_dist_legs_baseRegion,
                                 "scenario" = ms_dist_legs_scenarioRegion,
                                 .id = "case")
  write.csv(ms_dist_legs_region,file = paste0(outputDirectoryGeneral,"/ms_dist_legs_region.csv"))
}

#### #4.1 Sankey ####

if (x_sankey_diagram == 1){
sankey_dataframe <- function(x, y){
  inner_join(x, y, by = "trip_id") %>%
    select(trip_id, main_mode.x, longest_distance_mode.x, main_mode.y, longest_distance_mode.y) %>%
    group_by(main_mode.x, main_mode.y) %>%
    summarise(Freq = n())
}

#Base Case > Policy Case CITY
Base_city_to_Policy_city <- sankey_dataframe(base_trips_city, scenario_trips_city)

sankey_city <- alluvial(Base_city_to_Policy_city[1:2],
               freq= Base_case_city_to_Policy_city$Freq,
               border = NA,
               axis_labels = c("Base Case", "Policy Case"))

sankey_city <- as_tibble(t(sankey_city)) 
write.csv(sankey_city, file = paste0(outputDirectoryGeneral,"/sankey_city.csv"))

#Base Case > Policy Case REGION
Base_city_to_Policy_city <- sankey_dataframe(base_trips_region, scenario_trips_region)

sankey_region <- alluvial(Base_region_to_Policy_region[1:2],
                 freq= Base_case_region_to_Policy_region$Freq,
                 border = NA,
                 axis_labels = c("Base Case", "Policy Case"))

sankey_region <- as_tibble(t(sankey_region)) 
write.csv(sankey_region, file = paste0(outputDirectoryGeneral,"/sankey_region.csv"))
}
#### #5.1 Emissions ####
if (x_emissions == 1){
}
#### #6.1 Traffic ####
if (x_traffic == 1){
}
#### #7.1 Winner/Loser ####

if (x_winner_loser == 1){
  
TotalNumberRegionBase <- nrow(CitizenRegionBase)
MaxScoreRegionBase <- max(CitizenRegionBase$executed_score)
MinScoreRegionBase <- min(CitizenRegionBase$executed_score)
AvgScoreRegionBase <- mean(CitizenRegionBase$executed_score)
CitizenRegionBase1 <- left_join(CitizenRegionBase , BaseTripsRegion, by = "person")
# CitizenRegionBase2 <- CitizenRegionBase1 %>% filter(str_detect(start_activity_type, "home"))
# CitizenRegionBase3 <- CitizenRegionBase2 %>% distinct(start_link,person,executed_score,income,sex,age,carAvail)
# CitizenRegionBase3 <- CitizenRegionBase3 %>% relocate(start_link, .before = person)

#CitizenCityBase
TotalNumberCityBase <- nrow(CitizenCityBase)
MaxScoreCityBase <- max(CitizenCityBase$executed_score)
MinScoreCityBase <- min(CitizenCityBase$executed_score)
AvgScoreCityBase <- mean(CitizenCityBase$executed_score)
#left_join(CitizenCityBase , BaseTripsCity, by = person, type = "left", match = "all")

#CitizenRegionPolicy
TotalNumberegionPolicy <- nrow(CitizenRegionPolicy)
MaxScoreRegionPolicy <- max(CitizenRegionPolicy$executed_score)
MinScoreRegionPolicy <- min(CitizenRegionPolicy$executed_score)
AvgScoreRegionPolicy <- mean(CitizenRegionPolicy$executed_score)
#left_join(CitizenRegionPolicy , PolicyTripsRegion, by = person, type = "left", match = "all")

#CitizenCityPolicy
TotalNumberCityPolicy <- nrow(CitizenCityPolicy)
MaxScoreCityPolicy <- max(CitizenCityPolicy$executed_score)
MinScoreCityPolicy <- min(CitizenCityPolicy$executed_score)
AvgScoreCityPolicy <- mean(CitizenCityPolicy$executed_score)
#left_join(CitizenCityPolicy , PolicyTripsCity, by = person, type = "left", match = "all")


MaxScoreTable <- tibble(AREA= c("City","Region"), base = c(MaxScoreCityBase,MaxScoreRegionBase), policy = c(MaxScoreCityPolicy,MaxScoreRegionPolicy))
write.csv(MaxScoreTable, file = paste0(outputDirectoryGeneral,"/maxScoreTable.csv"))
MinScoreTable <- tibble(AREA= c("City","Region"), base = c(MinScoreCityBase,MinScoreRegionBase), policy = c(MinScoreCityPolicy,MinScoreRegionPolicy))
write.csv(MinScoreTable, file = paste0(outputDirectoryGeneral,"/minScoreTable.csv"))
AvgScoreTable <- tibble(AREA= c("City","Region"), base = c(AvgScoreCityBase,AvgScoreRegionBase), policy = c(AvgScoreCityPolicy,AvgScoreRegionPolicy))
write.csv(AvgScoreTable, file = paste0(outputDirectoryGeneral,"/AvgScoreTable.csv"))

#write.csv(CitizenRegionBase3, file = paste0(outputDirectoryGeneral,"/sankey_region.csv"))

}

