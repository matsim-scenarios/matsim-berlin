#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import argparse
import numpy as np

from matsim.scenariogen.data import TripMode, read_all
from matsim.scenariogen.data.preparation import fill, compute_economic_status, prepare_persons, create_activities

if __name__ == "__main__":

    parser = argparse.ArgumentParser(description="Converter for survey data")

    parser.add_argument("-d", "--directory", default=os.path.expanduser(
        "~/Development/matsim-scenarios/shared-svn/projects/matsim-berlin/data/SrV/"))
    parser.add_argument("--regiostar", default=os.path.expanduser(
        "~/Development/matsim-scenarios/shared-svn/projects/matsim-germany/zuordnung_plz_regiostar.csv"))

    parser.add_argument("--output", default="table", help="Output prefix")

    args = parser.parse_args()

    hh, persons, trips = read_all([args.directory + "Berlin+Umland", args.directory + "Brandenburg"],
                                      regio=args.regiostar)

    # Motorcycles are counted as cars
    trips.loc[trips.main_mode == TripMode.MOTORCYCLE, "main_mode"] = TripMode.CAR

    # Impute missing values
    fill(hh, "income", -1)
    compute_economic_status(hh)

    hh["income"] = hh.income / hh.equivalent_size

    hh.to_csv(args.output + "-households.csv")
    trips.to_csv(args.output + "-trips.csv")
    persons.to_csv(args.output + "-unscaled-persons.csv")

    print("Written survey csvs")

    df = prepare_persons(hh, persons, trips, augment=5, core_weekday=True, remove_with_invalid_trips=True)

    wm = lambda x: np.average(x, weights=df.loc[x.index, "p_weight"])

    df.to_csv(args.output + "-persons.csv", index_label="idx")
    print("Created %d synthetics persons" % len(df))

    berlin = df[df.region_type == 1]
    berlin["district"] = berlin.zone.str.split("-", n=1, expand=True)[0]
    berlin.groupby("district").agg(mean_income=("income", wm)).to_csv(args.output + "-income.csv")

    activities = create_activities(df, trips, include_person_context=False, cut_groups=False)
    activities.to_csv(args.output + "-activities.csv", index=False)