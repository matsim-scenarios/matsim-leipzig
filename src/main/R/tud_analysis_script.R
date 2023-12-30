##### TUD_analysis_script ####
print("TUD file is read")
## TUD Analysis list

x_population_seg_filter= 1
x_modal_split_trips_main_mode = 1
x_modal_split_legs_mode = 1
x_modal_split_trips_distance = 1
x_modal_split_legs_distance =1
x_trips_number_barchart = 1
x_trips_number_by_mode_and_distance_barchart = 1
x_trips_number_by_distance_barchart = 1
x_modal_shift = 1
x_shifted_trips_average_distance_bar_chart = 1
x_average_and_total_travel_distance_by_mode_barchart = 1
x_average_and_total_travel_distance_by_mode_leg_based_barchart = 1
x_average_and_total_distance_by_mode_just_main_leg_barchart = 1
x_average_walking_distance_by_mode_barchart = 1 
x_walking_distance_distribution_binchart = 1
x_walking_distance_distribution_linechart = 1
x_average_travel_time_by_mode_trips_based_barchart= 1
x_average_travel_time_by_mode_legs_based_barchart= 1
x_average_speed_by_mode_trip_based_barchart= 1
x_average_speed_by_mode_leg_based_barchart= 1
x_emissions_barchart = 1
X_winner_loser_analysis = 0 # Note: A more extensive analysis is performed by TUB.

##
plot_creation = 1

## base data reading and filtering

# trips reading and filtering
base.trips.table <- readTripsTable(pathToMATSimOutputDirectory = base.run.path)

base.trips.region <- filterByRegion(base.trips.table,region.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
base.trips.city <- filterByRegion(base.trips.table,city.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
base.trips.carfree.area <- filterByRegion(base.trips.table, carfree.area.shape, crs=CRS, start.inshape = TRUE, end.inshape = TRUE)

# To, from , within large car free area trips filtering (base case)
base.trips.T.carfree.area <- filterByRegion(base.trips.region,carfree.area.shape,crs=25832,start.inshape = FALSE,end.inshape = TRUE)
base.trips.F.carfree.area <- filterByRegion(base.trips.region,carfree.area.shape,crs=25832,start.inshape = TRUE,end.inshape = FALSE)
base.trips.TFW.carfree.area <- rbind(base.trips.T.carfree.area, base.trips.F.carfree.area ,base.trips.carfree.area) #TFW stand for to, from ,within

# To, from, within large car free area trips filtering (scenario case)
scenario.trips.T.carfree.area <- filterByRegion(scenario.trips.region,carfree.area.shape,crs=25832,start.inshape = FALSE,end.inshape = TRUE)
scenario.trips.F.carfree.area <- filterByRegion(scenario.trips.region,carfree.area.shape,crs=25832,start.inshape = TRUE,end.inshape = FALSE)
scenario.trips.TFW.carfree.area <- rbind(scenario.trips.T.carfree.area, scenario.trips.F.carfree.area ,scenario.trips.carfree.area)

# legs reading and filtering
base.legs.table <- read_delim(paste0(base.run.path,"/",list.files(path = base.run.path, pattern = "output_legs")), delim= ";")#, n_max = 3000)

base.legs.region <- filterByRegion(base.legs.table,region.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
base.legs.city <- filterByRegion(base.legs.table,city.shape,crs=CRS,start.inshape = TRUE,end.inshape = TRUE)
base.legs.carfree.area <- filterByRegion(base.legs.table, carfree.area.shape, crs=CRS, start.inshape = TRUE, end.inshape = TRUE)

# emission reading
emission_base  <- read_delim(paste0(base.run.path,"/",list.files(path = base.run.path, pattern = "emission")), delim= ";")
emission_scenario <- read_delim(paste0(scenario.run.path,"/",list.files(path = scenario.run.path, pattern = "emission")), delim= ";")

## List of scenarios: Define the scenarios to be included in the same plot.
## In view of structure of the functions, region legs list should be used for all analysis; therefore, for the analysis same leg table is used
trips.list.region <- list(base = base.trips.region, policy = scenario.trips.region)
legs.list.region <- list(base = base.legs.region, policy = scenario.legs.region)

trips.list.city <- list(base = base.trips.city, policy = scenario.trips.city)
legs.list.city <- list(base = base.legs.city, policy = scenario.legs.city)

trips.list.carfree.area <- list(base = base.trips.carfree.area, policy = scenario.trips.carfree.area)
legs.list.carfree.area <- list(base = base.legs.carfree.area, policy = scenario.legs.carfree.area) # legs belong to large car free area might have some legs out of the area

trips.list.TFW.carfree.area <- list(base = base.trips.TFW.carfree.area, policy = scenario.trips.TFW.carfree.area)
legs.list.TFW.carfree.area <- list(base = base.legs.region, policy = scenario.legs.region)

print(" TUD data is read and filtered")


## plot functions ##

plot_bar_chart_two_dimensional <- function(analyzed_data, main_title, x_label, y_label, output_filename) {
  
  library(RColorBrewer)
  # Convert data to long format for ggplot, excluding the first two columns
  long_data <- analyzed_data %>%
    pivot_longer(cols = -c(distance_class, scenario), names_to = "Data_Type", values_to = "Value") %>%
    mutate(DistanceClass = factor(distance_class, levels = unique(distance_class)),
           Scenario = factor(scenario, levels = unique(scenario)),
           Interaction = interaction(Scenario, DistanceClass, sep = " - "),
           Fill = interaction(DistanceClass, Data_Type, sep = " - ")) 
  
  number_of_fills <- length(unique(long_data$Fill))
  color_palette <- brewer.pal(min(9, number_of_fills), "Set1")
  if (number_of_fills > 9) {
    color_palette <- c(color_palette, grDevices::rainbow(number_of_fills - 9))
  }
  colors <- setNames(color_palette, levels(long_data$Fill))
  
  gg <- ggplot(long_data, aes(x = Interaction, y = Value, fill = Fill)) +
    geom_bar(stat = "identity", position = position_dodge(width = 0.75)) +
    scale_fill_manual(values = colors) +
    labs(
      title = main_title,
      x = x_label,
      y = y_label,
      fill = "Scenario and Data Type"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(size = 16),
      axis.title.x = element_text(size = 14),
      axis.title.y = element_text(size = 14),
      axis.text.x = element_text(size = 11, angle = 45, hjust = 1),
      axis.text.y = element_text(size = 11),
      legend.position = "bottom",
      legend.title = element_text(size = 12),
      legend.text = element_text(size = 12)
    )
  
  ggsave(filename = paste0(outputDirectoryScenario, "/", output_filename, ".pdf"), plot = gg, device = "pdf", width = 10, height = 7)
  
  return(gg)
}

plot_bar_chart <- function(analyzed_data, main_title, x_label, y_label, mode_col, output_filename) {
  
  if (!names(analyzed_data)[1] %in% c("interval", "distance_class")) {
    names(analyzed_data)[1] <- "main_mode"
    
    # Check if 'main_mode' is a column before renaming values
    if ("main_mode" %in% names(analyzed_data)) {
      analyzed_data$main_mode <- ifelse(analyzed_data$main_mode == "bike", "Bicycle",
                                        ifelse(analyzed_data$main_mode == "car", "Car",
                                               ifelse(analyzed_data$main_mode == "pt", "Public transport",
                                                      ifelse(analyzed_data$main_mode == "ride", "Car as passenger",
                                                             ifelse(analyzed_data$main_mode == "walk", "Walking", analyzed_data$main_mode)))))
    }
  }
  
  long_analyzed_data <- analyzed_data %>% # ggplot works with long_data
    gather(key = "Scenario", value = "Value", -mode_col)
  
  gg <- ggplot(long_analyzed_data, aes(x = get(mode_col), y = Value, fill = Scenario)) + 
    geom_bar(stat = "identity", position = "dodge") +
    labs(
      title = main_title,
      # subtitle = sub_title,# could be passed as argument if needed
      x = x_label,
      y = y_label
    ) +
    scale_fill_brewer(palette = "Set1", name = "Scenario") +  
    theme_minimal() +
    theme(
      legend.position = "bottom",                           
      title = element_text(size = 16),                      
      plot.subtitle = element_text(size = 14),
      axis.title.x = element_text(size = 14),               
      axis.title.y = element_text(size = 14),              
      axis.text.x = element_text(size = 11),               
      axis.text.y = element_text(size = 11),                
      legend.title = element_text(size = 12),               
      legend.text = element_text(size = 12)                 
    )
  
  ggsave(filename = paste0(outputDirectoryScenario, "/", output_filename, ".pdf"), plot = gg, device = "pdf", width = 10, height = 7)
  
  return(gg)
}

plot_pie_chart <- function(df, plot_title ,output_filename) {
  
  labels <- as.character(unlist(df[1, ]))
  values <- as.numeric(unlist(df[3, ]))
  
  pie_data <- data.frame(mode = labels, Value = values) %>%
    filter(mode != "drtNorth" & mode != "drtSoutheast")
  
  if ("mode" %in% names(pie_data)) {
    pie_data$mode <- ifelse(pie_data$mode == "bike", "Bicycle",
                                ifelse(pie_data$mode == "car", "Car",
                                       ifelse(pie_data$mode == "pt", "Public transport",
                                              ifelse(pie_data$mode == "ride", "Car as passenger",
                                                     ifelse(pie_data$mode == "walk", "Walking", pie_data$mode)))))
  }
  
  pie_chart <- ggplot(pie_data, aes(x="", y=Value, fill=mode, label=Value)) +
    geom_bar(stat="identity", width=1) +
    coord_polar("y", start=0) +
    geom_text(aes(label = sprintf("%.1f%%", 100 * Value/sum(Value))), 
              position = position_stack(vjust = 0.5))+
    theme_void() +
    theme(legend.title = element_blank()) +
    labs(fill = "Categories", title = plot_title)
  
  ggsave(filename = paste0(outputDirectoryScenario, "/", output_filename, ".pdf"), plot = pie_chart, device = "pdf", width = 10, height = 7)
  return(pie_chart)
}

plot_sankey <- function(trip_table,output_filename) {
  
  trip_table[[1]] <- as.factor(trip_table[[1]])
  trip_table[[2]] <- as.factor(trip_table[[2]])
  
  trip_table <- data.frame(base = trip_table[[1]], policy = trip_table[[2]], frequncy = trip_table[[3]]) %>%
    filter(base != "drtSoutheast" & base != "drtNorth") %>%
    filter(policy != "drtSoutheast" & policy != "drtNorth")
  
  sankey_chart <- ggplot(trip_table, aes(axis1 = base, axis2 = policy, y = frequncy)) +
    geom_alluvium(aes(fill = policy)) +
    geom_stratum(aes(fill = base)) +
    geom_stratum(aes(fill = policy)) +
    geom_text(stat = "stratum", aes(label = after_stat(stratum)), size = 3) +
    theme_void() + 
    theme(legend.position = "none")+
    labs(y = "Frequency", fill = "Main transportation of trip")
  
  ggsave(filename = paste0(outputDirectoryScenario, "/", output_filename, ".pdf"), plot = sankey_chart, device = "pdf", width = 10, height = 7)
  return(sankey_chart)
}

############### Analysis functions ###################

## Population segment filter 
## Concept for filtering residents: considering agents have home activity at the start or end of trip. Same approach for workers 
population_filtering_function <- function(trips_table, population_type) {
  if (population_type == "resident") {
    filtered_trips <- filter(
      trips_table,
      grepl('home', start_activity_type) | grepl('home', end_activity_type)
    )
  } else if (population_type == "worker") {
    filtered_trips <- filter(
      trips_table,
      grepl('work', start_activity_type) | grepl('work', end_activity_type)
    )
  } else {
    stop("Invalid trip type. Please enter either 'resident' or 'worker'.")
  }
  relevant_trips <- trips_table %>% 
    filter(person %in% filtered_trips$person)
  return(relevant_trips)
}

## modal split by trip count and main mode.
modal_split_trips_main_mode <- function(trips_table, output_filename) {
  
  df <- trips_table %>%
    count(main_mode) %>%
    mutate(percent = 100 * n / sum(n))
  
  df_t <- as.data.frame(t(df))
  colnames(df_t) <- df_t[1, ]
  write.csv(df_t, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation ==1){
    plot_pie_chart(df_t,"Modal split by counts (trips)", output_filename)
  }
}

## modal split by leg count and mode
modal_split_legs_mode <- function(legs_table, output_filename) {
  
  df <- legs_table %>%
    count(mode) %>%
    mutate(percent = 100 * n / sum(n))
  
  df_t <- as.data.frame(t(df))
  colnames(df_t) <- df_t[1, ]
  write.csv(df_t, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation ==1){
    plot_pie_chart(df_t,"Modal split by counts (legs)", output_filename)
  }
}

## modal split by trip distance and main mode
modal_split_trips_distance <- function(trips_table, output_filename ){
  
  df <- trips_table %>%
    group_by(main_mode) %>%
    summarise(distance = sum(traveled_distance)) %>%
    mutate(percent = round(100*distance/sum(distance),2))
  
  df_t <- as.data.frame(t(df))
  colnames(df_t) <- df_t[1, ]
  write.csv(df_t, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation ==1){
    plot_pie_chart(df_t,"Modal split by distance (trips)", output_filename)
  }
}

## modal split by leg distance and mode
modal_split_legs_distance <- function(legs_table, output_filename ){
  
  df <- legs_table %>%
    group_by(mode) %>%
    summarise(distance = sum(distance)) %>%
    mutate(percent = round(100*distance/sum(distance),2))
  
  df_t <- as.data.frame(t(df))
  colnames(df_t) <- df_t[1, ]
  write.csv(df_t, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation ==1){
    plot_pie_chart(df_t,"Modal split by distance (legs)", output_filename)
  }
}

## trips number by mode barchart
trips_number_by_mode_barchart <- function(trips_list, output_filename){
  
  calculation <- function(trips){
    trips %>%
      group_by(main_mode) %>%
      summarise(trips_number = n())%>%
      filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
  }
  
  for (i in seq_along(trips_list)) {
    scenario_name <- names(trips_list)[i]
    trips_number_by_mode <- calculation(trips_list[[i]]) %>%
      select(main_mode, trips_number) %>%
      rename(!!scenario_name := trips_number)
    
    if (i == 1) {
      combined_data <- trips_number_by_mode
    } else {
      combined_data <- left_join(combined_data, trips_number_by_mode, by = "main_mode")
    }
  }
  write.csv(combined_data, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  if(plot_creation == 1){
    plot_bar_chart(combined_data, "Number of trips by mode",  "Main trip mode",  "Number of trips", "main_mode" , output_filename )
    }
}

## trips number by mode and distance class bar chart
trips_number_by_mode_and_distance_barchart <- function(trips_list, output_filename) {
  
  calculation <- function(trips, scenario_name) {
    trips %>%
      mutate(distance_class = case_when(
        traveled_distance <= 1000 ~ "0-1000",
        traveled_distance <= 2000 ~ "1000-2000",
        traveled_distance <= 5000 ~ "2000-5000",
        traveled_distance <= 10000 ~ "5000-10000",
        traveled_distance <= 20000 ~ "10000-20000",
        TRUE ~ "20000 and more"
      )) %>%
      mutate(distance_class = factor(distance_class, levels = c("0-1000", "1000-2000", "2000-5000", "5000-10000", "10000-20000", "20000 and more"))) %>%
      group_by(main_mode, distance_class) %>%
      summarise(trips_number = n(), .groups = 'drop') %>%
      filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast") %>%
      pivot_wider(names_from = main_mode, values_from = trips_number, names_prefix = scenario_name)
  }
  
  
  combined_data <- NULL
  
  for (i in seq_along(trips_list)) {
    scenario_name <- names(trips_list)[i]
    trips_number_by_mode_distance <- calculation(trips_list[[i]], scenario_name)
    
    if (is.null(combined_data)) {
      combined_data <- trips_number_by_mode_distance
    } else {
      combined_data <- full_join(combined_data, trips_number_by_mode_distance, by = "distance_class")
    }
  }
  
  combined_data <- combined_data %>%
    pivot_longer(cols = -distance_class, names_to = c(".value", "scenario"), names_sep = "(?<=base|policy)")
  
  write.csv(combined_data, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart_two_dimensional(combined_data, "Number of trips by mode and distance", "Main trip mode and Distance class", "Number of trips", output_filename)
  }
}

trips_number_by_distance_barchart <- function(trips_list, output_filename) {
  
  calculation <- function(trips, mode) {
    mode_trips <- trips %>% 
    filter(main_mode == mode)  %>% 
    filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
    
    
    mode_trips %>%
      mutate(distance_class = case_when(
        traveled_distance <= 1000 ~ "0-1000",
        traveled_distance <= 2000 ~ "1000-2000",
        traveled_distance <= 5000 ~ "2000-5000",
        traveled_distance <= 10000 ~ "5000-10000",
        traveled_distance <= 20000 ~ "10000-20000",
        TRUE ~ "20000 and more"
      )) %>%
      mutate(distance_class = factor(distance_class, levels = c("0-1000", "1000-2000", "2000-5000", "5000-10000", "10000-20000", "20000 and more"))) %>%
      group_by(distance_class) %>%
      summarise(trips_number = n(), .groups = 'drop') %>%
      mutate(main_mode = mode)
  }
  
  combined_data_list <- list()
  
  for (i in seq_along(trips_list)) {
    scenario_name <- names(trips_list)[i]
    trips_data <- trips_list[[scenario_name]]
    modes <- unique(trips_data$main_mode)
    
    for (mode in modes) {
      mode_data <- calculation(trips_data, mode)
      mode_data$scenario <- scenario_name
      combined_data_list[[length(combined_data_list) + 1]] <- mode_data
    }
  }
  
  combined_data <- do.call(rbind, combined_data_list)
  unique_modes <- unique(combined_data$main_mode)
  
  for (mode in unique_modes) {
    mode_data <- subset(combined_data, main_mode == mode)
    
    mode_data_wide <- mode_data %>%
      select(-main_mode) %>%
      pivot_wider(names_from = scenario, values_from = trips_number, values_fill = list(trips_number = 0))
    
    output_filename_csv <- paste0(outputDirectoryScenario, "/", mode, ".", output_filename, ".csv")
    write.csv(mode_data_wide, file = output_filename_csv, row.names = FALSE, quote = FALSE)
    
    if(plot_creation == 1){
      plot_bar_chart(mode_data_wide, paste0("Number of trips by distance for mode ", mode), "Distance class", "Number of trips", "distance_class", paste0(mode, ".", output_filename))
    }
  }
}

# Note: For the inner_join function, the first argument should be 'base', followed by the 'policy' as the second argument.
modal_shift <- function(trips_list, output_filename){
  sankey_dataframe <- inner_join(trips_list$base , trips_list$policy, by = "trip_id") 
  sankey_dataframe <- sankey_dataframe %>%
    select(trip_id, main_mode.x, main_mode.y) %>%
    group_by(main_mode.x, main_mode.y) %>%
    summarise(Freq = n(), .groups = 'drop')
  write.csv(sankey_dataframe, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1)
  {
    plot_sankey(sankey_dataframe, output_filename)
  }
}

## shifted trips average distance bar chart
# interested_mode is the mode that its trips main mode changed after policy implementation 
shifted_trips_average_distance <- function(trip_lists, interested_mode, output_filename) {
  
  calculation <- function(base_trips, policy_trips, interested_mode) {
    joined_base_policy <- inner_join(base_trips, policy_trips, by="trip_id", suffix = c("_base", "_policy"))
    
    shifted_mode_trips <- joined_base_policy %>%
      filter(main_mode_base != main_mode_policy)
    
    specific_shifts <- shifted_mode_trips %>%
      filter(main_mode_base == interested_mode, main_mode_policy != interested_mode)
    
    average_distances_shifted_trip <- specific_shifts %>%
      group_by(main_mode_policy) %>%
      summarise(average_distance = mean(traveled_distance_policy, na.rm = TRUE)) %>%
      rename(main_mode = main_mode_policy)
    
    return(average_distances_shifted_trip)
  }
  
  base_trips <- trip_lists$base
  
  for (i in seq_along(trip_lists)) {
    if (names(trip_lists)[i] != "base") { # Skip the base, as it's not a "policy" to be compared.
      scenario_name <- names(trip_lists)[i]
      average_distances_shifted <- calculation(base_trips, trip_lists[[i]], interested_mode) %>%
        select(main_mode, average_distance) %>%
        rename(!!scenario_name := average_distance)
      
      if (!exists("combined_data")) {
        combined_data <- average_distances_shifted
      } else {
        combined_data <- left_join(combined_data, average_distances_shifted, by = "main_mode")
      }
    }
  }
  
  combined_data <- combined_data %>%
    filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
  
  write.csv(combined_data, file = paste0(outputDirectoryScenario, "/", "df.",interested_mode,".", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  if(plot_creation == 1){
    plot_bar_chart(combined_data, "Average distance of shifted trips",  "Main trip mode",  "Average distance (m)", "main_mode" , output_filename )
  }
}

## average total distance, average travel distance, and average distance traveled by an individual person
total_and_average_distance_by_mode <- function(trips_list, output_filename_total, output_filename_average, output_filename_person_average){
  
  calculation <- function(trips){
    total_and_average <- trips %>% 
      group_by(main_mode) %>%
      summarize(total_distance = sum(traveled_distance / 1000), 
                average_distance = mean(traveled_distance / 1000)) %>%
      filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
    
    average_per_person <- trips %>%
      group_by(main_mode, person) %>%
      summarize(total_distance_person = sum(traveled_distance / 1000)) %>%
      group_by(main_mode) %>%
      summarize(average_distance_person = mean(total_distance_person)) %>%
      filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
    
    return(list(total_and_average = total_and_average, average_per_person = average_per_person))
  }
  
  total_trip_distance <- tibble()
  average_trip_distance <- tibble()
  average_person_distance <- tibble()
  
  for (i in seq_along(trips_list)){
    scenario_name <- names(trips_list)[i]
    results <- calculation(trips_list[[i]])
    distance_by_mode <- results$total_and_average
    average_distance_per_person_by_mode <- results$average_per_person
    
    total_trip_distance <- if (i == 1) {
      distance_by_mode %>% select(main_mode, total_distance) %>%
        rename(!!scenario_name := total_distance)
    } else {
      left_join(total_trip_distance, 
                distance_by_mode %>% select(main_mode, total_distance) %>%
                  rename(!!scenario_name := total_distance), 
                by = "main_mode")
    }
    
    average_trip_distance <- if (i == 1) {
      distance_by_mode %>% select(main_mode, average_distance) %>%
        rename(!!scenario_name := average_distance)
    } else {
      left_join(average_trip_distance, 
                distance_by_mode %>% select(main_mode, average_distance) %>%
                  rename(!!scenario_name := average_distance), 
                by = "main_mode")
    }
    
    average_person_distance <- if (i == 1) {
      average_distance_per_person_by_mode %>% select(main_mode, average_distance_person) %>%
        rename(!!scenario_name := average_distance_person)
    } else {
      left_join(average_person_distance, 
                average_distance_per_person_by_mode %>% select(main_mode, average_distance_person) %>%
                  rename(!!scenario_name := average_distance_person), 
                by = "main_mode")
    }
  }
  
  # Writing to files
  write.csv(total_trip_distance, file = paste0(outputDirectoryScenario, "/", "df.", output_filename_total, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  write.csv(average_trip_distance, file = paste0(outputDirectoryScenario, "/", "df.", output_filename_average, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  write.csv(average_person_distance, file = paste0(outputDirectoryScenario, "/", "df.", output_filename_person_average, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart(total_trip_distance, "Total distance (based on main mode of trip)",  "Main trip mode",  "Total distance (km)", "main_mode" , output_filename_total )
    plot_bar_chart(average_trip_distance, "Average travel distance (based on main mode of trip)","Main trip mode","Average travel distance (km)", "main_mode" , output_filename_average )
    plot_bar_chart(average_person_distance, "Average distance per person",  "Main trip mode",  "Average distance per person (km)", "main_mode" , output_filename_person_average )
  }
}

## average and total distance bar chart leg based
average_and_total_travel_distance_by_mode_leg_based_barchart <- function(legs_list,output_filename_total,output_filename_average ){
    
  calculation <- function(legs){
    legs %>% 
      group_by(mode) %>%
      summarize(total_distance = sum(distance / 1000), 
                average_distance = mean(distance / 1000)) %>%
      filter(!is.na(mode) & mode != "drtNorth" & mode != "drtSoutheast")
  }
  
  combined_data_total <- tibble()
  combined_data_average <- tibble()
  
  for (i in seq_along(legs_list)){
    scenario_name <- names(legs_list)[i]
    distance_by_mode <- calculation(legs_list[[i]])
    
    combined_data_total <- if (i == 1) {
      distance_by_mode %>% select(mode, total_distance) %>%
        rename(!!scenario_name := total_distance)
    } else {
      left_join(combined_data_total, 
                distance_by_mode %>% select(mode, total_distance) %>%
                  rename(!!scenario_name := total_distance), 
                by = "mode")
    }
    
    combined_data_average <- if (i == 1) {
      distance_by_mode %>% select(mode, average_distance) %>%
        rename(!!scenario_name := average_distance)
    } else {
      left_join(combined_data_average, 
                distance_by_mode %>% select(mode, average_distance) %>%
                  rename(!!scenario_name := average_distance), 
                by = "mode")
    }
  }
  
  write.csv(combined_data_total, file = paste0(outputDirectoryScenario, "/", "df.", output_filename_total, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  write.csv(combined_data_average, file = paste0(outputDirectoryScenario, "/", "df.", output_filename_average, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart(combined_data_total, "Total distance (leg based)",  "Main trip mode",  "Total distance (km)", "main_mode" , output_filename_total )
    plot_bar_chart(combined_data_average, "Average travel distance (leg based)","Main trip mode","Average travel distance (km)", "main_mode" , output_filename_average )
  }
}


## total and average distance by mode just main leg bar chart
total_and_average_distance_by_mode_just_main_leg <- function(trips_list, legs_list, output_filename_total, output_filename_average){
  
  calculation <- function(trips, legs){
    legs_with_main_leg <- legs %>%
      left_join(select(trips, trip_id, main_mode), by = "trip_id") %>%
      filter(mode == main_mode) %>%  # Exclude the leg which has the same mode as the main_mode of the trip
      group_by(main_mode) %>%
      summarize(total_distance = sum(distance / 1000),
                average_distance = mean(distance / 1000)) %>%
      filter(main_mode != "drtNorth" & main_mode != "drtSoutheast")
    
    return(legs_with_main_leg) # Return the final result
  }
  
  combined_data_total <- tibble()
  combined_data_average <- tibble()
  
  for (i in seq_along(trips_list)){
    scenario_name <- names(trips_list)[i]
    distance_by_mode <- calculation(trips_list[[i]], legs_list[[i]])
    
    combined_data_total <- if (i == 1) {
      distance_by_mode %>% select(main_mode, total_distance) %>%
        rename(!!scenario_name := total_distance)
    } else {
      left_join(combined_data_total,
                distance_by_mode %>% select(main_mode, total_distance) %>%
                  rename(!!scenario_name := total_distance),
                by = "main_mode")
    }
    
    combined_data_average <- if (i == 1) {
      distance_by_mode %>% select(main_mode, average_distance) %>%
        rename(!!scenario_name := average_distance)
    } else {
      left_join(combined_data_average,
                distance_by_mode %>% select(main_mode, average_distance) %>%
                  rename(!!scenario_name := average_distance),
                by = "main_mode")
    }
  }
  
  write.csv(combined_data_total, file = paste0(outputDirectoryScenario, "/", "df.", output_filename_total, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  write.csv(combined_data_average, file = paste0(outputDirectoryScenario, "/", "df.", output_filename_average, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart(combined_data_total, "Total distance (main leg of the trip)",  "Main trip mode",  "Total distance (km)", "main_mode" , output_filename_total )
    plot_bar_chart(combined_data_average, "Average travel distance (main leg of the trip)","Main trip mode","Average travel distance (km)", "main_mode" , output_filename_average )
  }
}

# average walking distance by mode bar chart
average_walking_distance_by_mode <- function(trips_list, legs_list, output_filename) {
  
  add_main_mode <- function(legs, trips) {
    legs %>%
      left_join(select(trips, trip_id, main_mode), by = "trip_id") %>%
      filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
  }
  
  calculation <- function(legs) {
    walk_legs <- legs %>% filter(mode == "walk")
    
    each_mode <- walk_legs %>%
      group_by(main_mode) %>%
      summarise(
        total_walk_distance = sum(distance),
        n_trip = n_distinct(trip_id),
        average_walk_distance = total_walk_distance / n_trip,
        .groups = "drop"
      )
    
    all_modes <- walk_legs %>%
      summarise(
        main_mode = "All modes",
        total_walk_distance = sum(distance),
        n_trip = n_distinct(trip_id),
        average_walk_distance = total_walk_distance / n_trip
      )
    rbind(each_mode, all_modes)
  }
  
  for (i in seq_along(trips_list)) {
    scenario_name <- names(trips_list)[i]
    legs.modified <- add_main_mode(legs_list[[scenario_name]], trips_list[[scenario_name]])
    average_walking_distance_each_scenario <- calculation(legs.modified)%>%
      select(main_mode, average_walk_distance) %>%
      rename(!!scenario_name := average_walk_distance)
    
    if (i == 1){
      average_walking_distance_csv_data <- average_walking_distance_each_scenario
    } else {
      average_walking_distance_csv_data <- left_join(average_walking_distance_csv_data, average_walking_distance_each_scenario, by = "main_mode")
    }
  }
  write.csv(average_walking_distance_csv_data, file = paste0(outputDirectoryScenario, "/", "df." ,output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart(average_walking_distance_csv_data, "Average walking distance by main mode",  "Main trip mode",  "Average walking distance(km)", "main_mode" , output_filename )
  }
}

## walking distance distribution by mode bar or line chart
walking_distance_distribution_by_mode <- function(trips_list, legs_list, output_filename_prefix) {
  
  add_main_mode <- function(legs, trips) {
    legs %>%
      left_join(select(trips, trip_id, main_mode), by = "trip_id") %>%
      filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
  }
  
  distribution_calculation <- function(legs, scenario_type) {
    
    walk_legs_summarised <- legs %>%
      filter(mode == "walk") %>%
      group_by(trip_id) %>%
      summarise(total_distance = sum(distance), main_mode = first(main_mode), .groups = "drop")
    
    #The default breaks_seq is suitable for 'car' mode (big change for this type of chart happen in car mode).
    #Adjustments can be made for other modes if needed.
    breaks_seq <- seq(0, 600, by = 50)
    labels_seq <- paste(head(breaks_seq, -1), tail(breaks_seq, -1), sep = "-")
    
    walk_legs_summarised %>%
      mutate(interval = cut(total_distance, breaks = breaks_seq, include.lowest = TRUE, labels = labels_seq,  right = FALSE)) %>%
      group_by(main_mode, interval) %>%
      summarise(count = n(), .groups = "drop") %>%
      arrange(main_mode, interval) %>%
      mutate(scenario_type = scenario_type) # add a scenario_type column
  }
  
  combined_data_list <- list()
  for (i in seq_along(trips_list)) {
    scenario_name <- names(trips_list)[i]
    legs.modified <- add_main_mode(legs_list[[scenario_name]], trips_list[[scenario_name]])
    combined_data_list[[scenario_name]] <- distribution_calculation(legs.modified, scenario_name)
  }
  
  combined_data <- do.call(rbind, combined_data_list)
  unique_modes <- unique(combined_data$main_mode)
  for (mode in unique_modes) {
    mode_data <- subset(combined_data, main_mode == mode)
    
    mode_data_wide <- mode_data %>%
      select(-main_mode) %>%
      spread(key = scenario_type, value = count, fill = 0) 
    
    scenario_names <- unique(mode_data$scenario_type)
    new_colnames <- c("interval", scenario_names)
    colnames(mode_data_wide) <- new_colnames
    
    output_filename <- paste0(outputDirectoryScenario, "/", "df.", output_filename_prefix, ".", mode, ".TUD.csv")
    write.csv(mode_data_wide, file = output_filename, row.names = FALSE, quote = FALSE)
    
    if(plot_creation == 1){
      output_filename_pdf <- paste0(output_filename_prefix,".",mode)
      plot_bar_chart(mode_data_wide, "Number of trips by walking distance interval for car mode",  "Distance class",  "Number of trips", "interval" , output_filename_pdf )
    }
  }
}

## travel time by mode bar chart - trip based
travel_time_by_mode_trip_based_bar_chart <- function(trips_list, output_filename){
  
  calculation <- function(trips){
    trips %>%
      group_by(main_mode) %>%
      summarise(
        total_travel_time = sum(hour(hms(trav_time))*3600 + minute(hms(trav_time)) *60 + second(hms(trav_time))),
        n_trip = n_distinct(trip_id),
        average_travel_time = (total_travel_time/60) / n_trip )%>%
      filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
  }
  
  for (i in seq_along(trips_list)) {
    scenario_name <- names(trips_list)[i]
    travel_time_by_mode <- calculation(trips_list[[i]]) %>%
      select(main_mode, average_travel_time) %>%
      rename(!!scenario_name := average_travel_time)
    
    if (i == 1) {
      combined_data <- travel_time_by_mode
    } else {
      combined_data <- left_join(combined_data, travel_time_by_mode, by = "main_mode")
    }
  }
  write.csv(combined_data, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart(combined_data, "Average travel time (trip based)",  "Main trip mode",  "Average travel time (min)", "main_mode" , output_filename )
  }
}

## travel time by mode bar chart - leg based
travel_time_by_mode_leg_based_bar_chart <- function(legs_list, output_filename){
  
  calculation <- function(legs){
    legs %>%
      group_by(mode) %>%
      summarise(
        total_travel_time = sum(hour(hms(trav_time))*3600 + minute(hms(trav_time)) *60 + second(hms(trav_time))),
        n_trip = n_distinct(trip_id),
        average_travel_time = (total_travel_time/60) / n_trip )%>%
      filter(!is.na(mode) & mode != "drtNorth" & mode != "drtSoutheast")
  }
  
  for (i in seq_along(legs_list)) {
    scenario_name <- names(legs_list)[i]
    travel_time_by_mode <- calculation(legs_list[[i]]) %>%
      select(mode, average_travel_time) %>%
      rename(!!scenario_name := average_travel_time)
    
    if (i == 1) {
      combined_data <- travel_time_by_mode
    } else {
      combined_data <- left_join(combined_data, travel_time_by_mode, by = "mode")
    }
  }
  write.csv(combined_data, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart(combined_data, "Average travel time (leg based)",  "Mode",  "Average travel time (min)", "main_mode" , output_filename )
  }
}

## average speed by mode bar chart
average_speed_by_mode_trip_based_barchart <- function(trips_list, output_filename){
  
  calculation <- function(trips){
    trips %>%
      group_by(main_mode) %>%
      summarise(
        total_travel_distance = sum(traveled_distance),
        total_travel_time = sum(hour(hms(trav_time))*3600 + minute(hms(trav_time)) *60 + second(hms(trav_time))),
        average_speed = total_travel_distance/total_travel_time)%>% # m/s
      filter(!is.na(main_mode) & main_mode != "drtNorth" & main_mode != "drtSoutheast")
  }
  
  for (i in seq_along(trips_list)) {
    scenario_name <- names(trips_list)[i]
    average_speed_by_mode <- calculation(trips_list[[i]]) %>%
      select(main_mode, average_speed) %>%
      rename(!!scenario_name := average_speed)
    
    if (i == 1) {
      combined_data <- average_speed_by_mode
    } else {
      combined_data <- left_join(combined_data, average_speed_by_mode, by = "main_mode")
    }
  }
  write.csv(combined_data, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart(combined_data, "Average speed (trip based)",  "Main trip mode",  "Average speed (m/s)", "main_mode" , output_filename )
  }
}

## average speed by mode trip based bar chart
average_speed_by_mode_leg_based_barchart <- function(legs_list, output_filename){
  
  calculation <- function(trips){
    trips %>%
      group_by(mode) %>%
      summarise(
        total_travel_distance = sum(distance),
        total_travel_time = sum(hour(hms(trav_time))*3600 + minute(hms(trav_time)) *60 + second(hms(trav_time))),
        average_speed = total_travel_distance/total_travel_time)%>% # m/s
      filter(!is.na(mode) & mode != "drtNorth" & mode != "drtSoutheast")
  }
  
  for (i in seq_along(legs_list)) {
    scenario_name <- names(legs_list)[i]
    average_speed_by_mode <- calculation(legs_list[[i]]) %>%
      select(mode, average_speed) %>%
      rename(!!scenario_name := average_speed)
    
    if (i == 1) {
      combined_data <- average_speed_by_mode
    } else {
      combined_data <- left_join(combined_data, average_speed_by_mode, by = "mode")
    }
  }
  write.csv(combined_data, file = paste0(outputDirectoryScenario, "/", "df.", output_filename, ".TUD.csv"), row.names = FALSE, quote = FALSE)
  
  if(plot_creation == 1){
    plot_bar_chart(combined_data, "Average speed (leg based)",  "Mode",  "Average speed (m/s)", "main_mode" , output_filename )
  }
}

## emission bar chart
if (x_emissions_barchart == 1){
  # Load network 
  network_for_emission <- loadNetwork(network)
  links_network <- data.frame(network_for_emission[2])
  links_leipzig <- links_network %>% st_as_sf(coords = c("links.x.from", "links.y.from"), crs = CRS) %>% st_filter(region.shape)
  links_scenario <- links_network %>% st_as_sf(coords = c("links.x.from", "links.y.from"), crs = CRS) %>% st_filter(carfree.area.shape)
  
  # Renaming the column to match the 'Links Id' column in the other data frame
  colnames(links_leipzig)[1] <- "linkId"
  colnames(links_scenario)[1] <- "linkId"
  
  # Finding the corresponding emission information for the links
  links_emission_base <- merge(emission_base, links_scenario, by = 'linkId', all.x = FALSE)
  links_emission_scenario <- merge(emission_scenario, links_scenario, by = 'linkId', all.x = FALSE)
  
  # emission calculation
  emission_calc <- function(emission_type) {
    base_emission <- sum(links_emission_base[[paste0(emission_type, " [g/m]")]]*links_emission_base$links.length)
    scenario_emission <- sum(links_emission_scenario[[paste0(emission_type, " [g/m]")]]*links_emission_scenario$links.length)
    emission_df <- data.frame(emission_type = emission_type, base = base_emission, policy_90 = scenario_emission)
    
    write.csv(emission_df, file = paste0(outputDirectoryScenario, "/", "df." ,emission_type, "_emission_TUD.csv"), row.names = FALSE, quote = FALSE)
    
    if(plot_creation == 1){
      plot_bar_chart(emission_df, paste("emission" ,emission_type),  emission_type,  "Emisison (kg)", "main_mode" , "Emission" )
    }
  }
}


################## Analysis #####################

if(x_population_seg_filter == 1){
  
  residents.base.trips.carfree.area <- population_filtering_function(base.trips.carfree.area,"resident")
  residents.TFW.base.trips.carfree.area <- population_filtering_function(base.trips.TFW.carfree.area,"resident")
  residents.scenario.trips.carfree.area <- population_filtering_function(scenario.trips.carfree.area, "resident")
  residents.TFW.scenario.trips.carfree.area <- population_filtering_function(scenario.trips.TFW.carfree.area, "resident")
  worker.base.trips.carfree.area <- population_filtering_function(base.trips.carfree.area,"worker")
  worker.TFW.base.trips.carfree.area <- population_filtering_function(base.trips.TFW.carfree.area,"worker")
  worker.scenario.tirps.carfree.area <- population_filtering_function(scenario.trips.carfree.area,"worker")
  worker.TFW.scenario.tirps.carfree.area <- population_filtering_function(scenario.trips.TFW.carfree.area,"worker")
  
  trips.list.residents.TFW.carfree.area <- list(base = residents.TFW.base.trips.carfree.area, policy = residents.TFW.scenario.trips.carfree.area)
  trips.list.workers.TFW.carfree.area <- list(base = worker.TFW.base.trips.carfree.area, policy = worker.TFW.scenario.tirps.carfree.area)
  trips.list.residents.carfree.area <- list(base = residents.base.trips.carfree.area, policy = residents.scenario.trips.carfree.area)
  trips.list.workers.carfree.area <- list(base = worker.base.trips.carfree.area, policy = worker.scenario.tirps.carfree.area)
  # there is no difference regarding legs, and one of the legs defined previously can be used
}

if(x_modal_split_trips_main_mode == 1){
  
  modal_split_trips_main_mode(trips.list.region$base, "pie.ms.counts.trips.base.region")
  modal_split_trips_main_mode(trips.list.city$base, "pie.ms.counts.trips.base.city")
  modal_split_trips_main_mode(trips.list.carfree.area$base, "pie.ms.counts.trips.base.carfree.area")
  modal_split_trips_main_mode(trips.list.TFW.carfree.area$base, "pie.ms.counts.trips.base.TFW.carfree.area")
  modal_split_trips_main_mode(trips.list.residents.carfree.area$base, "pie.ms.counts.trips.base.residetns.carfree.area")
  modal_split_trips_main_mode(trips.list.workers.carfree.area$base, "pie.ms.counts.trips.base.workers.carfree.area")
  
  modal_split_trips_main_mode(trips.list.region$policy, "pie.ms.counts.trips.policy.region")
  modal_split_trips_main_mode(trips.list.city$policy, "pie.ms.counts.trips.policy.city")
  modal_split_trips_main_mode(trips.list.carfree.area$policy, "pie.ms.counts.trips.policy.carfree.area")
  modal_split_trips_main_mode(trips.list.TFW.carfree.area$policy, "pie.ms.counts.trips.policy.TFW.carfree.area")
  modal_split_trips_main_mode(trips.list.residents.carfree.area$policy, "pie.ms.counts.trips.policy.residetns.carfree.area")
  modal_split_trips_main_mode(trips.list.workers.carfree.area$policy, "pie.ms.counts.trips.policy.workers.carfree.area")
}

if(x_modal_split_legs_mode == 1 ){
  
  modal_split_legs_mode(legs.list.region$base, "pie.ms.counts.legs.base.region")
  modal_split_legs_mode(legs.list.city$base, "pie.ms.counts.legs.base.city")
  modal_split_legs_mode(legs.list.carfree.area$base, "pie.ms.counts.legs.base.carfree.area")

  modal_split_legs_mode(legs.list.region$policy, "pie.ms.counts.legs.policy.region")
  modal_split_legs_mode(legs.list.city$policy, "pie.ms.counts.legs.policy.city")
  modal_split_legs_mode(legs.list.carfree.area$policy, "pie.ms.counts.legs.policy.carfree.area")
}

if(x_modal_split_trips_distance == 1){
  
  modal_split_trips_distance(trips.list.region$base, "pie.ms.distance.trips.base.region")
  modal_split_trips_distance(trips.list.city$base, "pie.ms.distance.trips.base.city")
  modal_split_trips_distance(trips.list.carfree.area$base, "pie.ms.distance.trips.base.carfree.area")
  modal_split_trips_distance(trips.list.TFW.carfree.area$base, "pie.ms.distance.trips.base.TFW.carfree.area")
  modal_split_trips_distance(trips.list.residents.carfree.area$base, "pie.ms.distance.trips.base.residetns.carfree.area")
  modal_split_trips_distance(trips.list.workers.carfree.area$base, "pie.ms.distance.trips.base.workers.carfree.area")
  
  modal_split_trips_distance(trips.list.region$policy, "pie.ms.distance.trips.policy.region")
  modal_split_trips_distance(trips.list.city$policy, "pie.ms.distance.trips.policy.city")
  modal_split_trips_distance(trips.list.carfree.area$policy, "pie.ms.distance.trips.policy.carfree.area")
  modal_split_trips_distance(trips.list.TFW.carfree.area$policy, "pie.ms.distance.trips.policy.TFW.carfree.area")
  modal_split_trips_distance(trips.list.residents.carfree.area$policy, "pie.ms.distance.trips.policy.residetns.carfree.area")
  modal_split_trips_distance(trips.list.workers.carfree.area$policy, "pie.ms.distance.trips.policy.workers.carfree.area")
}

if(x_modal_split_legs_distance == 1){
  
  modal_split_legs_distance(legs.list.region$base, "pie.ms.distance.legs.base.region")
  modal_split_legs_distance(legs.list.city$base, "pie.ms.distance.legs.base.city")
  modal_split_legs_distance(legs.list.carfree.area$base, "pie.ms.distance.legs.base.carfree.area")
  
  modal_split_legs_distance(legs.list.region$policy, "pie.ms.distance.legs.policy.region")
  modal_split_legs_distance(legs.list.city$policy, "pie.ms.distance.legs.policy.city")
  modal_split_legs_distance(legs.list.carfree.area$policy, "pie.ms.distance.legs.policy.carfree.area")
}

if(x_trips_number_barchart == 1){
  
  trips_number_by_mode_barchart(trips.list.region, "trips.number.by.mode.region")
  trips_number_by_mode_barchart(trips.list.city, "trips.number.by.mode.city")
  trips_number_by_mode_barchart(trips.list.carfree.area, "trips.number.by.mode.carfree.area")
  trips_number_by_mode_barchart(trips.list.TFW.carfree.area,"trips.number.by.mode.TFW.carfree.area")
  trips_number_by_mode_barchart(trips.list.residents.TFW.carfree.area,"trips.number.by.mode.residents.TFW.carfree.area")
  trips_number_by_mode_barchart(trips.list.workers.TFW.carfree.area,"trips.number.by.mode.workers.TFW.carfree.area")
  trips_number_by_mode_barchart(trips.list.residents.carfree.area,"trips.number.by.mode.residents.carfree.area")
  trips_number_by_mode_barchart(trips.list.workers.carfree.area,"trips.number.by.mode.workers.carfree.area")
} 

if(x_trips_number_by_mode_and_distance_barchart == 1){
  
  trips_number_by_mode_and_distance_barchart(trips.list.region, "trips.number.by.mode.and.distance.region")
  trips_number_by_mode_and_distance_barchart(trips.list.city, "trips.number.by.mode.and.distance.city")
  trips_number_by_mode_and_distance_barchart(trips.list.carfree.area, "trips.number.by.mode.and.distance.carfree.area")
  trips_number_by_mode_and_distance_barchart(trips.list.TFW.carfree.area, "trips.number.by.mode.and.distance.TFW.carfree.area")
  trips_number_by_mode_and_distance_barchart(trips.list.residents.carfree.area, "trips.number.by.mode.and.distance.residents.TFW.carfree.area")
  trips_number_by_mode_and_distance_barchart(trips.list.workers.TFW.carfree.area, "trips.number.by.mode.and.distance.workers.TFW.carfree.area")
  trips_number_by_mode_and_distance_barchart(trips.list.residents.carfree.area, "trips.number.by.mode.and.distance.residents.carfree.area")
  trips_number_by_mode_and_distance_barchart(trips.list.workers.carfree.area, "trips.number.by.mode.and.distance.workers.carfree.area")
}

if(x_trips_number_by_distance_barchart == 1){
  
  trips_number_by_distance_barchart(trips.list.region, "trips.number.by.distance.region")
  trips_number_by_distance_barchart(trips.list.city, "trips.number.by.distance.city")
  trips_number_by_distance_barchart(trips.list.carfree.area, "trips.number.by.distance.carfree.area")
  trips_number_by_distance_barchart(trips.list.TFW.carfree.area, "trips.number.by.distance.TFW.carfree.area")
  trips_number_by_distance_barchart(trips.list.residents.carfree.area, "trips.number.by.distance.residents.TFW.carfree.area")
  trips_number_by_distance_barchart(trips.list.workers.TFW.carfree.area, "trips.number.by.distance.workers.TFW.carfree.area")
  trips_number_by_distance_barchart(trips.list.residents.carfree.area, "trips.number.by.distance.residents.carfree.area")
  trips_number_by_distance_barchart(trips.list.workers.carfree.area, "trips.number.by.distance.workers.carfree.area")
}
  
if(x_modal_shift == 1){
  
  modal_shift(trips.list.region,"sankey.region")
  modal_shift(trips.list.city,"sankey.city")
  modal_shift(trips.list.carfree.area,"sankey.carfree.area")
  modal_shift(trips.list.TFW.carfree.area,"sankey.TFW.carfree.area")
  modal_shift(trips.list.residents.TFW.carfree.area,"sankey.residents.TFW.carfree.area")
  modal_shift(trips.list.workers.TFW.carfree.area,"sankey.workers.TFW.carfree.area")
  modal_shift(trips.list.residents.carfree.area,"sankey.residents.carfree.area")
  modal_shift(trips.list.workers.carfree.area,"sankey.workers.carfree.area")
}

if(x_shifted_trips_average_distance_bar_chart == 1){
  
  shifted_trips_average_distance(trips.list.region,"car", "shifted.trips.average.distance.by.mode.region")
  shifted_trips_average_distance(trips.list.city,"car", "shifted.trips.average.distance.by.mode.city")
  shifted_trips_average_distance(trips.list.carfree.area,"car", "shifted.trips.average.distance.by.mode.carfree.area")
  shifted_trips_average_distance(trips.list.TFW.carfree.area, "car", "shifted.trips.average.distance.by.mode.TFW.carfree.area")
  shifted_trips_average_distance(trips.list.residents.TFW.carfree.area, "car", "shifted.trips.average.distance.by.mode.residents.TFW.carfree.area")
  shifted_trips_average_distance(trips.list.workers.TFW.carfree.area, "car", "shifted.trips.average.distance.by.mode.workers.TFW.carfree.area")
  shifted_trips_average_distance(trips.list.residents.carfree.area, "car", "shifted.trips.average.distance.by.mode.residents.carfree.area")
  shifted_trips_average_distance(trips.list.workers.carfree.area, "car", "shifted.trips.average.distance.by.mode.workers.carfree.area")
  ## for all the modes could be written.
}

if (x_average_and_total_travel_distance_by_mode_barchart == 1){
  
  total_and_average_distance_by_mode(trips.list.region, "total.distance.by.mode.region", "average.distance.by.mode.trip.based.region", "average.distance.by.mode.person.based.region")
  total_and_average_distance_by_mode(trips.list.city, "total.distance.by.mode.city", "average.distance.by.mode.trip.based.city","average.distance.by.mode.person.based.city" )
  total_and_average_distance_by_mode(trips.list.carfree.area, "total.distance.by.mode.carfree.area", "average.distance.by.mode.trip.based.carfree.area", "average.distance.by.mode.person.based.carfree.area")
  total_and_average_distance_by_mode(trips.list.TFW.carfree.area, "total.distance.by.mode.TFW.carfree.area", "average.distance.by.mode.trip.based.TFW.carfree.area" , "average.distance.by.mode.person.based.TFW.carfree.area")
  total_and_average_distance_by_mode(trips.list.residents.TFW.carfree.area, "total.distance.by.mode.residents.TFW.carfree.area", "average.distance.by.mode.trip.based.residents.TFW.carfree.area" , "average.distance.by.mode.person.based.residents.TFW.carfree.area")
  total_and_average_distance_by_mode(trips.list.workers.TFW.carfree.area, "total.distance.by.mode.workers.TFW.carfree.area", "average.distance.by.mode.trip.based.workers.TFW.carfree.area" ,"average.distance.by.mode.person.based.workers.TFW.carfree.area")
  total_and_average_distance_by_mode(trips.list.residents.carfree.area, "total.distance.by.mode.residents.carfree.area", "average.distance.by.mode.trip.based.residents.carfree.area" ,"average.distance.by.mode.person.based.residents.carfree.area")
  total_and_average_distance_by_mode(trips.list.workers.carfree.area, "total.distance.by.mode.workers.carfree.area", "average.distance.by.mode.trip.based.workers.carfree.area", "average.distance.by.mode.person.based.workers.carfree.area")
}

if(x_average_and_total_travel_distance_by_mode_leg_based_barchart == 1){
  
  average_and_total_travel_distance_by_mode_leg_based_barchart(legs.list.region,"total.distance.by.mode.leg.based.region", "average.distance.by.mode.leg.based.region")
  average_and_total_travel_distance_by_mode_leg_based_barchart(legs.list.city ,"total.distance.by.mode.leg.based.city", "average.distance.by.mode.leg.based.city")
  average_and_total_travel_distance_by_mode_leg_based_barchart(legs.list.carfree.area ,"total.distance.by.mode.leg.based.carfree.area", "average.distance.by.mode.leg.based.carfree.area")
}

if(x_average_and_total_distance_by_mode_just_main_leg_barchart == 1){
  
  total_and_average_distance_by_mode_just_main_leg(trips.list.region, legs.list.region, "total.distance.by.mode.main.leg.region.csv", "average.distance.by.mode.main.leg.region.csv")
  total_and_average_distance_by_mode_just_main_leg(trips.list.city, legs.list.region, "total.distance.by.mode.main.leg.city.csv", "average.distance.by.mode.main.leg.city.csv")
  total_and_average_distance_by_mode_just_main_leg(trips.list.carfree.area, legs.list.region, "total.distance.by.mode.main.leg.carfree.area.csv", "average.distance.by.mode.main.leg.carfree.area.csv")
  total_and_average_distance_by_mode_just_main_leg(trips.list.TFW.carfree.area, legs.list.region, "total.distance.by.mode.main.leg.TFW.carfree.area.csv", "average.distance.by.mode.main.leg.TFW.carfree.area.csv")
  total_and_average_distance_by_mode_just_main_leg(trips.list.residents.TFW.carfree.area, legs.list.region, "total.distance.by.mode.main.leg.residents.TFW.carfree.area.csv", "average.distance.by.mode.main.leg.residents.TFW.carfree.area.csv")
  total_and_average_distance_by_mode_just_main_leg(trips.list.workers.TFW.carfree.area, legs.list.region, "total.distance.by.mode.main.leg.workers.TFW.carfree.area.csv", "average.distance.by.mode.main.leg.workers.TFW.carfree.area.csv")
  total_and_average_distance_by_mode_just_main_leg(trips.list.residents.carfree.area, legs.list.region, "total.distance.by.mode.main.leg.residents.carfree.area.csv", "average.distance.by.mode.main.leg.residents.carfree.area.csv")
  total_and_average_distance_by_mode_just_main_leg(trips.list.workers.carfree.area, legs.list.region, "total.distance.by.mode.main.leg.workers.carfree.area.csv", "average.distance.by.mode.main.leg.workers.carfree.area.csv")
}

if(x_average_walking_distance_by_mode_barchart == 1 ){
  
  average_walking_distance_by_mode(trips.list.region, legs.list.region, "average.walking.distance.by.mode.region")
  average_walking_distance_by_mode(trips.list.city, legs.list.region, "average.walking.distance.by.mode.city")
  average_walking_distance_by_mode(trips.list.carfree.area, legs.list.region, "average.walking.distance.by.mode.carfree.area")
  average_walking_distance_by_mode(trips.list.TFW.carfree.area, legs.list.region, "average.walking.distance.by.mode.TFW.carfree.area")
  average_walking_distance_by_mode(trips.list.residents.TFW.carfree.area, legs.list.region, "average.walking.distance.by.mode.residents.TFW.carfree.area")
  average_walking_distance_by_mode(trips.list.workers.TFW.carfree.area, legs.list.region, "average.walking.distance.by.mode.workers.TFW.carfree.area")
  average_walking_distance_by_mode(trips.list.residents.carfree.area, legs.list.region, "average.walking.distance.by.mode.residents.carfree.area")
  average_walking_distance_by_mode(trips.list.workers.carfree.area, legs.list.region, "average.walking.distance.by.mode.workers.carfree.area")
}

if (x_walking_distance_distribution_binchart == 1 | x_walking_distance_distribution_linechart == 1) {
  
  walking_distance_distribution_by_mode(trips.list.region, legs.list.region, "walking.distance.distribution.by.mode.region")
  walking_distance_distribution_by_mode(trips.list.city, legs.list.region, "walking.distance.distribution.by.mode.city")
  walking_distance_distribution_by_mode(trips.list.carfree.area, legs.list.region, "walking.distance.distribution.by.mode.carfree.area")
  walking_distance_distribution_by_mode(trips.list.TFW.carfree.area, legs.list.region, "walking.distance.distribution.by.mode.TFW.carfree.area")
  walking_distance_distribution_by_mode(trips.list.residents.TFW.carfree.area, legs.list.region, "walking.distance.distribution.by.mode.residents.TFW.carfree.area")
  walking_distance_distribution_by_mode(trips.list.workers.TFW.carfree.area, legs.list.region, "walking.distance.distribution.by.mode.workers.TFW.carfree.area")
  walking_distance_distribution_by_mode(trips.list.residents.carfree.area, legs.list.region, "walking.distance.distribution.by.mode.residents.carfree.area")
  walking_distance_distribution_by_mode(trips.list.workers.carfree.area, legs.list.region, "walking.distance.distribution.by.mode.workers.carfree.area")
}

if(x_average_travel_time_by_mode_trips_based_barchart== 1){
  
  travel_time_by_mode_trip_based_bar_chart(trips.list.region, "travel.time.by.mode.trip.based.region")
  travel_time_by_mode_trip_based_bar_chart(trips.list.city, "travel.time.by.mode.trip.based.city")
  travel_time_by_mode_trip_based_bar_chart(trips.list.carfree.area, "travel.time.by.mode.carfree.trip.based.area")
  travel_time_by_mode_trip_based_bar_chart(trips.list.TFW.carfree.area, "travel.time.by.mode.TFW.carfree.trip.based.area")
  travel_time_by_mode_trip_based_bar_chart(trips.list.residents.TFW.carfree.area, "travel.time.by.mode.residents.TFW.carfree.trip.based.area")
  travel_time_by_mode_trip_based_bar_chart(trips.list.workers.TFW.carfree.area, "travel.time.by.mode.workers.TFW.carfree.trip.based.area")
  travel_time_by_mode_trip_based_bar_chart(trips.list.residents.carfree.area, "travel.time.by.mode.residents.carfree.trip.based.area")
  travel_time_by_mode_trip_based_bar_chart(trips.list.workers.carfree.area, "travel.time.by.mode.workers.carfree.trip.based.area")
}

if(x_average_travel_time_by_mode_legs_based_barchart== 1){
  
  travel_time_by_mode_leg_based_bar_chart(legs.list.region,"travel.time.by.mode.leg.based.region")
  travel_time_by_mode_leg_based_bar_chart(legs.list.city,"travel.time.by.mode.leg.based.city")
  travel_time_by_mode_leg_based_bar_chart(legs.list.carfree.area,"travel.time.by.mode.leg.based.carfree.area")
}

if(x_average_speed_by_mode_trip_based_barchart== 1){
  
  average_speed_by_mode_trip_based_barchart(trips.list.region, "average.speed.by.mode.trip.based.region")
  average_speed_by_mode_trip_based_barchart(trips.list.city, "average.speed.by.mode.trip.based.city")
  average_speed_by_mode_trip_based_barchart(trips.list.carfree.area, "average.speed.by.mode.trip.based.carfree.area")
  average_speed_by_mode_trip_based_barchart(trips.list.TFW.carfree.area, "average.speed.by.mode.trip.based.TFW.carfree.area")
  average_speed_by_mode_trip_based_barchart(trips.list.residents.TFW.carfree.area, "average.speed.by.mode.trip.based.residents.TFW.carfree.area")
  average_speed_by_mode_trip_based_barchart(trips.list.workers.TFW.carfree.area, "average.speed.by.mode.trip.based.workers.TFW.carfree.area")
  average_speed_by_mode_trip_based_barchart(trips.list.residents.carfree.area, "average.speed.by.mode.trip.based.residents.carfree.area")
  average_speed_by_mode_trip_based_barchart(trips.list.workers.carfree.area, "average.speed.by.mode.trip.based.workers.carfree.area")
}

if(x_average_speed_by_mode_leg_based_barchart== 1){
  
  average_speed_by_mode_leg_based_barchart(legs.list.region,"average.speed.by.mode.leg.based.region")
  average_speed_by_mode_leg_based_barchart(legs.list.city,"average.speed.by.mode.leg.based.city")
  average_speed_by_mode_leg_based_barchart(legs.list.carfree.area,"average.speed.by.mode.leg.based.carfree.area")
}

if (x_emissions_barchart == 1){
  
  emission_calc("CO")
  emission_calc("CO2_TOTAL")
}

print("End of TUD analysis")