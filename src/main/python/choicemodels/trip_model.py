#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Trip-level mode choice model with the specification written out explicitly.

The SPEC section below IS the model: every coefficient and every utility term
is literal. There are no defaults and no flags -- what you read is what gets
estimated.

  fixed(v)                     constant, not estimated
  est("NAME", start, lo, hi)   estimated parameter, starting at `start`
  taste("name")                person-level normal random coefficient
                               (any active taste() term turns the model into a
                               panel mixed logit over each person's trips)

Data columns available per mode m (from ComputeTripChoices):
  v[f"{m}_hours"], v[f"{m}_km"], v[f"{m}_walking_km"], v[f"{m}_switches"],
  v[f"{m}_bus_legs"], v[f"{m}_valid"]; person-level: v["income"], v["dist_weight"]
  (this trip's share of the person's total daily beeline distance -- used to
  prorate daily fixed costs to trips).

Usage:  python -u trip_model.py <trip-choices.csv> [model-name]
"""

import os
import sys

import biogeme.biogeme as bio
import biogeme.database as db
import biogeme.models as models
from biogeme.expressions import Beta, MonteCarlo, PanelLikelihoodTrajectory, bioDraws, log

from prepare import read_trip_choices

INPUT = sys.argv[1] if len(sys.argv) > 1 else "../../../../trip-choices.csv"
MODEL_NAME = sys.argv[2] if len(sys.argv) > 2 else "trip_model"

ds = read_trip_choices(INPUT)
df = ds.df * 1
database = db.Database("data/trip-model", df)
v = database.variables
GLOBAL_INCOME = ds.global_income

ESTIMATE, FIXED = 0, 1
_random_terms = []


def fixed(value):
    return value


def est(name, start, lower=None, upper=None):
    return Beta(name, start, lower, upper, ESTIMATE)


def taste(name):
    """Person-level normal random coefficient: sd is estimated, draw is shared
    across all trips of a person (panel)."""
    sd = Beta(name + "_s", 1, 0, None, ESTIMATE)
    _random_terms.append(name)
    return sd * bioDraws(name + "_rnd", "NORMAL_ANTI")


def money(eur):
    """Income-scaled utility of a money amount (negative = cost)."""
    return eur * UTIL_MONEY * (GLOBAL_INCOME / v["income"]) ** EXP_INCOME


# ================================ SPEC =====================================
# Coefficients. Published stage-1 values; swap the fixed()/est() lines to
# (un)estimate a coefficient.

# PERFORMING = fixed(6)            # utils/h opportunity cost of (travel) time
# DIST_CAR = fixed(-0.149)   # -0.149
# CAR_TIME = est("CAR_TIME", 0, -50, 50)
# PT_TIME = est("PT_TIME", 0, -50, 50)
# BIKE_TIME = est("BIKE_TIME", 0, -50, 50)
# WALK_TIME = est("WALK_TIME", 0, -50, 50)
# UTIL_MONEY = fixed(0.397322)            # utils per EUR
# EXP_INCOME = fixed(1)            # money scaled by (avg_income/income)^x
# PT_SWITCHES = est("PT_SWITCHES", 0, -10, 0)
#
#
# ASC_PT = est("ASC_pt", 0)               # walk is the reference (ASC = 0)
# ASC_CAR = est("ASC_car", 0)
# ASC_BIKE = est("ASC_bike", 0)
# ASC_RIDE = est("ASC_ride", 0)



#######

# PERFORMING = fixed(6)            # utils/h opportunity cost of (travel) time
# PERFORMING = est("PERFORMING", 0, 0, 50)
PERFORMING = est("PERFORMING", 6, 0, 15)


# DIST_CAR = est("DIST_CAR", 0, -15, 15)   # -0.149
DIST_CAR = fixed(-0.149)   # -0.149
CAR_TIME = fixed(0)
PT_TIME = fixed(0)
BIKE_TIME = fixed(0)
WALK_TIME = fixed(0)

# UTIL_MONEY = fixed(1)
UTIL_MONEY = fixed(0)            # utils per EUR
# UTIL_MONEY = est("UTIL_MONEY", 0.4, 0, 1.5)

EXP_INCOME = fixed(1)            # money scaled by (avg_income/income)^x
# EXP_INCOME = fixed(0.275502)            # money scaled by (avg_income/income)^x
# EXP_INCOME = est("EXP_INCOME", 0.3, 0, 1.5)

# PRICE_PERCEPTION = fixed(0)      # applied to daily (fixed) costs only
# PRICE_PERCEPTION = fixed(0.268622)      # applied to daily (fixed) costs only
# PRICE_PERCEPTION = est("PRICE_PERCEPTION", 0.27, 0, 1)


# PT_SWITCHES = est("PT_SWITCHES", 0, -10, 0)
# PT_SWITCHES = fixed(-1)
PT_SWITCHES = fixed(0)


ASC_PT = est("ASC_pt", 0)               # walk is the reference (ASC = 0)
ASC_CAR = est("ASC_car", 0)
ASC_BIKE = est("ASC_bike", 0)
ASC_RIDE = est("ASC_ride", 0)

# ASC_PT = fixed(0)               # walk is the reference (ASC = 0)
# ASC_CAR = fixed(0)
# ASC_BIKE = fixed(0)
# ASC_RIDE = fixed(0)


# To pin an ASC instead:  ASC_PT = fixed(-0.731102)

# Utilities. Each line is one term; comment in/out at will.

U = {}

U["walk"] = (
    - PERFORMING * v["walk_hours"]
    + WALK_TIME * v["walk_hours"]
)

U["pt"] = (
    ASC_PT
    # + taste("pt")
    - PERFORMING * v["pt_hours"]
    + PT_TIME * v["pt_hours"]
   # + money(-3.00 * v["dist_weight"] * PRICE_PERCEPTION)    # daily ticket, prorated to this trip
   + PT_SWITCHES * v["pt_switches"]                                # transfer penalty (utilityOfLineSwitch)
    # - 0.164051 * v["pt_bus_legs"]                         # bus submode penalty (stage-2 value)
)

U["car"] = (
    ASC_CAR
    # + taste("car")
    - PERFORMING * v["car_hours"]
   + CAR_TIME * v["car_hours"]
   + money(DIST_CAR * v["car_km"])                            # petrol only
  #  + money(- 14.30 * v["dist_weight"] * PRICE_PERCEPTION)  # daily fixed cost, prorated
)

U["bike"] = (
    ASC_BIKE
    # + taste("bike")
    - PERFORMING * v["bike_hours"]
    + BIKE_TIME * v["bike_hours"]
)

U["ride"] = (
    ASC_RIDE
    # + taste("ride")
    - 2 * PERFORMING * v["ride_hours"]
   + 2 * CAR_TIME * v["ride_hours"]
   + money(DIST_CAR * v["ride_km"])                          # petrol, driver's car
)
# ===========================================================================

assert set(U) == set(ds.modes), f"spec modes {set(U)} != data modes {set(ds.modes)}"
U_num = {i + 1: U[m] for i, m in enumerate(ds.modes)}
AV = {i + 1: v[f"{m}_valid"] for i, m in enumerate(ds.modes)}

if _random_terms:
    print("Random taste terms active:", _random_terms, "-> panel mixed logit")
    database.panel("person")
    logprob = log(MonteCarlo(PanelLikelihoodTrajectory(models.logit(U_num, AV, v["choice"]))))
else:
    logprob = models.loglogit(U_num, AV, v["choice"])

# A stale .iter file would silently warm-start a *different* spec; remove it.
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
