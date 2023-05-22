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
library(data.table)
library(chron)
library(xml2)
library(XML)
print("#### Libraries geladen! ####")
################################################################################ CASES #### please put (1=yes/0=no) for analyses 
scenarios <- list(
  "base-case"
  #,
  #,"carfree-area-95"
  #,"carfree-area-99"
  #,"drt-outskirts"
  #,"drt-whole-city"
  #,"slow-speed-absolute"
  #,"slow-speed-relative"
  #,"combined_scenarioA"
)

scenario = "carfree-area-90"
################################################################################ INPUT ####

for (scenario in scenarios){
  
  publicSVN = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/"
  #"/Users/mkreuschnervsp/Desktop/git/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/"
  #local = /Users/mkreuschnervsp/Desktop/VSP_projects/02_NaMAV
  
  runID = paste0(scenario, "/")                                        
  network <- paste(publicSVN,"base-case/leipzig-25pct-base.output_network.xml.gz")
  CRS <- 25832
  
  scenario_run_path <- paste0(publicSVN,runID)

  #comaprison path nur für Sankey und Winner/Loser Analysis (normalerweise base case)
  base_run_path <- "D:/VSP_Berlin/Leipzig/namav/base-case/"
  "/Users/mkreuschnervsp/Desktop/git/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/base-case/"
  

  region_shp_path <- "C:/Users/Simon/Documents/shared-svn/projects/NaMAV/data/shapefiles/leipzig_region/Leipzig_puffer.shp"
  city_shp_path <- "C:/Users/Simon/Documents/shared-svn/projects/NaMAV/data/shapefiles/leipzig_stadt/Leipzig_stadt.shp"
  area_shp_path <- "C:/Users/Simon/Documents/shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_large/Zonen90_update.shp"
  #other carfree area shapefiles here
  #area_shp_path <- "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_medeium/Zonen95_update.shp"
  #area_shp_path <- "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_small/Zonen99_update.shp"
  
  print("#### Inputpaths definiert! ####")
  ################################################################################ OUTPUT ####
  
  #/Users/mkreuschnervsp/Desktop/git/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/",runID,"/analysis/analysis-R
  outputDirectoryScenario <-  paste0(scenario_run_path, "analysis/analysis-R") # the plots are going to be saved here

  if(!file.exists(paste0(scenario_run_path,"analysis"))) {
    print("creating general analysis sub-directory")
    dir.create(paste0(scenario_run_path,"analysis"))
  }
  if(!file.exists(outputDirectoryScenario)){
    print("creating analysis sub-directory")
    dir.create(outputDirectoryScenario)  
  }
  
  print("#### Output folder geladen! ####")
  ################################################################################ ANALYSIS ####
  # PLEASE put (1=yes/0=no) for certain analysis 
  
  #### #1.1 Modal Split COUNTS - trips based
  x_ms_trips_count =          1
  #### #1.2 Modal Split DISTANCE - trips based
  x_ms_trips_distance =       1
  #### #1.3 Modal Split COUNTS- legs based
  x_ms_legs_count =           1
  #### #1.4 Modal Split DISTANCE - legs based  
  x_ms_legs_distance =        1
  
  #### #2.1 Modal Shift - trips based
  x_sankey_diagram = 0
  
  #### #3.1 Distances TRAVELED - trips based
  x_average_traveled_distance_trips =   1
  #### #3.2 Distances EUCLIDEAN - trips based
  x_average_euclidean_distance_trips =  1
  #### #3.3 Heatmap Distances traveled - trips based
  x_heatmap_distance_trips = 1
  #### #3.3 PKM - trips based  
  x_personen_km_trips =                 1
  #### #3.4 Distances TRAVELED - legs based
  x_average_traveled_distance_legs =    1
  #### #3.5 Distances EUCLIDEAN - legs based  
  x_average_euclidean_distance_legs =   0
  #### #3.6 PKM - legs based
  x_personen_km_legs =                  1
  
  #### #4.1 Time Traveled - trips based
  x_average_time_trips =      1
  #### #4.2 Time Traveled - legs based  
  x_average_time_legs =       1
  #### #4.3 ph - trips based    
  x_personen_h_trips =        1
  #### #4.4 ph - legs based 
  x_personen_h_legs =         1
  #### #4.5 Time Traveled Heatmap - trips based
  x_heatmap_time_trips =      1
  
  #### #5.1 Speed TRAVELED - trips based
  x_average_traveled_speed_trips =    1
  #### #5.2 Speed BEELINE - trips based
  x_average_beeline_speed_trips =     1
  
  #### #6.1 Traffic Volumes  
  x_traffic = 0
  
  #### #7.1 Emissions Analysis  
  x_emissions = 0  
  
  #### #8.1 Winner/Loser Analysis
  x_winner_loser = 0
  
  #### #9.1 DRT supply
  x_drt_supply = 1
  
  #### #9.2 DRT demand
  x_drt_demand = 1
  
  #### #9.3 DRT performance
  x_drt_performance = 1
  
  #### #9.4 DRT volumes
  x_drt_volumes = 1
  
  #### #9.5 DRT trip purposes 
  x_drt_trip_purposes = 1
  
  
  
  #x_distance_distribution_trips = 1
  #x_distance_distribution_legs =  0
  
  
  
  
  
  print("#### Auswahl getroffen! ####")
  ################################################################################ SOURCE ####

  source("https://raw.githubusercontent.com/matsim-scenarios/matsim-leipzig/masterscript_r/src/main/R/masteranalyse.R")

  if (x_drt_supply || x_drt_demand || x_drt_performance || x_drt_volumes || x_drt_trip_purposes == 1){
    source("https://raw.githubusercontent.com/matsim-scenarios/matsim-leipzig/masterscript_r/src/main/R/master_drt.R")
  }
  #("/Users/mkreuschnervsp/Desktop/R_Studio/mastersolver.R")
  
  print("#### Masterscript fertig! ####")
}
