#### reading shp files ####
RegionShape <- st_read(region_shp_path, crs=CRS) #study area
CityShape <- st_read(city_shp_path, crs=CRS) #city of Leipzig
AreaShape <- st_read(area_shp_path, crs=CRS)#scenario area
print("#### Shapes geladen! ####")

#### reading trips/legs files ####

## Trip File
baseTripsTable <- readTripsTable(pathToMATSimOutputDirectory = paste0(base_run_path,list.files(path = base_run_path, pattern = "output_trips")))
scenarioTripsTable <- readTripsTable(pathToMATSimOutputDirectory = paste0(scenario_run_path,list.files(path = scenario_run_path, pattern = "output_trips")))
print("#### Trips geladen! ####")

## Leg Files
scenarioLegsTable <- read_delim(paste0(scenario_run_path,list.files(path = scenario_run_path, pattern = "output_legs")), delim= ";", n_max = 30000)
print("#### Legs geladen! ####")

## Filters
base_trips_region <- filterByRegion(baseTripsTable,RegionShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
base_trips_city <- filterByRegion(baseTripsTable,CityShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
base_trips_area <- filterByRegion(baseTripsTable,AreaShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_trips_region <- filterByRegion(scenarioTripsTable,RegionShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_trips_city <- filterByRegion(scenarioTripsTable,CityShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_trips_area <- filterByRegion(scenarioTripsTable,AreaShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
print("#### Trips gefiltert! ####")
scenario_legs_region <- filterByRegion(scenariolegsTable,RegionShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_legs_city <- filterByRegion(scenariolegsTable,CityShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario_legs_area <- filterByRegion(scenariolegsTable,AreaShape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
print("#### Legs gefiltert! ####")

#### reading persons ####
scenario_persons <- read_delim(paste0(scenario_run_path,list.files(path = scenario_run_path, pattern = "output_persons")), delim = ";")

#### 0. Parameters ####

#BREAKING DIFFERENT DISTANCES IN M
breaks = c(0, 1000, 2000, 5000, 10000, 20000, Inf)
breaks2 = c(0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000, 20000, Inf)
#NAMES OF THE CASES
cases <- c("base", "scenario")

#### #1.1 Modal Split - trips based - main mode (count) ####

if (x_ms_trips_count == 1){
  
  modal_split_trips_main_mode <- function(x){
    x %>%
      count(main_mode) %>%
      mutate(percent = 100*n/sum(n))
  }
 
  ms_main_mode_trips_scenarioCity <- modal_split_trips_main_mode(scenario_trips_city)
  ms_main_mode_trips_ScenarioCity <- t(ms_main_mode_trips_scenarioCity)
  colnames(ms_main_mode_trips_ScenarioCity) <- ms_main_mode_trips_ScenarioCity[1, ]
  write.csv(ms_main_mode_trips_ScenarioCity,file = paste0(outputDirectoryScenario,"/pie.ms_counts.trips.city.csv"))
  
  ms_main_mode_trips_scenarioRegion <- modal_split_trips_main_mode(scenario_trips_region)
  ms_main_mode_trips_ScenarioRegion <- t(ms_main_mode_trips_scenarioRegion)
  colnames(ms_main_mode_trips_ScenarioRegion) <- ms_main_mode_trips_ScenarioRegion[1, ] 
  write.csv(ms_main_mode_trips_scenarioRegion,file = paste0(outputDirectoryScenario,"/pie.ms_counts.trips.region.csv"))
}

#### #1.2 Modal Split - trips based - distance ####
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
  write.csv(ms_dist_trips_ScenarioCity,file = paste0(outputDirectoryScenario,"/pie.ms_distance.trips.city.csv"))
  
  ms_dist_trips_scenarioRegion <- modal_split_trips_distance(scenario_trips_region)
  ms_dist_trips_ScenarioRegion <- t(ms_dist_trips_scenarioRegion)
  colnames(ms_dist_trips_ScenarioRegion) <- ms_dist_trips_ScenarioRegion[1, ] 
  write.csv(ms_dist_trips_scenarioRegion,file = paste0(outputDirectoryScenario,"/pie.ms_distance.trips.region.csv"))
}


#### #1.3 Modal Split - legs based - main mode (count) ####
if (x_ms_legs_count == 1){
  modal_split_legs_mode <- function(x){
    x %>%
      mutate(distance_cut = cut(distance, breaks = breaks,
                                labels = c("<1000m", "1 - 2km", "2 - 5km", "5 - 10km", "10 - 20km", ">20km" ))) %>%
      group_by(distance_cut) %>% 
      count(mode) %>%
      mutate(percent = 100*n/sum(n))
  }
  ms_mode_legs_scenarioCity <- modal_split_legs_mode(scenario_trips_city)
  ms_mode_legs_ScenarioCity <- t(ms_mode_legs_scenarioCity)
  colnames(ms_mode_legs_ScenarioCity) <- ms_mode_legs_ScenarioCity[1, ]
  write.csv(ms_mode_legs_ScenarioCity,file = paste0(outputDirectoryScenario,"/pie.ms_counts.legs.city.csv"))
  
  ms_mode_legs_scenarioRegion <- modal_split_legs_mode(scenario_trips_region)
  ms_mode_legs_ScenarioRegion <- t(ms_mode_legs_scenarioRegion)
  colnames(ms_mode_legs_ScenarioRegion) <- ms_mode_legs_ScenarioRegion[1, ] 
  write.csv(ms_mode_legs_scenarioRegion,file = paste0(outputDirectoryScenario,"/pie.ms_counts.legs.region.csv"))
}

#### #1.4 Modal Split - legs based - distance ####
if (x_ms_legs_distance == 1){
  modal_split_legs_distance <- function(x){
    x %>%
      group_by(mode) %>%
      summarise(distance = sum(distance)) %>%
      mutate(percent = round(100*distance/sum(distance),2))
  }
  ms_dist_legs_scenarioCity <- modal_split_legs_distance(scenario_trips_city)
  ms_dist_legs_ScenarioCity <- t(ms_dist_legs_scenarioCity)
  colnames(ms_dist_legs_ScenarioCity) <- ms_dist_legs_ScenarioCity[1, ]
  write.csv(ms_dist_legs_ScenarioCity,file = paste0(outputDirectoryScenario,"/pie.ms_distance.legs.city.csv"))
  
  ms_dist_legs_scenarioRegion <- modal_split_legs_distance(scenario_trips_region)
  ms_dist_legs_ScenarioRegion <- t(ms_dist_legs_scenarioRegion)
  colnames(ms_dist_legs_ScenarioRegion) <- ms_dist_legs_ScenarioRegion[1, ] 
  write.csv(ms_dist_legs_scenarioRegion,file = paste0(outputDirectoryScenario,"/pie.ms_distance.legs.region.csv"))
}

#### #2.1 Sankey Modal Shift ####

if (x_sankey_diagram == 1){
  # sankey_dataframe <- function(x, y){
  #   inner_join(x, y, by = "trip_id") %>%
  #     select(trip_id, main_mode.x, longest_distance_mode.x, main_mode.y, longest_distance_mode.y) %>%
  #     group_by(main_mode.x, main_mode.y) %>%
  #     summarise(Freq = n())
  # }
  # 
  # #Base Case > Policy Case CITY
  # Base_city_to_Scenario_city <- sankey_dataframe(base_trips_city, scenario_trips_city)
  # 
  # sankey_city <- alluvial(Base_city_to_Scenario_city[1:2],
  #                         freq= Base_city_to_Scenario_city$Freq,
  #                         border = NA,
  #                         axis_labels = c("Base Case", "Scenario Case"))
  # 
  # sankey_city <- as_tibble(t(sankey_city)) 
  # write.csv(sankey_city, file = paste0(outputDirectoryScenario,"/sankey.shift.city.csv"))
  # 
  # #Base Case > Policy Case REGION
  # Base_region_to_Scenario_region <- sankey_dataframe(base_trips_region, scenario_trips_region)
  # 
  # sankey_region <- alluvial(Base_region_to_Scenario_region[1:2],
  #                           freq= Base_region_to_Scenario_region$Freq,
  #                           border = NA,
  #                           axis_labels = c("Base Case", "Scenario Case"))
  # 
  # sankey_region <- as_tibble(t(sankey_region)) 
  # write.csv(sankey_region, file = paste0(outputDirectoryScenario,"/sankey.shift.region.csv"))
  # 
  # #Base Case > Policy Case NETWORK
  # Base_network_to_Scenario_network <- sankey_dataframe(baseTripsTable, scenarioTripsTable)
  # 
  # sankey_network <- alluvial(Base_network_to_Scenario_network[1:2],
  #                           freq= Base_network_to_Scenario_network$Freq,
  #                           border = NA,
  #                           axis_labels = c("Base Case", "Scenario Case"))
  # 
  # sankey_network <- as_tibble(t(sankey_network)) 
  # write.csv(sankey_network, file = paste0(outputDirectoryScenario,"/sankey.shift.network.csv"))
  
  
  plotModalShiftSankey(
    base_trips_city,
    scenario_trips_city,
    show.onlyChanges = FALSE,
    unite.columns = character(0),
    united.name = "united",
    dump.output.to = paste0(outputDirectoryScenario,"/city")
  )
  plotModalShiftSankey(
    base_trips_region,
    scenario_trips_region,
    show.onlyChanges = FALSE,
    unite.columns = character(0),
    united.name = "united",
    dump.output.to = paste0(outputDirectoryScenario,"/region")
  )
  plotModalShiftSankey(
    baseTripsTable,
    scenarioTripsTable,
    show.onlyChanges = FALSE,
    unite.columns = character(0),
    united.name = "united",
    dump.output.to = paste0(outputDirectoryScenario,"/network")
  )
  
}

#### #3.1 Average Traveled Distance - trips based####

if (x_average_traveled_distance_trips == 1){
  
  avg_trav_distance_trips_by_mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise_at(vars(traveled_distance), list(name=mean)) %>% 
      pivot_wider(names_from = main_mode, values_from = name)
    
  }
  #calculation
  avg_trav_dist_trips_scenario_network <- avg_trav_distance_trips_by_mode(scenarioTripsTable)/1000
  avg_trav_dist_trips_scenario_region <- avg_trav_distance_trips_by_mode(scenario_trips_region)/1000
  avg_trav_dist_trips_scenario_city <- avg_trav_distance_trips_by_mode(scenario_trips_city)/1000
  
  #write table
  df_list <- list(network = avg_trav_dist_trips_scenario_network,
                  region = avg_trav_dist_trips_scenario_region,
                  city = avg_trav_dist_trips_scenario_city)
  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.avg_trav_dist.trips.csv"))
}
#### #3.2 Average Euclidean Distance - trips based####
if (x_average_euclidean_distance_trips == 1){
  
  avg_eucl_distance_trips_by_mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise_at(vars(euclidean_distance), list(name=mean)) %>% 
      pivot_wider(names_from = main_mode, values_from = name)
  }
  #calculation
  avg_eucl_dist_trips_scenario_network <- avg_eucl_distance_trips_by_mode(scenarioTripsTable)/1000
  avg_eucl_dist_trips_scenario_region <- avg_eucl_distance_trips_by_mode(scenario_trips_region)/1000
  avg_eucl_dist_trips_scenario_city <- avg_eucl_distance_trips_by_mode(scenario_trips_city)/1000
  #write table
  df_list <- list(network = avg_eucl_dist_trips_scenario_network,
                  region = avg_eucl_dist_trips_scenario_region,
                  city = avg_eucl_dist_trips_scenario_city)
  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.avg_bee_dist.trips.csv"))
}
#### #3.3 Personen KM - trips based ####
if (x_personen_km_trips == 1){
  personen_km_trips <- function (x){
    x %>%
      filter(main_mode!="freight") %>%
      group_by(main_mode) %>%
      summarise(pers_km = sum(traveled_distance)) %>% 
      pivot_wider(names_from = main_mode, values_from = pers_km)
    
  }
  pkm_trips_scenario_city <- personen_km_trips(scenario_trips_city)/1000
  pkm_trips_scenario_region <- personen_km_trips(scenario_trips_region)/1000
  pkm_trips_scenario_network <- personen_km_trips(scenarioTripsTable)/1000
  
  df_list <- list(network = pkm_trips_scenario_network,
                  region = pkm_trips_scenario_region,
                  city = pkm_trips_scenario_city)
  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.trav_dist.trips.csv"))
}

#### #3.4 Distance Heatmap - trips based
# if (x_heatmap_distance_trips == 1){
#   
#   heatmap_trav_distance_trips_by_mode <- function(x){
#     x %>% 
#       mutate(dist_bin = as.numeric(cut(traveled_distance, breaks = breaks2))) %>% 
#       group_by(main_mode, dist_bin) %>% 
#       summarise(freq = n()) 
#   }
#   
#   heatmap_trav_distance_trips_city <- heatmap_trav_distance_trips_by_mode(scenario_trips_city)
#   
#   write.csv(heatmap_trav_distance_trips_city,file = paste0(outputDirectoryScenario,"/heatmap_trav_distance_trips_city.csv"))
#   
# }

#### #3.5 Average Traveled Distance - legs based#####
if (x_average_traveled_distance_legs == 1){
  
  avg_trav_distance_legs_by_mode <- function(x){
    x %>%
      group_by(mode) %>%
      summarise_at(vars(distance), list(name=mean)) %>% 
      pivot_wider(names_from = mode, values_from = name)
  }
  #calculation
  avg_trav_dist_legs_scenario_network <- avg_trav_distance_legs_by_mode(scenarioLegsTable)/1000
  avg_trav_dist_legs_scenario_region <- avg_trav_distance_legs_by_mode(scenario_legs_region)/1000
  avg_trav_dist_legs_scenario_city <- avg_trav_distance_legs_by_mode(scenario_legs_city)/1000
  #write table
  df_list <- list(network = avg_trav_dist_legs_scenario_network,
                  region = avg_trav_dist_legs_scenario_region,
                  city = avg_trav_dist_legs_scenario_city)
  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.avg_trav_dist.legs.csv"))
}
#### #3.6 Personen KM - legs based ####
if (x_personen_km_legs == 1){
  personen_km_legs <- function (x){
    x %>%
      group_by(mode) %>%
      summarise_at(vars(distance), list(name=mean)) %>% 
      pivot_wider(names_from = mode, values_from = name)
  }
  pkm_legs_scenario_city <- personen_km_legs(scenario_legs_city)/1000
  pkm_legs_scenario_region <- personen_km_legs(scenario_legs_region)/1000
  pkm_legs_scenario_network <- personen_km_legs(scenarioLegsTable)/1000

  df_list <- list(network = pkm_legs_scenario_network,
                  region = pkm_legs_scenario_region,
                  city = pkm_legs_scenario_city)

  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.trav_dist.legs.csv"))  
}
#### #4.1 Average Travel Time - trips based #####
if (x_average_traveled_distance_trips == 1){
  
  avg_time_trips_by_mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise_at(vars(trav_time), list(name=mean)) %>% 
      pivot_wider(names_from = main_mode, values_from = name)
  }
  #calculation
  avg_time_trips_scenario_network <- avg_time_trips_by_mode(scenarioTripsTable)
  avg_time_trips_scenario_region <- avg_time_trips_by_mode(scenario_trips_region)
  avg_time_trips_scenario_city <- avg_time_trips_by_mode(scenario_trips_city)
  #write table
  df_list <- list(network = avg_time_trips_scenario_network,
                  region = avg_time_trips_scenario_region,
                  city = avg_time_trips_scenario_city)
  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.avg_time.trips.csv"))
}

#### #4.2 Personen Stunden - trips based ####
if (x_personen_h_trips == 1){
  personen_stunden_trips <- function (x)
    {
    x %>% 
      filter(main_mode!="freight") %>%
      group_by(main_mode) %>%
      summarise(personen_stunden_trips = (sum(trav_time))) %>% 
      pivot_wider(names_from = main_mode, values_from = personen_stunden_trips)
    }
  
  ph_trips_scenario_city <- personen_km_trips(scenario_trips_city)
  ph_trips_scenario_region <- personen_km_trips(scenario_trips_region)
  ph_trips_scenario_network <- personen_km_trips(scenarioTripsTable)

  df_list <- list(network = ph_trips_scenario_network,
                  region = ph_trips_scenario_region,
                  city = ph_trips_scenario_city)
  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.trav_time.trips.csv"))
  }

#### #4.3 Average Travel Time - legs based #####

if (x_average_traveled_distance_legs == 1){
  
  avg_time_legs_by_mode <- function(x){
    x %>%
      group_by(mode) %>%
      summarise_at(vars(trav_time), list(name=mean)) %>%
      pivot_wider(names_from = mode, values_from = personen_stunden_legs)
  }
  #calculation
  avg_time_legs_scenario_network <- avg_time_legs_by_mode(scenarioLegsTable)
  avg_time_legs_scenario_region <- avg_time_legs_by_mode(scenario_legs_region)
  avg_time_legs_scenario_city <- avg_time_legs_by_mode(scenario_legs_city)
  #write table
  df_list <- list(network = avg_time_legs_scenario_network,
                  region = avg_time_legs_scenario_region,
                  city = avg_time_legs_scenario_city)
  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.avg_time.legs.csv"))
}

#### #4.4 Personen Stunden - legs based ####
if (x_personen_h_legs == 1){
  personen_stunden_legs <- function (x){
    x %>%
      group_by(mode) %>%
      summarise(personen_stunden_legs = (sum(trav_time))) %>% 
      pivot_wider(names_from = mode, values_from = personen_stunden_legs)
  }
  ph_legs_scenario_city <- personen_stunden_legs(scenario_legs_city)
  ph_legs_scenario_region <- personen_stunden_legs(scenario_legs_region)
  ph_legs_scenario_network <- personen_stunden_legs(scenarioLegsTable)

  df_list <- list(network = ph_legs_scenario_network,
                  region = ph_legs_scenario_region,
                  city = ph_legs_scenario_city)
  write.csv(bind_rows(df_list,
                      .id = "id"), 
            file = paste0(outputDirectoryScenario, "/df.trav_time.legs.csv"))
}

#### #5.1 Average Speed ####
if (x_average_traveled_speed_trips == 1){
# x) function
  avg_trav_distance <- function(x){
    x %>%
      select(main_mode, traveled_distance) %>% 
      group_by(main_mode) %>% 
      summarise(avg_trav_distance = mean(traveled_distance)) %>% 
      pivot_wider(names_from = main_mode, values_from = avg_trav_distance)
  }
  avg_trav_time <- function(x){
    x %>%
      select(main_mode, trav_time) %>% 
      mutate(trav_time = hms(trav_time)) %>% 
      group_by(main_mode) %>%  
      summarise(avgTime_s = mean(hour(trav_time)*3600 + minute(trav_time) *60 + second(trav_time) )) %>% 
      pivot_wider(names_from = main_mode, values_from = avgTime_s)
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
df_list <- list(network = avg_trav_speed_scenario_network,
                region = avg_trav_speed_scenario_region,
                city = avg_trav_speed_scenario_city)
write.csv(bind_rows(df_list,
                    .id = "id"), 
          file = paste0(outputDirectoryScenario, "/df.trav_speed.trips.csv"))
}

#### #5.2 Average Beeline Speed ####

if (x_average_beeline_speed_trips == 1){
# x) function
  avg_beeline_distance <- function(x){
    x %>%
      select(main_mode, euclidean_distance) %>% 
      group_by(main_mode) %>% 
      summarise(avg_beeline_distance = mean(euclidean_distance)) %>% 
      pivot_wider(names_from = main_mode, values_from = avg_beeline_distance)
  }
  
  avg_trav_time <- function(x){
    x %>%
      select(main_mode, trav_time) %>% 
      mutate(trav_time = hms(trav_time)) %>% 
      group_by(main_mode) %>%  
      summarise(avgTime_s = mean(hour(trav_time)*3600 + minute(trav_time) *60 + second(trav_time) )) %>% 
      pivot_wider(names_from = main_mode, values_from = avgTime_s)
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
df_list <- list(network = avg_beeline_speed_scenario_network,
                region = avg_beeline_speed_scenario_region,
                city = avg_beeline_speed_scenario_city)
write.csv(bind_rows(df_list,
                    .id = "id"), 
          file = paste0(outputDirectoryScenario, "/df.beeline_speed.trips.csv"))
} 

#### #6.1 Emissions ####
if (x_emissions == 1){
}
#### #7.1 Traffic ####
if (x_traffic == 1){
}
#### #8.1 Execution Scores Winner-Loser ####

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
  
  write.csv(base_scenario_persons, file = paste0(outputDirectoryBase,"/csv.winner_loser.values.csv"))
  
  
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



