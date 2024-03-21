# Title     : Analysis of FLEXA request data
# Objective : get KPIs for drt demand calibration
# Created by: Simon
# Created on: 14.04.2022

library(lubridate)
library(tidyverse)
library(dplyr)
library(ggplot2)
library(plotly)
library(hrbrthemes)
library(geosphere)

#####################################################################
####################################################
### INPUT DEFINITIONS ###

# set working directory
setwd("/Users/luchengqi/Desktop/namav")

# read data
allRidesFileName = "Flexa_Rides_ServiceAreaSouthEast_2021"
allRides <- read.csv2(paste0(allRidesFileName,".csv"), stringsAsFactors = FALSE, header = TRUE, encoding = "UTF-8")

analyzedArea = unlist(str_split(allRidesFileName, "Flexa_Rides_"))[2]

# In the VIA data they differentiate between requested PU time and requested DO time. Only 450 requests do not have a requested PU time
# Therefore the rows will get joined (otherwise it will lead to errors)
allRides <- allRides %>%
  mutate(request_time = ymd_hms(request_time, tz="Europe/Berlin"),
         actual_departure_time = ymd_hms(actual_departure_time, tz="Europe/Berlin"),
         actual_arrival_time = ymd_hms(actual_arrival_time, tz="Europe/Berlin"),
         date_ride = date(actual_departure_time),
         weekday_ride = wday(date_ride, label = TRUE)) %>%
  filter(! is.na(actual_departure_time))

weekdayRides <- allRides %>%
  filter(weekday_ride != "Fr",
         weekday_ride != "Sa",
         weekday_ride != "So",
         weekday_ride != "Mo")

#most of the "down times" in plots/FLEXA_2021_rides.png can be explained through holidays (e.g. end of july - sept, end of oct)
#there was a lockdown at the beginning of 2021 though
#unfortunately the provided data on pandemic measures is only until mid of april 2021
#pandemic measures data from: https://www.corona-datenplattform.de/viz/ihph/viz-massnahmen-bl.html
#so far I haven't found a source for when the lockdown ended in 2021 - sm apr 22
covid19_lockdown <- interval(ymd("2021-01-01", tz="Europe/Berlin"), ymd("2021-04-30", tz="Europe/Berlin"))
winter_holiday <- interval(ymd("2021-01-31", tz="Europe/Berlin"), ymd("2021-02-06", tz="Europe/Berlin"))
easter_holiday <- interval(ymd("2021-03-27", tz="Europe/Berlin"), ymd("2021-04-10", tz="Europe/Berlin"))
summer_holiday <- interval(ymd("2021-07-26", tz="Europe/Berlin"), ymd("2021-09-03", tz="Europe/Berlin"))
autumn_holiday <- interval(ymd("2021-10-18", tz="Europe/Berlin"), ymd("2021-10-30", tz="Europe/Berlin"))
holiday_newyear <- interval(ymd("2021-01-01", tz="Europe/Berlin"), ymd("2021-01-01", tz="Europe/Berlin"))
holiday_tagderarbeit <- interval(ymd("2021-05-01", tz="Europe/Berlin"), ymd("2021-05-01", tz="Europe/Berlin"))
holiday_germanunion <- interval(ymd("2021-10-03", tz="Europe/Berlin"), ymd("2021-10-03", tz="Europe/Berlin"))
holiday_himmelfahrt <- interval(ymd("2021-05-13", tz="Europe/Berlin"), ymd("2021-05-13", tz="Europe/Berlin"))
holiday_pfingsten <- interval(ymd("2021-05-23", tz="Europe/Berlin"), ymd("2021-05-24", tz="Europe/Berlin"))
holiday_reformation <- interval(ymd("2021-10-31", tz="Europe/Berlin"), ymd("2021-10-31", tz="Europe/Berlin"))
holiday_bettag <- interval(ymd("2021-11-17", tz="Europe/Berlin"), ymd("2021-11-17", tz="Europe/Berlin"))
holidays_christmas <- interval(ymd("2021-12-23", tz="Europe/Berlin"), ymd("2022-01-01", tz="Europe/Berlin"))

ridesToConsider <- weekdayRides %>%
  filter(! date_ride %within% covid19_lockdown,
         ! date_ride %within% winter_holiday,
         ! date_ride %within% easter_holiday,
         ! date_ride %within% summer_holiday,
         ! date_ride %within% autumn_holiday,
         ! date_ride %within% holiday_newyear,
         ! date_ride %within% holiday_tagderarbeit,
         ! date_ride %within% holiday_germanunion,
         ! date_ride %within% holiday_himmelfahrt,
         ! date_ride %within% holiday_pfingsten,
         ! date_ride %within% holiday_reformation,
         ! date_ride %within% holiday_bettag,
         ! date_ride %within% holidays_christmas
  ) %>%
  mutate( travelTime_s = actual_arrival_time - actual_departure_time) %>%
  # The dataset appears to have one entry with actual_arrival_time < actual_departure_time, which produces a negative travelTime
  #It (Ride with actual_departure_time == 2021-11-10 14:56:58) therefore is excluded
  filter(travelTime_s > 0)

##########################################################################################################################################################
#calculate Distance on an ellipsoid (the geodesic) between the calculated start and end points of each tour
ridesToConsider <- ridesToConsider  %>%
  rowwise() %>%
  mutate(distance_m = as.double(distGeo(c(as.double(requested_origin_lon), as.double(requested_origin_lat)),
                                              c(as.double(requested_destination_lon), as.double(requested_destination_lat)))))

############################################################################################################################################################

j <- ridesToConsider %>%
  mutate(travelTime_s = seconds(travelTime_s))
hist(j$travelTime_s, plot = TRUE)
boxplot(j$travelTime_s)
avgTravelTime_s <- mean(ridesToConsider$travelTime_s)
avgTravelTime_s

hist(j$distance_m, plot = TRUE)
boxplot(j$distance_m)

avgDistance_m <- mean(ridesToConsider$distance_m)
avgDistance_m

#ridesLessThan10Seconds <- ridesToConsider %>%
#  filter(travelTime_s <= 180)

# there are 1025 rides below tt=120s and 246 rides below tt=60s out of 7542 considerable rides. For three minutes, this goes up to 3481 rides.
# so for a first version, we cut everyhing below 2 minutes
#we also have 53 rides with traveltime > 30min, which seems odd to me
#I'll cut them out, too for now

# below120s <- ridesToConsider %>%
#   filter(travelTime_s < 120)
# below60s <- ridesToConsider %>%
#   filter(travelTime_s < 60)
# below180s <- ridesToConsider %>%
#   filter(travelTime_s < 180)
# over1800s <- ridesToConsider %>%
#   filter(travelTime_s > 1800)
# over1000s <- ridesToConsider %>%
#   filter(travelTime_s > 1000)

ridesToConsider <- ridesToConsider %>%
  filter(travelTime_s >= 120)

#calculate avg travel time of all rides
j <- ridesToConsider %>%
  mutate(travelTime_s = seconds(travelTime_s)) %>%
  filter(travelTime_s < 1800)
avgTravelTime_s <- mean(j$travelTime_s)
avgTravelTime_s

avgDistanceOnlyTTFilter_m <- mean(j$distance_m)
avgDistanceOnlyTTFilter_m

hist(j$travelTime_s, main = paste("Histogram FLEXA Travel Time",analyzedArea), plot = TRUE)
boxplot(j$travelTime_s, main = paste("Boxplot FLEXA Travel Time",analyzedArea), ylab = "travel time [s]")
abline(h = avgTravelTime_s - 2 * sd(j$travelTime_s), col="red",lty=2)
abline(h = avgTravelTime_s + 2 * sd(j$travelTime_s), col="red",lty=2)

k <- ridesToConsider %>%
  filter(distance_m <= 4000)

avgDistance_m <- mean(k$distance_m)
avgDistance_m

hist(k$distance_m, main = paste("Histogram FLEXA Travel Distance",analyzedArea), plot = TRUE)
boxplot(k$distance_m, main = paste("Boxplot FLEXA Travel Distance",analyzedArea), ylab = "travel distance [m]")
abline(h = avgDistance_m - 2 * sd(k$distance_m), col="red",lty=2)
abline(h = avgDistance_m + 2 * sd(k$distance_m), col="red",lty=2)

############################################################################################################################################################

#calculate avg rides per day
ridesPerDay <- ridesToConsider %>%
  group_by(date_ride) %>%
  tally()


avgRides <- mean(ridesPerDay$n)
avgRides

avgValues <- setNames(data.frame(matrix(ncol = 3, nrow = 1)), c("avgRidesPerDay", "avgDistance[m]", "avgTravelTime[s]"))

avgValues$avgRidesPerDay <- avgRides
avgValues$`avgDistance[m]` <- avgDistance_m
avgValues$`avgTravelTime[s]` <- avgTravelTime_s

write.csv2(avgValues,paste("avg_params_flexa_",analyzedArea,".csv"),quote=FALSE, row.names=FALSE, dec=".")


##########################################################################################################################

# #a typical day here can be seen as a day with no of rides close to the average no of rides (119)
# typicalDays <- filter(ridesPerDay, between(n, avgRides - 3, avgRides + 3))
#
# #5 days are chosen as typical references
# typicalDay_jul <- ymd("2021-07-21")
# typicalDay_sep <- ymd("2021-09-15")
# typicalDay_oct <- ymd("2021-10-12")
# typicalDay_dec <- ymd("2021-12-01")
# typicalDay_jan <- ymd("2022-01-27")
#
# typicalDaysList <- list(typicalDay_jul, typicalDay_sep, typicalDay_oct, typicalDay_dec, typicalDay_jan)
#
# # this is so ugly and hard coded right now, as you have to change the day you want to plot
# #but a for loop for this just does not seem to work -sm apr22
# typicalDayRidesPerInterval <- ridesToConsider %>%
#   filter(date == typicalDay_jan) %>%
#   mutate (interval = floor( (minute(Actual.PU.time) + hour(Actual.PU.time) * 60) / 5)  )  %>%
#   group_by(interval) %>%
#   tally()
#
# p <- typicalDayRidesPerInterval %>%
#   ggplot( aes(x=interval*5/60, y=n)) +
#   ggtitle(paste("Fahrten pro 5-Minuten-Intervall (VIA): typischer Tag im ", month(typicalDay_jan, label=TRUE))) +
#   geom_area(fill="#69b3a2", alpha=0.5) +
#   geom_line(color="#69b3a2") +
#   ylab("Anzahl Fahrten") +
#   xlab("Stunde") +
#   theme_ipsum()
#
# plotFile = paste("typicalDays/KEXI_rides_VIA_", month(typicalDay_jan, label=TRUE), ".png")
# paste("printing plot to ", plotFile)
# png(plotFile, width = 1200, height = 800)
# p
# dev.off()
# ggplotly(p)
#
boxplot(ridesPerDay$n, main = paste("Boxplot FLEXA Rides per day",analyzedArea), ylab = "rides")
abline(h = avgRides - 2 * sd(ridesPerDay$n), col="red",lty=2)
abline(h = avgRides + 2 * sd(ridesPerDay$n), col="red",lty=2)


