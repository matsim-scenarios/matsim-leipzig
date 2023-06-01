#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

import geopandas as gpd
import pandas as pd
from matsim import calibration

# %%

if os.path.exists("srv.csv"):
    srv = pd.read_csv("srv.csv")
    sim = pd.read_csv("sim.csv")

    _, adj = calibration.calc_adjusted_mode_share(sim, srv)

    print(srv.groupby("mode").sum())

    print("Adjusted")
    print(adj.groupby("mode").sum())

    adj.to_csv("srv_adj.csv", index=False)

# %%

modes = ["walk", "car", "ride", "pt", "bike"]
fixed_mode = "walk"
initial = {
    "bike": -1.9,
    "pt": -0.7,
    "car": -1.4,
    "ride": -4
}

# Original target
target = {
    "walk": 0.243,
    "bike": 0.206,
    "pt": 0.162,
    "car": 0.301,
    "ride": 0.086
}

# Adjusted for distance distribution
# target = {
#    "bike": 0.205680,
#    "car":  0.321617,
#    "pt":   0.186261,
#    "ride": 0.093713,
#    "walk": 0.192729
# }

city = gpd.read_file("../scenarios/input/leipzig-utm32n/leipzig-utm32n.shp")
homes = pd.read_csv("../input/v1.2/leipzig-v1.2-homes.csv", dtype={"person": "str"})


def f(persons):
    persons = pd.merge(persons, homes, how="inner", left_on="person", right_on="person")
    persons = gpd.GeoDataFrame(persons, geometry=gpd.points_from_xy(persons.home_x, persons.home_y))

    df = gpd.sjoin(persons.set_crs("EPSG:25832"), city, how="inner", op="intersects")

    print("Filtered %s persons" % len(df))

    return df


def filter_modes(df):
    return df[df.main_mode.isin(modes)]


study, obj = calibration.create_mode_share_study("calib", "matsim-leipzig-1.2-SNAPSHOT-25ee44a.jar",
                                                 "../input/v1.2/leipzig-v1.2-25pct.config.xml",
                                                 modes, target,
                                                 initial_asc=initial,
                                                 args="--25pct --config:TimeAllocationMutator.mutationRange=900",
                                                 jvm_args="-Xmx46G -Xms46G -XX:+AlwaysPreTouch -XX:+UseParallelGC",
                                                 person_filter=f, map_trips=filter_modes,
                                                 chain_runs=calibration.default_chain_scheduler)

# %%

study.optimize(obj, 10)
