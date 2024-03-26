#!/usr/bin/env python
# -*- coding: utf-8 -*-

import numpy as np
from matsim.scenariogen.data import read_all

from extract_ref_data import trip_filter

if __name__ == "__main__":
    hh, persons, trips = read_all("../../../../shared-svn/projects/matsim-berlin/data/SrV/Berlin+Umland")

    trips = trip_filter(trips)
    trips = trips[trips.valid]
    trips = trips[(~trips.from_zone.isna()) & (~trips.to_zone.isna())]
    trips = trips[(~trips.from_location.isna()) & (~trips.to_location.isna())]

    trips_hh = trips.merge(hh, left_on="hh_id", right_index=True)

    trips = trips[trips_hh.location == "Berlin"]

    # Duplication factor
    factor = 3

    repeats = np.maximum(1, np.rint(trips.t_weight * factor)).to_numpy(int)
    index = trips.index.repeat(repeats)
    df = trips.loc[index]

    # Each sample has a unique sequence number
    seq = np.zeros(len(df), dtype=int)

    i = 0
    for r in repeats:
        for s in range(r):
            seq[i] = s
            i += 1

    df["seq"] = seq

    df = df.drop(columns=["t_weight", "valid"])

    df_p = df.merge(persons, left_on="p_id", right_index=True)
    df["p_age"] = df_p["age"]

    df_hh = df.merge(hh, left_on="hh_id", right_index=True)
    df["hh_cars"] = df_hh["n_cars"]

    df = df.sort_values(["p_id", "seq", "n"])
    df.to_csv("trips-scaled.csv", index=False)
