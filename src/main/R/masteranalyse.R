#### reading shp files ####
RegionShape <- st_read(region_shp_path, crs=CRS) #study area
CityShape <- st_read(city_shp_path, crs=CRS) #city of Leipzig
AreaShape <- st_read(area_shp_path, crs=CRS)#scenario area
print("#### Shapes geladen! ####")

#### reading trips/legs files ####

## Trip File
scenarioTripsTable <- readTripsTable(pathToMATSimOutputDirectory = paste0(scenario_run_path,list.files(path = scenario_run_path, pattern = "output_trips")))
print("#### Trips geladen! ####")

## Leg Files
scenarioLegsTable <- read_delim(paste0(scenario_run_path,list.files(path = scenario_run_path, pattern = "output_legs")), delim= ";", n_max = 3000)
print("#### Legs geladen! ####")

## Filters
scenario_trips_region <- filterByRegion(scenarioTripsTable,RegionShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_trips_city <- filterByRegion(scenarioTripsTable,CityShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_trips_area <- filterByRegion(scenarioTripsTable,AreaShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
print("#### Trips gefiltert! ####")
scenario_legs_region <- filterByRegion(scenarioLegsTable,RegionShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_legs_city <- filterByRegion(scenarioLegsTable,CityShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_legs_area <- filterByRegion(scenarioLegsTable,AreaShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
print("#### Legs gefiltert! ####")

#### reading persons ####
scenario_persons <- read_delim(paste0(scenario_run_path,list.files(path = scenario_run_path, pattern = "output_persons")), delim = ";")
print("#### Personen geladen! ####")

#### files/filters for comparisons ####

if (x_sankey_diagram == 1){
baseTripsTable <- readTripsTable(pathToMATSimOutputDirectory = paste0(base_run_path,list.files(path = base_run_path, pattern = "output_trips")))

base_trips_region <- filterByRegion(baseTripsTable,RegionShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
base_trips_city <- filterByRegion(baseTripsTable,CityShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
base_trips_area <- filterByRegion(baseTripsTable,AreaShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
}

if (x_winner_loser == 1){
  base_persons <- read_delim(paste0(base_run_path,list.files(path = base_run_path, pattern = "output_persons")), delim = ";")
}

#### 0. Parameters ####

#BREAKING DIFFERENT DISTANCES IN M
breaks = c(0, 1000, 2000, 5000, 10000, 20000, Inf)
breaks2 = c(0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000, 20000, Inf)
#NAMES OF THE CASES
cases <- c("base", "scenario")
print("#### Parameter spezifiziert! ####")
#### #1.1 Modal Split - trips based - main mode (count) ####
print("#### in 1.1 ####")
if (x_ms_trips_count == 1){
  
  modal_split_trips_main_mode <- function(x){
    x %>%
      count(main_mode) %>%
      mutate(percent = 100*n/sum(n))
  }
 
  ms_main_mode_trips_scenarioCity <- modal_split_trips_main_mode(scenario_trips_city)
  ms_main_mode_trips_ScenarioCity <- t(ms_main_mode_trips_scenarioCity)
  colnames(ms_main_mode_trips_ScenarioCity) <- ms_main_mode_trips_ScenarioCity[1, ]
  write.csv(ms_main_mode_trips_ScenarioCity,file = paste0(outputDirectoryScenario,"/ms_main_mode_trips_city-modestats.csv"))
  
  ms_main_mode_trips_scenarioRegion <- modal_split_trips_main_mode(scenario_trips_region)
  ms_main_mode_trips_ScenarioRegion <- t(ms_main_mode_trips_scenarioRegion)
  colnames(ms_main_mode_trips_ScenarioRegion) <- ms_main_mode_trips_ScenarioRegion[1, ] 
  write.csv(ms_main_mode_trips_scenarioRegion,file = paste0(outputDirectoryScenario,"/ms_main_mode_trips_region-modestats.csv"))
}

#### #1.2 Modal Split - trips based - distance ####
print("#### in 1.2 ####")
if (x_ms_trips_distance == 1){
  modal_split_trips_distance <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise(distance = sum(traveled_distance)) %>%
      mutate(percent = round(100*distance/sum(distance),2))
  }
  ms_dist_trips_scenarioCity <- modal_split_trips_distance(scenario_trips_city)
  ms_dist_trips_ScenarioCity <- t(ms_dist_trips_scenarioCity)
  colnames(ms_dist_trips_ScenarioCity) <- ms_dist_trips_ScenarioCity[1, ]
  write.csv(ms_dist_trips_ScenarioCity,file = paste0(outputDirectoryScenario,"/ms_dist_trips_city-modestats.csv"))
  
  ms_dist_trips_scenarioRegion <- modal_split_trips_distance(scenario_trips_region)
  ms_dist_trips_ScenarioRegion <- t(ms_dist_trips_scenarioRegion)
  colnames(ms_dist_trips_ScenarioRegion) <- ms_dist_trips_ScenarioRegion[1, ] 
  write.csv(ms_dist_trips_scenarioRegion,file = paste0(outputDirectoryScenario,"/ms_dist_trips_region-modestats.csv"))
}


#### #1.3 Modal Split - legs based - main mode (count) ####
print("#### in 1.3 ####")
if (x_ms_legs_count == 1){
  modal_split_legs_mode <- function(x){
    x %>%
      mutate(distance_cut = cut(distance, breaks = breaks,
                                labels = c("<1000m", "1 - 2km", "2 - 5km", "5 - 10km", "10 - 20km", ">20km" ))) %>%
      group_by(distance_cut) %>% 
      count(mode) %>%
      mutate(percent = 100*n/sum(n))
  }
  ms_mode_legs_scenarioCity <- modal_split_legs_mode(scenario_legs_city)
  ms_mode_legs_ScenarioCity <- t(ms_mode_legs_scenarioCity)
  colnames(ms_mode_legs_ScenarioCity) <- ms_mode_legs_ScenarioCity[1, ]
  write.csv(ms_mode_legs_ScenarioCity,file = paste0(outputDirectoryScenario,"/ms_mode_legs_city-modestats.csv"))
  
  ms_mode_legs_scenarioRegion <- modal_split_legs_mode(scenario_legs_region)
  ms_mode_legs_ScenarioRegion <- t(ms_mode_legs_scenarioRegion)
  colnames(ms_mode_legs_ScenarioRegion) <- ms_mode_legs_ScenarioRegion[1, ] 
  write.csv(ms_mode_legs_scenarioRegion,file = paste0(outputDirectoryScenario,"/ms_mode_legs_region-modestats.csv"))
}

#### #1.4 Modal Split - legs based - distance ####
print("#### in 1.4 ####")
if (x_ms_legs_distance == 1){
  modal_split_legs_distance <- function(x){
    x %>%
      group_by(mode) %>%
      summarise(distance = sum(distance)) %>%
      mutate(percent = round(100*distance/sum(distance),2))
  }
  ms_dist_legs_scenarioCity <- modal_split_legs_distance(scenario_legs_city)
  ms_dist_legs_ScenarioCity <- t(ms_dist_legs_scenarioCity)
  colnames(ms_dist_legs_ScenarioCity) <- ms_dist_legs_ScenarioCity[1, ]
  write.csv(ms_dist_legs_ScenarioCity,file = paste0(outputDirectoryScenario,"/ms_dist_legs_city-modestats.csv"))
  
  ms_dist_legs_scenarioRegion <- modal_split_legs_distance(scenario_legs_region)
  ms_dist_legs_ScenarioRegion <- t(ms_dist_legs_scenarioRegion)
  colnames(ms_dist_legs_ScenarioRegion) <- ms_dist_legs_ScenarioRegion[1, ] 
  write.csv(ms_dist_legs_scenarioRegion,file = paste0(outputDirectoryScenario,"/ms_dist_legs_region-modestats.csv"))
}

#### #2.1 Sankey Modal Shift ####
print("#### in 2.1 ####")

if (x_sankey_diagram == 1){
  sankey_dataframe <- function(x, y){
    inner_join(x, y, by = "trip_id") %>%
      select(trip_id, main_mode.x, longest_distance_mode.x, main_mode.y, longest_distance_mode.y) %>%
      group_by(main_mode.x, main_mode.y) %>%
      summarise(Freq = n())
  }
  
  #Base Case > Policy Case CITY
  Base_city_to_Scenario_city <- sankey_dataframe(base_trips_city, scenario_trips_city)
  
  sankey_city <- alluvial(Base_city_to_Scenario_city[1:2],
                          freq= Base_city_to_Scenario_city$Freq,
                          border = NA,
                          axis_labels = c("Base Case", "Scenario Case"))
  
  sankey_city <- as_tibble(t(sankey_city)) 
  write.csv(sankey_city, file = paste0(outputDirectoryBase,"/sankey_city.csv"))
  
  #Base Case > Policy Case REGION
  Base_region_to_Scenario_city <- sankey_dataframe(base_trips_region, scenario_trips_region)
  
  sankey_region <- alluvial(Base_region_to_Scenario_region[1:2],
                            freq= Base_region_to_Scenario_region$Freq,
                            border = NA,
                            axis_labels = c("Base Case", "Scenario Case"))
  
  sankey_region <- as_tibble(t(sankey_region)) 
  write.csv(sankey_region, file = paste0(outputDirectoryBase,"/sankey_region.csv"))
}

#### #3.1 Average Traveled Distance - trips based####
print("#### in 3.1 ####")

if (x_average_traveled_distance_trips == 1){
  
  avg_trav_distance_trips_by_mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise_at(vars(traveled_distance), list(name=mean))
  }
  #calculation
  avg_trav_dist_trips_scenario_network <- avg_trav_distance_trips_by_mode(scenarioTripsTable)
  avg_trav_dist_trips_scenario_region <- avg_trav_distance_trips_by_mode(scenario_trips_region)
  avg_trav_dist_trips_scenario_city <- avg_trav_distance_trips_by_mode(scenario_trips_city)
  
  #write table
  write.csv(avg_trav_dist_trips_scenario_network, file = paste0(outputDirectoryScenario,"/avg_trav_dist_trips_network.csv"))
  write.csv(avg_trav_dist_trips_scenario_region, file = paste0(outputDirectoryScenario,"/avg_trav_dist_trips_region.csv"))
  write.csv(avg_trav_dist_trips_scenario_city, file = paste0(outputDirectoryScenario,"/avg_trav_dist_trips_city.csv"))
}
#### #3.2 Average Euclidean Distance - trips based####
print("#### in 3.2 ####")
if (x_average_euclidean_distance_trips == 1){
  
  avg_eucl_distance_trips_by_mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      #summarise_at(vars(traveled_distance), list(name=mean))
      summarise_at(vars(euclidean_distance), list(name=mean))
  }
  #calculation
  avg_eucl_dist_trips_scenario_network <- avg_eucl_distance_trips_by_mode(scenarioTripsTable)
  avg_eucl_dist_trips_scenario_region <- avg_eucl_distance_trips_by_mode(scenario_trips_region)
  avg_eucl_dist_trips_scenario_city <- avg_eucl_distance_trips_by_mode(scenario_trips_city)
  #write table
  write.csv(avg_eucl_dist_trips_scenario_network, file = paste0(outputDirectoryScenario,"/avg_eucl_dist_trips_network.csv"))
  write.csv(avg_eucl_dist_trips_scenario_region, file = paste0(outputDirectoryScenario,"/avg_eucl_dist_trips_region.csv"))
  write.csv(avg_eucl_dist_trips_scenario_city, file = paste0(outputDirectoryScenario,"/avg_eucl_dist_trips_city.csv"))
}
#### #3.3 Personen KM - trips based ####
print("#### in 3.3 ####")
if (x_personen_km_trips == 1){
  personen_km_trips <- function (x){
    x %>%
      filter(main_mode!="freight") %>%
      group_by(main_mode) %>%
      summarise(pers_km = sum(traveled_distance)/1000) 
    
  }
  pkm_trips_scenario_city <- personen_km_trips(scenario_trips_city)
  pkm_trips_scenario_region <- personen_km_trips(scenario_trips_region)
  pkm_trips_scenario_network <- personen_km_trips(scenarioTripsTable)
  
  write.csv(pkm_trips_scenario_city, file = paste0(outputDirectoryScenario,"/trips_city_pkm.csv"))
  write.csv(pkm_trips_scenario_region, file = paste0(outputDirectoryScenario,"/trips_region_pkm.csv"))
  write.csv(pkm_trips_scenario_network, file = paste0(outputDirectoryScenario,"/trips_network_pkm.csv"))
  
}

#### #3.4 Average Traveled Distance - legs based#####
print("#### in 3.4 ####")
if (x_average_traveled_distance_legs == 1){
  
  avg_trav_distance_legs_by_mode <- function(x){
    x %>%
      group_by(mode) %>%
      summarise_at(vars(distance), list(name=mean))
    #summarise_at(vars(euclidean_distance), list(name=mean))
  }
  #calculation
  avg_trav_dist_legs_scenario_network <- avg_trav_distance_legs_by_mode(scenarioLegsTable)
  avg_trav_dist_legs_scenario_region <- avg_trav_distance_legs_by_mode(scenario_legs_region)
  avg_trav_dist_legs_scenario_city <- avg_trav_distance_legs_by_mode(scenario_legs_city)
  #write table
  write.csv(avg_trav_dist_legs_scenario_network, file = paste0(outputDirectoryScenario,"/avg_trav_dist_legs_network.csv"))
  write.csv(avg_trav_dist_legs_scenario_region, file = paste0(outputDirectoryScenario,"/avg_trav_dist_legs_region.csv"))
  write.csv(avg_trav_dist_legs_scenario_city, file = paste0(outputDirectoryScenario,"/avg_trav_dist_legs_city.csv"))
}
#### #3.5 Average Euclidean Distance - legs based#####
print("#### in 3.5 ####")
if (x_average_euclidean_distance_legs == 1){
  
  avg_eucl_distance_legs_by_mode <- function(x){
    x %>%
      group_by(mode) %>%
      summarise_at(vars(euclidean_distance), list(name=mean))
  }
  #calculation
  avg_eucl_dist_legs_scenario_network <- avg_eucl_distance_legs_by_mode(scenarioLegsTable)
  avg_eucl_dist_legs_scenario_region <- avg_eucl_distance_legs_by_mode(scenario_legs_region)
  avg_eucl_dist_legs_scenario_city <- avg_eucl_distance_legs_by_mode(scenario_legs_city)
  #write table
  write.csv(avg_eucl_dist_legs_scenario_network, file = paste0(outputDirectoryScenario,"/avg_eucl_dist_legs_network.csv"))
  write.csv(avg_eucl_dist_legs_scenario_region, file = paste0(outputDirectoryScenario,"/avg_eucl_dist_legs_region.csv"))
  write.csv(avg_eucl_dist_legs_scenario_city, file = paste0(outputDirectoryScenario,"/avg_eucl_dist_legs_city.csv"))
}
#### #3.6 Personen KM - legs based ####
print("#### in 3.6 ####")
if (x_personen_km_legs == 1){
  personen_km_legs <- function (x){
    x %>%
      group_by(mode) %>%
      summarise(pers_km = sum(distance)/1000) 
    
  }
  pkm_legs_scenario_city <- personen_km_legs(scenario_legs_city)
  pkm_legs_scenario_region <- personen_km_legs(scenario_legs_region)
  pkm_legs_scenario_network <- personen_km_legs(scenarioLegsTable)

  write.csv(pkm_legs_scenario_city, file = paste0(outputDirectoryScenario,"/legs_city_pkm.csv"))
  write.csv(pkm_legs_scenario_region, file = paste0(outputDirectoryScenario,"/legs_region_pkm.csv"))
  write.csv(pkm_legs_scenario_network, file = paste0(outputDirectoryScenario,"/legs_network_pkm.csv"))
  
}
#### #4.1 Average Travel Time - trips based #####
print("#### in 4.1 ####")
if (x_average_traveled_distance_trips == 1){
  
  avg_time_trips_by_mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise_at(vars(trav_time), list(name=mean))
  }
  #calculation
  avg_time_trips_scenario_network <- avg_time_trips_by_mode(scenarioTripsTable)
  avg_time_trips_scenario_region <- avg_time_trips_by_mode(scenario_trips_region)
  avg_time_trips_scenario_city <- avg_time_trips_by_mode(scenario_trips_city)
  #write table
  write.csv(avg_time_trips_scenario_network, file = paste0(outputDirectoryScenario,"/avg_time_trips_network.csv"))
  write.csv(avg_time_trips_scenario_region, file = paste0(outputDirectoryScenario,"/avg_time_trips_region.csv"))
  write.csv(avg_time_trips_scenario_city, file = paste0(outputDirectoryScenario,"/avg_time_trips_city.csv"))
}

#### #4.2 Personen Stunden - trips based ####
print("#### in 4.2 ####")
if (x_personen_h_trips == 1){
  personen_stunden_trips <- function (x){
    x %>% 
      filter(main_mode!="freight") %>%
      group_by(mode) %>%
      summarise(personen_stunden_trips = (sum(trav_time)))
  }
  ph_trips_scenario_city <- personen_km_trips(scenario_trips_city)
  ph_trips_scenario_region <- personen_km_trips(scenario_trips_region)
  ph_trips_scenario_network <- personen_km_trips(scenarioTripsTable)

  write.csv(ph_trips_scenario_city, file = paste0(outputDirectoryScenario,"/trips_city_ph.csv"))
  write.csv(ph_trips_scenario_region, file = paste0(outputDirectoryScenario,"/trips_region_ph.csv"))
  write.csv(ph_trips_scenario_network, file = paste0(outputDirectoryScenario,"/trips_network_ph.csv"))
  
}

#### #4.3 Average Travel Time - legs based #####
print("#### in 4.3 ####")

if (x_average_traveled_distance_legs == 1){
  
  avg_time_legs_by_mode <- function(x){
    x %>%
      group_by(mode) %>%
      summarise_at(vars(trav_time), list(name=mean))
  }
  #calculation
  avg_time_legs_scenario_network <- avg_time_legs_by_mode(scenarioLegsTable)
  avg_time_legs_scenario_region <- avg_time_legs_by_mode(scenario_legs_region)
  avg_time_legs_scenario_city <- avg_time_legs_by_mode(scenario_legs_city)
  #write table
  write.csv(avg_time_legs_scenario_network, file = paste0(outputDirectoryScenario,"/avg_time_legs_network.csv"))
  write.csv(avg_time_legs_scenario_region, file = paste0(outputDirectoryScenario,"/avg_time_legs_region.csv"))
  write.csv(avg_time_legs_scenario_city, file = paste0(outputDirectoryScenario,"/avg_time_legs_city.csv"))
}

#### #4.4 Personen Stunden - legs based ####
print("#### in 4.4 ####")
if (x_personen_h_legs == 1){
  personen_stunden_legs <- function (x){
    x %>%
      group_by(mode) %>%
      summarise(personen_stunden_legs = (sum(trav_time)))
  }
  ph_legs_scenario_city <- personen_stunden_legs(scenario_legs_city)
  ph_legs_scenario_region <- personen_stunden_legs(scenario_legs_region)
  ph_legs_scenario_network <- personen_stunden_legs(scenarioLegsTable)

  write.csv(ph_legs_scenario_city, file = paste0(outputDirectoryScenario,"/legs_city_ph.csv"))
  write.csv(ph_legs_scenario_region, file = paste0(outputDirectoryScenario,"/legs_region_ph.csv"))
  write.csv(ph_legs_scenario_network, file = paste0(outputDirectoryScenario,"/legs_network_ph.csv"))
  
}

#### #5.1 Average Speed ####
print("#### in 5.1 ####")
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
avg_trav_dist_scenario_city <- avg_trav_distance(scenario_trips_city)
avg_trav_dist_scenario_region <- avg_trav_distance(scenario_trips_region)
avg_trav_dist_scenario_network <-avg_trav_distance(scenarioTripsTable)
avg_trav_time_scenario_city <- avg_trav_time(scenario_trips_city)
avg_trav_time_scenario_region <- avg_trav_time(scenario_trips_region)
avg_trav_time_scenario_network <- avg_trav_time(scenarioTripsTable)
avg_trav_speed_scenario_city = avg_trav_dist_scenario_city/avg_trav_time_scenario_city*3.6 #km/h
avg_trav_speed_scenario_region = avg_trav_dist_scenario_region/avg_trav_time_scenario_region*3.6 #km/h
avg_trav_speed_scenario_network = avg_trav_dist_scenario_network/avg_trav_time_scenario_network*3.6

#write tables
write.csv(avg_trav_speed_scenario_network, file = paste0(outputDirectoryScenario,"/avg_trav_speed_trips_network.csv"))
write.csv(avg_trav_speed_scenario_region, file = paste0(outputDirectoryScenario,"/avg_trav_speed_trips_region.csv"))
write.csv(avg_trav_speed_scenario_city, file = paste0(outputDirectoryScenario,"/avg_trav_speed_trips_city.csv"))
}

#### #5.2 Average Beeline Speed ####
print("#### in 5.2 ####")

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
avg_beeline_dist_scenario_city <- avg_beeline_distance(scenario_trips_city)
avg_beeline_dist_scenario_region <- avg_beeline_distance(scenario_trips_region)
avg_beeline_dist_scenario_network <- avg_beeline_distance(scenarioTripsTable)
avg_trav_time_scenario_city <- avg_trav_time(scenario_trips_city)
avg_trav_time_scenario_region <- avg_trav_time(scenario_trips_region)
avg_trav_time_scenario_network <- avg_trav_time(scenarioTripsTable)
# average beeline speed
avg_beeline_speed_scenario_city = avg_beeline_dist_scenario_city/avg_trav_time_scenario_city*3.6 #km/h
avg_beeline_speed_scenario_region = avg_beeline_dist_scenario_region/avg_trav_time_scenario_region*3.6 #km/h
avg_beeline_speed_scenario_network = avg_beeline_dist_scenario_network/avg_trav_time_scenario_network*3.6
#write tables
write.csv(avg_beeline_speed_scenario_network, file = paste0(outputDirectoryScenario,"/avg_bee_speed_trips_network.csv"))
write.csv(avg_beeline_speed_scenario_region, file = paste0(outputDirectoryScenario,"/avg_bee_speed_trips_region.csv"))
write.csv(avg_beeline_speed_scenario_city, file = paste0(outputDirectoryScenario,"/avg_bee_speed_trips_city.csv"))
} 

#### #6.1 Emissions ####
print("#### in 6.1 ####")
if (x_emissions == 1){
}
#### #7.1 Traffic ####
print("#### in 7.1 ####")
if (x_traffic == 1){
}
#### #8.1 Execution Scores Winner-Loser ####
print("#### in 8.1 ####")

if (x_winner_loser == 1){
  base_scenario_persons <- inner_join(base_persons, scenario_persons, by= "person") %>% 
    select(person, executed_score.x, executed_score.y, income.x, sex.x, age.x, carAvail.x, first_act_x.x, first_act_y.x) %>% 
    mutate(score_change = format((executed_score.y - executed_score.x), scientific = FALSE), person = as.character(person))
  
  home_trips <- baseTripsTable %>% 
    filter(grepl("home", start_activity_type)) %>% 
    distinct(person, .keep_all = TRUE) %>% 
    select(person, start_link, start_x, start_y)
  
  base_scenario_persons <-  full_join(base_scenario_persons, home_trips, by = "person") %>% 
    mutate(home_x = ifelse(is.na(start_x), first_act_x.x, start_x),
           home_y = ifelse(is.na(start_y), first_act_y.x, start_y)) %>% 
    select(person, executed_score.x, executed_score.y, score_change, income.x, sex.x, age.x, carAvail.x, home_x, home_y)
  
  write.csv(base_scenario_persons, file = paste0(outputDirectoryBase,"/ScoreTable.csv"))
  
  
  AgentsInNetwork <- nrow(base_scenario_persons)
  MaxScoreNetworkBase <- max(base_scenario_persons$executed_score.x)
  MinScoreNetworkBase <- min(base_scenario_persons$executed_score.x)
  AvgScoreNetworkBase <- mean(base_scenario_persons$executed_score.x)
  MaxScoreNetworkScenario <- max(base_scenario_persons$executed_score.y)
  MinScoreNetworkScenario <- min(base_scenario_persons$executed_score.y)
  AvgScoreNetworkScenario <- mean(base_scenario_persons$executed_score.y)
  BiggestLoserNetwork <- min(base_scenario_persons$score_change)
  GoodOrBadForNetwork =  AvgScoreNetworkScenario - AvgScoreNetworkBase
  BiggestWinnerNetwork <- max(base_scenario_persons$score_change)
  
  AgentsInRegion <- nrow(base_scenario_persons)
  MaxScoreRegionBase <- max(base_scenario_persons$executed_score.x)
  MinScoreRegionBase <- min(base_scenario_persons$executed_score.x)
  AvgScoreRegionBase <- mean(base_scenario_persons$executed_score.x)
  MaxScoreRegionScenario <- max(base_scenario_persons$executed_score.y)
  MinScoreRegionScenario <- min(base_scenario_persons$executed_score.y)
  AvgScoreRegionScenario <- mean(base_scenario_persons$executed_score.y)
  BiggestLoserRegion <- min(base_scenario_persons$score_change)
  GoodOrBadForRegion =  AvgScoreRegionScenario - AvgScoreRegionBase
  BiggestWinnerRegion <- max(base_scenario_persons$score_change)
  
  AgentsInCity <- nrow(base_scenario_persons)
  MaxScoreCityBase <- max(base_scenario_persons$executed_score.x)
  MinScoreCityBase <- min(base_scenario_persons$executed_score.x)
  AvgScoreCityBase <- mean(base_scenario_persons$executed_score.x)
  MaxScoreCityScenario <- max(base_scenario_persons$executed_score.y)
  MinScoreCityScenario <- min(base_scenario_persons$executed_score.y)
  AvgScoreCityScenario <- mean(base_scenario_persons$executed_score.y)
  BiggestLoserCity <- min(base_scenario_persons$score_change)
  GoodOrBadForCity =  AvgScoreCityScenario - AvgScoreCityBase
  BiggestWinnerCity <- max(base_scenario_persons$score_change) 
  
  
} 



