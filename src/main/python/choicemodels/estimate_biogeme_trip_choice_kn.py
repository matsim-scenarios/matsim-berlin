#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import argparse
import re
import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
from biogeme.expressions import Beta, bioDraws, PanelLikelihoodTrajectory, log, MonteCarlo, Variable

from prepare import read_trip_choices, daily_costs, km_costs

ESTIMATE = 0
FIXED = 1

if __name__ == "__main__":
    os.chdir("estimationOutput")
    parser = argparse.ArgumentParser(description="Estimate the trip choice model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../../../trip-choices.csv")

    parser.add_argument("--no-income", help="Don't consider the income", action="store_true", default=0)
    # this is useful since it removes the term from the equation. kai, mar'26

    args = parser.parse_args()

    ds = read_trip_choices(args.input)

    df = ds.df * 1     # Convert all the columns to numeric

    mean_dist = df.groupby("choice").agg(dist=("beelineDist", "mean")) * 1000 # mean_dist is needed later!

    # df.drop(df[(df["dist_weight"] * df["beelineDist"]) > 3].index, inplace=True)

    database = db.Database("data/choices", df)     # convert from pandas to biogeme data container.  "database" is needed later.

    vv = database.variables

    ASC = {}
    for mode in ds.modes:
        # Base asc
        ASC[mode] = Beta(f"ASC{mode}", 0, None, None, FIXED if mode == "walk" else ESTIMATE)

        # if mode in args.mxl_modes:
        #     sd = Beta(f"ASC_{mode}_s", 1, 0, None, ESTIMATE)
        #     ASC[mode] += sd * bioDraws(f"{mode}_RND", "NORMAL_ANTI")

    UTIL_MONEY = Beta('betaMoney', 1, 0., None, ESTIMATE )
    EXP_INCOME = Beta('lambdaIncome', 0.4, 0, 1.5, ESTIMATE )

    BETA_PERFORMING = Beta('betaTt', -6.88, -15, -1, ESTIMATE )

    BETA_CAR_PRICE_PERCEPTION = Beta('betaFcpCar', 0, 0, None, ESTIMATE )
    BETA_PT_PRICE_PERCEPTION = Beta('betaFcpPt', 0, 0, 1, ESTIMATE )

    BETA_PT_SWITCHES = Beta('betaPtSwitches', -1, None, 0, ESTIMATE )

    BETA_RIDE_ALPHA = Beta('alphaRide', 1, 0, 2, ESTIMATE )

    EXP_DIST = {}
    est_exp_dist = []
    for mode in est_exp_dist:
        print(f"Estimating distance elasticity for {mode}")
        EXP_DIST[mode] = (Beta(f'BETA_DIST_{mode}', 0, None, None, ESTIMATE), Beta(f'EXP_DIST_{mode}', 1, None, None, ESTIMATE))

    BETA_BIKE_EFFORT = Beta('betaBike', 0, 0, 10, ESTIMATE )

    # == overriding some things

    ASC['walk'] = 0
    BETA_RIDE_ALPHA = 1
    BETA_CAR_PRICE_PERCEPTION = 0.0
    BETA_PT_PRICE_PERCEPTION = BETA_CAR_PRICE_PERCEPTION
    # EXP_INCOME = 0.
    BETA_PT_SWITCHES = -1 # (this now needs to be "-" the way I have specified that!)
    # BETA_BIKE_EFFORT = 0

    # == end overriding some things

    U = {} # utility
    AV = {} # availability

    for i, mode in enumerate(ds.modes, 1):
        # Ride also incurs alpha x cost/time_cost of the driver
        u = ASC[mode] + BETA_PERFORMING * vv[f"{mode}_hours"] * ((1 + BETA_RIDE_ALPHA) if mode == "ride" else 1)
        # u = ASC[mode] + BETA_PERFORMING * v[f"{mode}_hours"] * ( (1 + BETA_RIDE_ALPHA) if mode == "ride" else 1) * ( 1.5 if mode == "pt" else 1)
        # u = ASC[mode] + BETA_PERFORMING * ( vv[f"{mode}_hours"] + ( vv["pt_walking_km"]/10 if mode=="pt" else 0 ) ) * ( (1 + BETA_RIDE_ALPHA) if mode == "ride" else 1)

        price = km_costs[mode] * vv[f"{mode}_km"] * (BETA_RIDE_ALPHA if mode == "ride" else 1)

        # the following has the advantage that it will not be in the printed equation when not relevant
        if daily_costs[mode] != 0:
            if mode == "car" and BETA_CAR_PRICE_PERCEPTION != 0:
                price += daily_costs[mode] * vv["dist_weight"] * BETA_CAR_PRICE_PERCEPTION
            if mode != "car" and BETA_PT_PRICE_PERCEPTION != 0:
                price += daily_costs[mode] * vv["dist_weight"] * BETA_PT_PRICE_PERCEPTION

        if EXP_INCOME != 0:
            price *= (ds.global_income / vv["income"]) ** EXP_INCOME

        u += price * UTIL_MONEY

        if mode == "pt":
            u += vv[f"{mode}_switches"] * BETA_PT_SWITCHES

        if mode == "bike" and BETA_BIKE_EFFORT!=0:
            u -= vv[f"{mode}_hours"] * BETA_BIKE_EFFORT

        if mode in EXP_DIST:
            beta, exp = EXP_DIST[mode]
            u += beta * ((vv[f"{mode}_km"] * 1000) / float(mean_dist.loc[i].dist)) ** exp

        U[i] = u
        AV[i] = vv[f"{mode}_valid"]

    # if not args.mxl_modes:
    logprob = models.loglogit(U, AV, vv["choice"])
    logprob = {'loglike': logprob, 'weight': vv["weight"]}

    # else:
    #     database.panel("person")
    #
    #     obsprob = models.logit(U, AV, vv["choice"])
    #     condprobIndiv = PanelLikelihoodTrajectory(obsprob)
    #     logprob = log(MonteCarlo(condprobIndiv))

    biogeme = bio.BIOGEME(database, logprob)
    biogeme.modelName = "trip_choice"
    biogeme.weight = vv["weight"]
    biogeme.calculateNullLoglikelihood(AV)

    results = biogeme.estimate()

    print(results.short_summary())

    def pretty(expr, ndigits=3):
        betas = results.getBetaValues()

        def substitute(match):
            name = match.group(1)
            if name in betas:
                return f"{betas[name]:.{ndigits}f}"
            return match.group(0)  # fallback: leave untouched

        s = str(expr)
        return re.sub(r"Beta\('([^']+)',[^)]*\)", substitute, s)

    for alt, expr in U.items():
        print(f"V_{alt} =", pretty(expr,2))

    print()

    print(results.getEstimatedParameters())

    # Generate LaTeX table
    results.writeLaTeX()

    # print()
    # print("Correlation matrix")
    #
    # corr_matrix = results.getCorrelationResults()
    # print(corr_matrix)

