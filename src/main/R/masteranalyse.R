# TODO: 1) adding carfreearea analysis tab
# TODO: 2) adding text to yaml file which clarifies that carfreearea analysis is only valid for carfree area scenarios
#### reading shp files ####
region.shape <- st_read(region.shp.path, crs=CRS) #study area
city.shape <- st_read(city.shp.path, crs=CRS) #city of Leipzig
area.shape <- st_read(area.shp.path, crs=CRS)#scenario area
print("#### Shapes geladen! ####")

#### reading trips/legs files ####

## Trip File
scenario.trips.table <- readTripsTable(pathToMATSimOutputDirectory = scenario.run.path)
print("#### Trips geladen! ####")

## Leg Files
scenario.legs.table <- read_delim(paste0(scenario.run.path,"/",list.files(path = scenario.run.path, pattern = "output_legs")), delim= ";") #, n_max = 3000)
print("#### Legs geladen! ####")

## Filters
scenario.trips.region <- filterByRegion(scenario.trips.table,region.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario.trips.city <- filterByRegion(scenario.trips.table,city.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario.trips.area <- filterByRegion(scenario.trips.table,area.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)

print("#### Trips gefiltert! ####")
scenario.legs.region <- filterByRegion(scenario.legs.table,region.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario.legs.city <- filterByRegion(scenario.legs.table,city.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
scenario.legs.area <- filterByRegion(scenario.legs.table,area.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
print("#### Legs gefiltert! ####")



#### files/filters for comparisons ####

if (x_sankey_diagram == 1){
  base.trips.table <- readTripsTable(pathToMATSimOutputDirectory = base.run.path)
  
  base.trips.region <- filterByRegion(base.trips.table,region.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
  base.trips.city <- filterByRegion(base.trips.table,city.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
  base.trips.area <- filterByRegion(base.trips.table,area.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
  
}

if (x_winner_loser == 1){
  base.persons <- readPersonsTable(base.run.path)
  #### reading persons #### just for winner-loser-analysis
  scenario.persons <- readPersonsTable(scenario.run.path)
  print("#### Personen geladen! ####")
}


#### 0. Parameters ####

#BREAKING DIFFERENT DISTANCES IN M
# for modal split
breaks <- c(0, 1000, 2000, 5000, 10000, 20000, Inf)
# for heatmap TODO refactor naming breaks2 to dist_breaks
breaks2 <- c(0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000, 20000, Inf)
time_breaks <- c(-Inf,  0, 1200, 2400,3600,4800,6000,7200,8400,9600,10800,12000,13200,14400,15600,16800,18000,Inf)
time_labels <- c("0 mins", "<20 mins","<40 mins", "<60 mins","<80 mins","<100 mins","<120 mins",
                "<140 mins" ,"<160 mins", "<180 mins" , "<200 mins", "<220 mins" ,"<240 mins",
                "<260 mins", "<280 mins", "<300 mins", ">= 300 mins")
#NAMES OF THE CASES
cases <- c("base", "scenario")
print("#### Parameter spezifiziert! ####")


#### #1.1 Modal Split - trips based - main mode (count) ####
print("#### in 1.1 ####")
if (x_ms_trips_count == 1){
  
  modal_split.trips.main_mode <- function(x){
    x %>%
      count(main_mode) %>%
      mutate(percent = 100*n/sum(n))
  }
 
  ms.main_mode.trips.city <- modal_split.trips.main_mode(scenario.trips.city)
  ms.main_mode.trips.city <- t(ms.main_mode.trips.city)
  colnames(ms.main_mode.trips.city) <- ms.main_mode.trips.city[1, ]
  write.csv(ms.main_mode.trips.city, file = paste0(outputDirectoryScenario, "/pie.ms.counts.trips.city.csv"))
  
  ms.main_mode.trips.region <- modal_split.trips.main_mode(scenario.trips.region)
  ms.main_mode.trips.region <- t(ms.main_mode.trips.region)
  colnames(ms.main_mode.trips.region) <- ms.main_mode.trips.region[1, ]
  write.csv(ms.main_mode.trips.region, file = paste0(outputDirectoryScenario, "/pie.ms.counts.trips.regio.csv"))

  #TODO adding modal split for carfree area
}

#### #1.2 Modal Split - trips based - distance ####
print("#### in 1.2 ####")
if (x_ms_trips_distance == 1){
  modal_split.trips.distance <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise(distance = sum(traveled_distance)) %>%
      mutate(percent = round(100*distance/sum(distance),2))
  }
  ms.dist.trips.city <- modal_split.trips.distance(scenario.trips.city)
  ms.dist.trips.city <- t(ms.dist.trips.city)
  colnames(ms.dist.trips.city) <- ms.dist.trips.city[1, ]
  write.csv(ms.dist.trips.city, file = paste0(outputDirectoryScenario, "/pie.ms.dist.trips.city.csv"))
  
  ms.dist.trips.region <- modal_split.trips.distance(scenario.trips.region)
  ms.dist.trips.region <- t(ms.dist.trips.region)
  colnames(ms.dist.trips.region) <- ms.dist.trips.region[1, ]
  write.csv(ms.dist.trips.region, file = paste0(outputDirectoryScenario, "/pie.ms.dist.trips.region.csv"))
}


#### #1.3 Modal Split - legs based - main mode (count) ####
print("#### in 1.3 ####")
if (x_ms_legs_count == 1){
  modal_split.legs.mode <- function(x){
    x %>%
      mutate(distance_cut = cut(distance, breaks = breaks,
                                labels = c("<1000m", "1 - 2km", "2 - 5km", "5 - 10km", "10 - 20km", ">20km" ))) %>%
      group_by(distance_cut) %>% 
      count(mode) %>%
      mutate(percent = 100*n/sum(n))
  }
  ms.mode.legs.city <- modal_split.legs.mode(scenario.legs.city)
  ms.mode.legs.city <- t(ms.mode.legs.city)
  colnames(ms.mode.legs.city) <- ms.mode.legs.city[1, ]
  write.csv(ms.mode.legs.city,file = paste0(outputDirectoryScenario,"/pie.ms.counts.legs.city.csv"))
  
  ms.mode.legs.region <- modal_split.legs.mode(scenario.legs.region)
  ms.mode.legs.region <- t(ms.mode.legs.region)
  colnames(ms.mode.legs.region) <- ms.mode.legs.region[1, ]
  write.csv(ms.mode.legs.region,file = paste0(outputDirectoryScenario,"/pie.ms.counts.legs.region.csv"))
}

#### #1.4 Modal Split - legs based - distance ####
print("#### in 1.4 ####")
if (x_ms_legs_distance == 1){
  modal_split.legs.distance <- function(x){
    x %>%
      group_by(mode) %>%
      summarise(distance = sum(distance)) %>%
      mutate(percent = round(100*distance/sum(distance),2))
  }
  ms.dist.legs.city <- modal_split.legs.distance(scenario.legs.city)
  ms.dist.legs.city <- t(ms.dist.legs.city)
  colnames(ms.dist.legs.city) <- ms.dist.legs.city[1, ]
  write.csv(ms.dist.legs.city,file = paste0(outputDirectoryScenario,"/pie.ms.dist.legs.city.csv"))
  
  ms.dist.legs.region <- modal_split.legs.distance(scenario.legs.region)
  ms.dist.legs.region <- t(ms.dist.legs.region)
  colnames(ms.dist.legs.region) <- ms.dist.legs.region[1, ]
  write.csv(ms.dist.legs.region,file = paste0(outputDirectoryScenario,"/pie.ms.dist.legs.region.csv"))
}

#### #2.1 Sankey Modal Shift ####
print("#### in 2.1 ####")

if (x_sankey_diagram == 1){
  sankey_dataframe <- function(x, y){
    inner_join(x, y, by = "trip_id") %>%
      select(main_mode.x, main_mode.y) %>%
      group_by(main_mode.x, main_mode.y) %>%
      summarise(Freq = n())
  }
  
  #Base Case > Policy Case CITY
  base_city_to_scenario_city <- sankey_dataframe(base.trips.city, scenario.trips.city)
  
  write.csv(base_city_to_scenario_city, file = paste0(outputDirectoryScenario,"/sankey.shift.city.csv"))
  
  #Base Case > Policy Case REGION
  base_region_to_scenario_region <- sankey_dataframe(base.trips.region, scenario.trips.region)
  
  write.csv(base_region_to_scenario_region, file = paste0(outputDirectoryScenario,"/sankey.shift.region.csv"))
}

#### #3.1 Average Traveled Distance - trips based####
print("#### in 3.1 ####")

if (x_average_traveled_distance_trips == 1){
  
  avg.trav_distance.trips.by.mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise_at(vars(traveled_distance), list(name=mean)) %>%
      pivot_wider(names_from = main_mode, values_from = name)
  }
  #calculation
  avg.trav_dist.trips.scenario.network <- avg.trav_distance.trips.by.mode(scenario.trips.table)
  avg.trav_dist.trips.scenario.region <- avg.trav_distance.trips.by.mode(scenario.trips.region)
  avg.trav_dist.trips.scenario.city <- avg.trav_distance.trips.by.mode(scenario.trips.city)
  
  #write table
  write.csv(avg.trav_dist.trips.scenario.network, file = paste0(outputDirectoryScenario,"/df.avg_trav_dist.trips.network.csv"))
  write.csv(avg.trav_dist.trips.scenario.region, file = paste0(outputDirectoryScenario,"/df.avg_trav_dist.trips.region.csv"))
  write.csv(avg.trav_dist.trips.scenario.city, file = paste0(outputDirectoryScenario,"/df.avg_trav_dist.trips.city.csv"))

  df.list <- list(network = avg.trav_dist.trips.scenario.network,
                  region = avg.trav_dist.trips.scenario.region,
                  city = avg.trav_dist.trips.scenario.city)
  write.csv(bind_rows(df.list,
                      .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.trav_dist.trips.csv"))

}

#### #3.2 Average Euclidean Distance - trips based####
print("#### in 3.2 ####")
if (x_average_euclidean_distance_trips == 1){
  
  avg.eucl_distance.trips.by.mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise_at(vars(euclidean_distance), list(name=mean)) %>%
      pivot_wider(names_from = main_mode, values_from = name)
  }
  #calculation
  avg.eucl_dist.trips.scenario.network <- avg.eucl_distance.trips.by.mode(scenario.trips.table)
  avg.eucl_dist.trips.scenario.region <- avg.eucl_distance.trips.by.mode(scenario.trips.region)
  avg.eucl_dist.trips.scenario.city <- avg.eucl_distance.trips.by.mode(scenario.trips.city)
  #write table
  write.csv(avg.eucl_dist.trips.scenario.network, file = paste0(outputDirectoryScenario,"/df.avg_eucl_dist.trips.network.csv"))
  write.csv(avg.eucl_dist.trips.scenario.region, file = paste0(outputDirectoryScenario,"/df.avg_eucl_dist.trips.region.csv"))
  write.csv(avg.eucl_dist.trips.scenario.city, file = paste0(outputDirectoryScenario,"/df.avg_eucl_dist.trips.city.csv"))

  df.list <- list(network = avg.eucl_dist.trips.scenario.network,
                  region = avg.eucl_dist.trips.scenario.region,
                  city = avg.eucl_dist.trips.scenario.city)
  write.csv(bind_rows(df.list,
            .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.eucl_dist.trips.csv"))
}
#### #3.3 Traveled Distance Heatmap - trips based ####

if (x_heatmap_distance_trips == 1){

  heatmap.trav_distance.trips.by.mode <- function(x){
    x %>%
      mutate(dist_bin = as.numeric(cut(traveled_distance, breaks = breaks2))) %>%
      group_by(main_mode, dist_bin) %>%
      summarise(freq = n()) %>%
      pivot_wider(names_from = main_mode, values_from = freq)
  }

  heatmap.trav_distance.trips.city <- heatmap.trav_distance.trips.by.mode(scenario.trips.city)

  write.csv(heatmap.trav_distance.trips.city,file = paste0(outputDirectoryScenario,"/heatmap.trav_distance.trips.city.csv"))

}

#### #3.4 Personen KM - trips based ####
print("#### in 3.4 ####")
if (x_personen_km_trips == 1){
  personen.km.trips <- function (x){
    x %>%
      filter(main_mode!="freight") %>%
      group_by(main_mode) %>%
      summarise(pers_km = sum(traveled_distance)/1000) %>%
      pivot_wider(names_from = main_mode, values_from = pers_km)
    
  }
  pkm.trips.city <- personen.km.trips(scenario.trips.city)
  pkm.trips.region <- personen.km.trips(scenario.trips.region)
  pkm.trips.network <- personen.km.trips(scenario.trips.table)
  
  write.csv(pkm.trips.city, file = paste0(outputDirectoryScenario,"/df.pkm.trips.city.csv"))
  write.csv(pkm.trips.region, file = paste0(outputDirectoryScenario,"/df.pkm.trips.region.csv"))
  write.csv(pkm.trips.network, file = paste0(outputDirectoryScenario,"/df.pkm.trips.network.csv"))
  
  df.list <- list(network = pkm.trips.network,
                  region = pkm.trips.region,
                  city = pkm.trips.city)
  write.csv(bind_rows(df.list,
                      .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.pkm.trips.csv"))
}

#### #3.5 Average Traveled Distance - legs based#####
print("#### in 3.5 ####")
if (x_average_traveled_distance_legs == 1){
  
  avg.trav_distance.legs.by.mode <- function(x){
    x %>%
      group_by(mode) %>%
      summarise_at(vars(distance), list(name=mean)) %>%
      pivot_wider(names_from = mode, values_from = name)

  }
  #calculation
  avg.trav_dist.legs.scenario.network <- avg.trav_distance.legs.by.mode(scenario.legs.table)
  avg.trav_dist.legs.scenario.region <- avg.trav_distance.legs.by.mode(scenario.legs.region)
  avg.trav_dist.legs.scenario.city <- avg.trav_distance.legs.by.mode(scenario.legs.city)
  #write table
  write.csv(avg.trav_dist.legs.scenario.network, file = paste0(outputDirectoryScenario,"/df.avg_trav_dist.legs.network.csv"))
  write.csv(avg.trav_dist.legs.scenario.region, file = paste0(outputDirectoryScenario,"/df.avg_trav_dist.legs.region.csv"))
  write.csv(avg.trav_dist.legs.scenario.city, file = paste0(outputDirectoryScenario,"/df.avg_trav_dist.legs.city.csv"))

  df.list <- list(network = avg.trav_dist.legs.scenario.network,
                  region = avg.trav_dist.legs.scenario.region,
                  city = avg.trav_dist.legs.scenario.city)
  write.csv(bind_rows(df.list,
                      .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.trav_dist.legs.csv"))
  }

#### #3.6 Personen KM - legs based ####
print("#### in 3.6 ####")
if (x_personen_km_legs == 1){
  personen_km.legs <- function (x){
    x %>%
      group_by(mode) %>%
      summarise(pers_km = sum(distance)/1000) %>%
      pivot_wider(names_from = mode, values_from = pers_km)
    
  }
  pkm.legs.city <- personen_km.legs(scenario.legs.city)
  pkm.legs.region <- personen_km.legs(scenario.legs.region)
  pkm.legs.network <- personen_km.legs(scenario.legs.table)

  write.csv(pkm.legs.city, file = paste0(outputDirectoryScenario,"/df.pkm.legs.city.csv"))
  write.csv(pkm.legs.region, file = paste0(outputDirectoryScenario,"/df.pkm.legs.region.csv"))
  write.csv(pkm.legs.network, file = paste0(outputDirectoryScenario,"/df.pkm.legs.network.csv"))
  
  df.list <- list(network = pkm.legs.network,
                  region = pkm.legs.region,
                  city = pkm.legs.city)
  write.csv(bind_rows(df.list,
                      .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.pkm.legs.csv"))
}
#### #4.1 Average Travel Time - trips based #####
print("#### in 4.1 ####")
if (x_average_traveled_distance_trips == 1){
  
  avg_time.trips.by.mode <- function(x){
    x %>%
      group_by(main_mode) %>%
      summarise_at(vars(trav_time), list(name=mean)) %>%
      pivot_wider(names_from = main_mode, values_from = name)
  }
  #calculation
  avg_time.trips.network <- avg_time.trips.by.mode(scenario.trips.table)
  avg_time.trips.region <- avg_time.trips.by.mode(scenario.trips.region)
  avg_time.trips.city <- avg_time.trips.by.mode(scenario.trips.city)
  #write table
  write.csv(avg_time.trips.network, file = paste0(outputDirectoryScenario,"/df.avg_time.trips.network.csv"))
  write.csv(avg_time.trips.region, file = paste0(outputDirectoryScenario,"/df.avg_time.trips.region.csv"))
  write.csv(avg_time.trips.city, file = paste0(outputDirectoryScenario,"/df.avg_time.trips.city.csv"))

  df.list <- list(network = avg_time.trips.network,
                  region = avg_time.trips.region,
                  city = avg_time.trips.city)
  write.csv(bind_rows(df.list,
                      .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.avg_time.trips.csv"))
  }

#### #4.2 Reisezeit - trips based ####
print("#### in 4.2 ####")
if (x_personen_h_trips == 1){
  person.hours.trips <- function (x){
    x %>% 
      filter(main_mode!="freight") %>%
      group_by(main_mode) %>%
      summarise(personen_stunden_trips = sum(trav_time)) %>%
      pivot_wider(names_from = main_mode, values_from = personen_stunden_trips)
  }
  ph.trips.city <- person.hours.trips(scenario.trips.city)
  ph.trips.region <- person.hours.trips(scenario.trips.region)
  ph.trips.network <- person.hours.trips(scenario.trips.table)

  write.csv(ph.trips.city, file = paste0(outputDirectoryScenario,"/df.ph.trips.city.csv"))
  write.csv(ph.trips.region, file = paste0(outputDirectoryScenario,"/df.ph.trips.region.csv"))
  write.csv(ph.trips.network, file = paste0(outputDirectoryScenario,"/df.ph.trips.network.csv"))
  
  df.list <- list(network = ph.trips.network,
                  region = ph.trips.region,
                  city = ph.trips.city)
  write.csv(bind_rows(df.list,
                      .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.ph.trips.csv"))
}

#### #4.3 Average Travel Time - legs based #####
print("#### in 4.3 ####")

if (x_average_traveled_distance_legs == 1){
  
  avg_time.legs.by.mode <- function(x){
    x %>%
      group_by(mode) %>%
      summarise_at(vars(trav_time), list(name=mean)) %>%
      pivot_wider(names_from = mode, values_from = name)
  }

  #calculation
  avg_time.legs.network <- avg_time.legs.by.mode(scenario.legs.table)
  avg_time.legs.region <- avg_time.legs.by.mode(scenario.legs.region)
  avg_time.legs.city <- avg_time.legs.by.mode(scenario.legs.city)
  #write table
  write.csv(avg_time.legs.network, file = paste0(outputDirectoryScenario,"/df.avg_time.legs.network.csv"))
  write.csv(avg_time.legs.region, file = paste0(outputDirectoryScenario,"/df.avg_time.legs.region.csv"))
  write.csv(avg_time.legs.city, file = paste0(outputDirectoryScenario,"/df.avg_time.legs.city.csv"))

  df.list <- list(network = avg_time.legs.network,
                  region = avg_time.legs.region,
                  city = avg_time.legs.city)
  write.csv(bind_rows(df.list,
                      .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.avg_time.legs.csv"))
  }

#### #4.4 Reisezeit - legs based ####
print("#### in 4.4 ####")
if (x_personen_h_legs == 1){
  person_hours.legs <- function (x){
    x %>%
      group_by(mode) %>%
      summarise(person_hours_legs = (sum(trav_time))) %>%
      pivot_wider(names_from = mode, values_from = person_hours_legs)

  }
  ph.legs.city <- person_hours.legs(scenario.legs.city)
  ph.legs.region <- person_hours.legs(scenario.legs.region)
  ph.legs.network <- person_hours.legs(scenario.legs.table)

  write.csv(ph.legs.city, file = paste0(outputDirectoryScenario,"/df.ph.legs.city.csv"))
  write.csv(ph.legs.region, file = paste0(outputDirectoryScenario,"/df.ph.legs.region.csv"))
  write.csv(ph.legs.network, file = paste0(outputDirectoryScenario,"/df.ph.legs.network.csv"))
  
  df.list <- list(network = ph.legs.network,
                  region = ph.legs.region,
                  city = ph.legs.city)
  write.csv(bind_rows(df.list,
                      .id = "id"),
            file = paste0(outputDirectoryScenario, "/df.ph.legs.csv"))
}

#### #4.5 Travel Time Heatmap - trips based ####
if (x_heatmap_time_trips == 1){

  heatmap.trav_time.trips.by.mode <- function(x){

    x %>%
      mutate(trav_time_s = as.integer(x$trav_time, scientific = FALSE) ) %>%
      mutate(time_bin = cut(trav_time_s, breaks = time_breaks, labels = time_labels)) %>%
      group_by(main_mode, time_bin) %>%
      summarise(freq = n()) %>%
      pivot_wider(names_from = main_mode, values_from = freq)
  }


  heatmap.trav_time.trips.city <- heatmap.trav_time.trips.by.mode(scenario.trips.city)

  write.csv(heatmap.trav_time.trips.city,file = paste0(outputDirectoryScenario,"/heatmap.trav_distance.trips.city.csv"))

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
    pivot_wider(names_from = main_mode, values_from = avg_trav_distance)
}

avg_trav_time <- function(x){
  x %>%
    select(main_mode, trav_time) %>%
    group_by(main_mode) %>%  
    summarise(avgTime = mean(trav_time))%>%
    mutate(avgTime = round(as.numeric(avgTime)/60)) %>%
    pivot_wider(names_from = main_mode, values_from = avgTime)
}  

avg_trav_dist.city <- avg_trav_distance(scenario.trips.city)
avg_trav_dist.region <- avg_trav_distance(scenario.trips.region)
avg_trav_dist.network <-avg_trav_distance(scenario.trips.table)

avg_trav_time.city <- avg_trav_time(scenario.trips.city)
avg_trav_time.region <- avg_trav_time(scenario.trips.region)
avg_trav_time.network <- avg_trav_time(scenario.trips.table)

avg_trav_speed.city = round(avg_trav_dist.city/avg_trav_time.city*3.6, digits =   3) #km/h
avg_trav_speed.region = avg_trav_dist.region/avg_trav_time.region*3.6 #km/h
avg_trav_speed.network = avg_trav_dist.network/avg_trav_time.network*3.6

#write tables
write.csv(avg_trav_speed.network, file = paste0(outputDirectoryScenario,"/df.avg_trav_speed.trips.network.csv"))
write.csv(avg_trav_speed.region, file = paste0(outputDirectoryScenario,"/df.avg_trav_speed.trips.region.csv"))
write.csv(avg_trav_speed.city, file = paste0(outputDirectoryScenario,"/df.avg_trav_speed.trips.city.csv"))

df.list <- list(network = avg_trav_speed.network,
                region = avg_trav_speed.region,
                city = avg_trav_speed.city)
write.csv(bind_rows(df.list,
                    .id = "id"),
          file = paste0(outputDirectoryScenario, "/df.avg_trav_speed.csv"))
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
avg_beeline_dist.city <- avg_beeline_distance(scenario.trips.city)
avg_beeline_dist.region <- avg_beeline_distance(scenario.trips.region)
avg_beeline_dist.network <- avg_beeline_distance(scenario.trips.table)
avg_trav_time.city <- avg_trav_time(scenario.trips.city)
avg_trav_time.region <- avg_trav_time(scenario.trips.region)
avg_trav_time.network <- avg_trav_time(scenario.trips.table)
# average beeline speed
avg_beeline_speed.city = avg_beeline_dist.city/avg_trav_time.city*3.6 #km/h
avg_beeline_speed.region = avg_beeline_dist.region/avg_trav_time.region*3.6 #km/h
avg_beeline_speed.network = avg_beeline_dist.network/avg_trav_time.network*3.6
#write tables
write.csv(avg_beeline_speed.network, file = paste0(outputDirectoryScenario,"/df.avg_bee_speed_trips_network.csv"))
write.csv(avg_beeline_speed.region, file = paste0(outputDirectoryScenario,"/df.avg_bee_speed_trips_region.csv"))
write.csv(avg_beeline_speed.city, file = paste0(outputDirectoryScenario,"/df.avg_bee_speed_trips_city.csv"))

df.list <- list(network = avg_beeline_speed.network,
                region = avg_beeline_speed.region,
                city = avg_beeline_speed.city)
write.csv(bind_rows(df.list,
                    .id = "id"),
          file = paste0(outputDirectoryScenario, "/df.avg_bee_speed.csv"))
} 

#### #6.1 Emissions ####
print("#### in 6.1 ####")
if (x_emissions == 1){
}
#### #7.1 Traffic ####
print("#### in 7.1 ####")
if (x_traffic == 1){
}

#### #8.1 Equity / Winner-Loser Analysis ####
if (x_winner_loser == 1){
  
  # Data Prep
  persons_joined <- join_base_and_policy_persons(base.persons, scenario.persons, city.shape)
  
  persons_joined_sf <- transform_persons_sf(persons_joined, filter_shp = city.shape, first_act_type_filter = "home") %>% 
    mutate(score_diff = executed_score_policy - executed_score_base,
           score_pct_change = score_diff/executed_score_base * 100) %>%
    drop_na(score_pct_change)
  
  # 8.1.1. SCORE
  # a) Score Plot - Base vs. Policy Case

  persons_joined_sf %>%
    st_drop_geometry() %>%
    select(executed_score_base,executed_score_policy) %>%
    pivot_longer(starts_with("executed_score"), names_to = "scenario", values_to = "score") %>%
    transmute(scenario = scenario, score = as.factor(round(score))) %>%
    group_by(scenario, score) %>%
    summarise(count = n()) %>%
    ungroup() %>%
    pivot_wider(names_from = scenario, values_from = count, values_fill = NA) %>%
    arrange(score) %>%
    write.csv(file = paste0(outputDirectoryScenario,"/chart.equity.score_distrib.csv"), quote = FALSE, row.names = FALSE)
  
  # b) Score Summary Stats Table
  
  pct_change_mean <-  (mean(persons_joined_sf$executed_score_policy) - mean(persons_joined_sf$executed_score_base)) / mean(persons_joined_sf$executed_score_base) * 100
  pct_change_median <-  (median(persons_joined_sf$executed_score_policy) - median(persons_joined_sf$executed_score_base)) / median(persons_joined_sf$executed_score_base) * 100

  
  score_stats_table <- data.frame(
    scenario = c("base", "policy"),
    mean_score = c(mean(persons_joined_sf$executed_score_base),mean(persons_joined_sf$executed_score_policy)),
    pct_change_mean = c(NA,pct_change_mean),
    median_score = c(median(persons_joined_sf$executed_score_base),median(persons_joined_sf$executed_score_policy)),
    pct_change_median = c(NA,pct_change_median),
    sd_score = c(sd(persons_joined_sf$executed_score_base),sd(persons_joined_sf$executed_score_policy)))

  score_stats_table$mean_score %>% round(3) %>% print()

  write.csv(score_stats_table,file = paste0(outputDirectoryScenario,"/df.equity.score_stats.csv"), quote = FALSE, row.names = FALSE)


  # c) Score Maps

  joined_hex <- persons_attributes_on_hex_grid(persons_joined_sf, city.shape, n = 50) %>%
    dplyr::rename(score_base = executed_score_base, score_policy = executed_score_policy)

  joined_centroids <- joined_hex %>% st_centroid()

  st_write(joined_hex, paste0(outputDirectoryScenario, "/shp.equity.hex.shp"), append = FALSE)
  st_write(joined_centroids, paste0(outputDirectoryScenario, "/shp.equity.centroid.shp"), append = FALSE)

  # 8.1.2. MAIN MODE
  main_modes <- unique(base.trips.city$main_mode)
  mode_user_cnt <- c()
  mode_score_pct_change_mean <- c(length(main_modes))
  mode_score_pct_change_median <- c(length(main_modes))
  mode_score_pct_change_sd <- c(length(main_modes))

  for(i in seq(length(main_modes))){

    user_ids <- base.trips.city %>%
      filter(main_mode == main_modes[i]) %>%
      pull(person) %>%
      unique()

    users_score_pct_changes <- persons_joined_sf %>%
      filter(person %in% user_ids) %>%
      pull(score_pct_change)

    mode_user_cnt[i] <- length(user_ids)

    mode_score_pct_change_mean[i] <- users_score_pct_changes %>%
      mean(na.rm = TRUE) %>%
      round(3)

    mode_score_pct_change_median[i] <- users_score_pct_changes %>%
      median(na.rm = TRUE) %>%
      round(3)

    mode_score_pct_change_sd[i] <- users_score_pct_changes %>%
      sd(na.rm = TRUE) %>%
      round(3)

  }

  mode_user_score_diff <- data.frame("Main_Mode" = main_modes,
                                     "Count" = mode_user_cnt,
                                     "Score_Pct_Change_Mean" = mode_score_pct_change_mean,
                                     "Score_Pct_Change_Median" = mode_score_pct_change_median,
                                     "Score_Pct_Change_sd" = mode_score_pct_change_sd)

  mode_user_score_diff
  write.csv(mode_user_score_diff, file = paste0(outputDirectoryScenario,"/df.equity.mode.csv"), quote = FALSE, row.names = FALSE)

  # 8.1.3. SEX
  sex_diff <- persons_joined_sf %>%
    st_drop_geometry() %>%
    group_by(sex) %>%
    summarise(Count = n(),
              "Mean_Percent_Change_Score" = round(mean(score_pct_change),3),
              "Median_Percent_Change_Score" = round(median(score_pct_change),3),
              "sd" = sd(score_pct_change))

  write.csv(sex_diff, file = paste0(outputDirectoryScenario,"/df.equity.sex.csv"), quote = FALSE, row.names = FALSE)

  # 8.1.4. INCOME GROUP
  income_diff <- persons_joined_sf %>%
    st_drop_geometry() %>%
    dplyr::rename("Income_Group" = "MiD.hheink_gr2") %>%
    group_by(Income_Group) %>%
    summarise(Count = n(),
              "Mean_Percent_Change_Score" = round(mean(score_pct_change),3),
              "Median_Percent_Change_Score" = round(median(score_pct_change),3),
              "sd" = sd(score_pct_change))

  income_diff

  write.csv(income_diff, file = paste0(outputDirectoryScenario,"/df.equity.income.csv"), quote = FALSE, row.names = FALSE)


  # 8.1.5. HH Size
  householdsize_diff <- persons_joined_sf %>%
    st_drop_geometry() %>%
    dplyr::rename("Household_Size" = "MiD.hhgr_gr") %>%
    group_by(Household_Size) %>%
    summarise(Count = n(),
              "Mean_Percent_Change_Score" = round(mean(score_pct_change),3),
              "Median_Percent_Change_Score" = round(median(score_pct_change),3),
              "sd" = sd(score_pct_change))
  householdsize_diff

  write.csv(householdsize_diff, file = paste0(outputDirectoryScenario,"/df.equity.hh_size.csv"), quote = FALSE, row.names = FALSE)


  # 8.1.6 PT ABO AVAILABLE
  pt_subscription_diff <- persons_joined_sf %>%
    st_drop_geometry() %>%
    group_by(sim_ptAbo) %>%
    summarise(Count = n(),
              "Mean_Percent_Change_Score" = round(mean(score_pct_change),3),
              "Median_Percent_Change_Score" = round(median(score_pct_change),3),
              "sd" = sd(score_pct_change))

  pt_subscription_diff

  write.csv(pt_subscription_diff, file = paste0(outputDirectoryScenario,"/df.equity.pt_abo.csv"), quote = FALSE, row.names = FALSE)

  # 8.1.7 CAR AVAILABLE
  car_avail_diff <- persons_joined_sf %>%
    st_drop_geometry() %>%
    group_by(carAvail) %>%
    summarise(Count = n(),
              "Mean_Percent_Change_Score" = round(mean(score_pct_change),3),
              "Median_Percent_Change_Score" = round(median(score_pct_change),3),
              "sd" = sd(score_pct_change))

  car_avail_diff

  write.csv(car_avail_diff, file = paste0(outputDirectoryScenario,"/df.equity.car_avail.csv"), quote = FALSE, row.names = FALSE)

}




