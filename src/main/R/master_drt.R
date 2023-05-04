#### counting DRT services ####

drt_services <- unique(scenarioTripsTable$main_mode) %>% 
  str_subset(pattern ="^drt") %>% 
  as.list()

for (drt in drt_services){
  
#### reading DRT files ####
  
drt_vehicle_stats_north <- read.csv(drt_vehicle_stats_north_path, sep = ";") %>% 
  tail(n=1)
drt_vehicle_stats_south <- read.csv(drt_vehicle_stats_south_path, sep = ";") %>% 
  tail(n=1)

drt_vehicles_north <- read.csv(drt_vehicle_north_path, sep = ";")
drt_vehicles_south <- read.csv(drt_vehicle_south_path, sep = ";")

drt_stops_north <- read.csv(drt_stops_north_path, sep = ";")
drt_stops_south <- read.csv(drt_stops_north_path, sep = ";")

drt_customers_north <- read.csv(drt_customers_north_path, sep = ";")
drt_customers_southeast <- read.csv(drt_customers_south_path, sep = ";")

drt_KPI_north <- read.csv(drt_KPI_north_path, sep = ";")
drt_KPI_southeast<- read.csv(drt_KPI_south_path, sep = ";")


#### DRT supply ####

nr_conventional_vehicles <- drt_vehicle_stats_north$vehicles
conventional_fleet_distance <- drt_vehicle_stats_north$totalDistance/1000
flexa_passenger_distance <- drt_vehicle_stats_north$totalPassengerDistanceTraveled/1000

flexa_empty_ratio <- drt_vehicle_stats_north$emptyRatio
flexa_rides <- drt_customers_north
flexa_rides_per_vehicle



#### DRT demand ####

#### DRT performance ####

#### DRT volumes ####

#### DRT trip purposes ####

}