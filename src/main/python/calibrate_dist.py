#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pandas as pd

from matsim.calibration import create_calibration, ASCDistCalibrator, constraints, utils

# %%

modes = ["walk", "car", "pt", "bike", "ride"]
fixed_mode = "walk"
initial = {
    "bike": -2.23,
    "pt": -0.25,
    "car": -0.62,
    "ride": -1.37
}

target = pd.read_csv("mode_share_ref.csv")


def filter_persons(persons):
    df = persons[persons.person.str.startswith("berlin")]
    print("Filtered %s persons" % len(df))
    return df


def filter_modes(df):
    return df[df.main_mode.isin(modes)]


study, obj = create_calibration(
    "calib",
    ASCDistCalibrator(modes, initial, target, lr=utils.linear_scheduler(start=0.3, interval=15),
                      fixed_mode_dist="car", constraints=dict(ride=constraints.negative, walk=constraints.zero)),
    "matsim-berlin-6.1-SNAPSHOT.jar",
    "../../../input/v6.1/berlin-v6.1.config.xml",
    args="--1pct --iterations 0",
    jvm_args="-Xmx20G -Xms20G -XX:+AlwaysPreTouch -XX:+UseParallelGC",
    transform_persons=filter_persons,
    transform_trips=filter_modes,
    chain_runs=utils.default_chain_scheduler, debug=True
)

# %%

study.optimize(obj, 10)