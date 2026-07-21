#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
import numpy as np
import os
from biogeme.expressions import Beta, bioDraws, log, exp, MonteCarlo

from prepare import read_plan_choices, tn_generator, gumbel_generator, gumbel_zero_generator
from prepare import tn_s1_generator, tn_s2_generator, ztn_s2_generator, triangular_generator, cauchy_generator

ESTIMATE = 0
FIXED = 1

# This runs the plan based choice model
# For the thesis this script was run with:
# --mxl-distribution NORMAL_ANTI
# --mxl-param constant
# --car-util 0
# --performing 5.501071
# --exp-income 0.275502
# --util-money 0.397322
# --price-perception 0.268622
# --est-bus-legs
# --effort ride nan
# --effort bike nan
# --same-price-perception

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate choice model for daily trip usage")
    parser.add_argument("--input", help="Path to the input file", type=str,
                        default="/Users/gregorr/Downloads/plan-choices-subtour_70.csv")
    parser.add_argument("--name", help="Model name prefix in the output", type=str, default="")
    parser.add_argument("--mxl-modes", help="Modes to use mixed logit for", nargs="*", type=set,
                        default=["pt", "bike", "ride", "car"])
    parser.add_argument("--mxl-distribution", help="Mixing distribution", default="NORMAL_ANTI",
                        choices=["NORMAL_ANTI", "LOG_NORMAL", "GUMBEL", "GUMBEL_SCALE", "TN", "TN_SCALE", "CAUCHY",
                                 "NORMAL_SCALE", "LOG_NORMAL_SCALE", "TN_S2", "TN_S2_SCALE", "TN_S1_SCALE", "ZTN_S2",
                                 "TRIANGULAR"])
    parser.add_argument("--mxl-param", help="Which parameter to variate", type=str, default="constant",
                        choices=["none", "constant", "car_util", "tt_hours"])
    parser.add_argument("--est-performing", help="Estimate the beta for performing", action="store_true")
    parser.add_argument("--performing", help="Beta for performing", type=float, default=5)
    parser.add_argument("--est-exp-income", help="Estimate exponent for income", action="store_true")
    parser.add_argument("--exp-income", help="Exponent for income", type=float, default=1)
    parser.add_argument("--util-money", help="Utility of money", type=float, default=1)
    parser.add_argument("--est-bus-legs", help="Estimate the beta for Bus legs", action="store_true")
    parser.add_argument("--est-pt-switches", help="Estimate beta for number of pt switches", action="store_true")
    parser.add_argument("--est-util-money", help="Estimate utility of money", action="store_true")
    parser.add_argument("--est-error-component", help="Add a normal error component to each trip choice",
                        action="store_true")
    parser.add_argument("--ec", help="Factor for error component", type=float, default=None)
    parser.add_argument("--est-price-perception-car", help="Estimate price perception", action="store_true")
    parser.add_argument("--est-price-perception-pt", help="Estimate price perception", action="store_true")
    parser.add_argument("--same-price-perception", help="Only estimate one fixed price perception factor",
                        action="store_true")
    parser.add_argument("--price-perception", help="Given value for fixed price perception", type=float, default=1)
    parser.add_argument("--effort", help="Additional marginal utility of travel time", nargs="+", action='append',
                        default=[])
    parser.add_argument("--ascs", help="Predefined ASCs", nargs="+", action='append', default=[])
    parser.add_argument("--car-util", help="Fixed utility for car", type=float, default=None)
    parser.add_argument("--no-income", help="Don't consider the income", action="store_true")

    args = parser.parse_args()

    ds = read_plan_choices(args.input)
    ds.df.to_csv("plan_choices_debug.csv", index=False)
    print(ds.df.head())

    # Needs to be numeric
    ds.df["choice"] = 1

    # Convert all the columns to numeric
    df = ds.df * 1

    database = db.Database("data/plan-choices", df)
    v = database.variables

    database.setRandomNumberGenerators({
        "TN": (tn_generator, "truncated normal [0, inf]"),
        "TN_S2": (tn_s2_generator, "truncated normal [-2, 2]"),
        "TN_S1": (tn_s1_generator, "truncated normal [-1, 1]"),
        "ZTN_S2": (ztn_s2_generator, "truncated normal [0, 2]"),
        "GUMBEL": (gumbel_generator, "Gumbel generator for mixed logit"),
        "GUMBEL_SCALE": (gumbel_zero_generator, "Gumbel generator with zero mean for mixed logit"),
        "TRIANGULAR": (triangular_generator, "Triangular distribution [-1, 0, 1]"),
        "CAUCHY": (cauchy_generator, "Cauchy distribution"),
    })


    def bio_draws(name, B, dist=None):
        """ Helper function to draw from configured distribution """

        if dist is None:
            dist = args.mxl_distribution

        lower = None
        higher = None
        # Scale must be positive for these distributions
        if dist in ("LOG_NORMAL", "LOG_NORMAL_SCALE", "TN", "TN_SCALE", "ZTN_S2"):
            lower = 0

        # Ensure that drawn values never exceed the performing value
        if dist == "TN_S1_SCALE":
            lower = -args.performing
            higher = args.performing

        sd = Beta(name + "_s", 1, lower, higher, ESTIMATE)
        rnd_name = name + "_rnd"

        if dist == "LOG_NORMAL":
            return exp(B + sd * bioDraws(rnd_name, "NORMAL"))
        elif dist == "LOG_NORMAL_SCALE":
            return sd * exp(bioDraws(rnd_name, "NORMAL"))
        elif dist == "TN_SCALE":
            # TN_SCALE is truncated normal without bias
            return sd * bioDraws(rnd_name, "TN")
        elif dist == "NORMAL_SCALE":
            # NORMAL_SCALE is normal without bias
            return sd * bioDraws(rnd_name, "NORMAL_ANTI")
        elif dist == "GUMBEL_SCALE":
            # this distribution has zero mean
            return sd * bioDraws(rnd_name, "GUMBEL_SCALE")
        elif dist == "TN_S2_SCALE":
            return sd * bioDraws(rnd_name, "TN_S2")
        elif dist == "TN_S1_SCALE":
            return sd * bioDraws(rnd_name, "TN_S1")

        return B + sd * bioDraws(rnd_name, dist)


    fixed_ascs = {x: float(y) for x, y in args.ascs}
    if fixed_ascs:
        print("Using fixed ascs", fixed_ascs)

    effort = {x: float(y) for x, y in args.effort}
    if effort:
        # Nan values are estimated
        for mode in effort:
            if np.isnan(effort[mode]):
                effort[mode] = Beta(f"BETA_TT_{mode}", 0, 0, None, ESTIMATE)

        print("Using time effort", effort)

    print("Using distribution", args.mxl_distribution)
    print("Using MXL param", args.mxl_param)

    # Variables for constants
    ASC = {}

    # Factor on marginal utility of money
    EXP_INCOME = Beta('EXP_INCOME', args.exp_income, 0, 1.5, ESTIMATE if args.est_exp_income else FIXED)

    UTIL_MONEY = Beta('UTIL_MONEY', args.util_money, 0.2, 1.5, ESTIMATE if args.est_util_money else FIXED)

    BETA_PERFORMING = Beta('BETA_PERFORMING', args.performing, 1, 15, ESTIMATE if args.est_performing else FIXED)
    BETA_CAR_PRICE_PERCEPTION = Beta('BETA_CAR_PRICE_PERCEPTION', args.price_perception, 0, 1,
                                     ESTIMATE if args.est_price_perception_car else FIXED)

    if args.same_price_perception:
        BETA_PT_PRICE_PERCEPTION = BETA_CAR_PRICE_PERCEPTION
    else:
        BETA_PT_PRICE_PERCEPTION = Beta('BETA_PT_PRICE_PERCEPTION', args.price_perception, 0, 1,
                                        ESTIMATE if args.est_price_perception_pt else FIXED)

    BETA_BUS_LEGS = Beta('BETA_BUS_LEGS', 0, None, 0, ESTIMATE if args.est_bus_legs else FIXED)
    BETA_PT_SWITCHES = Beta('BETA_PT_SWITCHES', -1, None, 0, ESTIMATE if args.est_pt_switches else FIXED)

    is_est_car = "car" in args.mxl_modes or args.mxl_param == "none"

    for mode in ds.modes:

        val = fixed_ascs.get(mode, 0)
        status = FIXED if mode in ("walk", "car" if not is_est_car else "_") or mode in fixed_ascs else ESTIMATE

        # Base asc
        asc = Beta(f"ASC_{mode}", val, None, None, status)

        if mode in args.mxl_modes and args.mxl_param == "constant":
            ASC[mode] = bio_draws(f"ASC_{mode}", asc)
        else:
            ASC[mode] = asc

        if args.mxl_param == "tt_hours" and mode in args.mxl_modes:

            # Here ride is always ignored because the interpretation is not reasonable and leads to very wrong results as well
            if mode == "ride":
                # Ride will have random asc
                ASC[mode] = bio_draws(f"ASC_{mode}", asc, dist="NORMAL_ANTI")

            else:
                b = Beta(f"BETA_TT_{mode}", 0, None, None, ESTIMATE)
                effort[mode] = bio_draws(f"BETA_TT_{mode}", b)

    if args.car_util is not None:
        print("Using fixed utility for car", args.car_util)

    B_UTIL = Beta('B_CAR_UTIL', 8 if args.car_util is None else args.car_util,
                  0, None, ESTIMATE if args.car_util is None else FIXED)

    if args.mxl_param == "car_util":
        B_CAR = bio_draws('B_CAR_UTIL_RND', B_UTIL)
    else:
        B_CAR = B_UTIL

    # Use asc instead of car utility
    if is_est_car:
        print("Estimating car asc, instead of daily utility")
        B_CAR = 0

    EC = {}
    if args.est_error_component:
        print("Estimating error component")

        # Draw modes x trips random terms
        for m in ds.modes:
            EC[m] = []

            for j in range(7):
                EC[m].append(bioDraws(f"ec_{m}_{j}", "NORMAL_ANTI"))

        if args.ec is None:
            EC_S = Beta("ec_s", 0.5, 0, None, ESTIMATE)
        else:
            EC_S = args.ec

    print("Using MXL modes", args.mxl_modes)
    U = {}
    AV = {}

    # By default, ride incurs double performing, expect when it is estimated separately
    ride_factor = 2 if "ride" not in effort else 1

    print("Using ride beta performing *", ride_factor)

    for i in range(1, ds.k + 1):
        # Price is already negative
        perceived_price = (BETA_CAR_PRICE_PERCEPTION * v[f"plan_{i}_car_price"] +
                           BETA_PT_PRICE_PERCEPTION * v[f"plan_{i}_pt_price"] +
                           v[f"plan_{i}_other_price"])

        u = perceived_price * UTIL_MONEY * (1 if args.no_income else (ds.global_income / v["income"]) ** EXP_INCOME)
        u += v[f"plan_{i}_transfers"] * BETA_PT_SWITCHES
        u += v[f"plan_{i}_bus_legs"] * BETA_BUS_LEGS

        for mode in ds.modes:
            f = (ride_factor if mode == "ride" else 1)

            u += ASC[mode] * v[f"plan_{i}_{mode}_usage"]
            u += -BETA_PERFORMING * v[f"plan_{i}_{mode}_hours"] * f

        u += v[f"plan_{i}_car_used"] * B_CAR

        # Add the effort component (additional time disutility)
        for mode, val in effort.items():
            u -= v[f"plan_{i}_{mode}_hours"] * val

        if args.est_error_component:
            errs = 0
            for mode in ds.modes:
                for j in range(7):
                    errs += v[f"plan_{i}_trip_{j}_mode_{mode}"] * EC[mode][j]

            u += EC_S * errs

        U[i] = u
        AV[i] = v[f"plan_{i}_valid"]

    if args.mxl_param == "none":
        logprob = models.loglogit(U, AV, v["choice"])
    else:
        prob = models.logit(U, AV, v["choice"])
        logprob = log(MonteCarlo(prob))

    logprob = {'loglike': logprob, 'weight': v["weight"]}
    biogeme = bio.BIOGEME(database, logprob)

    modelName = os.path.basename(args.input).replace(".csv", "") + ("_" if args.name else "") + args.name

    modelName += f"_{args.mxl_distribution}"
    modelName += f"_{args.mxl_param}"

    if args.est_performing:
        modelName += "_performing"
    if args.est_exp_income:
        modelName += "_exp_income"
    if args.est_util_money:
        modelName += "_util_money"
    if args.ascs:
        modelName += "_fixed_ascs"
    if effort:
        modelName += "_effort"
    if args.no_income:
        modelName += "_no_income"
    if args.est_price_perception_car:
        modelName += "_price_perception_car"
    if args.est_price_perception_pt:
        modelName += "_price_perception_pt"
    if args.est_pt_switches:
        modelName += "_pt_switches"
    if args.est_bus_legs:
        modelName += "_bus_legs"
    if args.est_error_component:
        modelName += "_ec"

    biogeme.modelName = modelName
    biogeme.weight = v["weight"]

    biogeme.calculateNullLoglikelihood(AV)

    results = biogeme.estimate()
    print(results.short_summary())

    pandas_results = results.getEstimatedParameters()
    print(pandas_results)

    print()
    print("Correlation matrix")

    corr_matrix = results.getCorrelationResults()
    print(corr_matrix)
