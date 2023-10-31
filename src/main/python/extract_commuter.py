#!/usr/bin/env python
# -*- coding: utf-8 -*-

from matsim.scenariogen.data import read_all
from matsim.scenariogen.data.preparation import calc_commute


if __name__ == "__main__":
    hh, persons, trips = read_all("../../../../shared-svn/projects/matsim-berlin/data/SrV/Berlin+Umland")

    berlin = hh[hh.location == "Berlin"]
    bb = hh[hh.location != "Berlin"]

    be_persons = persons[persons.hh_id.isin(berlin.index)]
    bb_persons = persons[persons.hh_id.isin(bb.index)]

    print("Berlin: %s" % len(be_persons))
    print("Brandenburg: %s" % len(bb_persons))

    berlin_work, berlin_edu = calc_commute(be_persons, trips)
    bb_work, bb_edu = calc_commute(bb_persons, trips)

    berlin_work.to_csv("berlin_work_commute.csv", index=False)
    berlin_edu.to_csv("berlin_edu_commute.csv", index=False)
    bb_work.to_csv("bb_work_commute.csv", index=False)
    bb_edu.to_csv("bb_edu_commute.csv", index=False)