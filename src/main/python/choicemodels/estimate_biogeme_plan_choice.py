#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import argparse

import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
from biogeme.expressions import Beta, bioDraws, log, MonteCarlo

from prepare import read_plan_choices, tn_generator

ESTIMATE = 0
FIXED = 1

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate choice model for daily trip usage")
    parser.add_argument("--input", help="Path to the input file", type=str,
                        default="../../../../plan-choices-bestK_9-tt-only.csv")
    parser.add_argument("--mxl-modes", help="Modes to use mixed logit for", nargs="+", type=set, default=["pt", "bike", "ride"])
    parser.add_argument("--est-performing", help="Estimate the beta for performing", action="store_true")
    parser.add_argument("--est-exp-income", help="Estimate exponent for income", action="store_true")
    parser.add_argument("--est-util-money", help="Estimate utility of money", action="store_true")

    args = parser.parse_args()

    ds = read_plan_choices(args.input)

    # Needs to be numeric
    ds.df["choice"] = 1

    # Convert all the columns to numeric
    df = ds.df * 1

    database = db.Database("data/plan-choices", df)
    v = database.variables

    database.setRandomNumberGenerators({
        "TN": (tn_generator, "truncated normal generator for mixed logit")
    })

    mixed_logit = True

    # Variables for constants
    ASC = {}

    # Variables for variation
    SD = {}

    # Factor on marginal utility of money
    EXP_INCOME = Beta('EXP_INCOME', 1, 0, 1.5, ESTIMATE if args.est_exp_income else FIXED)

    UTIL_MONEY = Beta('UTIL_MONEY', 1, 0, 2, ESTIMATE if args.est_util_money else FIXED)

    BETA_PERFORMING = Beta('BETA_PERFORMING', 6.88, 0, 10, ESTIMATE if args.est_performing else FIXED)

    for mode in ds.modes:
        # Base asc
        asc = Beta(f"ASC_{mode}", 0, None, None, FIXED if mode in ("walk", "car") else ESTIMATE)

        # Pt does not have its own random parameter
        if mode not in args.mxl_modes:
            ASC[mode] = asc
        else:
            # The random parameter
            SD[mode] = Beta(f"ASC_{mode}_s", 1, None, None, ESTIMATE)
            ASC[mode] = asc + SD[mode] * bioDraws(f"{mode}_RND", "NORMAL_ANTI")

    B_UTIL = Beta('B_CAR_UTIL', 10, 0, 15, ESTIMATE)
    B_UTIL_S = Beta('B_CAR_UTIL_SD', 1, 0, 15, ESTIMATE)

    B_CAR_RND = B_UTIL + B_UTIL_S * bioDraws('B_CAR_UTIL_RND', 'TN')

    U = {}
    AV = {}

    for i in range(1, ds.k + 1):
        u = v[f"plan_{i}_price"] * UTIL_MONEY * (ds.global_income / v["income"]) ** EXP_INCOME
        u -= v[f"plan_{i}_pt_n_switches"]

        for mode in ds.modes:
            u += ASC[mode] * v[f"plan_{i}_{mode}_usage"]

            u += -BETA_PERFORMING * v[f"plan_{i}_{mode}_hours"] * (2 if mode == "ride" else 1)

        if mixed_logit:
            u += v[f"plan_{i}_car_used"] * B_CAR_RND

        U[i] = u
        AV[i] = v[f"plan_{i}_valid"]

    if mixed_logit:
        prob = models.logit(U, AV, v["choice"])
        logprob = log(MonteCarlo(prob))
    else:
        logprob = models.loglogit(U, AV, v["choice"])

    biogeme = bio.BIOGEME(database, logprob)

    biogeme.modelName = os.path.basename(args.input).replace(".csv", "")
    biogeme.weight = v["weight"]

    biogeme.calculateNullLoglikelihood(AV)

    results = biogeme.estimate()
    print(results.short_summary())

    pandas_results = results.getEstimatedParameters()
    print(pandas_results)
