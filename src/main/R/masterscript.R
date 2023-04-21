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
print("#### Libraries geladen! ####")
################################################################################ CASES #### please put (1=yes/0=no) for analyses 
#base-case = 0
#carfree-area-90 = 0
#carfree-area-95 = 0
#carfree-area-99 = 0
#drt-outskirts = 0
#drt-whole-city = 0
#slow-speed-absolute = 0
#slow-speed-relative = 0
#combined_scenarioA = 0
################################################################################ INPUT ####
publicSVN = "~/git/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/"
#local = /Users/mkreuschnervsp/Desktop/VSP_projects/02_NaMAV

runID = "carfree-area-90"                                        
network <- paste(publicSVN,"base-case/leipzig-25pct-base.output_network.xml.gz")
CRS <- 25832

scenario_run_path <- paste0(publicSVN,runID)

#base path nur für Sankey und Winner/Loser Analysis
base_run_path <- "~/git/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/base-case/"


region_shp_path <- "/Users/mkreuschnervsp/Desktop/VSP_projects/02_NaMAV/R/shapefiles/Leipzig_puffer.shp"
city_shp_path <- "/Users/mkreuschnervsp/Desktop/VSP_projects/02_NaMAV/R/shapefiles/Leipzig_stadt.shp"
area_shp_path <- "/Users/mkreuschnervsp/Desktop/VSP_projects/02_NaMAV/R/shapefiles/Zonen90_update.shp"


print("#### Inputspath definiert! ####")
################################################################################ OUTPUT ####

outputDirectoryBase <-  "~/git/simwrapper-example-project/data/matsim-leipzig/base"# the plots are going to be saved here
if(!file.exists(outputDirectoryBase)){
  print("creating analysis sub-directory")
  dir.create(outputDirectoryBase)  
}
#/Users/mkreuschnervsp/Desktop/git/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/",runID,"/analysis/analysis-R
outputDirectoryScenario <-  "~/git/simwrapper-example-project/data/matsim-leipzig/policy"
  #paste(scenario_run_path, "analysis/analysis-R", sep = "") # the plots are going to be saved here
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
  x_ms_legs_count =           0
#### #1.4 Modal Split DISTANCE - legs based  
  x_ms_legs_distance =        0

#### #2.1 Modal Shift - trips based
  x_sankey_diagram = 1

#### #3.1 Distances TRAVELED - trips based
  x_average_traveled_distance_trips =   1
#### #3.2 Distances EUCLIDEAN - trips based
  x_average_euclidean_distance_trips =  1
#### #3.3 PKM - trips based  
  x_personen_km_trips =                 0
#### #3.4 Distances TRAVELED - legs based
  x_average_traveled_distance_legs =    0
#### #3.5 Distances EUCLIDEAN - legs based  
  x_average_euclidean_distance_legs =   0
#### #3.6 PKM - legs based
  x_personen_km_legs =                  1

#### #4.1 Time Traveled - trips based
  x_average_time_trips =      1
#### #4.2 Time Traveled - legs based  
  x_average_time_legs =       0
#### #4.3 ph - trips based    
  x_personen_h_trips =        0
#### #4.4 ph - legs based 
  x_personen_h_legs =         1

#### #5.1 Speed TRAVELED - trips based
  x_average_traveled_speed_trips =    1
#### #5.2 Speed BEELINE - trips based
  x_average_beeline_speed_trips =     1

#### #6.1 Traffic Volumes  
  #x_traffic = 0

#### #7.1 Emissions Analysis  
  #x_emissions = 0  
  
#### #8.1 Winner/Loser Analysis
  x_winner_loser = 1

#x_distance_distribution_trips = 1
#x_distance_distribution_legs =  0
  



  
print("#### Auswahl getroffen! ####")
################################################################################ SOURCE ####

source("~/git/matsim-leipzig/src/main/R/masteranalyse.R")

print("#### Masterscript fertig! ####")










