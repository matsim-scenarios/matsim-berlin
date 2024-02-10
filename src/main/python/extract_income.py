#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import numpy as np
import os
from matsim.scenariogen.data import read_all

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Converter for survey data")

    parser.add_argument("-d", "--directory", default=os.path.expanduser(
        "~/Development/matsim-scenarios/shared-svn/projects/matsim-berlin/data/SrV/"))

    args = parser.parse_args()

    hh, persons, trips = read_all([args.directory + "Berlin+Umland", args.directory + "Brandenburg"])

    hh = hh[hh.income >= 0]

    # Large households are underrepresented and capped (same operation as in input)
    hh.n_persons = np.minimum(hh.n_persons, 5)

    groups = list(sorted(set(hh.income)))

    def calc(x):
        counts = x.groupby("income").size()
        prob = counts / sum(counts)
        return prob.to_frame().transpose()


    dist = hh.groupby(["economic_status"]).apply(calc).fillna(0).reset_index().drop(columns=["level_1"])

    print("Income groups:", groups)

    for t in dist[["economic_status"] + groups].itertuples():
        print('"%s", new double[]{%s},' % (t.economic_status, ", ".join("%.3f" % x for x in t[2:])))
