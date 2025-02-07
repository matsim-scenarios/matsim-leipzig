library(tidyverse)
library(readr)
library(sf)
library(ssh)

FILES_DIR = "C:/Users/ACER/Desktop/Uni/VSP/Leipzig/FLEXA/"


####### DOWNLOAD FILES FROM CLUSTER #######
REMOTE_DIR = "zoerner@cluster-a.math.tu-berlin.de"

PROJECT_DIR <- "/net/ils/matsim-leipzig/run-drt/namav-output/runsScaledFleet3-2/"

pattern.trips <- ".output_trips.csv.gz"
pattern.legs <- ".output_legs.csv.gz"

#create ssh connection to math cluster
connection = ssh_connect(host = REMOTE_DIR)

files = capture.output(
  ssh_exec_wait(connection, 
                command = paste0(
                  "ls ", PROJECT_DIR))
)

files = files[str_detect(files, pattern = "Namav")]
print(files)

for(file in files){
  target.dir <- capture.output(
    ssh_exec_wait(connection, 
                  command = paste0(
                    "ls ", PROJECT_DIR, "/", file))
  )
  
  target.files <- target.dir[str_detect(target.dir, pattern = pattern.trips) | str_detect(target.dir, pattern = pattern.legs)]
  
  for(t in target.files) scp_download(connection, files = paste0(PROJECT_DIR, "/", file, "/", t), to = FILES_DIR)
}

ssh_disconnect(connection)
rm(connection, file, files, pattern.legs, t, target.dir, target.files, PROJECT_DIR, REMOTE_DIR)


####### ANALYSIS #######

TRIP_FILES <- list.files(path = FILES_DIR, pattern = pattern.trips)
trips.compare <- tibble(main_mode = character(),
                        main_mode_ger = character(),
                        n = numeric(),
                        share = double(),
                        scenario = character())

intermodal.drt.trips = NULL

for(file in TRIP_FILES){
  
  split.1 <- unlist(str_split(file, pattern = "case"))[2]
  scenario <- unlist(str_split(split.1, pattern = "[.]"))[1]
  
  TRIPS <- paste0(FILES_DIR, "/", file)
  
  trips <- read_csv2(TRIPS)
  
  trips.drt <- trips %>%
    filter(str_detect(string = modes, pattern = "drt"))
  
  trips.intermodal <- trips.drt %>%
    filter(main_mode != "drt") %>%
    mutate(scenario = scenario)
  
  if(is.null(intermodal.drt.trips)){
    intermodal.drt.trips = trips.intermodal
  } else {
    
    intermodal.drt.trips = bind_rows(intermodal.drt.trips, trips.intermodal)
  }
  
  drt.sum <- trips.drt %>%
    group_by(main_mode) %>%
    summarise(n = n()) %>%
    ungroup() %>%
    mutate(share = n / sum(n),
           share = round(share, 3)) %>%
    mutate(main_mode_ger = ifelse(main_mode == "drt", "DRT", "ÖPNV"),
           scenario = scenario)
  
  write_csv2(x = trips.intermodal, file = paste0(FILES_DIR, "/", scenario, ".filtered_drt_trips.csv.gz"))
  
  trips.compare = bind_rows(trips.compare, drt.sum)
}

write_csv2(x = intermodal.drt.trips, file = paste0(FILES_DIR, "/", scenario, ".filtered_drt_trips_all_scenarios.csv.gz"))
rm(drt.sum, trips.drt, trips)

ggplot(data = trips.compare, aes(main_mode_ger, share, fill = main_mode)) +
  
  geom_col(color = "black") +
  
  geom_text(aes(label = paste("n =", n)), vjust = -1, size = 3.5) +
  
  facet_wrap(scenario ~.) +
  
  coord_cartesian(ylim = c(0, 1.1)) +
  
  scale_y_continuous(breaks = seq(0, 1, 0.2)) +
  
  labs(x = "Hauptverkehrsmittel", y = "Anteil", fill = "Hauptverkehrsmittel", title = "Hauptverkehrsmittelwahl unter Nutztung von FLEXA") +
  
  theme_bw() +
  
  theme(legend.position = "none")

ggsave(plot = last_plot(), filename = paste0(FILES_DIR, "/", "Hauptverkehrsmittelwahl_FLEXA.jpg"))
#rm(file, pattern.trips, split.1, TRIP_FILES, TRIPS)