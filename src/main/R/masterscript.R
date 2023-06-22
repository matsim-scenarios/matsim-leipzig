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

#pretty sure we don't need these
# library(gridExtra)
# library(tidyr)
# library(viridis)
# library(ggsci)
# library(dplyr)
# library(ggplot2)
# library(purrr)
# library(networkD3)
# library(stringr)
# library(data.table)
# library(chron)
# library(xml2)


print("#### Libraries geladen! ####")
################################################################################ CASES #### please put (1=yes/0=no) for analyses 
scenarios <- list(
  #TODO: so we're comparing the base-case to the base-case? -jr May'23
  # edit: I dont know if base-case here is necessary for some other stuff. Hence, Ill try to comment it out and run the script
  #if this does work we can delete base case here -sme0623
  #"base-case,"
  "carfree-area-large"
  # ,"carfree-area-medium"
  #,"carfree-area-small"
  #,"drt-outskirts"
  #,"drt-whole-city"
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
    carfree.area.shp.path <- paste0("../../shared-svn/projects/NaMAV/data/shapefiles/leipzig_",scenario,"/Zonen99_update.shp")
  } else if (scenario == "carfree-area-medium") {
    carfree.area.shp.path <- paste0("../../shared-svn/projects/NaMAV/data/shapefiles/leipzig_",scenario,"/Zonen95_update.shp")
  } else {
    carfree.area.shp.path <- "../../shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_large/Zonen90_update.shp"
  }

  network <- Sys.glob(file.path(base.run.path, "*output_network.xml.gz"))
  CRS <- 25832

  scenario.run.path <- paste0(publicSVN,runID)
  # scenario.run.path <- "Y:/net/ils/matsim-leipzig/run-drt/namav-output/runsScaledFleet3-2/drtDemandExperiments/ASC0.00837001732397158-dist0.0-travel0.0-intermodal-leipzig-flexa-25pct-scaledFleet-caseNamav-randomFleet-wholeCity-400veh/"
  # scenario.run.path <- "Y:/net/ils/matsim-leipzig/run-drt/namav-output/runsScaledFleet3-2/ASC0.00837001732397158-dist0.0-travel-2.0-intermodal-leipzig-flexa-25pct-scaledFleet-caseNamav-randomFleet/"

  print("#### Input paths definiert! ####")
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
    x_ms_trips_count = 0
  #### #1.2 Modal Split DISTANCE - trips based
    x_ms_trips_distance = 0
  #### #1.3 Modal Split COUNTS- legs based
  x_ms_legs_count = 0
  #### #1.4 Modal Split DISTANCE - legs based
  x_ms_legs_distance = 0

  #### #2.1 Modal Shift - trips based
  x_sankey_diagram = 0

  #### #3.1 Distances TRAVELED - trips based
  x_average_traveled_distance_trips = 0
  #### #3.2 Distances EUCLIDEAN - trips based
  x_average_euclidean_distance_trips = 0
  #### #3.3 Heatmap Distances traveled - trips based
  x_heatmap_distance_trips = 0
  #### #3.3 PKM - trips based
  x_personen_km_trips = 0
  #### #3.4 Distances TRAVELED - legs based
  x_average_traveled_distance_legs = 0
  #### #3.5 Distances EUCLIDEAN - legs based
  x_average_euclidean_distance_legs = 0
  #### #3.6 PKM - legs based
  x_personen_km_legs = 0

  #### #4.1 Time Traveled - trips based
  x_average_time_trips = 0
  #### #4.2 Time Traveled - legs based
  x_average_time_legs = 0
  #### #4.3 ph - trips based
  x_personen_h_trips = 0
  #### #4.4 ph - legs based
    x_personen_h_legs = 0
  #### #4.5 Time Traveled Heatmap - trips based
  x_heatmap_time_trips = 0

  #### #5.1 Speed TRAVELED - trips based
    x_average_traveled_speed_trips = 0
  #### #5.2 Speed BEELINE - trips based
    x_average_beeline_speed_trips = 0

  #### #6.1 Traffic Volumes
  x_traffic = 0

  #### #7.1 Emissions Analysis
  x_emissions = 0

  # this analysis should stay inactive as it is not finished yet -sme0623
  #### #8.1 Winner/Loser Analysis
  x_winner_loser = 0

  #### #9.1 DRT supply
  x_drt_supply = 1

  #### #9.2 DRT demand
  x_drt_demand = 1

  #### #9.3 DRT performance
  x_drt_performance = 1

  #### #9.4 DRT trip purposes
  x_drt_trip_purposes = 1

  print("#### Auswahl getroffen! ####")
  ################################################################################ SOURCE ####

  source("../matsim-leipzig/src/main/R/masteranalyse.R")

  if (x_drt_supply == 1 || x_drt_demand == 1|| x_drt_performance == 1 || x_drt_trip_purposes == 1){

    outputDirectoryScenarioDrt <- paste0(scenario.run.path, "analysis/analysis-drt/")

    print("HERE3")

    # this dir should already exist as it is created by java analysis before running this analysis -sm30623
    # if(!file.exists(outputDirectoryScenarioDrt)) {
    #   print("creating drt-analysis sub-directory")
    #   dir.create(outputDirectoryScenarioDrt)
    # }
    source("../matsim-leipzig/src/main/R/master_drt.R")
  }

  print("#### Masterscript fertig! ####")
}
