#!/usr/bin/env python
# -*- coding: utf-8 -*-

from matsim.calibration import create_calibration, ASCCalibrator, utils

# %%

modes = ["walk", "car", "pt", "bike", "ride"]
fixed_mode = "walk"
initial = {
    "bike": -2.14,
    "pt": -0.39,
    "car": -0.30,
    "ride": -1.20
}

# Original modal split
target = {
    "walk": 0.296769,
    "bike": 0.177878,
    "pt": 0.265073,
    "car": 0.200673,
    "ride": 0.059607
}


def filter_persons(persons):
    df = persons[persons.person.str.startswith("berlin")]
    print("Filtered %s persons" % len(df))
    return df


def filter_modes(df):
    return df[df.main_mode.isin(modes)]


study, obj = create_calibration(
    "calib",
    ASCCalibrator(modes, initial, target, lr=utils.linear_scheduler(start=0.3, interval=15)),
    "matsim-berlin-6.x-SNAPSHOT.jar",
    "../input/v6.1/berlin-v6.1.config.xml",
    args="--10pct",
    jvm_args="-Xmx60G -Xms60G -XX:+AlwaysPreTouch -XX:+UseParallelGC",
    transform_persons=filter_persons,
    transform_trips=filter_modes,
    chain_runs=utils.default_chain_scheduler, debug=False
)

# %%

study.optimize(obj, 5)
