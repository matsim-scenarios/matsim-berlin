#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
import numpy as np
import pandas as pd
from biogeme.expressions import Beta, bioDraws, log, MonteCarlo
from collections import defaultdict
from scipy.stats import truncnorm

TN = truncnorm(0, np.inf)


def tn_generator(sample_size: int, number_of_draws: int) -> np.ndarray:
    """
    User-defined random number generator to the database.
    See the numpy.random documentation to obtain a list of other distributions.
    """
    return TN.rvs((sample_size, number_of_draws))


def calc_costs(df, k, modes):
    # Cost parameter from current berlin model

    fixed_costs = defaultdict(lambda: 0.0, car=-15, pt=-3)
    km_costs = defaultdict(lambda: 0.0, car=-0.149, ride=-0.149)
    time_cost = -6.88

    for i in range(1, k + 1):

        # Costs will also include time costs
        df[f"plan_{i}_costs"] = 0
        # Price is only monetary costs
        df[f"plan_{i}_price"] = 0

        df[f"plan_{i}_tt_hours"] = 0

        for mode in modes:

            df[f"plan_{i}_{mode}_fixed_cost"] = (df[f"plan_{i}_{mode}_usage"] > 0) * fixed_costs[mode]
            df[f"plan_{i}_{mode}_used"] = (df[f"plan_{i}_{mode}_usage"] > 0) * 1

            df[f"plan_{i}_price"] += df[f"plan_{i}_{mode}_fixed_cost"] + df[f"plan_{i}_{mode}_km"] * km_costs[mode]
            df[f"plan_{i}_tt_hours"] += df[f"plan_{i}_{mode}_hours"]

            # Add configured time costs
            df[f"plan_{i}_costs"] += df[f"plan_{i}_{mode}_km"] * km_costs[mode]
            df[f"plan_{i}_costs"] += time_cost * df[f"plan_{i}_{mode}_hours"]

            # Add additional ride time costs
            if mode == "ride":
                df[f"plan_{i}_costs"] += time_cost * df[f"plan_{i}_{mode}_hours"]

            if mode == "pt":
                df[f"plan_{i}_costs"] += (df[f"plan_{i}_pt_usage"] > 0) * fixed_costs[mode]

        # Defragment df
        df = df.copy()

    return df


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

    df = calc_costs(df, k, modes)

    database = db.Database("data/plan-choices", df)
    v = database.variables

    database.setRandomNumberGenerators({
        "TN": (tn_generator, "truncated normal generator for mixed logit")
    })

    mixed_logit = True

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

    B_UTIL = Beta('B_CAR_UTIL', 0, None, None, 0)
    B_UTIL_S = Beta('B_CAR_UTIL_SD', 1, None, None, 0)

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
