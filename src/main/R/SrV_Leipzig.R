
#Package Comments
library(tidyverse)
library(kableExtra)
library(flextable)
library(ggplot2)
library(labelled)
library(haven)

rm(list = ls())

#Data sets (please update the right path to your files)
H_SrV2018_Leipzig <- read_sav("../../../input/srv/H_SrV2018_Leipzig.sav", user_na = TRUE)
P_SrV2018_Leipzig <- read_sav("../../../input/srv/P_SrV2018_Leipzig.sav", user_na = TRUE)
W_SrV2018_Leipzig <- read_sav("../../../input/srv/W_SrV2018_Leipzig.sav", user_na = TRUE)


#STEP 1:

#Group the transport modes in a new variable E_HVM_namav
W_SrV2018_Leipzig <- W_SrV2018_Leipzig  %>% mutate(E_HVM_namav = case_when(
  E_HVM == 1 ~ 1,
  E_HVM == 2 | E_HVM == 18 | E_HVM == 19 ~ 2,
  E_HVM >=3 & E_HVM <= 6  ~ 3,
  E_HVM >=7 & E_HVM <= 9  ~ 4,
  E_HVM >=10 & E_HVM <= 17 ~ 5,
  TRUE ~ -7
)) %>% 
  set_variable_labels(E_HVM_namav = "Main Transport Mode") %>% 
  set_value_labels(E_HVM_namav = c(
    "Walk" = 1,
    "Bike" = 2,
    "Car" = 3,
    "Ride" = 4,
    "PT" = 5,
    "missing values" = -7))

#STEP 2: FILTER: remove all the trips that are invalid + all trips of persons with at least one invalid trip

#join W and P data
W_SrV2018_Leipzig_w_person <- left_join(W_SrV2018_Leipzig, P_SrV2018_Leipzig, by= "P_ID")

#filter trips which are invalid
W_SrV2018_Leipzig_w_person_invalid_trips <- W_SrV2018_Leipzig_w_person %>% 
  filter(GIS_LAENGE < 0 | E_DAUER <= 0)

#filter all trips that are made by the persons with at least one invalid trip
W_SrV2018_Leipzig_w_person_trips_of_persons_w_invalid_trips <- W_SrV2018_Leipzig_w_person %>% semi_join(W_SrV2018_Leipzig_w_person_invalid_trips, by="P_ID")

#remove all trips of the particular persons which have at least one invalid trip reported
W_SrV2018_Leipzig_w_person_valid_trips <- W_SrV2018_Leipzig_w_person %>%
  anti_join(W_SrV2018_Leipzig_w_person_trips_of_persons_w_invalid_trips , by="P_ID")


#STEP 3: Calculating MODAL SPLIT with filter GIS-trip length <100km

W_SrV2018_Leipzig_w_person_valid_trips %>%
  filter(E_HVM_namav != -7, GIS_LAENGE >= 0, GIS_LAENGE <100) %>% 
  count("Main Transport Mode" = as_factor(E_HVM_namav), wt = GEWICHT_W) %>% 
  mutate("Share in percent" = round(n/sum(n)*100, 1)) %>% 
  select(-n) %>% flextable()

#STEP 4: Calculating MODAL SPLIT by distance groups

# creating new distance groups
W_SrV2018_Leipzig_w_person_valid_trips <- W_SrV2018_Leipzig_w_person_valid_trips %>%
  mutate(GIS_LAENGE_namav = case_when(
    GIS_LAENGE >= 0 & GIS_LAENGE < 1 ~ 1,
    GIS_LAENGE >= 1 & GIS_LAENGE < 2 ~ 2,
    GIS_LAENGE >= 2 & GIS_LAENGE < 5 ~ 3,
    GIS_LAENGE >= 5 & GIS_LAENGE < 10 ~ 4,
    GIS_LAENGE >= 10 & GIS_LAENGE < 20 ~ 5,
    GIS_LAENGE >= 20 ~ 6,
    TRUE ~ -999
  )) %>% set_variable_labels(GIS_LAENGE_namav = "Distance Classes") %>% 
  set_value_labels(GIS_LAENGE_namav = c("0-1 km" = 1, 
                                        "1-2 km" = 2,
                                        "2-5 km" = 3,
                                        "5-10 km" = 4,
                                        "10-20 km" = 5,
                                        "20 km and more" = 6,
                                        "missing values" = -999))

#Calculating MODAL SPLIT by distance groups with filter GIS-trip length <100km
W_SrV2018_Leipzig_w_person_valid_trips %>% 
  filter(E_HVM_namav >=0, GIS_LAENGE >= 0, GIS_LAENGE <100) %>% 
  select(GIS_LAENGE_namav, E_HVM_namav) %>%
  group_by(GIS_LAENGE_namav, E_HVM_namav) %>%
  summarise(n = n()) %>% 
  group_by(GIS_LAENGE_namav) %>% 
  mutate(Share = n / sum(n) * 100) %>% 
  ungroup() %>% 
  ggplot(aes(x = as_factor(GIS_LAENGE_namav), y = Share, fill = as_factor(E_HVM_namav))) +
  geom_bar(stat = "identity", position = "fill") +
  labs(
    title = "Modal Split by Distance - GIS_LAENGE (trip length <100km)",
    x  = "Distance Classes", 
    fill = "Transport Modes")+
  geom_text(aes(label = ifelse(Share < 3, "", paste0(round(Share), "%"))), 
            position = position_fill(vjust = 0.5)) +
  theme_light() +
  theme(text = element_text(size = 16))