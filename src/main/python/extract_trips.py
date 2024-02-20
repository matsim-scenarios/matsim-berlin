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

    # Duplication factor
    factor = 3

    index = trips.index.repeat(np.maximum(1, np.rint(trips.t_weight * factor)))
    df = trips.loc[index]

    df = df.drop(columns=["t_weight", "valid"])
    df.to_csv("trips-scaled.csv", index=False)