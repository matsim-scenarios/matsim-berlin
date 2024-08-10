#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pandas as pd

def home_work_relation(x):
    """ Searches for home and work location of a person. """

    home = pd.NA
    work = pd.NA

    for t in x.itertuples():

        # Collects the first part of the zone string, which is the general District (Bezirk)
        if t.type == "home" and "-" in t.zone and t.location == "Berlin":
            home, _, _ = t.zone.partition("-")
        elif t.type == "work" and "-" in t.zone and t.location == "Berlin":
            work, _, _ = t.zone.partition("-")

    return pd.Series(data={"home": home, "work": work, "n": x.a_weight.iloc[0]})


if __name__ == "__main__":
    df = pd.read_csv("table-activities.csv")
    df = df[df.type.isin(["work", "home"])]

    df.zone = df.zone.astype(str)

    aggr = df.groupby("p_id").apply(home_work_relation)
    aggr = aggr.dropna()

    aggr = aggr.groupby(["home", "work"]).count()

    aggr.to_csv("berlin-work-commuter.csv", columns=["n"], index=True)
