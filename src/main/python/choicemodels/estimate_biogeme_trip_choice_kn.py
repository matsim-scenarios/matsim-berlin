#!/usr/bin/env python
# -*- coding: utf-8 -*-

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
    parser = argparse.ArgumentParser(description="Estimate the trip choice model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../../trip-choices.csv")

    parser.add_argument("--mxl-modes", help="Modes to use mixed logit for", nargs="*", type=set, default=[])

    parser.add_argument("--est-performing", help="Estimate the beta for performing", action="store_true", default=1)
    # parser.add_argument("--est-exp-performing", help="Estimate the exp for performing", action="store_true", default=0)

    # parser.add_argument("--est-util-money", help="Estimate utility of money", action="store_true", default=1)
    parser.add_argument("--est-exp-income", help="Estimate exponent for income", action="store_true", default=1)

    parser.add_argument("--est-pt-switches", help="Estimate the beta for PT switches", action="store_true", default=1)

    parser.add_argument("--est-price-perception-car", help="Estimate fixed cost perception car", action="store_true",default=1)
    parser.add_argument("--est-price-perception-pt", help="Estimate fixed cost perceptionpt ", action="store_true", default=1)
    parser.add_argument("--same-price-perception", help="Only estimate one fixed price perception factor", action="store_true", default=1)

    parser.add_argument("--est-ride-alpha", help="Estimate ride detour parameter", action="store_true")

    parser.add_argument("--est-bike-effort", help="Estimate parameter for bike effort", action="store_true",default=1)

    parser.add_argument("--est-exp-dist", help="Modes for which to estimate distance elasticity", nargs="+", type=str, default=[])


    parser.add_argument("--no-income", help="Don't consider the income", action="store_true", default=0)

    args = parser.parse_args()

    ds = read_trip_choices(args.input)

    # Convert all the columns to numeric
    df = ds.df * 1

    # df.drop(df[(df["dist_weight"] * df["beelineDist"]) > 3].index, inplace=True)

    # convert from pandas to biogeme data container.  "database" is needed later.
    database = db.Database("data/choices", df)
    vv = database.variables

    ASC = {}
    for mode in ds.modes:
        # Base asc
        ASC[mode] = Beta(f"ASC{mode}", 0, None, None, FIXED if mode == "walk" else ESTIMATE)

        # if mode in args.mxl_modes:
        #     sd = Beta(f"ASC_{mode}_s", 1, 0, None, ESTIMATE)
        #     ASC[mode] += sd * bioDraws(f"{mode}_RND", "NORMAL_ANTI")

    U = {}
    AV = {}

    UTIL_MONEY = Beta('betaMoney', 1, 0., None, ESTIMATE )
    EXP_INCOME = Beta('lambdaIncome', 0.4, 0, 1.5, ESTIMATE if args.est_exp_income else FIXED)

    BETA_PERFORMING = Beta('betaTt', -6.88, -15, -1, ESTIMATE )

    BETA_CAR_PRICE_PERCEPTION = Beta('betaFcpCar', 0, 0, None, ESTIMATE if args.est_price_perception_car else FIXED)

    if args.same_price_perception:
        BETA_PT_PRICE_PERCEPTION = BETA_CAR_PRICE_PERCEPTION
    else:
        BETA_PT_PRICE_PERCEPTION = Beta('betaFcpPt', 0, 0, 1, ESTIMATE if args.est_price_perception_pt else FIXED)

    BETA_PT_SWITCHES = Beta('betaPtSwitches', -1, None, 0, ESTIMATE if args.est_pt_switches else FIXED)

    # THe detour factor for ride trip, influences the time costs, as well as distance cost
    BETA_RIDE_ALPHA = Beta('alphaRide', 1, 0, 2, ESTIMATE if args.est_ride_alpha else FIXED)

    EXP_DIST = {}
    for mode in args.est_exp_dist:
        print(f"Estimating distance elasticity for {mode}")
        EXP_DIST[mode] = (Beta(f'BETA_DIST_{mode}', 0, None, None, ESTIMATE), Beta(f'EXP_DIST_{mode}', 1, None, None, ESTIMATE))

    BETA_BIKE_EFFORT = Beta('BETA_BIKE_UTIL_H', 0, 0, 10, ESTIMATE if args.est_bike_effort else FIXED)

    # == overriding some things

    ASC['walk'] = 0
    BETA_RIDE_ALPHA = 1
    # BETA_CAR_PRICE_PERCEPTION = 0.
    # BETA_PT_PRICE_PERCEPTION = 0
    # EXP_INCOME = 0.
    BETA_PT_SWITCHES = -1
    # (this now needs to be "-" the way I have specified that!)

    # == end overriding some things

    for i, mode in enumerate(ds.modes, 1):
        # Ride incurs double the cost as car, to account for the driver and passenger
        u = ASC[mode] + BETA_PERFORMING * vv[f"{mode}_hours"] * ((1 + BETA_RIDE_ALPHA) if mode == "ride" else 1)
        # u = ASC[mode] + BETA_PERFORMING * v[f"{mode}_hours"] * ( (1 + BETA_RIDE_ALPHA) if mode == "ride" else 1) * ( 1.5 if mode == "pt" else 1)
        # u = ASC[mode] + BETA_PERFORMING * ( vv[f"{mode}_hours"] + ( vv["pt_walking_km"]/10 if mode=="pt" else 0 ) ) * ( (1 + BETA_RIDE_ALPHA) if mode == "ride" else 1)

        price = km_costs[mode] * vv[f"{mode}_km"] * (BETA_RIDE_ALPHA if mode == "ride" else 1)

        price += daily_costs[mode] * vv["dist_weight"] * (BETA_CAR_PRICE_PERCEPTION if mode == "car" else BETA_PT_PRICE_PERCEPTION)
        # price += daily_costs[mode] * v["dist_weight"]

        u += price * UTIL_MONEY * (1 if args.no_income else (ds.global_income / vv["income"]) ** EXP_INCOME)
        # u += price * UTIL_MONEY

        # u += BETA_SHORT_TRAVEL * (DIST_WEIGHT * KM < 0.8)

        if mode == "pt":
            u += vv[f"{mode}_switches"] * BETA_PT_SWITCHES

        # if mode == "bike":
        #     u -= v[f"{mode}_hours"] * BETA_BIKE_EFFORT

        if mode in EXP_DIST:
            beta, exp = EXP_DIST[mode]
            u += beta * ((vv[f"{mode}_km"] * 1000) / float(mean_dist.loc[i].dist)) ** exp

        U[i] = u
        AV[i] = vv[f"{mode}_valid"]

    if not args.mxl_modes:
        logprob = models.loglogit(U, AV, vv["choice"])
        logprob = {'loglike': logprob, 'weight': vv["weight"]}

    else:
        database.panel("person")

        obsprob = models.logit(U, AV, vv["choice"])
        condprobIndiv = PanelLikelihoodTrajectory(obsprob)
        logprob = log(MonteCarlo(condprobIndiv))

    biogeme = bio.BIOGEME(database, logprob)

    # if args.est_performing:
    #     modelName += "_performing"
    # if args.est_exp_income:
    #     modelName += "_exp_income"
    # if args.est_util_money:
    #     modelName += "_util_money"
    # if args.est_price_perception_car:
    #     modelName += "_car_price_perception"
    # if args.est_price_perception_pt:
    #     modelName += "_pt_price_perception"
    # if args.est_pt_switches:
    #     modelName += "_pt_switches"

    biogeme.modelName = "trip_choice"

    biogeme.weight = vv["weight"]

    biogeme.calculateNullLoglikelihood(AV)

    results = biogeme.estimate()

    print(results.short_summary())

    # print("Symbolic utility specification:\n")
    # for alt, expr in U.items():
    #     print(f"V_{alt} =", expr)

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

