#!/usr/bin/env python
# -*- coding: utf-8 -*-

from matsim.scenariogen.data import TripMode, run_create_ref_data


def person_filter(df):
    """ Default person filter for reference data. """
    return df[df.reporting_day <= 5]


def trip_filter(df):
    # Motorcycles are counted as cars
    df.loc[df.main_mode == TripMode.MOTORCYCLE, "main_mode"] = TripMode.CAR

    return df



if __name__ == "__main__":
    person, trips, share = run_create_ref_data.create("../../../../shared-svn/projects/NaMAV/data/SrV_2018",
                                                      person_filter, trip_filter,
                                                      run_create_ref_data.InvalidHandling.REMOVE_TRIPS)

    # TODO: double check

    print(share)
