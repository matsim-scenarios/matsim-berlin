#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
from biogeme.expressions import Beta, Derive, bioDraws, PanelLikelihoodTrajectory, log, MonteCarlo

from prepare import read_trip_choices, daily_costs, km_costs

ESTIMATE = 0
FIXED = 1

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate the trip choice model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../../trip-choices.csv")
    parser.add_argument("--mxl-modes", help="Modes to use mixed logit for", nargs="*", type=set,
                        default=["pt", "bike", "ride", "car"])
    parser.add_argument("--est-performing", help="Estimate the beta for performing", action="store_true")
    parser.add_argument("--est-exp-income", help="Estimate exponent for income", action="store_true")
    parser.add_argument("--est-util-money", help="Estimate utility of money", action="store_true")
    parser.add_argument("--est-pt-switches", help="Estimate the beta for PT switches", action="store_true")
    parser.add_argument("--est-bus-legs", help="Estimate the beta for Bus legs", action="store_true")
    parser.add_argument("--est-price-perception-car", help="Estimate price perception", action="store_true")
    parser.add_argument("--est-price-perception-pt", help="Estimate price perception", action="store_true")
    parser.add_argument("--est-ride-alpha", help="Estimate ride detour parameter", action="store_true")
    parser.add_argument("--est-bike-effort", help="Estimate parameter for bike effort", action="store_true")
    parser.add_argument("--est-dist", metavar="KEY=VALUE", help="Modes for which to estimate distance elasticity",
                        nargs="+", type=str)
    parser.add_argument("--same-price-perception", help="Only estimate one fixed price perception factor",
                        action="store_true")

    parser.add_argument("--no-income", help="Don't consider the income", action="store_true")

    args = parser.parse_args()

    ds = read_trip_choices(args.input)

    # Convert all the columns to numeric
    df = ds.df * 1

    # Needed as column for elasticities
    for mode in ds.modes:
        df[f"{mode}_daily_costs"] = daily_costs[mode]
        df[f"{mode}_km_costs"] = km_costs[mode]

    database = db.Database("data/choices", df)
    v = database.variables

    ASC = {}
    for mode in ds.modes:
        # Base asc
        ASC[mode] = Beta(f"ASC_{mode}", 0, None, None, FIXED if mode == "walk" else ESTIMATE)

        if mode in args.mxl_modes:
            sd = Beta(f"ASC_{mode}_s", 1, 0, None, ESTIMATE)
            ASC[mode] += sd * bioDraws(f"{mode}_RND", "NORMAL_ANTI")

    U = {}
    AV = {}

    EXP_INCOME = Beta('EXP_INCOME', 1, 0, 1.5, ESTIMATE if args.est_exp_income else FIXED)
    UTIL_MONEY = Beta('UTIL_MONEY', 1, 0.3, 1.5, ESTIMATE if args.est_util_money else FIXED)
    BETA_PERFORMING = Beta('BETA_PERFORMING', 6.88, 1, 15, ESTIMATE if args.est_performing else FIXED)

    BETA_CAR_PRICE_PERCEPTION = Beta('BETA_CAR_PRICE_PERCEPTION', 1, 0, 1,
                                     ESTIMATE if args.est_price_perception_car else FIXED)
    if args.same_price_perception:
        BETA_PT_PRICE_PERCEPTION = BETA_CAR_PRICE_PERCEPTION
    else:
        BETA_PT_PRICE_PERCEPTION = Beta('BETA_PT_PRICE_PERCEPTION', 1, 0, 1,
                                        ESTIMATE if args.est_price_perception_pt else FIXED)

    BETA_PT_SWITCHES = Beta('BETA_PT_SWITCHES', 1, 0, None, ESTIMATE if args.est_pt_switches else FIXED)
    BETA_BUS_USAGE = Beta('BETA_BUS_LEGS', 0, 0, None, ESTIMATE if args.est_bus_legs else FIXED)

    # THe detour factor for ride trip, influences the time costs, as well as distance cost
    BETA_RIDE_ALPHA = Beta('BETA_RIDE_ALPHA', 1, 0, 2, ESTIMATE if args.est_ride_alpha else FIXED)

    EXP_DIST = {}

    if args.est_dist is not None:
        args.est_dist = dict([x.split("=") for x in args.est_dist])
    else:
        args.est_dist = {}

    for mode, d in args.est_dist.items():
        print(f"Estimating distance elasticity for {mode} with distance {d}")
        EXP_DIST[mode] = (Beta(f'BETA_DIST_{mode}', 1, None, None, ESTIMATE), Beta(f'EXP_DIST_{mode}', 1, None, None, ESTIMATE))

    BETA_BIKE_EFFORT = Beta('BETA_BIKE_UTIL_H', 0, 0, 10, ESTIMATE if args.est_bike_effort else FIXED)

    for i, mode in enumerate(ds.modes, 1):
        # Ride incurs double the cost as car, to account for the driver and passenger
        u = ASC[mode] - BETA_PERFORMING * v[f"{mode}_hours"] * ((1 + BETA_RIDE_ALPHA) if mode == "ride" else 1)

        #price = v[f"{mode}_km_costs"] * v[f"{mode}_km"] * (BETA_RIDE_ALPHA if mode == "ride" else 1)
        if mode in ["car", "ride"]:
            price = v[f"{mode}_km_costs"] * v[f"{mode}_km"] * (BETA_RIDE_ALPHA if mode == "ride" else 1)
        else: price = 0

        price += v[f"{mode}_daily_costs"] * v["dist_weight"] * (
            BETA_CAR_PRICE_PERCEPTION if mode == "car" else BETA_PT_PRICE_PERCEPTION
        )

        u += price * UTIL_MONEY * (1 if args.no_income else (ds.global_income / v["income"]) ** EXP_INCOME)


        if mode in EXP_DIST:
            beta, exp = EXP_DIST[mode]
            u += beta * (v[f"{mode}_km"] / float(args.est_dist[mode])) ** exp

        U[i] = u
        AV[i] = v[f"{mode}_valid"]

    if not args.mxl_modes:
        logprob = models.loglogit(U, AV, v["choice"])
        logprob = {'loglike': logprob, 'weight': v["weight"]}

    else:
        database.panel("person")

        obsprob = models.logit(U, AV, v["choice"])
        condprobIndiv = PanelLikelihoodTrajectory(obsprob)
        logprob = log(MonteCarlo(condprobIndiv))

    biogeme = bio.BIOGEME(database, logprob)

    modelName = "trip_choice"
    if args.est_performing:
        modelName += "_performing"
    if args.est_exp_income:
        modelName += "_exp_income"
    if args.est_util_money:
        modelName += "_util_money"
    if args.est_price_perception_car:
        modelName += "_car_price_perception"
    if args.est_price_perception_pt:
        modelName += "_pt_price_perception"
    if args.est_pt_switches:
        modelName += "_pt_switches"
    if args.est_bus_legs:
        modelName += "_bus_legs"

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

    probs = {f"Prob {ds.modes[i]}": models.logit(U, AV, i + 1) for i in range(len(ds.modes))}
    probs["weight"] = v["weight"]

    for i in range(len(ds.modes)):
        mode = ds.modes[i]
        prob = probs[f"Prob {mode}"]

        direct_elas_time = Derive(prob, f"{mode}_hours") * v[f"{mode}_hours"] / prob

        direct_elas_cost = Derive(prob, f"{mode}_daily_costs") * v[f"{mode}_daily_costs"] / prob
        direct_elas_km_costs = Derive(prob, f"{mode}_km_costs") * v[f"{mode}_km_costs"] / prob

        probs[f"Direct elasticity {mode} time"] = direct_elas_time
        probs[f"Direct elasticity {mode} daily costs"] = direct_elas_cost
        probs[f"Direct elasticity {mode} km costs"] = direct_elas_km_costs


    simulation = bio.BIOGEME(database, probs)

    p = simulation.simulate(results.getBetaValues())

    # Calculate weighted probs
    for i in range(len(ds.modes)):
        mode = ds.modes[i]

        prob_weighted = p["weight"] * p[f"Prob {mode}"]
        denom = prob_weighted.sum()

        aggr = ((prob_weighted * p[f"Direct elasticity {mode} time"]) / denom).sum()
        print("Aggregated direct elasticity of %s wrt to time: %.3f" % (mode, aggr))

        aggr = ((prob_weighted * p[f"Direct elasticity {mode} daily costs"]) / denom).sum()
        print("Aggregated direct elasticity of %s wrt to cost: %.3f" % (mode, aggr))

        aggr = ((prob_weighted * p[f"Direct elasticity {mode} km costs"]) / denom).sum()
        print("Aggregated direct elasticity of %s wrt to km costs: %.3f" % (mode, aggr))
