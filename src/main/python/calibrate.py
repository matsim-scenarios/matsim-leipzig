#!/usr/bin/env python
# -*- coding: utf-8 -*-

import calibration
import geopandas as gpd

# %%

modes = ["walk", "car", "ride", "pt", "bike"]
fixed_mode = "walk"
initial = {
    "bike": -0.56,
    "pt": 0.01,
    "car": -0.799,
    "ride": -1.30
}

# Target from SrV
target = {
    "walk": 0.2278,
    "bike": 0.2179,
    "pt": 0.1669,
    "car": 0.2966,
    "ride": 0.0908
}

city = gpd.read_file("../scenarios/input/leipzig-utm32n/leipzig-utm32n.shp")


def f(persons):
    persons = gpd.GeoDataFrame(persons, geometry=gpd.points_from_xy(persons.home_x, persons.home_y))

    df = gpd.sjoin(persons.set_crs("EPSG:25832"), city, how="inner", op="intersects")

    print("Filtered %s persons" % len(df))

    return df


def filter_modes(df):
    return df[df.main_mode.isin(modes)]


study, obj = calibration.create_mode_share_study("calib", "matsim-leipzig-1.2-SNAPSHOT-e85f5c7.jar",
                                                 "../input/v1.2/leipzig-v1.2-25pct.config.xml",
                                                 modes, target,
                                                 initial_asc=initial,
                                                 args="--10pct --parking-cost-area ../input/v1.2/parkingCostArea/Bewohnerparken_2020.shp --config:TimeAllocationMutator.mutationRange=900",
                                                 jvm_args="-Xmx46G -Xms46G -XX:+AlwaysPreTouch -XX:+UseParallelGC",
                                                 transform_persons=f, transform_trips=filter_modes,
                                                 lr=calibration.linear_lr_scheduler(start=0.3, interval=8),
                                                 chain_runs=calibration.default_chain_scheduler)

# %%

study.optimize(obj, 4)
