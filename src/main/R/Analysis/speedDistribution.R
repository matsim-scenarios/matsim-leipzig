library(tidyverse)
library(dplyr)
library(patchwork)
library(networkD3)
library(sf) #=> geography
library(tmap)
library(lubridate)


###legs.csv
completeLegs <- read_csv2("/leipzig-drt-base-case-25pct.output_legs.csv.gz")

completeBikeLegs <- filter(completeLegs, mode == "car")
completeBikeLegs <- mutate(completeBikeLegs,  trav_time = period_to_seconds(hms(trav_time)))
completeBikeLegs <- mutate(completeBikeLegs,  trav_speed = (distance/trav_time) *3.6)

trav_speed <- ggplot(completeBikeLegs, aes(trav_speed)) +
  geom_area(stat="bin") +
  theme_classic() +
  labs(title = "Car Speed Case Tempo 20") +
  ylab("Anzahl") +
  theme_minimal() +
  xlab("trav_speed km/h") 

trav_speed
