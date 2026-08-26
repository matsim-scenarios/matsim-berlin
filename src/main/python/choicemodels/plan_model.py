#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Plan-level (whole-day) mode choice model with the specification written out
explicitly -- the stage-2 counterpart of trip_model.py.

One observation = one reference person; alternatives = the k routed plan
candidates (first one is the observed/chosen plan). The SPEC section IS the
model. Random taste terms use one draw per person, shared across that
person's alternatives (this is what makes it a mixed logit).

Data columns per plan i and mode m (from ComputePlanChoices + prepare.py):
  v[f"plan_{i}_{m}_usage"]   number of trips with main mode m
  v[f"plan_{i}_{m}_used"]    0/1: mode used at all in this plan
  v[f"plan_{i}_{m}_hours"]   door-to-door hours summed over m-trips
  v[f"plan_{i}_{m}_km"]      routed km summed over m-trips
  v[f"plan_{i}_transfers"], v[f"plan_{i}_bus_legs"], v[f"plan_{i}_act_util"],
  v[f"plan_{i}_trip_{j}_mode_{m}"] 0/1 indicators, v[f"plan_{i}_valid"]
  person-level: v["income"]

Usage:  python -u plan_model.py <plan-choices-*.csv> [model-name]
"""

import os
import sys

import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
from biogeme.expressions import Beta, MonteCarlo, bioDraws, log

from prepare import read_plan_choices

INPUT = sys.argv[1] if len(sys.argv) > 1 else "../../../../plan-choices-subtour_9.csv"
MODEL_NAME = sys.argv[2] if len(sys.argv) > 2 else "plan_model"

ds = read_plan_choices(INPUT)
ds.df["choice"] = 1  # chosen plan is always the first candidate

# Deduplicate candidates: the subtour generator emits identical trip-mode
# sequences (incl. copies of the observed plan) -- red-bus/blue-bus artifacts
# that penalize the chosen plan's likelihood and reward the error components
# for deflating phantom clusters. Always on here (use --dedup for the same in
# estimate_biogeme_plan_choice.py).
from prepare import invalidate_duplicate_candidates

invalidate_duplicate_candidates(ds.df, INPUT, ds.k)

df = ds.df * 1
database = db.Database("data/plan-model", df)
v = database.variables
GLOBAL_INCOME = ds.global_income
K = ds.k
MODES = ds.modes

ESTIMATE, FIXED = 0, 1
_random_terms = []


def fixed(value):
    return value


def est(name, start, lower=None, upper=None):
    return Beta(name, start, lower, upper, ESTIMATE)


def taste(name):
    """Person-level normal random coefficient; one draw per person, shared
    across all plan candidates. sd is estimated."""
    sd = Beta(name + "_s", 1, 0, None, ESTIMATE)
    _random_terms.append(name)
    return sd * bioDraws(name + "_rnd", "NORMAL_ANTI")


# ================================ SPEC =====================================
# Money. UTIL_MONEY is a *transfer choice* (performing-equivalent / VTTS),
# not estimable from this data (no price variation) -- document, don't fit.
UTIL_MONEY = fixed(0.397322)
EXP_INCOME = fixed(0.7)                   # literature band 0.5..1.0
# FUEL = fixed(0)                             # EUR/km car+ride; 0 because fuel is
                                            # inside the pasted CAR_TIME (see guard below)
DAILY_CAR = fixed(-14.30)                   # EUR/day if car used, full value
DAILY_PT = fixed(-3.00)                     # EUR/day if pt used, full value
# DAILY_CAR = fixed(-14.30 * 0.268622)      # published perception variant
# DAILY_PT = fixed(-3.00 * 0.268622)

# Time, structured exactly like trip_model.py so its printed estimates paste
# directly: effective utils/h for mode m = -PERFORMING + <MODE>_TIME.
# PERFORMING must match the value fixed in trip_model.py for the pasted run.
PERFORMING = fixed(6.0)

FUEL = fixed(-0.149)

# --- paste from trip_model.py -------------------
# input
EXP_INCOME = fixed(0)
# EXP_INCOME = est("EXP_INCOME", 0, 0, 1)

# output
WALK_TIME = fixed(-1.129768)
PT_TIME = fixed(4.162085)
CAR_TIME = fixed(3.987524)
BIKE_TIME = fixed(2.085135)
PT_SWITCHES = fixed(-0.318692)

# WALK_TIME = est("WALK_TIME", -1.129768, -5, 5)
# PT_TIME = est("PT_TIME", 4.162085, -5, 5)
# CAR_TIME = est("CAR_TIME", 3.987524, -5, 5)
# BIKE_TIME = est("BIKE_TIME", 2.085135, -5, 5)
# PT_SWITCHES = est("PT_SWITCHES", -0.318692, -5, 5)


# WALK_TIME = fixed(0)
# PT_TIME = fixed(0)
# CAR_TIME = fixed(0)
# BIKE_TIME = fixed(0)
# PT_SWITCHES = fixed(-1)

# ---------------------------------------------------------------------------

# # --- paste from trip_model.py -------------------
# # input
# # EXP_INCOME = fixed(1)
# EXP_INCOME = est("EXP_INCOME", 0, 0, 1)
#
# # output
# WALK_TIME = fixed(-1.171766)
# PT_TIME = fixed(4.172261)
# CAR_TIME = fixed(3.986923)
# BIKE_TIME = fixed(2.030163)
# PT_SWITCHES = fixed(-0.338447)
# # ---------------------------------------------------------------------------

TIME = {
    "walk": -PERFORMING + WALK_TIME,
    "pt": -PERFORMING + PT_TIME,
    "car": -PERFORMING + CAR_TIME,
    "bike": -PERFORMING + BIKE_TIME,
    "ride": 2 * (-PERFORMING + CAR_TIME),   # trip_model convention: 2x car
}

# Note for the eventual config transfer: with performing = PERFORMING, the
# pasted *_TIME values above ARE the marginalUtilityOfTraveling config values
# (positive values are legal but need vspDefaultsChecking=warn). Only ride's
# 2x-car convention needs collapsing: mUTT_ride = 2*CAR_TIME - PERFORMING.
BUS_LEGS = fixed(0)
# BUS_LEGS = est("BUS_LEGS", 0, None, 0)

# ASC_car_inc = est("ASC_car_inc", 0)
# ASC_pt_inc = est("ASC_pt_inc", 0)

ASC_car_inc = fixed(0)
ASC_pt_inc = fixed(0)

# Mode constants: estimated means + person-level random sds. This is the
# experiment: do the sds shrink under the richer systematic spec above?
#
# INC: generic income covariate on the cost-bearing modes' constants.
# Applied per trip (via the usage multiplication below), so it competes for
# exactly the same accumulated variance as the taste draws. Discriminating
# test vs EXP_INCOME: free per-mode income shifts vs money-proportional ones.
# Expected under "income = price sensitivity": ASC_car_inc > 0 (rich less
# deterred from car), ASC_pt_inc near 0, magnitudes tracking the cost gap.
# Variant (threshold form): move the term into plan_utility multiplied by
# v[f"plan_{i}_car_used"] instead -- income acts on the daily commitment.
INC = log(v["income"] / GLOBAL_INCOME)
CONST = {
    "walk": 0,
    "pt": est("ASC_pt", -1), # + ASC_pt_inc * INC + taste("pt"),
    "car": est("ASC_car", -2),
    "bike": est("ASC_bike", -1), # + taste("bike"),
    "ride": est("ASC_ride", -3) + taste("ride"),
}

# Optional: per-trip error components matching the simulation's frozen error
# structure (draw per mode x trip index, shared across alternatives).
EC_S = est("EC_S", 0.5, 0, None)
_EC = {(m, j): bioDraws(f"ec_{m}_{j}", "NORMAL_ANTI") for m in MODES for j in range(7)}


def plan_utility(i):
    u = 0
    for m in MODES:
        u += CONST[m] * v[f"plan_{i}_{m}_usage"]
        u += TIME[m] * v[f"plan_{i}_{m}_hours"]

    money = FUEL * (v[f"plan_{i}_car_km"] + v[f"plan_{i}_ride_km"])
    money += DAILY_CAR * v[f"plan_{i}_car_used"] + DAILY_PT * v[f"plan_{i}_pt_used"]
    u += money * UTIL_MONEY * (GLOBAL_INCOME / v["income"]) ** EXP_INCOME

    u += PT_SWITCHES * v[f"plan_{i}_transfers"]
    u += BUS_LEGS * v[f"plan_{i}_bus_legs"]

    # u += fixed(1) * v[f"plan_{i}_act_util"]   # schedule (activity) utility differences
    u += EC_S * sum(_EC[(m, j)] * v[f"plan_{i}_trip_{j}_mode_{m}"] for m in MODES for j in range(7))
    return u


# ===========================================================================

U = {i: plan_utility(i) for i in range(1, K + 1)}
AV = {i: v[f"plan_{i}_valid"] for i in range(1, K + 1)}

if _random_terms or "EC_S" in dir():
    print("Random terms active:", _random_terms, "-> mixed logit (MonteCarlo)")
    logprob = log(MonteCarlo(models.logit(U, AV, v["choice"])))
else:
    logprob = models.loglogit(U, AV, v["choice"])

iter_file = f"__{MODEL_NAME}.iter"
if os.path.exists(iter_file):
    print(f"Removing stale {iter_file}")
    os.remove(iter_file)

biogeme = bio.BIOGEME(database, {"loglike": logprob, "weight": v["weight"]})
biogeme.modelName = MODEL_NAME
biogeme.calculateNullLoglikelihood(AV)

results = biogeme.estimate()
print(results.short_summary())
print(results.getEstimatedParameters())
