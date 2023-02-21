#Masterscript
################################################################################ Libraries ####
library(gridExtra)
library(tidyr)
library(tidyverse)
library(lubridate)
library(viridis)
library(ggsci)
library(sf)
library(dplyr)
library(ggplot2)
library(matsim)
library(purrr)
library(networkD3)
library(alluvial)
library(ggalluvial)
library(stringr)
print("#### Libraries geladen! ####")
################################################################################ INPUT ####
workingDirectory <- "/Users/mkreuschnervsp/Desktop/VSP_projects/02_NaMAV/MATSim/speed_scenario"
setwd(workingDirectory)
################################################################################ fill out paths
runID = "Leipzig_speed_20relativ_25pct"                                        
network <- "base/leipzig-25pct-base.output_network.xml.gz"
base_persons <- "base/leipzig-25pct-base.output_persons.csv.gz"
base_trips_path <- "base/leipzig-25pct-base.output_trips.csv.gz"
base_legs_path <- "base/leipzig-25pct-base.output_legs.csv.gz"
#base_traffic <- "traffic_output_base.csv"
#base_emission <- ""
scenario_persons <- "20relativ/leipzig-drt-20relativ-25pct.output_persons.csv.gz"
scenario_trips_path <- "20relativ/leipzig-drt-20relativ-25pct.output_trips.csv.gz"
scenario_legs_path <- "20relativ/leipzig-drt-20relativ-25pct.output_legs.csv.gz"
#scenario_traffic <- "..."
#scenario_emission <- "..."

print("#### Inputspath definiert! ####")
################################################################################ OUTPUT ####

outputDirectoryGeneral <-  paste(workingDirectory, "/output_general", sep = "") # the plots are going to be saved here
if(!file.exists(outputDirectoryGeneral)){
  print("creating analysis sub-directory")
  dir.create(outputDirectoryGeneral)  
}

outputDirectoryScenario <-  paste(workingDirectory, "/output_scenario", sep = "") # the plots are going to be saved here
if(!file.exists(outputDirectoryScenario)){
  print("creating analysis sub-directory")
  dir.create(outputDirectoryScenario)  
}

print("#### Output folder geladen! ####")
################################################################################ ANALYSIS ####

x_average_traveled_distance_trips =  1
x_average_traveled_distance_legs =   0

x_average_time_trips =      1
x_average_time_legs =       0

x_average_traveled_speed_trips =    1
x_average_beeline_speed_trips =     1

x_personen_km_trips =       0
x_personen_km_legs =        1

x_personen_h_trips =        0
x_personen_h_legs =         1

x_ms_trips_count =          1
x_ms_trips_distance =       1

x_ms_legs_count =           0
x_ms_legs_distance =        0

#x_distance_distribution_trips = 1
#x_distance_distribution_legs =  0
  
x_sankey_diagram = 1

#x_emissions = 0

#x_traffic = 0

x_winner_loser = 0

print("#### Auswahl getroffen! ####")
################################################################################ SOURCE ####

source("/Users/mkreuschnervsp/Desktop/R_Studio/masteranalyse.R")

print("#### Masterscript fertig! ####")










