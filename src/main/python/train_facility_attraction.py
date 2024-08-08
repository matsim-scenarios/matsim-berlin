#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

import geopandas as gpd
import numpy as np
import pandas as pd

from sklearn.model_selection import KFold

from matsim.scenariogen.ml import MLRegressor

if __name__ == "__main__":
    parser = argparse.ArgumentParser(prog="train_facility", description="Train facility model")
    parser.add_argument("--facilities", help="Path to facilities .gpkg",
                        default="../../../input/ref_facilities.gpkg")
    parser.add_argument("--visitations", help="Path to visitations file",
                        default="visitations.csv.gz")
    parser.add_argument("--mapping", help="Mapping of OSM ids to facilities",
                        default="../../../input/ref_facilities_mapping.csv.gz")

    args = parser.parse_args()

    visits = pd.read_csv(args.visitations)
    mapping = pd.read_csv(args.mapping)

    visits = pd.merge(visits, mapping, left_on="location", right_on="osm_id", how="inner", validate="m:1")

    # Aggregates all parent ids
    visits = visits.groupby(["parent_id", "purpose"]).agg(n=("n", "sum")).reset_index().rename(
        columns={"parent_id": "location"})

    visits.n = np.minimum(visits.n, visits.n.quantile(0.99))

    visits[visits.purpose == "other"].to_csv("visitations_mapped.csv", index=False)

    shp = gpd.read_file(args.facilities, layer="facilities", engine="pyogrio", read_geometry=False)

    for purpose in ("other", "work"):

        tf = visits[visits.purpose == purpose]

        df = pd.merge(shp, tf, left_on="osm_id", right_on="location", how="inner", validate="1:1")
        df["target"] = df.n / df.area

        # Drop outliers
        upper = df.target.quantile(0.95)
        df.target = np.minimum(df.target, upper)

        ml = MLRegressor(fold=KFold(n_splits=3, shuffle=True, random_state=0), bounds=(0, upper))

        ml.fit(df, "target", exclude=["purpose", "osm_id", "location", "n"])

        ml.write_java("../../../src/main/java", "org.matsim.prepare.facilities", "FacilityAttractionModel" + purpose.capitalize())
