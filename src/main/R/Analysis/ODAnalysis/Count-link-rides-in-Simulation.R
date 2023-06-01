library(optparse)
library(plyr)
library(readr)
library(tidyverse)

#the necessary paths are pasted in as program / script arguments just like for matsim run scripts
option_list <- list(
  make_option(c("-s", "--stops"), type="character", default=NULL,
              help="Path to stops file. Avoid using '\', use '/' instead", metavar="character"),
  make_option(c("-d", "--runDir"), type="character", default=NULL,
              help="Path run directory. Avoid using '\', use '/' instead", metavar="character"),
  make_option(c("-m", "--mode"), type="character", default=NULL,
              help="Mode to be analyzed. Either drt or av", metavar="character"))

opt_parser <- OptionParser(option_list=option_list)
opt <- parse_args(opt_parser)

if (is.null(opt$stops) | is.null(opt$runDir) | is.null(opt$mode)){

  print_help(opt_parser)
  stop("At least 3 arguments must be supplied. Use -h for help.", call.=FALSE)
}

######################
##INPUT##


# Path to stops file
stopsPath <- opt$stops
# path to run main output dir
runDirectory <- opt$runDir
  runDirectory
# mode to be analyzed. set either drt or av
mode <- opt$mode

##############################
## SCRIPT ##
# stopsPath <-"C:/Users/Simon/Documents/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/flexa-scenario/input/leipzig-v1.1-drt-stops-locations-north.csv"
# runDirectory <-"C:/Users/Simon/Documents/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/flexa-scenario/output/flexa-base-case/flexa-base-case/"
# mode <- "drtNorth"


outputDir <- paste(runDirectory, "analysis/analysis-drt", sep = "") # the plots are going to be saved here
if(!file.exists(outputDir)){
  print("creating analysis sub-directory")
  dir.create(outputDir)  
}

#Daten Stopdaten einlesen
#stops <- read.csv(stopsPath,
   #               stringsAsFactors = FALSE,
  #                header = TRUE,
   #               encoding = "UTF-8")
stops <- ldply(list(stopsPath), function(x) read.csv(x,stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8"))


fileEnding <- paste("*.drt_legs_", mode, ".csv", sep ="")

maxIteration <- max(list.dirs(path=paste0(runDirectory, "ITERS/"),full.names=FALSE, recursive = TRUE), na.rm=TRUE)

# Simulierte drt Daten einlesen
movements <- read.csv(list.files(paste0(runDirectory, "ITERS/",maxIteration,"/"), pattern = fileEnding, full.names = T, include.dirs = F),
                      stringsAsFactors = FALSE,
                      header = TRUE,
                      encoding = "UTF-8",
                     sep= ";")

stops <- stops[!duplicated(stops$Link.ID),]

#select necessary information only + get additional columns from stops file
movements <- select(movements, fromLinkId, toLinkId) %>%
  rowwise() %>%
  mutate(fromstopIds=stops[stops$Link.ID==fromLinkId,]$Stop.ID) %>%
  mutate(fromX=stops[stops$Link.ID==fromLinkId,]$X) %>%
  mutate(fromY=stops[stops$Link.ID==fromLinkId,]$Y) %>%
  mutate(tostopIds=stops[stops$Link.ID==toLinkId,]$Stop.ID) %>%
  mutate(toX=stops[stops$Link.ID==toLinkId,]$X) %>%
  mutate(toY=stops[stops$Link.ID==toLinkId,]$Y) %>%
  add_column(anzahlFahrten=as.integer(1)) %>%
  unite("merged",fromstopIds,tostopIds,remove = FALSE)

#duplicates <- movements[duplicated(movements),]

dupl <- aggregate(anzahlFahrten ~ merged, movements, sum)

movements <- movements[!duplicated(movements),] %>%
  rowwise() %>%
  mutate(anzahlFahrten=dupl[dupl$merged==merged,]$anzahlFahrten)

#re-arrange order of cols because of already existing simwrapper configs
col_order <- c("fromstopIds", "tostopIds", "fromLink", "toLink", "fromX","fromY","toX","toY","anzahlFahrten")

movements <- select(movements, !merged) %>%
  rename("fromLink" = fromLinkId,
         "toLink" = toLinkId)

movements <- movements[, col_order]

movementsSmall <- select(movements, fromstopIds,tostopIds,anzahlFahrten) %>%
  filter(!is.na(fromstopIds) & !is.na(tostopIds))

print(paste( "writing to ", outputDir, "/drt_stop-2-stop_", mode, ".csv", sep=""))
write.csv2(movementsSmall,paste(outputDir, "/drt_stop-2-stop_", mode, ".csv", sep=""),quote=FALSE, row.names=FALSE)
print(paste( "writing to ", outputDir, "/drt_stop-2-stop_", mode, ".tsv", sep=""))
write.table(movements,paste(outputDir, "/drt_stop-2-stop_", mode, "-detailed.tsv", sep=""),quote=FALSE, sep="\t",col.names = NA,row.names = TRUE)
