#Masterscript
################################################################################ Libraries ####
library(tidyverse)
library(sf)
library(alluvial)
library(lubridate)
library(XML)
# make sure you use winnerLoserUtils branch of matsim-r until the changes are merged
# devtools::install_github("matsim-vsp/matsim-r", ref="winnerLoserUtils", force = TRUE) 
devtools::load_all("~/git/matsim-r", reset = TRUE)
# library(matsim)


print("#### Libraries geladen! ####")
################################################################################ CASES #### please put (1=yes/0=no) for analyses 
scenarios <- list(
  #TODO: so we're comparing the base-case to the base-case? -jr May'23
  "base-case"
  ,"carfree-area-large"
  # ,"carfree-area-95"
  #,"carfree-area-99"
  #,"drt-outskirts"
  #,"drt-whole-city"
  #,"slow-speed-absolute"
  #,"slow-speed-relative"
  #,"combined_scenarioA"
)

################################################################################ INPUT ####

for (scenario in scenarios){

  publicSVN <- "~/git/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/"

  runID <- paste0(scenario, "/")
  network <- paste(publicSVN,"base-case/leipzig-25pct-base.output_network.xml.gz")
  CRS <- 25832

  scenario.run.path <- paste0(publicSVN,runID)

  #base path nur für Sankey und Winner/Loser Analysis
  base.run.path <- "~/git/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/base-case/"


  region.shp.path <- "~/git/shared-svn/projects/NaMAV/data/shapefiles/leipzig_region/Leipzig_puffer.shp"
  city.shp.path <- "~/git/shared-svn/projects/NaMAV/data/shapefiles/leipzig_stadt/Leipzig_stadt.shp"
  area.shp.path <- "~/git/shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_large/Zonen90_update.shp"


  print("#### Inputspath definiert! ####")
  ################################################################################ OUTPUT ####

  outputDirectoryScenario <- paste0(scenario.run.path, "/analysis/analysis-R") # the plots are going to be saved here
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
  x_sankey_diagram = 1

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
  x_average_euclidean_distance_legs =   1
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
  x_traffic = 1

  #### #7.1 Emissions Analysis
  x_emissions = 1

  #### #8.1 Winner/Loser Analysis
  x_winner_loser = 1

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

  source("~/git/matsim-leipzig/src/main/R/masteranalyse.R")

  if (x_drt_supply == 1 || x_drt_demand == 1|| x_drt_performance == 1 || x_drt_volumes == 1|| x_drt_trip_purposes == 1){
    source("~/git/matsim-leipzig/src/main/R/master_drt.R")
  }

  print("#### Masterscript fertig! ####")
}
