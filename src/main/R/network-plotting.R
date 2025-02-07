library(tidyverse)
library(matsim)
library(tmap)
library(tmaptools)
library(OpenStreetMap)
library(sf)

NETWORK = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/leipzig-v1.1-network-with-pt-drt.xml"
TILES = "https://services.arcgisonline.com/arcgis/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}"
DILUTION_AREA = "C:/Users/ACER/Desktop/Uni/VSP/NaMAV/matsim-input-files/senozon/20210520_leipzig/dilutionArea.shp"
v1.1_freight <- readLinkStats(runId = "v1.1-freight", file = "Y:/matsim-leipzig/run-freight/leipzig-25pct_with_freight.csv")
LINKSTATS.V1.0 = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/039/v1.0_run039.linkstats.tsv"

#### IMPORT OUTPUT NETWORK AND LINKS ####
network = loadNetwork(NETWORK)
links = network$links

#rm(network)

sf <- read_sf(DILUTION_AREA, crs = 25832)

link.coords <- links %>%
  select(id, ends_with(".to"), ends_with(".from"), type)

link.filtered <- links %>%
  filter(!str_detect(id, pattern = "pt")) %>%
  st_as_sf(coords = c("x.to", "y.to"), crs = 25832) %>%
  st_filter(sf) %>%
  st_as_sf(coords = c("y.from", "y.to"), crs = 25832) %>%
  st_filter(sf)

data.frame(id = link.filtered$id) %>%
  readr::write_csv(file = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/linkIdsFiltered.csv")

linkstats.v1 <- readLinkStats(runId = "v1.0-runId-037", file = LINKSTATS.V1.0)

counts <- readCounts(file = "output/leipzig-v1.1-counts_Pkw.xml")

join <- mergeCountsAndLinks(linkStats = list(linkstats.v1, v1.1_freight), network = list("links" = links), counts = counts, aggr_to = "day")

est.breaks = c(-Inf, 0.8, 1.2, Inf)
est.labels = c("less", "exact", "more")

quality <- processDtvEstimationQuality(joinedFrame = join, aggr = F)

linkstats <- linkstats.v1 %>%
  mutate(key = paste0(linkId, "-", time)) %>%
  left_join(x = mutate(v1.1_freight, key = paste0(linkId, "-", time)), by = "key")

vol.breaks <- c(-Inf, -20000, -10000, -5000, -1000, 0, 1000, 5000, Inf)
vol.labels <- c("< -20k", "-20k < -10k", "-10k < -5k", "-5k < -1k", "-1k < 0", "0 < 1k", "1k < 5k", "> 5k")

linkstats.1 <- linkstats %>%
  select(-ends_with(".y")) %>%
  select(-c(avgTravelTime.x, key)) %>%
  group_by(linkId.x) %>%
  summarise_at(c("vol_car_v1.0-runId-037", "vol_car_v1.1-freight", "vol_freight_v1.0-runId-037", "vol_freight_v1.1-freight"), sum) %>%
  mutate(diff_car = `vol_car_v1.1-freight`- `vol_car_v1.0-runId-037`,
         diff_freight = `vol_freight_v1.1-freight` - `vol_freight_v1.0-runId-037`) %>% 
  mutate(diff_interval = cut(x = diff_car, labels = vol.labels, breaks = vol.breaks))
  
  
link.geom <- readr::read_csv(file = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/linkIdsFiltered.csv") %>%
  left_join(link.coords, by = "id") %>%
  left_join(linkstats.1, by = c("id" = "linkId.x")) %>%
  mutate(geom = sprintf("LINESTRING(%s %s, %s %s)", x.from, y.from, x.to, y.to)) %>%
  st_as_sf(crs = 25832, wkt = "geom") %>%
  select(id, type, starts_with("diff"), geom) %>%
  filter(diff_car != Inf | !is.na(diff_car))

geom.reduced <- link.geom %>%
  filter(str_detect(string = type, pattern = "motorway") | 
           str_detect(string = type, pattern = "primary") | 
           str_detect(string = type, pattern = "secondary"))
  
tmap_mode("view")

tm_shape(shp = geom.reduced) +
  tm_lines(col = "diff_car", style = "cont", midpoint = 0, breaks = c(-23000, 0, 6000), lwd = 3.5, palette = c("red", "green"))

tm_shape(shp = geom.reduced) +
  tm_lines(col = "diff_interval", style = "cont", lwd = 3.5, palette = c("#B71C1C", "#C62828", "#E53935", "#F44336", "#F57F17", "#9E9D24", "#4CAF50", "#26A69A"))


#### Plot count stations only ####

link.geom.count <- readr::read_csv(file = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/linkIdsFiltered.csv") %>%
  left_join(quality, by = c("id" = "loc_id")) %>%
  left_join(link.coords, by = "id") %>%
  filter(!is.na(cs_id)) %>%
  mutate(geom = sprintf("LINESTRING(%s %s, %s %s)", x.from, y.from, x.to, y.to)) %>%
  st_as_sf(crs = 25832, wkt = "geom") %>%
  select(id, type.x, estimation, geom) %>%
  filter(!is.na(estimation))

tm_shape(shp = link.geom.count) +
  tm_lines(col = "estimation", style = "cont", lwd = 3.5, palette = c("red", "green", "blue"))
