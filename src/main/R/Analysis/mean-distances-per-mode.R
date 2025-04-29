library(tidyverse)

trips_base <- read_delim("Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/base-case-cycle-highways-25pct/base-case-cycle-highways-25pct.output_trips.csv.gz",
                         locale = locale(decimal_mark = "."),
                         n_max = Inf,
                         col_types = cols(
                           start_x = col_character(),
                           start_y = col_character(),
                           end_x = col_character(),
                           end_y = col_character(),
                           end_link = col_character(),
                           start_link = col_character()
                         ))

mean_dist_car <- trips_base %>%
  filter(main_mode == "car") %>%
  summarize(mean_distance = mean(euclidean_distance)) %>%
  pull(mean_distance)

mean_dist_bike <- trips_base %>%
  filter(main_mode == "bike") %>%
  summarize(mean_distance = mean(euclidean_distance)) %>%
  pull(mean_distance)

mean_dist_pt <- trips_base %>%
  filter(main_mode == "pt") %>%
  summarize(mean_distance = mean(euclidean_distance)) %>%
  pull(mean_distance)

mean_dist_ride <- trips_base %>%
  filter(main_mode == "ride") %>%
  summarize(mean_distance = mean(euclidean_distance)) %>%
  pull(mean_distance)

mean_dist_walk <- trips_base %>%
  filter(main_mode == "walk") %>%
  summarize(mean_distance = mean(euclidean_distance)) %>%
  pull(mean_distance)
