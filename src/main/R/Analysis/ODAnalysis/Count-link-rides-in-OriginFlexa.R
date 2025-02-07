library(geosphere)
library(tidyverse)
library(dplyr)
library(terra)
library(lubridate)
library(optparse)
######################

#the necessary paths are pasted in as program / script arguments just like for matsim run scripts
option_list <- list(
  make_option(c("-p", "--programPath"), type="character", default=NULL,
              help="Path to function countdrivenstops.R. Avoid using '\', use '/' instead", metavar="character"),
  make_option(c("-w", "--workDir"), type="character", default=NULL,
              help="Path to working directory. The demand source file should be here. Avoid using '\', use '/' instead", metavar="character"),
  make_option(c("-s", "--sourceFileName"), type="character", default=NULL,
              help="Name of source demand file", metavar="character"),
  make_option(c("-d", "--date"), type="character", default="none",
            help="Date, which should be analyzed (format yyyy-mm-dd) . Put <none> for analyzing the whole file.", metavar="character"),
  make_option(c("-n", "--noDays"), type="integer", default=1,
              help="Number of days to be analyzed", metavar="character"),
  make_option(c("-o", "--outputFile"), type="character", default=NULL,
              help="Name of output file", metavar="character"))

opt_parser <- OptionParser(option_list=option_list)
opt <- parse_args(opt_parser)

if (is.null(opt$programPath) | is.null(opt$workDir) | is.null(opt$sourceFileName) | is.null(opt$date)
  | is.null(opt$noDays) | is.null(opt$outputFile)){

  print_help(opt_parser)
  stop("At least 6 arguments must be supplied. Use -h for help.", call.=FALSE)
}

programPath <- opt$programPath
workDir <- opt$workDir
sourceFileName <- opt$sourceFileName
startDate <- opt$date
noDays <- opt$noDays
outputFile <- opt$outputFile

# the parameter, which you have to adapt
# programPath <- "/Users/tomkelouisa/Documents/VSP/matsim-leipzig/src/main/R/ODAnalysis" # the function coundrivenstops.R should be there
# workDir <- "/Users/tomkelouisa/Documents/VSP/Leipzig" # the outputfile is going to there and the source file should be there
# sourceFileName <- "Flexa_Rides_2021.csv" # source file
#startDate <- "2021-07-02"
# startdate <- -1 #when the whole source file should be operated, so write -1, otherwise write the  date you want to start with
# noDays <- 1 # and here the number of days you want to have starting at the startDate
# outputFile <-"stop2stopridesFlexa.tsv" # filename outputfile
#csvfilename <- "stop2stopridesVIA.csv"
setwd(programPath)

source("countdrivenstops.R")

setwd(workDir)
#read source data
flexaDataframe <- read.csv(sourceFileName, stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8", sep= ";")

flexaDataframe <- flexaDataframe %>% mutate(actual_departure_time = ymd_hms(actual_departure_time))

if (startDate!="none"){
  outputFile <- paste0(startDate,"_",outputFile)
  d <- interval(ymd(startDate),ymd(startDate)+days(x=noDays))
  flexaDataframe <- flexaDataframe %>% filter(actual_departure_time %within% d)
}

# counts the number of stop-to-stop connections
countdrivenlinks(flexaDataframe, 'requested_origin_assigned_flexa_stop', 'requested_destination_assigned_flexa_stop', outputFile)




