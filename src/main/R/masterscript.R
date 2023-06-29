#Masterscript
################################################################################ Libraries ####
library(tidyverse)
library(sf)
library(alluvial)
library(lubridate)
library(XML)
# make sure you use winnerLoserUtils branch of matsim-r until the changes are merged
# the following 2 lines are needed for winner loser analysis, which currently is under development
# hence they are commented put for now -sme0623
#devtools::install_github("matsim-vsp/matsim-r", ref="winnerLoserUtils", force = TRUE)
# devtools::load_all("~/git/matsim-r", reset = TRUE)
library(matsim)
library(ggalluvial)
library(getopt)

print("#### Libraries loaded! ####")
################################################################################ CASES #### please put (1=yes/0=no) for analyses 
scenarios <- list(
  #so we're comparing the base-case to the base-case? -jr May'23
  # I guess well use base-case for analyzing the base case here. All the diffplots / comparisons are rather useless then, but thats normal I guess -sme0623
  # "base-case",
  "carfree-area-large"
  # "carfree-area-medium"
  # "carfree-area-small"
  # "drt-outskirts"
  # "drt-whole-city"
  #,"slow-speed-absolute"
  #,"slow-speed-relative"
  #,"combined_scenarioA"
  #,"combined_scenarioB"
  #,"combined_scenarioC"
  #,"combined_scenarioD"
)

################################################################################ INPUT ####

for (scenario in scenarios){

  publicSVN <- "../../public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/"

  runID <- paste0(scenario, "/")

  #base path nur für Sankey und Winner/Loser Analysis
  base.run.path <- "../../public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/base-case/"

  region.shp.path <- "../../shared-svn/projects/NaMAV/data/shapefiles/leipzig_region/Leipzig_puffer.shp"
  city.shp.path <- "../../shared-svn/projects/NaMAV/data/shapefiles/leipzig_stadt/Leipzig_stadt.shp"

  # choose shp path for carfree-area-scenarios, choose carfree_area_large for all other scenarios to avoid errors
  if (scenario == "carfree-area-small") {
    carfree.area.shp.path <- "../../shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_small/Zonen99_update.shp"
  } else if (scenario == "carfree-area-medium") {
    carfree.area.shp.path <- "../../shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_medium/Zonen95_update.shp"
  } else {
    carfree.area.shp.path <- "../../shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_large/Zonen90_update.shp"
  }

  network <- Sys.glob(file.path(base.run.path, "*output_network.xml.gz"))
  CRS <- 25832

  scenario.run.path <- paste0(publicSVN,runID)
  # if you want to run the masterscript on your mounted cluster, you have to define the scenario.run.path here
  ################################################################################################################################################################################################################
  # somehow there are problem when readTripsTable() has to handle a path that is too long. I could not resolve said issue.
  # you might have to re-name your dirs in order to run the masterscript
  # the following example is a path which apparently is just too long
  # scenario.run.path <- "Y:/net/ils/matsim-leipzig/run-drt/namav-output/runsScaledFleet3-2/drtDemandExperiments/ASC0.00837001732397158-dist0.0-travel0.0-intermodal-leipzig-flexa-25pct-scaledFlee/"
  ################################################################################################################################################################################################################
  # scenario.run.path <- "Y:/net/ils/matsim-leipzig/run-drt/namav-output/runsScaledFleet3-2/drtDemandExperiments/wholeCity-210veh/"

  print("#### Input paths defined! ####")
  ################################################################################ OUTPUT ####

  outputDirectoryScenario <-  paste0(scenario.run.path, "analysis/analysis-R") # the plots are going to be saved here

  if(!file.exists(paste0(scenario.run.path,"analysis"))) {
    print("creating general analysis sub-directory")
    dir.create(paste0(scenario.run.path,"analysis"))
  }
  if(!file.exists(outputDirectoryScenario)){
    print("creating analysis sub-directory")
    dir.create(outputDirectoryScenario)
  }

  print("#### Output folder geladen! ####")
  ################################################################################ ANALYSIS ####
  # PLEASE put (1=yes/0=no) for certain analysis

  #### #1.1 Modal Split COUNTS - trips based
    x_ms_trips_count = 1
  #### #1.2 Modal Split DISTANCE - trips based
    x_ms_trips_distance = 1
  #### #1.3 Modal Split COUNTS- legs based
  x_ms_legs_count = 1
  #### #1.4 Modal Split DISTANCE - legs based
  x_ms_legs_distance = 1

  #### #2.1 Modal Shift - trips based
  x_sankey_diagram = 1

  #### #3.1 Distances TRAVELED - trips based
  x_average_traveled_distance_trips = 1
  #### #3.2 Distances EUCLIDEAN - trips based
  x_average_euclidean_distance_trips = 1
  #### #3.3 Heatmap Distances traveled - trips based
  x_heatmap_distance_trips = 1
  #### #3.3 PKM - trips based
  x_personen_km_trips = 1
  #### #3.4 Distances TRAVELED - legs based
  x_average_traveled_distance_legs = 1
  #### #3.5 Distances EUCLIDEAN - legs based
  x_average_euclidean_distance_legs = 1
  #### #3.6 PKM - legs based
  x_personen_km_legs = 1

  #### #4.1 Time Traveled - trips based
  x_average_time_trips = 1
  #### #4.2 Time Traveled - legs based
  x_average_time_legs = 1
  #### #4.3 ph - trips based
  x_personen_h_trips = 1
  #### #4.4 ph - legs based
    x_personen_h_legs = 1
  #### #4.5 Time Traveled Heatmap - trips based
  x_heatmap_time_trips = 1

  #### #5.1 Speed TRAVELED - trips based
    x_average_traveled_speed_trips = 1
  #### #5.2 Speed BEELINE - trips based
    x_average_beeline_speed_trips = 1

  #### #6.1 Traffic Volumes
  x_traffic = 1

  #### #7.1 Emissions Analysis
  x_emissions = 0

  # this analysis should stay inactive as it is not finished yet -sme0623
  #### #8.1 Winner/Loser Analysis
  x_winner_loser = 0

  #### #9.1 DRT supply
  x_drt_supply = 0

  #### #9.2 DRT demand
  x_drt_demand = 0

  #### #9.3 DRT performance
  x_drt_performance = 0

  #### #9.4 DRT trip purposes
  x_drt_trip_purposes = 0

  print("#### Analysis choice succesful! ####")
  print(paste0("#### Starting to analyze output for dir: ", scenario.run.path, " ####"))
  ################################################################################ SOURCE ####

  source("../matsim-leipzig/src/main/R/masteranalyse.R")

  if (x_drt_supply == 1 || x_drt_demand == 1|| x_drt_performance == 1 || x_drt_trip_purposes == 1){

    outputDirectoryScenarioDrt <- paste0(scenario.run.path, "analysis/analysis-drt/")

    source("../matsim-leipzig/src/main/R/master_drt.R")
  }

  print("#### Masterscript done! ####")
}
