#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
import pandas as pd
from biogeme.expressions import Beta, bioDraws, log, MonteCarlo

from prepare import calc_plan_variables, tn_generator

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate choice model for daily trip usage")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../plan-choices.csv")

    args = parser.parse_args()

    df = pd.read_csv(args.input)

    df.drop(columns=["person"], inplace=True)

    # TODO: for testing only
    df = df.sample(frac=0.2)

    # Convert all the columns to numeric
    df = df * 1

    k = df.columns.str.extract(r"plan_(\d+)", expand=False).dropna().to_numpy(int).max()

    print("Number of choices: ", len(df))
    print("Number of plans per choice: ", k)

    modes = list(df.columns.str.extract(r"_([a-zA-z]+)_usage", expand=False).dropna().unique())
    print("Modes: ", modes)

    df = calc_plan_variables(df, k, modes)

    database = db.Database("data/plan-choices", df)
    v = database.variables

    database.setRandomNumberGenerators({
        "TN": (tn_generator, "truncated normal generator for mixed logit")
    })

    mixed_logit = True

    ASC = {}
    for mode in modes:
        # Base asc
        asc = Beta(f"ASC_{mode}", 0, None, None, 1 if mode in ("walk", "car") else 0)

        # Pt does not have its own random parameter
        if True or mode == "walk" or mode == "pt":
            ASC[mode] = asc
        else:
            # The random parameter
            asc_s = Beta(f"ASC_{mode}_s", 1, None, None, 0)
            ASC[mode] = asc + asc_s * bioDraws(f"ASC_{mode}_RND", "NORMAL_ANTI")

    B_UTIL = Beta('B_CAR_UTIL', 10, 0, 20, 0)
    B_UTIL_S = Beta('B_CAR_UTIL_SD', 1, 0, 10, 0)

    B_TIME_RND = B_UTIL + B_UTIL_S * bioDraws('B_CAR_UTIL_RND', 'TN')

    U = {}
    AV = {}

    for i in range(1, k + 1):
        u = v[f"plan_{i}_car_fixed_cost"]
        u += v[f"plan_{i}_costs"]
        u -= v[f"plan_{i}_pt_n_switches"]

        for mode in modes:
            u += ASC[mode] * v[f"plan_{i}_{mode}_usage"]

        if mixed_logit:
            u += v[f"plan_{i}_car_used"] * B_TIME_RND

        U[i] = u
        AV[i] = v[f"plan_{i}_valid"]

    if mixed_logit:
        prob = models.logit(U, AV, v["choice"])
        logprob = log(MonteCarlo(prob))
    else:
        logprob = models.loglogit(U, AV, v["choice"])

    biogeme = bio.BIOGEME(database, logprob)

    biogeme.calculateNullLoglikelihood(AV)
    biogeme.modelName = "plan_choice"
    biogeme.generate_pickle = False

    results = biogeme.estimate()
    print(results.short_summary())

    pandas_results = results.getEstimatedParameters()
    print(pandas_results)
