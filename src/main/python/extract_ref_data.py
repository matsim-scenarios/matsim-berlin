#!/usr/bin/env python
# -*- coding: utf-8 -*-

import numpy as np
import os
from matsim.scenariogen.data import EconomicStatus, TripMode, preparation, run_create_ref_data


def person_filter(df):
    """ Default person filter for reference data. """
    df = df[df.reporting_day <= 4]
    df = df[df.location == "Berlin"]

    df["age"] = preparation.cut(df.age, [0, 12, 18, 25, 35, 66, np.inf])

    preparation.fill(df, "economic_status", EconomicStatus.UNKNOWN)
    preparation.fill(df, "income", -1)

    df["income"] = preparation.cut(df.income, [0, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, 5600, np.inf])

    return df


def trip_filter(df):
    # Motorcycles are counted as cars
    df.loc[df.main_mode == TripMode.MOTORCYCLE, "main_mode"] = TripMode.CAR

    # Other mode are ignored in the total share
    df = df[df.main_mode != TripMode.OTHER]

    return df


if __name__ == "__main__":
    d = os.path.expanduser("~/Development/matsim-scenarios/shared-svn/projects/matsim-berlin/data/SrV/")

    result = run_create_ref_data.create(
        d + "Berlin+Umland",
        person_filter, trip_filter,
        run_create_ref_data.InvalidHandling.REMOVE_TRIPS,
        ref_groups=["age", "income", "economic_status", "employment", "car_avail", "bike_avail", "pt_abo_avail"]
    )

    print(result.share)

    print(result.groups)