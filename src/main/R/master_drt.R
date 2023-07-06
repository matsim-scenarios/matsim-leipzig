#### counting DRT services ####

drt.modes <- unique(scenario.trips.table$main_mode) %>% 
  str_subset(pattern ="^drt") %>% 
  as.list()

##### Step 1: reading files and doing calculations for each DRT service #####
# create dfs, which are to be filled with values when iterating
df.supply.names <- c("Number of simulated vehicles", "Total Service Hours", "Total Fleet Km")
df.supply <- data.frame(df.supply.names)
colnames(df.supply) <- "Title"

df.demand.names <- c("Nr of rides", "Mean in-vehicle travel time [s]", "Mean euclidean stop2stop distance [m]")
df.demand <- data.frame(df.demand.names)
colnames(df.demand) <- "Title"

df.performance.names <- c("Total fleet km", "Total passenger km", "Empty ratio", "Total nr of rides", "Avg. rides per vehicle", "Rides per veh-km", "Rides per operating hour")
df.performance <- data.frame(df.performance.names)
colnames(df.performance) <- "Title"

df.waiting.time.names <- c("Waiting mean [s]", "Waiting median [s]", "Waiting p95 [s]")
df.waiting.time <- data.frame(df.waiting.time.names)
colnames(df.waiting.time) <- "Title"

for (drtMode in drt.modes){
  #### reading DRT files ####
  drt.files <- as.vector(list.files(path = scenario.run.path, pattern = drtMode))
  drt.vehicle.stats <- read.csv(paste0(scenario.run.path, str_subset(drt.files, pattern = "vehicle_stats")), sep = ";") %>%
    tail(n=1)
  drt.customers <- read.csv(paste0(scenario.run.path, str_subset(drt.files, pattern = "customer_stats")), sep = ";")%>%
    tail(n=1)
  drt.KPI <- read_tsv(paste0(outputDirectoryScenarioDrt, drtMode, "_KPI.tsv"))

  #temporary solution: drt stops file do not follow same naming pattern

  if (drtMode == "drtNorth"){
    stop.pattern = "drt.*north"
  } else if (drtMode == "drtSoutheast") {
    stop.pattern = "drt.*southeast"
  } else {
    stop.pattern = "drt.*"
  }

  ### XML files ###
  drt.vehicles <- xmlParse(paste0(scenario.run.path, str_subset(drt.files, pattern = "vehicles"))) %>%
    xmlToList(vehicle)
  drt.vehicles <- data.frame(do.call(rbind.data.frame,drt.vehicles))
  names(drt.vehicles) <- c("vehicle", "start_link", "t_0", "t_1", "capacity")

  # talk about omitting this analysis as it is rather useless anyways, we can just use a hexagon plot.. -sme0623
  # result of discussion: lets use hexagon plots
  # we keep this here though, we may want to include this analysis sometime
  #mb use this: https://vsp.berlin/avoev/v/aggregate-od/gladbeck/output-snzDrtO444l/viz-od-2.yml
  # DRT stops files can now be read as XML - the correct ones just need to be put into the folder on public svn & the code un-commented
  # drt.stops.raw <- xmlParse(paste0(outputDirectoryScenarioDrt, str_subset(list.files(path = outputDirectoryScenarioDrt), pattern = stop.pattern)))
  # drt.stops <- xmlToList(drt_stops_raw)
  # drt.stops <- data.frame(do.call(cbind.data.frame,drt.stops))
  # drt.stops <- data.frame(t(drt.stops[-1]))


  #### 9.1 DRT supply ####
  if (x_drt_supply ==1){
    print("#### in 9.1 ####")

    nr.conventional.vehicles <- drt.vehicle.stats$vehicles
    conventional.fleet.distance <- drt.vehicle.stats$totalDistance/1000
    op.hours <- (as.numeric(drt.vehicles[1, 4]) - as.numeric(drt.vehicles[1, 3]))/3600

    Title <- df.supply.names
    Value <- c(nr.conventional.vehicles, op.hours, conventional.fleet.distance)

    df.supply.drtMode <- data.frame(Title, Value)
    colnames(df.supply.drtMode)[2] <- drtMode
    write.csv(df.supply.drtMode, file = paste0(outputDirectoryScenarioDrt, "/table.supply.", drtMode, ".csv"), row.names = FALSE, quote=FALSE)

    df.supply <- cbind(df.supply, df.supply.drtMode[2])
  }


  #### 9.2 DRT demand ####
  if (x_drt_demand ==1) {
    print("#### in 9.2 ####")

    rides <- drt.customers$rides
    in.vehicle.trav.time.mean <- drt.customers$inVehicleTravelTime_mean
    euclidean.distance.traveled.mean <- drt.KPI$trips_euclidean_distance_mean

    Title <- df.demand.names
    Value <- c(rides, in.vehicle.trav.time.mean, euclidean.distance.traveled.mean)

    df.demand.drtMode <- data.frame(Title, Value)
    colnames(df.demand.drtMode)[2] <- drtMode
    write.csv(df.demand.drtMode, file = paste0(outputDirectoryScenarioDrt, "/table.demand.", drtMode, ".csv"), row.names = FALSE, quote=FALSE)

    df.demand <- cbind(df.demand, df.demand.drtMode[2])
  }
  #### 9.3 DRT performance ####
  if (x_drt_performance) {
    print("#### in 9.3 ####")
    nr.conventional.vehicles <- drt.vehicle.stats$vehicles
    conventional.fleet.distance <- drt.vehicle.stats$totalDistance/1000

    flexa.passengerdistance <- drt.vehicle.stats$totalPassengerDistanceTraveled/1000
    flexa.empty_ratio <- drt.vehicle.stats$emptyRatio
    flexa.rides <- drt.customers$rides
    flexa.rides.per.vehicle <- flexa.rides / nr.conventional.vehicles
    flexa.rides.per.vehKM<-  flexa.rides / conventional.fleet.distance

    op.hours <- (as.numeric(drt.vehicles[1, 4]) - as.numeric(drt.vehicles[1, 3]))/3600
    flexa.rides.per.opHour <-  flexa.rides / op.hours

    Title <- df.performance.names
    Value <- c(conventional.fleet.distance, flexa.passengerdistance, flexa.empty_ratio, flexa.rides, flexa.rides.per.vehicle, flexa.rides.per.vehKM, flexa.rides.per.opHour)

    df.performance.drtMode <- data.frame(Title, Value)
    colnames(df.performance.drtMode)[2] <- drtMode
    write.csv(df.performance.drtMode, file = paste0(outputDirectoryScenarioDrt, "/table.performance.", drtMode, ".csv"), row.names = FALSE, quote=FALSE)

    df.performance <- cbind(df.performance, df.performance.drtMode[2])

    #----------

    flexa.waiting.mean <- drt.KPI$waiting_time_mean
    flexa.waiting.median <- drt.KPI$waiting_time_median
    flexa.waiting.p95 <- drt.KPI$waiting_time_95_percentile

    Title <- df.waiting.time.names
    Value <- c(flexa.waiting.mean, flexa.waiting.median, flexa.waiting.p95)

    df.waiting.time.drtMode <- data.frame(Title, Value)
    colnames(df.waiting.time.drtMode)[2] <- drtMode
    write.csv(df.waiting.time.drtMode, file = paste0(outputDirectoryScenarioDrt, "/table.waitingtime.", drtMode, ".csv"), row.names = FALSE, quote=FALSE)

    df.waiting.time <- cbind(df.waiting.time, df.waiting.time.drtMode[2])
  }
}

##### Step 2: print the joined tables for supply, demand and performance #####
#### DRT supply ####
if (x_drt_supply == 1){
  write.csv(df.supply, file = paste0(outputDirectoryScenarioDrt, "/table.supply.csv"), row.names = FALSE, quote=FALSE)
}
  
  
#### DRT demand ####
  
if (x_drt_demand == 1) {
  write.csv(df.demand, file = paste0(outputDirectoryScenarioDrt, "/table.demand.csv"), row.names = FALSE, quote=FALSE)
}

#### DRT performance ####
if (x_drt_performance == 1) {
  write.csv(df.performance, file = paste0(outputDirectoryScenarioDrt, "/table.performance.csv"), row.names = FALSE, quote=FALSE)
  write.csv(df.waiting.time, file = paste0(outputDirectoryScenarioDrt, "/table.waitingtime.csv"), row.names = FALSE, quote=FALSE)
}

#### DRT volumes ####
  #no calculations in R necessary at the moment

#### 9.4 DRT trip purpose analysis ####
if (x_drt_trip_purposes == 1) {
  print("#### in 9.4 ####")

  source_with_args <- function(file, ...){
    system(paste("Rscript", file, ...))
  }
  source_with_args(
    "../matsim-leipzig/src/main/R/Analysis/ODAnalysis/Trippurpose-Analysis.R",
    paste0("--runDir=", scenario.run.path)
  )
}

  
