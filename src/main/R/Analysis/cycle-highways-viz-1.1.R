library(tidyverse)
library(ggokabeito)

base_mode_share <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/base-case-cycle-highways-25pct/analysis/analysis-cycle-highway/mode_share.csv")
speed15_mode_share <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/mode_share.csv")
speed25_mode_share <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_25-cycle-highways-25pct/analysis/analysis-cycle-highway/mode_share.csv")
speed1500_mode_share <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_1500-cycle-highways-25pct/analysis/analysis-cycle-highway/mode_share.csv")

df_mode_share <- base_mode_share %>% 
  full_join(speed15_mode_share, by=c("traveled_distance_group", "main_mode")) %>% 
  full_join(speed25_mode_share, by=c("traveled_distance_group", "main_mode")) %>% 
  full_join(speed1500_mode_share, by=c("traveled_distance_group", "main_mode")) %>% 
  rename(base = "share.x",
         speed15 = "share.y",
         speed25 = "share.x.x",
         speed1500 = "share.y.y")

aggr <- df_mode_share %>% 
  group_by(main_mode) %>% 
  summarise(base = sum(base),
            speed15 = sum(speed15),
            speed25 = sum(speed25),
            speed1500 = sum(speed1500))

df_long <- pivot_longer(aggr, cols=c("base", "speed15", "speed25", "speed1500")) %>% 
  mutate(name = factor(name, levels = c("base", "speed15", "speed25", "speed1500")))

# write.csv(df_long, file="C:/Users/Simon/Desktop/wd/2024-12-09/test.csv")

ggplot(df_long, aes(x = name, y = value, fill = main_mode)) +
  geom_bar(stat = "identity", color = "black") +
  geom_text(aes(label = round(value, 2)), position = position_stack(vjust = 0.5), size = 8) +
  # scale_fill_manual(values = mode_colors) +
  theme_minimal() +
  scale_color_okabe_ito() +
  scale_y_continuous(labels = NULL) +
  ylab("") +
  xlab("") +
  theme(legend.title = element_blank(),
        panel.spacing = unit(3, "lines")) +
  theme(
    axis.text = element_text(size = 14),
    legend.title = element_text(size = 14),
    legend.text = element_text(size = 12),
    strip.text = element_text(size = 14)
  )

############################################### income related plots ##########################################################
# base income distr for bike is the same for all cases, so take it from speed15 case
base_income_bike <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/bike_income_groups_base.csv") %>% 
  rename(base = "Count..trip_id.",
         share_base = "share") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))
speed15_income_bike <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/bike_income_groups.csv") %>% 
  rename(speed15 = "Count..trip_id.",
         share_speed15 = "share") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))
speed25_income_bike <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_25-cycle-highways-25pct/analysis/analysis-cycle-highway/bike_income_groups.csv") %>% 
  rename(speed25 = "Count..trip_id.",
         share_speed25 = "share") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))
speed1500_income_bike <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_1500-cycle-highways-25pct/analysis/analysis-cycle-highway/bike_income_groups.csv") %>% 
  rename(speed1500 = "Count..trip_id.",
         share_speed1500 = "share") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))


income_distr_bike <- merge(base_income_bike, speed15_income_bike, by="income_group") %>% 
  merge(speed25_income_bike, by="income_group") %>% 
  merge(speed1500_income_bike, by="income_group")

# income distr plots per case, single plots:
income_dfs_bike <- list(base_income_bike, speed15_income_bike, speed25_income_bike, speed1500_income_bike)
column_names <- c("base", "speed15", "speed25", "speed1500")

i <- 1

for (df in income_dfs_bike) {
 
  col_name <- column_names[i]
  
  bike_only_income_plot <- ggplot(df, aes(x = income_group, y = .data[[col_name]], fill = income_group)) +
    geom_bar(stat = "identity", position = "dodge") +
    labs(x = "Income Group",
         y = "Count") +
    theme_minimal() +
    theme(plot.title = element_text(hjust = 0.5),
          legend.position = "none",
          axis.text.x = element_text(angle = 90, vjust = 0.5, hjust=1)) +
    theme(
      plot.title = element_text(size = 23, hjust = 0.5),
      axis.title = element_text(size = 23),
      axis.text = element_text(size = 23),
      axis.text.x = element_text(angle = 90, hjust = 1),
      strip.text = element_text(size = 23)) +
    scale_x_discrete(labels = function(x) {
      # Replace one label while keeping others the same
      ifelse(x == "5600.0+", "5600+", x)
    }) +
    ylim(0,110000)
  print(bike_only_income_plot)
  i <- i + 1
}


# facet plot of bike income distr for all cases:
income_bike_long <- income_distr_bike %>%
  gather(key = "count_type", value = "count_value", base, speed15, speed25, speed1500) %>%
  mutate(count_type = factor(count_type, levels=c("base", "speed15", "speed25", "speed1500")))

ggplot(income_bike_long, aes(x = income_group, y = count_value, fill = income_group)) +
  geom_bar(stat = "identity", position = "dodge") +
  facet_wrap(~count_type) +
  labs(x = "Income Group",
       y = "Count") +
  theme_minimal() +
  theme(plot.title = element_text(hjust = 0.5),
        legend.position = "none",
        axis.text.x = element_text(angle = 90, vjust = 0.5, hjust=1)) +
  theme(
    plot.title = element_text(size = 13, hjust = 0.5),
    axis.title = element_text(size = 13),
    axis.text = element_text(size = 13),
    axis.text.x = element_text(angle = 90, hjust = 1),
    strip.text = element_text(size = 13)) +
  scale_x_discrete(labels = function(x) {
    # Replace one label while keeping others the same
    ifelse(x == "5600.0+", "5600+", x)
  }) + ylim(0,110000)
# +
#   theme(
#     axis.text.x = element_text(angle = 45, hjust = 1, vjust = 1, size = 16),
#     axis.title.x = element_text(margin = margin(t = 10)) # Increase margin above x-axis title
#   )


############################################## income group distribution of mode users in base case #####################################################
base_income_leipzig_overall <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/allModes_income_groups_base_leipzig.csv") %>% 
  rename(count = "Count..trip_id.") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))
base_income_leipzig_bike <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/bike_income_groups_base_leipzig.csv") %>% 
  rename(count = "Count..trip_id.") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))
base_income_leipzig_car <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/car_income_groups_base_leipzig.csv") %>% 
  rename(count = "Count..trip_id.") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))
base_income_leipzig_ride <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/ride_income_groups_base_leipzig.csv") %>% 
  rename(count = "Count..trip_id.") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))
base_income_leipzig_pt <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/pt_income_groups_base_leipzig.csv") %>% 
  rename(count = "Count..trip_id.") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))
base_income_leipzig_walk <- read.csv(file = "Y:/net/ils/meinhardt/leipzig-v1.1-cycle-highway-paper/output/SPEED_15-cycle-highways-25pct/analysis/analysis-cycle-highway/walk_income_groups_base_leipzig.csv") %>% 
  rename(count = "Count..trip_id.") %>% 
  mutate(income_group = factor(income_group, levels = c("0 - 500", "500 - 900", "900 - 1500", "1500 - 2000", "2000 - 2600", "2600 - 3000", "3000 - 3600", "3600 - 4600", "4600 - 5600", "5600.0+")))

# add dataframes to list to iterate through them
income_dfs_leipzig_base <- list(base_income_leipzig_overall, base_income_leipzig_bike, base_income_leipzig_car, base_income_leipzig_ride, base_income_leipzig_pt, base_income_leipzig_walk)

for (dataframe in income_dfs_leipzig_base) {
  bike_income_plot <- ggplot(dataframe, aes(x = income_group, y = share, fill = income_group)) +
                        geom_bar(stat = "identity", position = "dodge") +
                        labs(x = "Income Group",
                             y = "Count") +
                        theme_minimal() +
                        theme(plot.title = element_text(hjust = 0.5),
                              legend.position = "none",
                              axis.text.x = element_text(angle = 90, vjust = 0.5, hjust=1)) +
                        theme(
                          plot.title = element_text(size = 30, hjust = 0.5),
                          axis.title = element_text(size = 30),
                          axis.text = element_text(size = 30),
                          axis.text.x = element_text(angle = 90, hjust = 1),
                          strip.text = element_text(size = 30)) +
                        scale_x_discrete(labels = function(x) {
                          # Replace one label while keeping others the same
                          ifelse(x == "5600.0+", "5600+", x)
                        }) +
                        ylim(0,0.6)
  print(bike_income_plot)
}






