#### DRT paths ####

scenario.drt.path = paste0(scenario_run_path, "analysis/analysis-drt/" )


#### counting DRT services ####

drt.services <- unique(scenarioTripsTable$main_mode) %>% 
  str_subset(pattern ="^drt") %>% 
  as.list()

##### Step 1: reading files and doing calculations for each DRT service #####
for (drt in drt.services){
#### reading DRT files ####

drt.files <- as.vector(list.files(path = scenario.drt.path, pattern = drt))
drt.vehicle.stats <- read.csv(paste0(scenario.drt.path, str_subset(drt.files, pattern = "vehicle_stats")), sep = ";") %>% 
  tail(n=1)
drt.customers <- read.csv(paste0(scenario.drt.path, str_subset(drt.files, pattern = "customer")), sep = ";")%>% 
  tail(n=1)
drt.KPI <- read_tsv(paste0(scenario.drt.path, str_subset(drt.files, pattern = "KPI")))

#temporary solution: drt stops file do not follow same naming pattern

if (drt == "drtNorth"){
  stop_pattern = "drt.*north"
} else {
  stop_pattern = "drt.*south"
}


### XML files ###
drt.vehicles <- xmlParse(paste0(scenario.drt.path, str_subset(drt.files, pattern = "vehicles"))) %>%
  xmlToList(vehicle)
drt.vehicles <- data.frame(do.call(rbind.data.frame,drt.vehicles))
names(drt.vehicles) <- c("vehicle", "start_link", "t_0", "t_1", "capacity")

#drt_stops <- read_xml("D:/VSP_Berlin/Leipzig/leipzig-v1.1-drt-stops-caseNamav.xml") %>% 
 # xmlToList(stopFacility)
#drt_stops <- data.frame(do.call(rbind.data.frame,drt_stops)) %>% 
 # pivot
 
#### DRT supply ####

if (x_drt_supply ==1){

nr.conventional.vehicles <- drt.vehicle.stats$vehicles
conventional.fleet.distance <- drt.vehicle.stats$totalDistance/1000
nr.stops <- as.numeric(nrow(drt.stops))
op.hours <- (as.numeric(drt.vehicles[1, 4]) - as.numeric(drt.vehicles[1, 3]))/3600

Title <- c("Number of simulated vehicles", "Total Service Hours", "Total Fleet Km", "Number of served stops")
Value <- c(nr.conventional.vehicles, conventional.fleet.distance, nr.stops, op.hours)

df.supply <- data.frame(Title, Value)
write.csv(df.supply, file = paste0(outputDirectoryScenario, "/table.supply.", drt, ".csv"))
assign(paste0("df.supply.", drt), df.supply)

}


#### DRT demand ####

if (x_drt_demand ==1) {

rides <- drt.customers$rides
in.vehicle.trav.time.mean <- drt.customers$inVehicleTravelTime_mean
euclidean.distance.traveled.mean <- drt.KPI$trips_euclidean_distance_mean

Title <- c("Nr of rides", "Mean in-vehicle travel time [s]", "Mean euclidean stop2stop distance [m]")
Value <- c(rides, in.vehicle.trav.time.mean, euclidean.distance.traveled.mean)

df.demand <- data.frame(Title, Value)
write.csv(df.demand, file = paste0(outputDirectoryScenario, "/table.demand.", drt, ".csv"))
assign(paste0("df.demand.", drt), df.demand) 
}
#### DRT performance ####

if (x_drt_performance) {
nr.conventional.vehicles <- drt.vehicle.stats$vehicles
conventional.fleet.distance <- drt.vehicle.stats$totalDistance/1000

flexa.passengerdistance <- drt.vehicle.stats$totalPassengerDistanceTraveled/1000 
flexa.empty_ratio <- drt.vehicle.stats$emptyRatio
flexa.rides <- drt.customers$rides
flexa.rides.per.vehicle <- flexa.rides / nr.conventional.vehicles
flexa.rides.per.vehKM<-  flexa.rides / conventional.fleet.distance

op.hours <- (as.numeric(drt.vehicles[1, 4]) - as.numeric(drt.vehicles[1, 3]))/3600
flexa.rides.per.opHour <-  flexa.rides / op.hours

Title <- c("Total fleet km", "Total passenger km", "Empty ratio", "Total nr of rides", "Avg. rides per vehicle", "Rides per veh-km", "Rides per operating hour")
Value <- c(conventional.fleet.distance, flexa.passengerdistance, flexa.empty_ratio, flexa.rides, flexa.rides.per.vehicle, flexa.rides.per.vehKM, flexa.rides.per.opHour)

df.performance <- data.frame(Title, Value)
write.csv(df.performance, file = paste0(outputDirectoryScenario, "/table.performance.", drt, ".csv"))
assign(paste0("df.performance.", drt), df.performance)


#----------

flexa.waiting.mean <- drt.KPI$waiting_time_mean
flexa.waiting.median <- drt.KPI$waiting_time_median
flexa.waiting.p95 <- drt.KPI$waiting_time_95_percentile

Title <- c("Waiting mean [s]", "Waiting median [s]", "Waiting p95 [s]")
Value <- c(flexa.waiting.mean, flexa.waiting.median, flexa.waiting.p95)

df.waitingtime <- data.frame(Title, Value)
write.csv(df.waitingtime, file = paste0(outputDirectoryScenario, "/table.waitingtime.", drt, ".csv"))
assign(paste0("df.waitingtime.", drt), df.waitingtime)

}

#### DRT volumes ####
 #no calculations in R necessary at the moment

#### DRT trip purposes ####
 #no calculations in R necessary at the moment
}





##### Step 2: combining df for drt services into one #####
#### DRT supply ####
  
  if (x_drt_supply ==1){
     
    df.supply <- cbind(eval(parse(text = paste0("df.supply.", drt.services[1]))),
                       eval(parse(text = paste0("df.supply.", drt.services[2])))[,2]) 
     colnames(df.supply)[2:3] <- c(paste(drt.services[1]), paste(drt.services[2]))
    
    write.csv(df.supply, file = paste0(outputDirectoryScenario, "/table.supply.csv"))
  }
  
  
#### DRT demand ####
  
  if (x_drt_demand ==1) {
    
    df.demand <- cbind(eval(parse(text = paste0("df.demand.", drt.services[1]))),
                       eval(parse(text = paste0("df.demand.", drt.services[2])))[,2]) 
    colnames(df.demand)[2:3] <- c(paste(drt.services[1]), paste(drt.services[2]))
    
   
    write.csv(df.demand, file = paste0(outputDirectoryScenario, "/table.demand.csv"))
   
  }
#### DRT performance ####
  
  if (x_drt_performance) {
    df.performance <- cbind(eval(parse(text = paste0("df.performance.", drt.services[1]))),
                       eval(parse(text = paste0("df.performance.", drt.services[2])))[,2]) 
    colnames(df.performance)[2:3] <- c(paste(drt.services[1]), paste(drt.services[2]))
    
    
    write.csv(df.performance, file = paste0(outputDirectoryScenario, "/table.performance.csv"))
    
    
    df.waitingtime <- cbind(eval(parse(text = paste0("df.waitingtime.", drt.services[1]))),
                       eval(parse(text = paste0("df.waitingtime.", drt.services[2])))[,2]) 
    colnames(df.waitingtime)[2:3] <- c(paste(drt.services[1]), paste(drt.services[2]))
    
    
    write.csv(df.waitingtime, file = paste0(outputDirectoryScenario, "/table.waitingtime.csv"))

    
  }
  
#### DRT volumes ####
  #no calculations in R necessary at the moment
  
#### DRT trip purposes ####
  #no calculations in R necessary at the moment
  
