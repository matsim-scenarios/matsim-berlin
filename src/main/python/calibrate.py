#!/usr/bin/env python
# -*- coding: utf-8 -*-

from matsim.calibration import create_calibration, ASCCalibrator, utils

# %%

modes = ["walk", "car", "pt", "bike", "ride"]
fixed_mode = "walk"
initial = {
    "bike": -1.208608,
    "pt": -1.118097,
    "car": -1.857432,
    "ride": -5.1
}

# modal split according to SrV 2018 (shared-svn/projects/matsim-berlin/data/SrV)
# Also see https://www.berlin.de/sen/uvk/presse/pressemitteilungen/2020/pressemitteilung.906382.php
# The calibration only considers persons living in Berlin
target = {
    "walk": 0.296769,
    "bike": 0.177878,
    "pt": 0.265073,
    "car": 0.200673,
    "ride": 0.059607
}

# Path to additional (mode choice) parameters, usually contains the estimated parameters from choice model
# E.g see input/v7.0/mode_params_estimated.yaml
base_params = "./mode_params_estimated.yaml"

def filter_persons(persons):
    df = persons[persons.person.str.startswith("berlin")]
    print("Filtered %s persons" % len(df))
    return df


def filter_modes(df):
    return df[df.main_mode.isin(modes)]


study, obj = create_calibration(
    "calib",
    ASCCalibrator(modes, initial, target, lr=utils.linear_scheduler(start=0.8, end=1.4, interval=15)),
    "matsim-berlin-7.0-SNAPSHOT-v6.4-59-g22f4db6.jar",
    "../input/v7.0/berlin-v7.0.config.xml",
    args="--10pct --iterations 500 --plan-selector BestScore --config:simwrapper.exclude=NoiseDashboard --config:vehicles.vehiclesFile=../v7.0/berlin-v7.0-vehicleTypes.xml",
    jvm_args="-Xmx60G -Xms60G -XX:+AlwaysPreTouch -XX:+UseParallelGC",
    transform_persons=filter_persons,
    transform_trips=filter_modes,
    base_params=base_params,
    chain_runs=utils.default_chain_scheduler, debug=False
)

# %%

study.optimize(obj, 2)
