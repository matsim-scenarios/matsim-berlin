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

    # Only keep way entities, which are the ones that can be visited in the ref data
    mapping = mapping[mapping["type"] == "way"]

    mapped_visits = pd.merge(mapping, visits, left_on="osm_id", right_on="location", how="inner", validate="1:m")
    unmapped = mapping[~mapping.osm_id.isin(mapped_visits.osm_id)]

    # Only keep non duplicated member ids
    unmapped = unmapped[~unmapped.member_id.duplicated(keep='first')]

    # Maps visits of members ids
    mapped_members = pd.merge(unmapped, visits, left_on="member_id", right_on="location", how="inner", validate="1:m")
    mapped_members = mapped_members.groupby(["osm_id", "purpose"]).agg(n=("n", "sum"))

    mapped_visits = pd.concat([mapped_visits, mapped_members.reset_index()])

    # Aggregates all parent ids
    mapped_visits = mapped_visits.groupby(["osm_id", "purpose"]).agg(n=("n", "sum")).reset_index().rename(
        columns={"osm_id": "location"})

    mapped_visits[mapped_visits.purpose == "other"].to_csv("visitations_mapped.csv", index=False)

    shp = gpd.read_file(args.facilities, layer="facilities", engine="pyogrio", read_geometry=False)
    shp = shp[shp.osm_type == "way"]

    for purpose in ("other", "work"):

        tf = mapped_visits[mapped_visits.purpose == purpose]

        df = pd.merge(shp, tf, left_on="osm_id", right_on="location", how="inner", validate="1:1")
        df["target"] = df.n / df.area

        # Drop outliers
        upper = df.target.quantile(0.95)
        df.target = np.minimum(df.target, upper)

        ml = MLRegressor(fold=KFold(n_splits=3, shuffle=True, random_state=0), bounds=(0, upper))

        ml.fit(df, "target", exclude=["purpose", "osm_id", "osm_type", "location", "n"])

        ml.write_java("../../../src/main/java", "org.matsim.prepare.facilities", "FacilityAttractionModel" + purpose.capitalize())
