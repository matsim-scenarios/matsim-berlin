#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

from matsim.scenariogen.data import TripMode, run_create_ref_data


def person_filter(df):
    """ Default person filter for reference data. """
    df = df[df.reporting_day <= 4]
    df = df[df.location == "Berlin"]

    return df


def trip_filter(df):
    # Motorcycles are counted as cars
    df.loc[df.main_mode == TripMode.MOTORCYCLE, "main_mode"] = TripMode.CAR

    # Other mode are ignored in the total share
    df = df[df.main_mode != TripMode.OTHER]

    return df


if __name__ == "__main__":
    d = os.path.expanduser("~/Development/matsim-scenarios/shared-svn/projects/matsim-berlin/data/SrV/")

    person, trips, share = run_create_ref_data.create(d + "Berlin+Umland",
                                                      person_filter, trip_filter,
                                                      run_create_ref_data.InvalidHandling.REMOVE_TRIPS)

    print(share)
