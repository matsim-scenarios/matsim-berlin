#!/usr/bin/env python

import argparse
import os

import pandas as pd


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert simulation results from detector files")

    parser.add_argument("--output", default="output", help="Folder containing the results")
    parser.add_argument("--result", required=True, help="Path to result csv")

    args = parser.parse_args()

    data = []

    for f in os.listdir(args.output):
        if not f.endswith(".csv"):
            continue

        df = pd.read_csv(os.path.join(args.output, f))
        data.append(df)

    df = pd.concat(data)

    if "scale" in df.columns:
        df = df.groupby(["edgeId", "laneId"]).agg(flow=("flow", "max"))
        df.to_csv(args.result, index=True)

    else:

        df.to_csv(args.result, index=False)