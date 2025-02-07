
library(gridExtra)
library(tidyverse)
library(lubridate)
library(viridis)
library(ggsci)
library(sf)


# setwd("~/Development/matsim-scenarios/matsim-leipzig/src/main/R")

# Person data from srv
############################

persons <- read_delim("../../../../shared-svn/projects/NaMAV/data/SrV_2018/SrV2018_Einzeldaten_Leipzig_LE_SciUse_P2018.csv", delim = ";", 
                      locale = locale(decimal_mark = ",")) %>%
  filter(ST_CODE_NAME=="Leipzig") %>%
  filter(STICHTAG_WTAG <= 5) %>%
  filter(E_ANZ_WEGE >= 0)

# Avg. number of trips per person per day
per_day <- weighted.mean(persons$E_ANZ_WEGE, persons$GEWICHT_P)

# number of total tripos, based on residents
tt <- per_day * 600000



# Trip data from srV
#############################

trips <- read_delim("../../../../shared-svn/projects/NaMAV/data/SrV_2018/SrV2018_Einzeldaten_Leipzig_LE_SciUse_W2018.csv", delim = ";", 
                    col_types = cols(
                      V_ZIEL_LAND = col_character(),
                      GIS_LAENGE = col_double(),
                      E_HVM = col_number()
                    ), locale = locale(decimal_mark = ","))


# categories as defined in SrV mapped to matsim mode
lookup <- tibble(category = c(1, 2, 18, 19, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17), 
                 mode = c("walk", "bike", "bike", "bike", "car", "car", "car", "car", "ride", "ride", "ride", "pt", "pt", "pt", "pt", "pt", "pt", "pt", "pt"))


# trip distance groups
levels = c("0 - 1000", "1000 - 3000", "3000 - 5000", "5000 - 10000", "10000 - 20000", "20000+")
breaks = c(0, 1, 3, 5, 10, 20, Inf)


# Filter invalid modes and trip distances, also filter for weekdays
relevant <- trips %>%
  filter(ST_CODE_NAME=="Leipzig") %>%
  filter(E_HVM < 70) %>%
  filter(GIS_LAENGE >= 0 & E_DAUER > 0) %>%
  filter(GIS_LAENGE < 100) %>%
  filter(STICHTAG_WTAG <= 5) %>%
  mutate(dist_group = cut(GIS_LAENGE, breaks=breaks, labels=levels, right=F))

matched <- relevant %>% left_join(lookup, by=c("E_HVM"="category"))


srv <- matched %>%
  group_by(dist_group, mode) %>%
  summarise(trips=sum(GEWICHT_W)) %>%
  mutate(mode = fct_relevel(mode, "walk", "bike", "pt", "ride", "car")) %>%
  mutate(source = "srv")

# Trips in survey
st <- sum(srv$trips)

srv_aggr <- srv %>% group_by(mode) %>%
  summarise(trips=sum(trips) / st)

srv <- srv %>%
  mutate(scaled_trips=trips*(tt/st)) %>%
  mutate(share=trips / sum(srv$trips))

#write_csv(srv, "srv.csv")

# agents in city 115209, younger population is missing
# scale factor 5.2 instead of 4

# Read from trips and persons directly

f <- "\\\\sshfs.kr\\rakow@cluster.math.tu-berlin.de\\net\\ils\\matsim-leipzig\\calibration-25pct\\runs\\009"
sim_scale <- 4

# breaks in meter
breaks = c(0, 1000, 2000, 5000, 10000, 20000, Inf)

shape <- st_read("../../../../../shared-svn/projects/NaMAV/data/Leipzig_Stadtbezirke_gesamt_UTM32N/leipzig-utm32n-gesamt/leipzig-utm32n-gesamt.shp", crs=25832)

persons <- read_delim(list.files(f, pattern = "*.output_persons.csv.gz", full.names = T, include.dirs = F) , delim = ";", trim_ws = T, 
                      col_types = cols(
                        person = col_character(),
                        good_type = col_integer()
                      )) %>%
  st_as_sf(coords = c("first_act_x", "first_act_y"), crs = 25832) %>%
  st_filter(shape)


trips <- read_delim(list.files(f, "*.output_trips.csv.gz", full.names = T, include.dirs = F) , delim = ";", trim_ws = T, 
                    col_types = cols(
                      person = col_character()
                    )) %>%
  filter(main_mode!="freight") %>%
  semi_join(persons) %>%
  mutate(dist_group = cut(traveled_distance, breaks=breaks, labels=levels, right=F))

sim <- trips %>%
  group_by(dist_group, main_mode) %>%
  summarise(trips=n()) %>%
  mutate(mode = fct_relevel(main_mode, "walk", "bike", "pt", "ride", "car")) %>%
  mutate(scaled_trips=sim_scale * trips) %>%
  mutate(source = "sim")

sim <- sim %>%
      mutate(share=trips/sum(sim$trips))

write_csv(sim, "sim.csv")

######
# Total modal split
#######

srv_aggr <- srv %>%
  mutate(share=trips/sum(srv$trips)) %>%
  group_by(mode) %>%
  summarise(share=sum(share)) %>%  # assume shares sum to 1
  mutate(mode=fct_relevel(mode, "walk", "bike", "pt", "ride", "car"))  

aggr <- sim %>%
  group_by(mode) %>%
  summarise(share=sum(trips) / sum(sim$trips))

p1_aggr <- ggplot(data=srv_aggr, mapping =  aes(x=1, y=share, fill=mode)) +
  labs(subtitle = "Survey data") +
  geom_bar(position="fill", stat="identity") +
  coord_flip() +
  geom_text(aes(label=scales::percent(share, accuracy = 0.1)), size= 5, position=position_fill(vjust=0.5)) +
  scale_fill_locuszoom() +
  theme_void() +
  theme(legend.position="none")

p2_aggr <- ggplot(data=aggr, mapping =  aes(x=1, y=share, fill=mode)) +
  labs(subtitle = "Simulation") +
  geom_bar(position="fill", stat="identity") +
  coord_flip() +
  geom_text(aes(label=scales::percent(share, accuracy = 0.1)), size= 5, position=position_fill(vjust=0.5)) +
  scale_fill_locuszoom() +
  theme_void()

g <- arrangeGrob(p1_aggr, p2_aggr, nrow = 2)
ggsave(filename = "modal-split.png", path = ".", g,
       width = 6, height = 2, device='png', dpi=300)


#########
# Combined plot by distance
##########

total <- bind_rows(srv, sim)

# Maps left overgroups
dist_order <- factor(total$dist_group, level = levels)
dist_order <- fct_explicit_na(dist_order, "20000+")

ggplot(total, aes(fill=mode, y=scaled_trips, x=source)) +
  labs(subtitle = paste("Leipzig scenario",  substring(f, 52)), x="distance [m]", y="trips") +
  geom_bar(position="stack", stat="identity", width = 0.5) +
  facet_wrap(dist_order, nrow = 1) +
  scale_y_continuous(labels = scales::number_format(suffix = " M", scale = 1e-6)) +
  theme_classic()


# Needed for adding short distance trips

calib_sum <- sum(sim$trips)
calib_aggr <- sim %>%
  group_by(dist_group) %>%
  summarise(share=sum(trips) / calib_sum)

# Needed share of trips
tripShare <- 0.2418
shortDistance <- sum(filter(sim, dist_group=="0 - 1000")$trips)
numTrips = (shortDistance - calib_sum * tripShare) / (tripShare - 1)


########################

srv0 <- srv %>%
  filter(dist_group=="0 - 1000") %>%
  mutate(sum_group = sum(trips))

srv13 <- srv %>%
  filter(dist_group=="1000 - 3000") %>%
  mutate(sum_group = sum(trips))

srv35 <- srv %>%
  filter(dist_group=="3000 - 5000") %>%
  mutate(sum_group = sum(trips))

srv510 <- srv %>%
  filter(dist_group=="5000 - 10000") %>%
  mutate(sum_group = sum(trips))

srv1020 <- srv %>%
  filter(dist_group=="10000 - 20000") %>%
  mutate(sum_group = sum(trips))

srv20 <- srv %>%
  filter(dist_group=="20000+") %>%
  mutate(sum_group = sum(trips))

srv <- bind_rows(srv0,srv13,srv35,srv510,srv1020,srv20) %>%
  mutate(share_group = trips / sum_group)

sim0 <- sim %>%
  filter(dist_group=="0 - 1000") %>%
  mutate(sum_group = sum(trips))

sim13 <- sim %>%
  filter(dist_group=="1000 - 3000") %>%
  mutate(sum_group = sum(trips))

sim35 <- sim %>%
  filter(dist_group=="3000 - 5000") %>%
  mutate(sum_group = sum(trips))

sim510 <- sim %>%
  filter(dist_group=="5000 - 10000") %>%
  mutate(sum_group = sum(trips))

sim1020 <- sim %>%
  filter(dist_group=="10000 - 20000") %>%
  mutate(sum_group = sum(trips))

sim20 <- sim %>%
  filter(dist_group=="20000+") %>%
  mutate(sum_group = sum(trips))

sim <- bind_rows(sim0,sim13,sim35,sim510,sim1020,sim20) %>%
  mutate(share_group = trips / sum_group)

by_distance <- bind_rows(filter(srv, mode!="total_distance_distribution"), sim) %>%
  mutate(mode=fct_relevel(mode, "walk", "bike", "pt", "ride", "car"))

dist_order <- factor(by_distance$dist_group, level = levels)
dist_order <- fct_explicit_na(dist_order, "100000+")

g <- ggplot(by_distance, aes(y=share_group, x=source, fill=mode)) +
  labs(subtitle = paste("Kelheim scenario", substring(f, 52)), x="distance [m]", y="share") +
  geom_bar(position="stack", stat="identity", width = 0.5) +
  facet_wrap(dist_order, nrow = 1) +
  scale_fill_locuszoom() +
  theme_minimal()
g

ggsave(filename = "modal-distance-distribution-relative.png", path = ".", g,
       width = 12, height = 10, device='png', dpi=300)



