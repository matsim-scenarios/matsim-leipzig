#### DRT paths ####

# scenario.drt.path = paste0(scenario.run.path, "analysis/analysis-drt/")

print("HERE2")
#### counting DRT services ####

drt.modes <- unique(scenario.trips.table$main_mode) %>% 
  str_subset(pattern ="^drt") %>% 
  as.list()

print("HERE4")

##### Step 1: reading files and doing calculations for each DRT service #####

df.supply <- data.frame(matrix(ncol = 0, nrow = 3))
df.supply.names <- c("Number of simulated vehicles", "Total Service Hours", "Total Fleet Km")
add_column(df.supply, Title = df.supply.names)

for (drtMode in drt.modes){
  #### reading DRT files ####

  print("HERE5")

  drt.files <- as.vector(list.files(path = scenario.run.path, pattern = drtMode))
  print("HERE6")
  drt.vehicle.stats <- read.csv(paste0(scenario.run.path, str_subset(drt.files, pattern = "vehicle_stats")), sep = ";") %>%
    tail(n=1)
  print("HERE7")
  drt.customers <- read.csv(paste0(scenario.run.path, str_subset(drt.files, pattern = "customer_stats")), sep = ";")%>%
    tail(n=1)
  print("HERE8")
  drt.KPI <- read_tsv(paste0(outputDirectoryScenarioDrt, drtMode, "_KPI.tsv"))
  print("HERE9")

  #temporary solution: drt stops file do not follow same naming pattern

  if (drtMode == "drtNorth"){
    stop.pattern = "drt.*north"
  } else if (drtMode == "drtSoutheast") {
    stop.pattern = "drt.*southeast"
  } else {
    stop.pattern = "drt.*"
  }

  print("HERE1")

  ### XML files ###
  drt.vehicles <- xmlParse(paste0(scenario.run.path, str_subset(drt.files, pattern = "vehicles"))) %>%
    xmlToList(vehicle)
  drt.vehicles <- data.frame(do.call(rbind.data.frame,drt.vehicles))
  names(drt.vehicles) <- c("vehicle", "start_link", "t_0", "t_1", "capacity")

  # TODO talk about omitting this analysis as it is rather useless anyways, we can just use a hexagon plot..
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
    # nr.stops <- as.numeric(nrow(drt.stops))
    op.hours <- (as.numeric(drt.vehicles[1, 4]) - as.numeric(drt.vehicles[1, 3]))/3600

    # Title <- c("Number of simulated vehicles", "Total Service Hours", "Total Fleet Km", "Number of served stops")
    Title <- df.supply.names
    # Value <- c(nr.conventional.vehicles, op.hours, conventional.fleet.distance, nr.stops)
    Value <- c(nr.conventional.vehicles, op.hours, conventional.fleet.distance)

    df.supply.drtMode <- data.frame(Title, Value)
    colnames(df.supply.drtMode)[2] <- drtMode
    write.csv(df.supply.drtMode, file = paste0(outputDirectoryScenarioDrt, "/table.supply.", drtMode, ".csv"))
    assign(paste0("df.supply.", drtMode), df.supply.drtMode)

   add_column(df.supply, drtMode = c(df.supply.drtMode$drtMode[1], df.supply.drtMode$drtMode[2], df.supply.drtMode$drtMode[3]))
  }


  #### 9.2 DRT demand ####
  if (x_drt_demand ==1) {
    print("#### in 9.2 ####")

    rides <- drt.customers$rides
    in.vehicle.trav.time.mean <- drt.customers$inVehicleTravelTime_mean
    euclidean.distance.traveled.mean <- drt.KPI$trips_euclidean_distance_mean

    Title <- c("Nr of rides", "Mean in-vehicle travel time [s]", "Mean euclidean stop2stop distance [m]")
    Value <- c(rides, in.vehicle.trav.time.mean, euclidean.distance.traveled.mean)

    df.demand <- data.frame(Title, Value)
    write.csv(df.demand, file = paste0(outputDirectoryScenarioDrt, "/table.demand.", drtMode, ".csv"))
    assign(paste0("df.demand.", drtMode), df.demand)
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

    Title <- c("Total fleet km", "Total passenger km", "Empty ratio", "Total nr of rides", "Avg. rides per vehicle", "Rides per veh-km", "Rides per operating hour")
    Value <- c(conventional.fleet.distance, flexa.passengerdistance, flexa.empty_ratio, flexa.rides, flexa.rides.per.vehicle, flexa.rides.per.vehKM, flexa.rides.per.opHour)

    df.performance <- data.frame(Title, Value)
    write.csv(df.performance, file = paste0(outputDirectoryScenarioDrt, "/table.performance.", drtMode, ".csv"))
    assign(paste0("df.performance.", drtMode), df.performance)


    #----------

    flexa.waiting.mean <- drt.KPI$waiting_time_mean
    flexa.waiting.median <- drt.KPI$waiting_time_median
    flexa.waiting.p95 <- drt.KPI$waiting_time_95_percentile

    Title <- c("Waiting mean [s]", "Waiting median [s]", "Waiting p95 [s]")
    Value <- c(flexa.waiting.mean, flexa.waiting.median, flexa.waiting.p95)

    df.waitingtime <- data.frame(Title, Value)
    write.csv(df.waitingtime, file = paste0(outputDirectoryScenarioDrt, "/table.waitingtime.", drtMode, ".csv"))
    assign(paste0("df.waitingtime.", drtMode), df.waitingtime)

  }

}

##### Step 2: combining df for drt services into one #####
#### DRT supply ####
  
  if (x_drt_supply ==1){

    print("HERE10")

    # TODO figure out how to build df.supply by looping through drt modes. I do not understand what eval() does
    df.supply2 <- data.frame(matrix(ncol = 1, nrow = 0))
    df.supply2 <- df.supply2 %>%
      add_row("Title") %>%
      add_row("Total Service Hours") %>%
      add_row("Total Fleet Km")

    i <- 1
    for (drtMode in drt.modes) {
      df.supply2 <- cbind(eval(parse(text = paste0("df.supply.", drt.modes[1]))),
                         eval(parse(text = paste0("df.supply.", drt.modes[2])))[, 2])
      colnames(df.supply)[2:3] <- c(paste(drt.modes[1]), paste(drt.modes[2]))

      i <- i + 1
    }

    df.supply <- cbind(eval(parse(text = paste0("df.supply.", drt.modes[1]))),
                       eval(parse(text = paste0("df.supply.", drt.modes[2])))[, 2])
    colnames(df.supply)[2:3] <- c(paste(drt.modes[1]), paste(drt.modes[2]))

    print("HERE11")
    
    write.csv(df.supply, file = paste0(outputDirectoryScenarioDrt, "/table.supply.csv"))
  }
  
  
#### DRT demand ####
  
  if (x_drt_demand ==1) {
    
    df.demand <- cbind(eval(parse(text = paste0("df.demand.", drt.modes[1]))),
                       eval(parse(text = paste0("df.demand.", drt.modes[2])))[, 2]) 
    colnames(df.demand)[2:3] <- c(paste(drt.modes[1]), paste(drt.modes[2]))
    
   
    write.csv(df.demand, file = paste0(outputDirectoryScenarioDrt, "/table.demand.csv"))
   
  }
#### DRT performance ####
  
  if (x_drt_performance) {
    df.performance <- cbind(eval(parse(text = paste0("df.performance.", drt.modes[1]))),
                       eval(parse(text = paste0("df.performance.", drt.modes[2])))[, 2]) 
    colnames(df.performance)[2:3] <- c(paste(drt.modes[1]), paste(drt.modes[2]))
    
    
    write.csv(df.performance, file = paste0(outputDirectoryScenarioDrt, "/table.performance.csv"))
    
    
    df.waitingtime <- cbind(eval(parse(text = paste0("df.waitingtime.", drt.modes[1]))),
                       eval(parse(text = paste0("df.waitingtime.", drt.modes[2])))[, 2]) 
    colnames(df.waitingtime)[2:3] <- c(paste(drt.modes[1]), paste(drt.modes[2]))
    
    
    write.csv(df.waitingtime, file = paste0(outputDirectoryScenarioDrt, "/table.waitingtime.csv"))

    
  }
  
#### DRT volumes ####
  #no calculations in R necessary at the moment
  
#### DRT trip purposes ####
# TODO put script into here
  #no calculations in R necessary at the moment
  
