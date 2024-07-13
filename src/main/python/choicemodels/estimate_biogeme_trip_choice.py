#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
from collections import defaultdict

import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
from biogeme.expressions import Beta

from prepare import read_trip_choices

ESTIMATE = 0
FIXED = 1

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate the trip choice model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../../trip-choices.csv")
    parser.add_argument("--est-performing", help="Estimate the beta for performing", action="store_true")
    parser.add_argument("--no-income", help="Don't consider the income", action="store_true")

    args = parser.parse_args()

    ds = read_trip_choices(args.input)

    df = ds.df.drop(columns=["person"])

    # Convert all the columns to numeric
    df = df * 1

    database = db.Database("data/choices", df)
    v = database.variables

    database.remove(v["choice"] == 0)

    km_costs = defaultdict(lambda: 0.0, car=-0.149, ride=-0.149)

    ASC = {}
    for mode in ds.modes:
        # Base asc
        ASC[mode] = Beta(f"ASC_{mode}", 0, None, None, 1 if mode == "walk" else 0)

    U = {}
    AV = {}

    B_TIME = Beta('B_TIME', 6.88, None, None, ESTIMATE if args.est_performing else FIXED)
    UTIL_MONEY = 1
    EXP_INCOME = 1

    for i, mode in enumerate(ds.modes, 1):
        u = ASC[mode] - B_TIME * v[f"{mode}_hours"]

        price = km_costs[mode] * v[f"{mode}_km"]
        u += price * UTIL_MONEY * (1 if args.no_income else (ds.global_income / v["income"]) ** EXP_INCOME)

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
