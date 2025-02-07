library(tidyverse)

# read in agents who live in Lpz from base case sim run - mb we have to process the plans file first
pop_base_case_sim <- read.csv2(file="Y:/net/ils/matsim-leipzig/00-base-case/output_simwrapper/analysis/analysis-age-distribution/age-distribution.csv", sep="\t", header = TRUE, encoding = "UTF-8",stringsAsFactors = FALSE)

# read in statistics
# source : https://statistik.leipzig.de/statcity/table.aspx?cat=2&rub=2&obj=0
# TODO : put this onto shared-svn
stat_leipzig_2021 <- read.csv2(file="../../shared-svn/projects/NaMAV/data/Zaehldaten/Leipzig_Einwohnende_2021/Bevölkerungsbestand_Einwohner_nach_Alter_Leipzig.csv", sep=",", header = TRUE, encoding = "UTF-8",stringsAsFactors = FALSE)

# source : S.26 https://static.leipzig.de/fileadmin/mediendatenbank/leipzig-de/Stadt/02.1_Dez1_Allgemeine_Verwaltung/12_Statistik_und_Wahlen/Statistik/Statistisches_Jahrbuch_Leipzig_2022.pdf
stat_yearbook_leipzig_2021 <- read.csv(text="
'Altersgruppe,'Anzahl'
'unter 18',97655
'18-25',55099
'25-30',44960
'30-35',58706
'35-40',51720
'40-45',43333
'45-50',31410
'50-55',35591
'55-60',36986
'60-65',31890
'65-70',29420
'70-75',24995
'75-80',22909
'80-85',25761
'85 und aelter',19434", header=TRUE, sep=",")

stat_leipzig_2021 <- stat_leipzig_2021 %>%
  select(Kennziffer,X2021) %>%
  slice(1:(n()-16)) %>%
  pivot_wider(names_from = c(Kennziffer), values_from = c(X2021))

stat_leipzig_2021 <- stat_leipzig_2021 %>%
  mutate('<=18'=sum(stat_leipzig_2021$`unter 5 Jahre`, stat_leipzig_2021$`5 bis unter 10 Jahre`, stat_leipzig_2021$`10 bis unter 15 Jahre`, stat_leipzig_2021$`15 bis unter 20 Jahre`),
         '18-70'=sum(stat_leipzig_2021$`20 bis unter 25 Jahre`, stat_leipzig_2021$`25 bis unter 30 Jahre`, stat_leipzig_2021$`30 bis unter 35 Jahre`, stat_leipzig_2021$`35 bis unter 40 Jahre`,
                     stat_leipzig_2021$`40 bis unter 45 Jahre`, stat_leipzig_2021$`45 bis unter 50 Jahre`, stat_leipzig_2021$`50 bis unter 55 Jahre`, stat_leipzig_2021$`55 bis unter 60 Jahre`,
         stat_leipzig_2021$`60 bis unter 65 Jahre`, stat_leipzig_2021$`65 bis unter 70 Jahre`),
  '>70'=sum(stat_leipzig_2021$`70 bis unter 75 Jahre`, stat_leipzig_2021$`75 bis unter 80 Jahre`, stat_leipzig_2021$`80 bis unter 85 Jahre`, stat_leipzig_2021$`85 Jahre und aelter`),
         dataset="leipzig.de") %>%
    select('<=18','18-70','>70', dataset) %>%
    mutate(sum=select(., where(is.numeric)) %>% rowSums())

stat_yearbook_leipzig_2021 <- stat_yearbook_leipzig_2021 %>%
  pivot_wider(names_from = c('X.Altersgruppe'), values_from = c('X.Anzahl.'))

colnames(stat_yearbook_leipzig_2021)[1] <- '<=18'
colnames(pop_base_case_sim) <- c('<=18','18-70','>70','dataset')

pop_base_case_sim <- pop_base_case_sim %>%
  mutate(select(., where(is.numeric)) * 4) %>%
  mutate(sum=select(., where(is.numeric)) %>% rowSums())

stat_yearbook_leipzig_2021 <- stat_yearbook_leipzig_2021 %>%
  mutate('18-70'=sum(stat_yearbook_leipzig_2021$`'18-25'`, stat_yearbook_leipzig_2021$`'25-30'`,stat_yearbook_leipzig_2021$`'30-35'`,stat_yearbook_leipzig_2021$`'35-40'`,
                     stat_yearbook_leipzig_2021$`'40-45'`,stat_yearbook_leipzig_2021$`'45-50'`,stat_yearbook_leipzig_2021$`'50-55'`,stat_yearbook_leipzig_2021$`'55-60'`,
  stat_yearbook_leipzig_2021$`'60-65'`,stat_yearbook_leipzig_2021$`'65-70'`),
         '>70'=sum(stat_yearbook_leipzig_2021$`'70-75'`,stat_yearbook_leipzig_2021$`'75-80'`,stat_yearbook_leipzig_2021$`'80-85'`,stat_yearbook_leipzig_2021$`'85 und aelter'`),
         dataset='statYearbook') %>%
  select('<=18','18-70','>70',dataset) %>%
  mutate(sum=select(., where(is.numeric)) %>% rowSums())

allData <- union(stat_leipzig_2021, stat_yearbook_leipzig_2021)
allData <- union(allData, pop_base_case_sim)

write.csv2(allData, "Y:/net/ils/matsim-leipzig/00-base-case/output_simwrapper/analysis/analysis-age-distribution/age-distribution-comparison-2021.csv", quote = FALSE, row.names = FALSE)