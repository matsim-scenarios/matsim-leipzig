library(tidyverse)
library(xml2)
library(readr)
library(matsim)
library(scales)
library(geomtextpath)

savePlotAsJPG <- function(name, plot = last_plot()){
  plotDir = "C:/Users/ACER/Desktop/Uni/VSP/NaMAV/data/Zaehldaten/Plots/"
  filename = paste0(plotDir, name, ".jpg")
  
  print(paste("Save plot to", filename))
  ggsave(filename = filename, plot = plot)
}

readMATSimLinkStats <- function(runId){
  
  OUTPUT_DIR = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/"
  LINKSTATS = ".linkstats.tsv"
  
  split = str_split(runId, pattern = "_")
  runNr = split[[1]][2]
  nr = str_remove(runNr ,pattern = "run")
  
  PATH = paste0(OUTPUT_DIR, nr, "/", runId, LINKSTATS)
  
  linkstats = read_csv(PATH)
  
  linkstats.1 = linkstats %>%
    group_by(linkId) %>%
    summarise_at(c("vol_car", "vol_bike"), sum)
  
  names = colnames(linkstats.1)
  newNames = character()
  
  for(name in names){
    
    if(str_detect(name, pattern = "vol_")){
      name = paste0(name, "_", runId)
    }
    
    newNames[length(newNames) + 1] = name
  }
  
  colnames(linkstats.1) = newNames
  
  linkstats.1
}

symlog_trans <- function(base = 10, thr = 1, scale = 1){
  trans <- function(x)
    ifelse(abs(x) < thr, x, sign(x) * 
             (thr + scale * suppressWarnings(log(sign(x) * x / thr, base))))
  
  inv <- function(x)
    ifelse(abs(x) < thr, x, sign(x) * 
             base^((sign(x) * x - thr) / scale) * thr)
  
  breaks <- function(x){
    sgn <- sign(x[which.max(abs(x))])
    if(all(abs(x) < thr))
      pretty_breaks()(x)
    else if(prod(x) >= 0){
      if(min(abs(x)) < thr)
        sgn * unique(c(pretty_breaks()(c(min(abs(x)), thr)),
                       log_breaks(base)(c(max(abs(x)), thr))))
      else
        sgn * log_breaks(base)(sgn * x)
    } else {
      if(min(abs(x)) < thr)
        unique(c(sgn * log_breaks()(c(max(abs(x)), thr)),
                 pretty_breaks()(c(sgn * thr, x[which.min(abs(x))]))))
      else
        unique(c(-log_breaks(base)(c(thr, -x[1])),
                 pretty_breaks()(c(-thr, thr)),
                 log_breaks(base)(c(thr, x[2]))))
    }
  }
  trans_new(paste("symlog", thr, base, scale, sep = "-"), trans, inv, breaks)
}

LINKSTATS = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/linkStats.tsv"
COUNTS = "output/leipzig-v1.1-counts_Pkw.xml"


#### IMPORT TRAFFIC COUNTS ####
counts.xml <- read_xml(COUNTS) 

station = xml_find_all(counts.xml, "//count") %>%
  xml2::xml_attrs() %>%
  purrr::map_df(~as.list(.)) %>%
  readr::type_convert()

volume = xml_find_all(counts.xml, "//volume") %>%
  xml2::xml_attrs() %>%
  purrr::map_df(~as.list(.)) %>%
  readr::type_convert()

counts.raw = bind_cols(station, volume)

rm(counts.xml, station, volume)


#### IMPORT LEIPZIG NETWORK ####

NETWORK = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/leipzig-v1.1-network-with-pt-drt.xml"

network = loadNetwork(NETWORK)
links = network$links

rm(network)

links.1 = links %>%
  select(id, type) 

#### IMPORT MATSIM COUNTS AND MERGE WITH TRAFFIC COUNTS####

linkstats.007 = readMATSimLinkStats(runId = "v1.2_run007")
linkstats.039 = readMATSimLinkStats(runId = "v1.0_run039")

counts.1 = left_join(x = counts.raw, y = linkstats.007, by = c("loc_id" = "linkId")) %>%
  left_join(linkstats.039, by = c("loc_id" = "linkId")) %>%
  left_join(links.1, by = c("loc_id" = "id"))

write_csv(counts.1, file = "C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/link-volumes-join.csv")

rm(counts.raw, counts.1, linkstats.1, linkstats.raw, links, links.1, linkstats.007, linkstats.039)


#### ANALYSIS  ####

join.raw = read_csv("C:/Users/ACER/Desktop/Uni/VSP/matsim-leipzig/output/link-volumes-join.csv")

join.na = join.raw %>%
  filter(is.na(type) | is.na(vol_car_v1.0_run039) | is.na(vol_car_v1.2_run007))

log = paste(nrow(join.na), "of", nrow(join.raw), "count stations are not correctly matched to matsim links")
print(log)

join.1 = join.raw %>%
  filter(!is.na(type)) %>%
  filter(val > 0) %>%
  select(loc_id, val, starts_with("vol_car_"),type) %>%
  mutate_at(c("vol_car_v1.2_run007", "vol_car_v1.0_run039"), function(x){ x * 4}) %>%
  mutate(type = str_remove(type, pattern = "highway."),
         type = factor(type, levels = c("motorway", "primary", "secondary", "tertiary", "residential", "unclassified", "motorway_link", "primary_link", "trunk_link"))) %>%
  rename("vol_car_traffic_counts" = "val") %>%
  select(-starts_with("vol_bike"))

suspicious = filter(join.1, str_detect(type, pattern = "_link"))
log = paste(nrow(suspicious), "matsim network links don't seem to be matched correctly")
print(log)
rm(log, suspicious, join.na)


#### Scatter Plot coloured according to road type ####

join.scatterplot = join.1 %>%
  pivot_longer(cols = c(vol_car_v1.0_run039, vol_car_v1.2_run007), names_to = "runId", values_to = "vol_sim", names_prefix = "vol_car_")

line.size <- 0.7

threshold <- 100

x <- seq(threshold, round(max(join.scatterplot$vol_car_traffic_counts), -2), 10)

upper.limit <- data.frame(x = x,
                          y = 1.2 * x)

lower.limit <- data.frame(x = x,
                          y = 0.8 * x)

middle.line <- data.frame(x = x,
                          y = x)

ggplot(join.scatterplot, aes(x = vol_car_traffic_counts, y = vol_sim, color = type)) +
  
  geom_point() +
  
  geom_line(data = middle.line, mapping = aes(x, y), size = line.size, linetype = "dashed", color = "grey60") +
  
  geom_line(data = upper.limit, mapping = aes(x, y), color = "black", size = line.size + 0.1) +
  
  geom_line(data = lower.limit, mapping = aes(x, y), color = "black", size = line.size + 0.1) +
  
  geom_vline(xintercept = threshold, linetype = "dashed") +
  
  geom_textvline(xintercept = threshold, label = "x = 100", linetype = "dashed", vjust = -0.3) +
  
  scale_x_continuous(trans = symlog_trans(thr = 100, scale = 1000), breaks = c(0, 300, 1000, 3000, 10000, 30000, 100000)) + 
  
  scale_y_continuous(trans = "symlog")+ 
  
#  scale_x_log10(limits = c(min(join.scatterplot$vol_car_traffic_counts), max(upper.limit$x))) +
  
#  scale_y_log10(limits = c(min(join.scatterplot$vol_sim), max(upper.limit$y))) +
  
  facet_wrap(.~ runId) +
  
  labs(x = "Daily traffic volume from Count Stations", y = "Daily traffic volume from MATSim Data", color = "Road type:") +
  
  theme_bw() +
  
  theme(legend.position = "bottom", panel.background = element_rect(fill = "grey90"),
        panel.grid = element_line(colour = "white"))


savePlotAsJPG(name = "Traffic_Counts_Scatter_Plot")
rm(join.scatterplot, lower.limit, upper.limit, middle.line)

#### Counts in each bin
options(scipen=999)
breaks = seq(0, 40000, 5000)
labels = character()

for(i in 1:length(breaks) - 1){
  
  label = paste0(breaks[i] / 1000, "k < ", breaks[i + 1] / 1000, "k")
  labels[i] = label
  rm(label)
}

#### data frame in long format for plotting
join.2 = join.1 %>%
  pivot_longer(cols = starts_with("vol_car"), names_to = "src", names_prefix = "vol_car_", values_to = "traffic_vol") %>%
  mutate(src = str_remove(src, pattern = "_vol"),
         traffic_bin = cut(traffic_vol, labels = labels, breaks = breaks, right = T)) %>%
  group_by(type, src, traffic_bin) %>%
  summarise(n = n()) %>%
  group_by(type, src) %>%
  mutate(share = n / sum(n)) %>%
  filter(!str_detect(type, pattern = "_link"))

ggplot(join.2, aes(x = traffic_bin, y = share, fill = type)) +
  
  geom_col() +
  
  facet_grid(src ~ type) +
  
  labs(x = "Daily traffic volume", y = "Share") +
  
  theme_bw() +
  
  theme(legend.position = "none", axis.text.x = element_text(angle = 90))

savePlotAsJPG(name = "Traffic_volume_distribution_by_road_type")

ggplot(filter(join.2, !type %in% c("residential", "unclassified")), aes(x = traffic_bin, y = share, fill = type)) +
  
  geom_col() +
  
  facet_grid(src ~ type) +
  
  labs(x = "Daily traffic volume", y = "Share") +
  
  theme_bw() +
  
  theme(legend.position = "none", axis.text.x = element_text(angle = 90))

savePlotAsJPG(name = "Traffic_volume_distribution_main_road_type")


#### create error plots ####

join.3 = join.2 %>%
  select(-share) %>% 
  pivot_wider(names_from = src, values_from = n, values_fill = 0.0) %>%
  filter(!is.na(traffic_bin)) %>%
  mutate(diff1 = v1.0_run039 - traffic_counts,
         diff2 = v1.2_run007 - traffic_counts,
         v1.0_run039 = diff1,
         v1.2_run007 = diff2) %>%
  select(-c(starts_with("diff"), traffic_counts))

join.3.1 = pivot_longer(join.3, cols = c(v1.0_run039, v1.2_run007), names_to = "src", values_to = "diff") %>%
  filter(!type %in% c("residential", "unclassified"))

ggplot(join.3.1, aes(x = traffic_bin, y = diff, fill = type)) +
  
  geom_col() +
  
  facet_grid(src ~ type, scales = "free") +
  
  labs(x = "Difference to Count Stations", y = "Daily traffic volume") +
  
  coord_flip() +
  
  theme_bw() +
  
  theme(legend.position = "none")

savePlotAsJPG(name = "Difference_plot_main_road_type")

rm(join.3.1)

#### categorized data ####

cat.labels = c("grossly underestimated", "slightly underestimated", "well estimated", "slightly overestimated", "grossly overestimated")
cat.breaks = c(0, .6, .8, 1.2, 1.4, Inf)

join.4 = join.1 %>%
  mutate(rel_vol_v1.2_run007 = vol_car_v1.2_run007 / vol_car_traffic_counts,
         rel_vol_v1.0_run039 = vol_car_v1.0_run039 / vol_car_traffic_counts) %>%
  select(- starts_with("vol_car_")) %>%
  pivot_longer(cols = c(rel_vol_v1.2_run007, rel_vol_v1.0_run039), names_prefix = "rel_vol_", names_to = "src", values_to = "rel_vol") %>%
  mutate(quality = cut(rel_vol, breaks = cat.breaks, labels = cat.labels))

mean.vol = mean(join.4$rel_vol, na.rm = T)
median.vol = median(join.4$rel_vol, na.rm = T)

join.4.1 = join.4 %>%
  group_by(type, src, quality) %>%
  summarise(n = n()) %>%
  mutate(share = n / sum(n)) %>%
  filter(!type %in% c("residential", "unclassified"))

ggplot(join.4.1, aes(quality, share, fill = type)) +
  
  geom_col() +
  
  labs(y = "Share", x = "Quality category") +
  
  facet_grid(src ~ type) +
  
  theme_bw() +
  
  theme(legend.position = "none", axis.text.x = element_text(angle = 90))

savePlotAsJPG(name = "Estimation_quality_by_road_type")

n = join.1 %>%
  group_by(type) %>%
  summarise(n = n()) %>%
  mutate(share = n / sum(n)) %>%
  select(type, share)

summary = join.1 %>%
  mutate(diff_v1.2_run007 = abs(vol_car_v1.2_run007 - vol_car_traffic_counts) / vol_car_traffic_counts,
         diff_v1.0_run039= abs(vol_car_v1.0_run039 - vol_car_traffic_counts) / vol_car_traffic_counts) %>%
  group_by(type) %>%
  summarise_at(c("diff_v1.2_run007", "diff_v1.0_run039"), mean, na.rm = T) %>%
  left_join(n, by = "type")

score = summary %>%
  mutate(weightet_score_v1.2_run007 = diff_v1.2_run007 * share,
         weightet_score_v1.0_run039 = diff_v1.0_run039 * share) %>%
  select(type, starts_with("weightet_score_")) %>%
  pivot_longer(cols = -type, names_to = "runId", values_to = "score_road_type", names_prefix = "weightet_score_") %>%
  group_by(runId) %>%
  summarise(score = sum(score_road_type))

print("Summary of mean deviation of traffic volume:")
print(summary)

est.breaks = c(-Inf, 0.8, 1.2, Inf)
est.labels = c("less", "exact", "more")

join.5 = join.4 %>%
  filter(! type %in% c("residential", "unclassified")) %>%
  arrange(rel_vol) %>%
  mutate(rel_vol_round = round(rel_vol, 2),
         estimation = cut(rel_vol_round, breaks = est.breaks, labels = est.labels)) %>%
  group_by(src, type, estimation) %>%
  summarise(n = n()) %>%
  mutate(share = n / sum(n)) %>%
  ungroup()

ggplot(join.5, aes(estimation, share, fill = type)) +
  
  geom_col() +
  
  labs(y = "Share", x = "Quality category") +
  
  facet_grid(src ~ type) +
  
  theme_bw() +
  
  theme(legend.position = "none", axis.text.x = element_text(angle = 90))

savePlotAsJPG(name = "Estimation_quality_2_by_road_type")

summary.2 = join.5 %>%
  group_by(src, estimation) %>%
  summarise(n = sum(n)) %>%
  mutate(share = n / sum(n)) %>%
  filter(estimation == "exact")

view(summary.2)

summary.3 = join.5 %>%
  filter(estimation == "exact") %>%
  select(-n)

view(summary.3)

rm(join.1, join.2, join.3, join.4, join.4.1, join.raw, n, score, summary)
