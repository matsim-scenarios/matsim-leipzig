#### DRT paths ####

scenario_drt_path = paste0(scenario_run_path, "analysis/analysis-drt/" )

#temporary solution: pick drt stops file that is supposed to be read
drt_stops <- read.csv(paste0(scenario_drt_path, list.files(scenario_drt_path, pattern = "drt.*north")))
#drt_stops <- read.csv(paste0(scenario_drt_path, list.files(scenario_drt_path, pattern = "drt.*south")))


#### counting DRT services ####

drt_services <- unique(scenarioTripsTable$main_mode) %>% 
  str_subset(pattern ="^drt") %>% 
  as.list()

for (drt in drt_services){
  
#### reading DRT files ####

drt_files <- as.vector(list.files(path = scenario_drt_path, pattern = drt))
drt_vehicle_stats <- read.csv(paste0(scenario_drt_path, str_subset(drt_files, pattern = "vehicle_stats")), sep = ";") %>% 
  tail(n=1)
drt_customers <- read.csv(paste0(scenario_drt_path, str_subset(drt_files, pattern = "customer")), sep = ";")%>% 
  tail(n=1)
drt_KPI <- read_tsv(paste0(scenario_drt_path, str_subset(drt_files, pattern = "KPI")))



### XML files ###
drt_vehicles <- xmlParse(paste0(scenario_drt_path, str_subset(drt_files, pattern = "vehicles"))) %>%
  xmlToList(vehicle)
drt_vehicles <- data.frame(do.call(rbind.data.frame,drt_vehicles))
names(drt_vehicles) <- c("vehicle", "start_link", "t_0", "t_1", "capacity")

#drt_stops <- read_xml("D:/VSP_Berlin/Leipzig/leipzig-v1.1-drt-stops-caseNamav.xml") %>% 
 # xmlToList(stopFacility)
#drt_stops <- data.frame(do.call(rbind.data.frame,drt_stops)) %>% 
 # pivot
 
#### DRT supply ####

nr_conventional_vehicles <- drt_vehicle_stats$vehicles
conventional_fleet_distance <- drt_vehicle_stats$totalDistance/1000
nr_stops <- as.numeric(nrow(drt_stops))
op_hours <- (as.numeric(drt_vehicles[1, 4]) - as.numeric(drt_vehicles[1, 3]))/3600

Title <- c("Number of simulated vehicles", "Total Service Hours", "Total Fleet Km", "Number of served stops")
Value <- c(nr_conventional_vehicles, conventional_fleet_distance, nr_stops, op_hours)

supply_df <- data.frame(Title, Value)
write.csv(supply_df, file = paste0(outputDirectoryScenario, "/supply_table_", drt, ".csv"))

#### DRT demand ####

rides <- drt_customers$rides
in_vehicle_trav_time_mean <- drt_customers$inVehicleTravelTime_mean
euclidean_distance_traveled_mean <- drt_KPI$trips_euclidean_distance_mean

Title <- c("Nr of rides", "Mean in-vehicle travel time [s]", "Mean euclidean stop2stop distance [m]")
Value <- c(rides, in_vehicle_trav_time_mean, euclidean_distance_traveled_mean)

demand_df <- data.frame(Title, Value)
write.csv(demand_df, file = paste0(outputDirectoryScenario, "/demand_table_", drt, ".csv"))
#### DRT performance ####

#### DRT volumes ####

#### DRT trip purposes ####

}