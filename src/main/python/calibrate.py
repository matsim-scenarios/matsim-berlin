#!/usr/bin/env python
# -*- coding: utf-8 -*-

import calibration

# %%

modes = ["walk", "car", "pt", "bike"]
fixed_mode = "walk"
initial = {
    "bike": -2,
    "pt": 0,
    "car": 0,
    "ride": -4
}

# Original modal split
target = {
    "walk": 0.3026,
    "bike": 0.1883,
    "pt": 0.2437,
    "car": 0.2046,
    "ride": 0.0608
}

def f(persons):
    df = persons[persons.person.str.startswith("berlin")]
    print("Filtered %s persons" % len(df))
    return df

def filter_modes(df):
    return df[df.main_mode.isin(modes)]

study, obj = calibration.create_mode_share_study(
    "calib",
    "matsim-berlin-6.x-SNAPSHOT.jar",
    "../input/v6.0/berlin-v6.0-base-calib.config.xml",
    modes, target,
    initial_asc=initial,
    args="--10pct --config:TimeAllocationMutator.mutationRange=600",
    jvm_args="-Xmx55G -Xms55G -XX:+AlwaysPreTouch -XX:+UseParallelGC",
    lr=calibration.linear_lr_scheduler(start=0.3, interval=10),
    person_filter=f, map_trips=filter_modes,
    chain_runs=calibration.default_chain_scheduler
)

# %%

study.optimize(obj, 10)
