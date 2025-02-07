library(lubridate)
library(tidyverse)
library(dplyr)

# set working directory
setwd("C:/Users/Simon/Documents/shared-svn/projects/NaMAV/data/Flexa/")

# read data
FlexaData2021 <- read.csv2("Nachfragedaten_LVB_2021/Nachfragedaten_Flexa2021_anonymisiert/anonymisierteDatenNeu.csv", stringsAsFactors = FALSE, header = TRUE, sep=",",encoding = "UTF-8")

#filter
FlexaData2021 <- FlexaData2021 %>%
  select(request_time, actual_departure_time, actual_arrival_time, requested_origin_lat, requested_origin_lon, requested_destination_lat,
         requested_destination_lon, requested_origin_assigned_flexa_stop, requested_destination_assigned_flexa_stop, requested_origin_flexa_area,
         requested_destination_flexa_area, number_of_passengers)

write.csv2(FlexaData2021, "Nachfragedaten_LVB_2021/Nachfragedaten_Flexa_2021_anonymisiert_filtered.csv", quote = FALSE)

#filter only realized rides
#there are some 43 rides, which have an arrival time but do not have a departure time
#better filter them out
completedRides <- FlexaData2021 %>%
  filter(actual_arrival_time > 0) %>%
  filter(actual_departure_time > 0)


#Origin flexa zone: 951 NA values; destination flexa zone: 1019 NA values
#we know that Flexa rides are only possible inside of the same service area
#therefore missing origin area values are filled with the destination area values and vice versa - sm 05 22
noOriginZone <- completedRides %>%
  filter(requested_origin_flexa_area=="") %>%
  mutate(requested_origin_flexa_area = requested_destination_flexa_area)

noDestinationZone <- completedRides %>%
  filter(requested_destination_flexa_area=="") %>%
  mutate(requested_destination_flexa_area = requested_origin_flexa_area)

completedRides <- completedRides %>%
  filter(requested_origin_flexa_area!="") %>%
  filter(requested_destination_flexa_area!="") %>%
  bind_rows(noOriginZone, noDestinationZone)

#watch out for the right time zone handling here!
saturday_rides <- completedRides %>%
  mutate(actual_departure_time = ymd_hms(actual_departure_time, tz="Europe/Berlin")) %>%
  mutate(weekday_ride = wday(actual_departure_time, label = TRUE)) %>%
  filter(weekday_ride == "Sa")

sunday_rides <- completedRides %>%
  mutate(actual_departure_time = ymd_hms(actual_departure_time, tz="Europe/Berlin")) %>%
  mutate(weekday_ride = wday(actual_departure_time, label = TRUE)) %>%
  filter(weekday_ride == "So")

#for now: 2 service areas, 4 is north, 7 is southeast
flexaNorth_rides <- completedRides %>%
  filter(requested_destination_flexa_area == "4.0")

flexaSoutheast_rides <- completedRides %>%
  filter(requested_destination_flexa_area == "7.0")

#dump output
write.csv2(completedRides, "Flexa_Rides_allServiceAreas_2021.csv", quote = FALSE)
write.csv2(saturday_rides, "Flexa_Rides_Saturdays_2021.csv", quote = FALSE)
write.csv2(sunday_rides, "Flexa_Rides_Sundays_2021.csv", quote = FALSE)
write.csv2(flexaNorth_rides, "Flexa_Rides_ServiceAreaNorth_2021.csv", quote = FALSE)
write.csv2(flexaSoutheast_rides, "Flexa_Rides_ServiceAreaSouthEast_2021.csv", quote = FALSE)