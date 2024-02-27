#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
from collections import defaultdict

import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
import pandas as pd
from biogeme.expressions import Beta, bioDraws, log, MonteCarlo

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate choice model for daily trip usage")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../plan-choices.csv")

    args = parser.parse_args()

    df = pd.read_csv(args.input)

    df.drop(columns=["person"], inplace=True)

    # TODO: for testing only
    df = df.sample(frac=0.1)

    # Convert all the columns to numeric
    df = df * 1

    k = df.columns.str.extract(r"plan_(\d+)", expand=False).dropna().to_numpy(int).max()

    print("Number of choices: ", len(df))
    print("Number of plans per choice: ", k)

    modes = list(df.columns.str.extract(r"_([a-zA-z]+)_usage", expand=False).dropna().unique())
    print("Modes: ", modes)

    database = db.Database("data/plan-choices", df)
    v = database.variables

    ASC = {}
    for mode in modes:
        # Base asc
        asc = Beta(f"ASC_{mode}", 0, None, None, 1 if mode == "walk" else 0)

        # Pt does not have its own random parameter
        if True or mode == "walk" or mode == "pt":
            ASC[mode] = asc
        else:
            # The random parameter
            asc_s = Beta(f"ASC_{mode}_s", 1, None, None, 0)
            ASC[mode] = asc + asc_s * bioDraws(f"ASC_{mode}_RND", "NORMAL_ANTI")

    # TODO: distance specific utility parameters

    # B_TIME = Beta('B_TIME', 0, None, None, 0)
    B_COST = Beta('B_COST', 0.5, 0, 0, 0)

    U = {}
    AV = {}

    # Cost parameter from current berlin model

    # fixed_costs = defaultdict(lambda: 0.0, car=-14.13, pt=-3)
    fixed_costs = defaultdict(lambda: 0.0, car=-3, pt=-3)
    km_costs = defaultdict(lambda: 0.0, car=-0.149, ride=-0.149)

    time_cost = -6.88

    for i in range(1, k + 1):
        u = 0
        for mode in modes:
            # Is 1 if a mode was used once
            FIXED_COSTS = database.DefineVariable(f"plan_{i}_{mode}_fixed_cost",
                                                  fixed_costs[mode] * (v[f"plan_{i}_{mode}_usage"] > 0))
            KM_COSTS = database.DefineVariable(f"plan_{i}_{mode}_km_cost", km_costs[mode] * v[f"plan_{i}_{mode}_km"])

            u += ASC[mode] * v[f"plan_{i}_{mode}_usage"]
            u += time_cost * v[f"plan_{i}_{mode}_hours"]

            # u += FIXED_COSTS * B_COST * bioDraws('B_TIME_RND', 'NORMAL_ANTI')
            u += FIXED_COSTS
            u += KM_COSTS

            if mode == "ride":
                u += time_cost * v[f"plan_{i}_{mode}_hours"]

            if mode != "walk":
                slope = Beta(f"B_{mode}_DIST", 0, None, None, 0)
                u += slope * v[f"plan_{i}_{mode}_km"]

        U[i] = u
        AV[i] = v[f"plan_{i}_valid"]

    # prob = models.logit(U, AV, v["choice"])
    # logprob = log(MonteCarlo(prob))

    logprob = models.loglogit(U, AV, v["choice"])

    biogeme = bio.BIOGEME(database, logprob)

    biogeme.modelName = "plan_choice"
    biogeme.generate_pickle = False

    results = biogeme.estimate()
    print(results.short_summary())

    pandas_results = results.getEstimatedParameters()
    print(pandas_results)
