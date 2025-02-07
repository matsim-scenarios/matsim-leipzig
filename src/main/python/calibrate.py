#!/usr/bin/env python
# -*- coding: utf-8 -*-

import geopandas as gpd

from matsim.calibration import create_calibration, ASCCalibrator, utils


# %%

modes = ["walk", "car", "ride", "pt", "bike"]
fixed_mode = "walk"
initial = {
    "bike": 0,
    "pt": -0.4,
    "car": 0,
    "ride": -1.4
}

# Target from SrV
target = {
    "walk": 0.270794,
    "bike": 0.196723,
    "pt": 0.166204,
    "car": 0.286468,
    "ride": 0.079811
}

city = gpd.read_file("../scenarios/input/leipzig-utm32n/leipzig-utm32n.shp")


def f(persons):
    persons = gpd.GeoDataFrame(persons, geometry=gpd.points_from_xy(persons.home_x, persons.home_y))

    df = gpd.sjoin(persons.set_crs("EPSG:25832"), city, how="inner", op="intersects")

    print("Filtered %s persons" % len(df))

    return df


def filter_modes(df):
    return df[df.main_mode.isin(modes)]

study, obj = create_calibration(
    "calib",
    ASCCalibrator(modes, initial, target, lr=utils.linear_scheduler(start=0.3, interval=8)),
    "matsim-leipzig-1.2-SNAPSHOT-e85f5c7.jar",
    "../input/v1.3/leipzig-v1.3.1-10pct.config.xml",
    args="--10pct --parking-cost-area ../input/v1.3/parkingCostArea/Bewohnerparken_2020.shp --config:TimeAllocationMutator.mutationRange=900",
    jvm_args="-Xmx46G -Xms46G -XX:+AlwaysPreTouch -XX:+UseParallelGC",
    transform_persons=f,
    transform_trips=filter_modes,
    chain_runs=utils.default_chain_scheduler, debug=False
)

# %%

study.optimize(obj, 8)
