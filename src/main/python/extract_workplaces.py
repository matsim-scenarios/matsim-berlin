#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pandas as pd
import geopandas as gpd

if __name__ == "__main__":

    # This script reads the IHK Berlin Gewerbedaten from github, aggregates and normalizes the number of workplaces to zones

    # Note that this data is not actually used in the model
    # The IHK data registers company locations, but not the actual work places
    # Large companies produce unrealistic high number of workplaces at one location, which is not the case in reality

    df = pd.read_csv("https://github.com/IHKBerlin/IHKBerlin_Gewerbedaten/blob/master/data/IHKBerlin_Gewerbedaten.csv?raw=true")
    df = gpd.GeoDataFrame(df, geometry=gpd.points_from_xy(df.longitude, df.latitude), crs="EPSG:4326").to_crs("EPSG:25833")

    shp = gpd.read_file("../../../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/shp/Planungsraum_EPSG_25833.shp").set_crs("EPSG:25833")

    df = gpd.sjoin(df, shp, op="within")

    df["employees_min"] = df.employees_range.str.extract(r"(\d+)").fillna(0).astype(int) + 1
    df["employees_max"] = df.employees_range.str.extract(r"- (\d+)").fillna(0).astype(int) + 1

    aggr = df.groupby("SCHLUESSEL").agg(min=("employees_min", "sum"), max=("employees_max", "sum")).reset_index().rename(columns={"SCHLUESSEL": "location"})

    aggr["employees"] = (aggr["min"] + aggr["max"]) / 2
    aggr["zone"] = aggr.location.str.slice(0, 2).astype(int)

    aggr.to_csv("workplaces.csv", columns=["location", "zone", "employees"], index=False)
