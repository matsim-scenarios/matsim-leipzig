
## 1. Libraries

library(tidyverse)
library(kableExtra) # for styling tables and output them in the viewer pane
library(flextable) # for styling tables and outputs
library(ggplot2)
library(haven)

## 2. Load Data

H_SrV2018_Leipzig <- read_sav("../../../input/srv/H_SrV2018_Leipzig.sav", user_na = TRUE)
P_SrV2018_Leipzig <- read_sav("../../../input/srv/P_SrV2018_Leipzig.sav", user_na = TRUE)
W_SrV2018_Leipzig <- read_sav("../../../input/srv/W_SrV2018_Leipzig.sav", user_na = TRUE)


## 3. Data Processing

#creating district-groups
H_SrV2018_Leipzig %>% count(UNTERBEZIRK)

library(labelled)

H_SrV2018_Leipzig <- H_SrV2018_Leipzig %>% mutate(UNTERBEZIRK_namav = case_when(
  UNTERBEZIRK == "1"| UNTERBEZIRK == "2" ~ 1,
  UNTERBEZIRK == "3" | UNTERBEZIRK == "4" | UNTERBEZIRK == "5" ~ 2,
  UNTERBEZIRK == "6" | UNTERBEZIRK == "7" ~ 3,
  UNTERBEZIRK == "8" ~ 4,
  TRUE ~ -10
)) %>% 
  set_variable_labels(UNTERBEZIRK_namav = "Lower Level") %>% 
  set_value_labels(UNTERBEZIRK_namav = c(
    "Zone 1" = 1,
    "Zone 2" = 2,
    "Zone 3" = 3,
    "Zone 4" = 4,
    "missing values" = -10))

#checking the new variable "UNTERBEZIRK_namav"
H_SrV2018_Leipzig %>% count(UNTERBEZIRK_namav)

H_SrV2018_Leipzig %>% count(as_factor(UNTERBEZIRK)) %>% flextable()

#creating age groups
P_SrV2018_Leipzig <- P_SrV2018_Leipzig %>% mutate(E_ALTER_GRUP = case_when(
  V_ALTER >= 0 & V_ALTER < 18 ~ 1,
  V_ALTER >= 18 & V_ALTER < 25 ~ 2,
  V_ALTER >= 25 & V_ALTER < 35 ~ 3,
  V_ALTER >= 35 & V_ALTER < 50 ~ 4,
  V_ALTER >= 50 & V_ALTER < 65 ~ 5,
  V_ALTER >= 65 ~ 6,
  TRUE ~ -10
)) %>% 
  set_variable_labels(E_ALTER_GRUP = "Age Groups") %>% 
  set_value_labels(E_ALTER_GRUP = c(
    "0-18 years" = 1,
    "18-24 years" = 2,
    "25-34 years" = 3,
    "35-49 years" = 4,
    "50-64 years" = 5,
    "65 and more years" = 6,
    "missing values" = -10))


# checking age groups
P_SrV2018_Leipzig %>% count(E_ALTER_GRUP)
P_SrV2018_Leipzig %>% count(E_ALTER_5)



#E_HVM (variable for mean mode of transport)
W_SrV2018_Leipzig %>% count(E_HVM_4)
W_SrV2018_Leipzig %>% count(E_HVM) %>% print(n=Inf)

#creating new variable E_HVM_namav
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

W_SrV2018_Leipzig %>% count(E_HVM_namav)

#creating activity groups
W_SrV2018_Leipzig %>% count(V_ZWECK) %>% print(n=Inf)

W_SrV2018_Leipzig <- W_SrV2018_Leipzig  %>% mutate(V_ZWECK_namav = case_when(
  V_ZWECK == 2 ~ 1,
  V_ZWECK == 6 ~ 2,
  V_ZWECK == 3 ~ 3,
  V_ZWECK == 7 ~ 4,
  V_ZWECK == 4 ~ 5,
  V_ZWECK == 5 ~ 6,
  V_ZWECK == 19 | V_ZWECK == 70 ~ 7,
  V_ZWECK == 18 ~ 8,
  V_ZWECK >= 11 & V_ZWECK <= 17 ~ 9,
  V_ZWECK == 10 | V_ZWECK == 8 ~ 10,
  V_ZWECK == 9 ~ 11,
  V_ZWECK == 14 ~ 12,
  V_ZWECK == 1 ~ 13,
  TRUE ~ -10
)) %>% 
  set_variable_labels(V_ZWECK_namav = "Trip Purposes") %>% 
  set_value_labels(V_ZWECK_namav = c(
    "business" = 1,
    "educ_higher" = 2,
    "educ_kiga" = 3,
    "educ_other" = 4,
    "educ_primary" = 5,
    "educ_secondary and educ_tertiary" = 6,
    "errands" = 7,
    "home" = 8,
    "leisure" = 9,
    "shop_daily" = 10,
    "shop_other" = 11,    
    "visit" = 12,
    "work" = 13,
    "missing values" = -10))

W_SrV2018_Leipzig %>% count(V_ZWECK_namav)
W_SrV2018_Leipzig %>% count(STICHTAG_WTAG)




## 4. Person Level

### 4.1 Number of Trips by Age Groups

library(gt)

P_SrV2018_Leipzig %>% select(E_ALTER_GRUP, E_ANZ_WEGE, GEWICHT_P) %>%
  filter(E_ALTER_GRUP != -10 & E_ANZ_WEGE >= 0) %>% 
  summarise(Mean_Trips = weighted.mean(E_ANZ_WEGE, GEWICHT_P)) %>%
  gt() %>% fmt_number(decimals = 2)

P_SrV2018_Leipzig %>% select(E_ALTER_GRUP, E_ANZ_WEGE, GEWICHT_P) %>%
  filter(E_ALTER_GRUP != -10 & E_ANZ_WEGE >= 0) %>%  
  group_by(as_factor(E_ALTER_GRUP)) %>% 
  summarise(Mean_Trips = weighted.mean(E_ANZ_WEGE, GEWICHT_P)) %>% 
  gt() %>% fmt_number(decimals = 2)


unique(W_SrV2018_Leipzig$E_WEG_GUELTIG)


### 4.2 Analysis of Mobile Persons by Age

# Mobile Personen 
P_SrV2018_Leipzig %>% count(E_MOBIL)

library(gt)
library(janitor)
library(pollster)

P_SrV2018_Leipzig %>% 
  filter(E_ALTER_GRUP != -10, E_MOBIL != -7) %>% 
  mutate(E_MOBIL = as_factor(E_MOBIL)) %>% 
  crosstab(E_ALTER_GRUP, E_MOBIL, weight = GEWICHT_P, pct_type = "row", unwt_n = TRUE) %>% gt() %>% fmt_number(decimals = 1)


unique(P_SrV2018_Leipzig$E_MOBIL)


### 4.3 Analysis of Mobile Persons by Age and District

# Unterbezirk ist auf Haushaltsebene kodiert
# Schritt 1: auf Personenebene holen

Join_Unterbezirk <- H_SrV2018_Leipzig %>% select(ST_CODE, HHNR, UNTERBEZIRK, UNTERBEZIRK_namav)

P_SrV2018_Leipzig <- left_join(P_SrV2018_Leipzig, Join_Unterbezirk, by = c("ST_CODE", "HHNR"))

P_SrV2018_Leipzig %>% 
  filter(E_ALTER_GRUP != -10, E_MOBIL != -7) %>% 
  mutate(E_MOBIL = as_factor(E_MOBIL)) %>% 
  crosstab_3way(z = E_ALTER_GRUP, y = E_MOBIL, x = UNTERBEZIRK_namav, weight = GEWICHT_P, pct_type = "row", unwt_n = TRUE) %>% gt() %>% fmt_number(decimals = 1)

P_SrV2018_Leipzig %>% count(UNTERBEZIRK)



## 5. Trip Level

### 5.1 Analysis of Trip Length


W_SrV2018_Leipzig %>% filter(!is.na(E_LAENGE_5G) & E_WEG_GUELTIG == -1) %>% 
  ggplot(aes(x = as_factor(E_LAENGE_5G))) +
  geom_bar()

#### 5.1.1 Average Trip Length by Age

Join_E_ALTER_GRUP <- P_SrV2018_Leipzig %>% select(ST_CODE, HHNR, PNR, E_ALTER_GRUP)

W_SrV2018_Leipzig <- left_join(W_SrV2018_Leipzig, Join_E_ALTER_GRUP, by = c("ST_CODE", "HHNR", "PNR"))
#rm(test)

W_SrV2018_Leipzig %>% count(V_LAENGE) %>% tail()

W_SrV2018_Leipzig %>%
  filter(E_ALTER_GRUP != -10 & E_WEG_GUELTIG == -1) %>%
  group_by(as_factor(E_ALTER_GRUP)) %>% 
  summarise(Mean_Laenge = weighted.mean(V_LAENGE, GEWICHT_W)) %>%
  gt() %>% fmt_number(decimals = 2)


### 5.2 Analysis of Main Transport Mode

#### 5.2.1 Main Transport Mode

W_SrV2018_Leipzig %>% filter(E_HVM_namav != -7) %>% 
  count("Main Transport Mode" = as_factor(E_HVM_namav), wt = GEWICHT_W) %>% 
  mutate("Share in percent" = round(n/sum(n)*100, 1)) %>% 
  select(-n) %>% flextable()


#### 5.2.2 Modal Split by Distance

unique(W_SrV2018_Leipzig$V_LAENGE)

# creating new distance groups
W_SrV2018_Leipzig <- W_SrV2018_Leipzig %>%
  mutate(V_LAENGE_namav = case_when(
    V_LAENGE >= 0 & V_LAENGE < 1 ~ 1,
    V_LAENGE >= 1 & V_LAENGE < 2 ~ 2,
    V_LAENGE >= 2 & V_LAENGE < 5 ~ 3,
    V_LAENGE >= 5 & V_LAENGE < 10 ~ 4,
    V_LAENGE >= 10 & V_LAENGE < 20 ~ 5,
    V_LAENGE >= 20 ~ 6,
    TRUE ~ -999
  )) %>% set_variable_labels(V_LAENGE_namav = "Distance Classes") %>% 
  set_value_labels(V_LAENGE_namav = c("0-1 km" = 1, 
                                      "1-2 km" = 2,
                                      "2-5 km" = 3,
                                      "5-10 km" = 4,
                                      "10-20 km" = 5,
                                      "20 km and more" = 6,
                                      "missing values" = -999))

unique(W_SrV2018_Leipzig$V_LAENGE_namav)
W_SrV2018_Leipzig %>% count(V_LAENGE_namav)

#check distance groups
W_SrV2018_Leipzig_Laenge <- W_SrV2018_Leipzig %>% select(V_LAENGE,V_LAENGE_namav)


#check distance groups and Main Transport Mode (NO FILTER FOR TRIP LENGTH)
W_SrV2018_Leipzig %>% 
  select(V_LAENGE_namav, E_HVM_namav) %>%
  filter(V_LAENGE_namav >=0, E_HVM_namav >=0) %>% 
  group_by(V_LAENGE_namav, E_HVM_namav) %>%
  summarise(n = n()) %>% 
  group_by(V_LAENGE_namav) %>% 
  mutate(Share = n / sum(n) * 100) %>% 
  ungroup() %>% 
  ggplot(aes(x = as_factor(V_LAENGE_namav), y = Share, fill = as_factor(E_HVM_namav))) +
  geom_bar(stat = "identity", position = "fill") +
  labs(
    title = "Modal Split by Distance",
    x  = "Distance Classes", 
    fill = "Transport Modes")+
  geom_text(aes(label = ifelse(Share < 3, "", paste0(round(Share), "%"))), 
            position = position_fill(vjust = 0.5)) +
  theme_light() +
  theme(text = element_text(size = 16))



#check distance groups and Main Transport Mode (FILTER FOR TRIP LENGTH <=100km)

#creating V_LAENGE_namav100 for filtering
W_SrV2018_Leipzig <- W_SrV2018_Leipzig %>% 
  mutate(V_LAENGE_namav100 = case_when(
    V_LAENGE >=0 & V_LAENGE < 100 ~ 1,
    TRUE ~ 0))

W_SrV2018_Leipzig %>% count(V_LAENGE_namav100)

W_SrV2018_Leipzig %>%
  filter(V_LAENGE_namav >=0, E_HVM_namav >=0,V_LAENGE_namav100 == 1) %>% 
  select(V_LAENGE_namav, E_HVM_namav) %>%
  group_by(V_LAENGE_namav, E_HVM_namav) %>%
  summarise(n = n()) %>% 
  group_by(V_LAENGE_namav) %>% 
  mutate(Share = n / sum(n) * 100) %>% 
  ungroup() %>% 
  ggplot(aes(x = as_factor(V_LAENGE_namav), y = Share, fill = as_factor(E_HVM_namav))) +
  geom_bar(stat = "identity", position = "fill") +
  labs(
    title = "Modal Split by Distance with filter: trip length <100km",
    x  = "Distance Classes", 
    fill = "Transport Modes")+
  geom_text(aes(label = ifelse(Share < 3, "", paste0(round(Share), "%"))), 
            position = position_fill(vjust = 0.5)) +
  theme_light() +
  theme(text = element_text(size = 16))

### 5.3 Analysis of Duration of Trips

#### 5.3.1 Average Trip Duration

glimpse(W_SrV2018_Leipzig$E_DAUER)

W_SrV2018_Leipzig %>% 
  filter(E_DAUER != -7 & E_WEG_GUELTIG == -1) %>% 
  summarise(Mean_Duration = weighted.mean(E_DAUER, wt = GEWICHT_W)) %>% 
  gt() %>% fmt_number(decimals = 2)


#### 5.3.2 Average Trip Duration by Age

W_SrV2018_Leipzig %>% 
  filter(E_ALTER_GRUP != -10 & E_DAUER != -7 & E_WEG_GUELTIG == -1) %>%  
  group_by(as_factor(E_ALTER_GRUP)) %>% 
  summarise(Mean_Duration = weighted.mean(E_DAUER, wt = GEWICHT_W)) %>% 
  gt() %>% fmt_number(decimals = 2)


### 5.4 Analysis of Trip Purpose

#### 5.4.1 Trip Purpose

W_SrV2018_Leipzig %>% count(V_ZWECK_namav)

W_SrV2018_Leipzig %>% 
  filter(V_ZWECK_namav != -10) %>%  
  count(as_factor(V_ZWECK_namav), wt = GEWICHT_W) %>% 
  mutate("Anteil in %" = round(n/sum(n)*100, 1)) %>% 
  # select(-n) %>% 
  gt() %>% fmt_number(decimals = 1)

#### 5.4.2 Trip Purpose by Age

W_SrV2018_Leipzig %>% 
  filter(E_ALTER_GRUP != -10 & V_ZWECK_namav != -10) %>%  
  mutate(V_ZWECK = as_factor(V_ZWECK_namav)) %>% 
  crosstab(E_ALTER_GRUP, V_ZWECK_namav, weight = GEWICHT_W, pct_type = "row", unwt_n = TRUE) %>% gt() %>% fmt_number(decimals = 1)




























