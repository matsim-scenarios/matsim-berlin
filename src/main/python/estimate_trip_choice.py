#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.expressions as exp
import biogeme.models as models
import numpy as np
import pandas as pd
from scipy.special import softmax

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate the trip choice model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../trip-choices.csv")
    parser.add_argument("--modes", nargs="+", help="Modes to include in the model", type=str,
                        default=["walk", "pt", "car", "bike"])

    args = parser.parse_args()

    df = pd.read_csv(args.input)

    df.drop(columns=["p_id"], inplace=True)

    # Convert all the columns to numeric
    df = df * 1

    database = db.Database("data/choices", df)
    v = database.variables

    database.remove(v["choice"] == 0)

    # Variables
    WALK_TT_SCALED = database.DefineVariable('WALK_TT_SCALED', v["walk_travelTime"] / 1000)
    WALK_COST_SCALED = database.DefineVariable('WALK_COST_SCALED', v["walk_walkDist"] / 1000)
    PT_TT_SCALED = database.DefineVariable('PT_TT_SCALED', v["pt_travelTime"] / 1000)
    PT_COST_SCALED = database.DefineVariable('PT_COST_SCALED', v["pt_travelTime"] / 1000)
    CAR_TT_SCALED = database.DefineVariable('CAR_TT_SCALED', v["car_travelTime"] / 1000)
    CAR_CO_SCALED = database.DefineVariable('CAR_CO_SCALED', v["car_travelTime"] / 1000)
    BIKE_TT_SCALED = database.DefineVariable('BIKE_TT_SCALED', v["bike_travelTime"] / 1000)
    BIKE_CO_SCALED = database.DefineVariable('BIKE_CO_SCALED', v["bike_travelTime"] / 1000)

    # Coefficients
    ASC_PT = exp.Beta('ASC_PT', 0, None, None, 0)
    ASC_CAR = exp.Beta('ASC_CAR', 0, None, None, 0)
    ASC_BIKE = exp.Beta('ASC_BIKE', 0, None, None, 0)

    B_TIME = exp.Beta('B_TIME', 0, None, None, 0)
    #    B_COST = exp.Beta('B_COST', 0, None, None, 0)

    V_WALK = B_TIME * WALK_TT_SCALED
    V_PT = ASC_PT + B_TIME * PT_TT_SCALED
    V_CAR = ASC_CAR + B_TIME * CAR_TT_SCALED
    V_BIKE = ASC_BIKE + B_TIME * BIKE_TT_SCALED

    V = {1: V_WALK, 2: V_PT, 3: V_CAR, 4: V_BIKE}
    av = {1: v["walk_valid"], 2: v["pt_valid"], 3: v["car_valid"], 4: v["bike_valid"]}

    logprob = models.loglogit(V, av, v["choice"])

    biogeme = bio.BIOGEME(database, logprob)

    biogeme.modelName = "trip_choice"

    results = biogeme.estimate()
    print(results.short_summary())

    pandas_results = results.getEstimatedParameters()
    print(pandas_results)

    simulate = {
        #        'prob': logprob,
        'Utility walk': V_WALK,
        'Utility PT': V_PT,
        'Utility car': V_CAR,
        'Utility bike': V_BIKE,
    }

    sim = bio.BIOGEME(database, simulate)

    sim_results = sim.simulate(results.getBetaValues())

    print(sim_results)

    print("Given shares")

    # TODO: might not respect the availability of modes

    dist = df.groupby("choice").count()["seq"]
    print(dist / dist.sum())

    print("Simulated shares")
    choices = softmax(sim_results, axis=1)
    print(np.mean(choices, axis=0))
