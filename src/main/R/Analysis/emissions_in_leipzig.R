library(gridExtra)
library(tidyverse)
library(lubridate)
library(viridis)
library(ggsci)
library(sf)
library(ggplot2)
library(matsim)
library(dplyr)
library(purrr)

network <- loadNetwork("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/networks/leipzig-flexa-25pct-scaledFleet-baseCase_noDepot.output_network.xml.gz")

links_network <- data.frame(links_Leipzig[2])



shape_L <- st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Leipzig_stadt.shp", crs=25832)
shape_99 <-  st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen99_update.shp", crs=25832)
shape_95 <-  st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen95_update.shp", crs=25832)
shape_90 <-  st_read("/Users/mkreuschnervsp/Desktop/Projects/03_NaMaV/MATSim/shapefiles/Zonen90_update.shp", crs=25832)


#links in Leipzig_Stadt
links_Leipzig <- links_network %>% st_as_sf(coords = c("links.x.from", "links.y.from"), crs = 25832) %>% st_filter(shape_L)
#links in Zonen
links_Zonen99 <- links_network %>% st_as_sf(coords = c("links.x.from", "links.y.from"), crs = 25832) %>% st_filter(shape_99)
links_Zonen95 <- links_network %>% st_as_sf(coords = c("links.x.from", "links.y.from"), crs = 25832) %>% st_filter(shape_95)
links_Zonen90 <- links_network %>% st_as_sf(coords = c("links.x.from", "links.y.from"), crs = 25832) %>% st_filter(shape_90)

length(which(links_Leipzig$links.type == "highway.motorway"))
length(which(links_Leipzig$links.type == "highway.motorway_link"))
length(which(links_Leipzig$links.type == "highway.service"))
length(which(links_Leipzig$links.type == "highway.primary"))
length(which(links_Leipzig$links.type == "highway.primary_link"))
length(which(links_Leipzig$links.type == "highway.secondary"))
length(which(links_Leipzig$links.type == "highway.secondary_link"))
length(which(links_Leipzig$links.type == "highway.tertiary"))
length(which(links_Leipzig$links.type == "highway.residential"))
length(which(links_Leipzig$links.type == "highway.living_street"))
length(which(links_Leipzig$links.type == "highway.trunk"))
length(which(links_Leipzig$links.type == "highway.trunk_link"))
length(which(links_Leipzig$links.type == "highway.unclassified"))

Haupt_Leipzig <- filter(links_Leipzig, links.type == "highway.primary" | links.type =="highway.primary_link" | links.type == "highway.secondary" | links.type =="highway.secondary_link" | links.type =="highway.service" | links.type =="highway.motorway")
Neben_Leipzig <- filter(links_Leipzig, links.type == "highway.residential" | links.type == "highway.tertiary" | links.type == "highway.living_street" | links.type == "highway.unclassified")


emissions_file_base <- read_delim("/Users/...")
emissions_file_99 <- read_delim("/Users/...")
emissions_file_95 <- read_delim("/Users/...")
emissions_file_90 <- read_delim("/Users/...")
#Emissionen auf Hauptstraßenlinks in Leipzig
emissions_haupt_leipzig <- filter(emissions_file_base, )
  mean()

# Transform our 'x' vector
x <- data.frame(x)

# Boxplot with vector
ggplot(data = x, aes(x = "", y = x)) +
  stat_boxplot(geom = "errorbar",      # Error bars
               width = 0.2) +
  geom_boxplot(fill = "#4271AE",       # Box color
               outlier.colour = "red", # Outliers color
               alpha = 0.9) +          # Box color transparency
  ggtitle("Boxplot with vector") + # Plot title
  xlab("") +   # X-axis label
  coord_flip() # Horizontal boxplot

#Emissionen auf Nebenstraßenlinks in Leipzig
emissions_neben_leipzig <- 
  mean()

#Emissionen auf Nebenstraßenlinks innerhalb Zonen
emissions_neben_zonen99 <- 
  mean()
emissions_neben_zonen95 <- 
  mean()
emissions_neben_zonen90 <- 
  mean()

#Verkehr auf Hauptstraßenlinks in Leipzig
traffic_haupt_leipzig <- 
  mean()


#Verkehr auf Nebenstraßenlinks in Leipzig
traffic_neben_leipzig <- 
  mean()


#Verkehr auf Nebenstraßenlinks innerhalb Zonen
traffic_neben_zonen99 <- 
  mean()
traffic_neben_zonen95 <- 
  mean()
traffic_neben_zonen90 <- 
  mean()


