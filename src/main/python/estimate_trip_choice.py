#!/usr/bin/env python
# -*- coding: utf-8 -*-

from collections import defaultdict

import argparse
import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
from biogeme.expressions import Beta

import numpy as np
import pandas as pd
from scipy.special import softmax

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate the trip choice model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../trip-choices.csv")

    args = parser.parse_args()

    df = pd.read_csv(args.input)

    df.drop(columns=["p_id"], inplace=True)

    # Convert all the columns to numeric
    df = df * 1

    modes = list(df.columns.str.extract(r"([a-zA-z]+)_valid", expand=False).dropna().unique())
    print("Modes: ", modes)
    print("Number of choices: ", len(df))

    database = db.Database("data/choices", df)
    v = database.variables

    database.remove(v["choice"] == 0)

    fixed_costs = defaultdict(lambda: 0.0)
    km_costs = defaultdict(lambda: 0.0, car=-0.149, ride=-0.149)

    ASC = {}
    for mode in modes:
        # Base asc
        ASC[mode] = Beta(f"ASC_{mode}", 0, None, None, 1 if mode == "walk" else 0)

    U = {}
    AV = {}

    # B_TIME = Beta('B_TIME', 0, None, None, 0)
    B_TIME = -6.88

    # TODO: use person id for mixed logit

    for i, mode in enumerate(modes, 1):

        u = ASC[mode] + B_TIME * v[f"{mode}_hours"] + (fixed_costs[mode] + km_costs[mode] * v[f"{mode}_km"])

        if mode != "walk":
            slope = Beta(f"B_{mode}_DIST", 0, None, None, 0)
            u += slope * v[f"{mode}_km"]

        U[i] = u
        AV[i] = v[f"{mode}_valid"]

    logprob = models.loglogit(U, AV, v["choice"])

    biogeme = bio.BIOGEME(database, logprob)

    biogeme.modelName = "trip_choice"

    results = biogeme.estimate()
    print(results.short_summary())

    pandas_results = results.getEstimatedParameters()
    print(pandas_results)

    sim_results = biogeme.simulate(results.getBetaValues())

    # print(sim_results)
    #
    # print("Given shares")
    #
    # # TODO: might not respect the availability of modes
    #
    # dist = df.groupby("choice").count()["seq"]
    # print(dist / dist.sum())
    #
    # print("Simulated shares")
    # choices = softmax(sim_results, axis=1)
    # print(np.mean(choices, axis=0))
