#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Ex-ante identification analysis for the stage-1 (trip-level) mean parameters
-- the companion of trip_model.py, computed directly from the trip-choices CSV
with no estimation. All scenarios are reported; the SPECS list at the bottom IS
the menu -- add an entry to analyze another parameter combination.

Mean parameters enter the utility differences linearly, so the relevant object
is the matrix of differenced regressors x(alternative) - x(chosen) stacked over
(trip, valid non-chosen mode) rows; the Fisher information is a probability-
weighted Gram matrix of exactly these rows, so severe collinearity here is
decisive before any estimation is run. The published spec is nonlinear in its
money block, so each of its columns is the derivative of the utility w.r.t.
that parameter at the published estimates (local identification design):

  u_m = ASC_m - PERF*h_m*(2 if ride) + price_m * UM * (g/inc)^EXP - switches_pt
  price_m = km_cost_m * km_m + daily_m * dist_weight * PERCEPTION

Reported per scenario: singular value spectrum, condition number, pairwise
design correlations, and "uniqueness" = share of a column's design variation
that no other column can imitate (low = ridge; the estimates on such a pair
come out strongly anti-correlated, cf. UTIL_MONEY-PERCEPTION -0.79 in the
published log).

Usage:  python -u trip_identification.py <trip-choices.csv>
"""

import sys

import numpy as np
import pandas as pd

path = sys.argv[1] if len(sys.argv) > 1 else "../../../../trip-choices.csv"

raw = pd.read_csv(path, comment="#")
print(f"{len(raw)} trips (stage-1 trip-level data)")
with open(path) as fh:
    g_inc = float(fh.readline().split(":")[1])

UM0, EXP0, PERC0 = 0.397322, 0.275502, 0.268622  # published estimates
KM_COST = {"car": -0.149, "ride": -0.149}
DAILY = {"car": -14.30, "pt": -3.0}
trip_modes = ["walk", "pt", "car", "bike", "ride"]  # choice is 1-based here

# ---------------------------------------------------------------------------
# design columns: one matrix holding every parameter's regressor; scenarios
# below pick subsets, so mixed published/ours combinations are possible too.
# ---------------------------------------------------------------------------

ALL_COLS = (["asc_" + m for m in ["bike", "ride", "car", "pt"]]      # walk base
            + ["performing", "util_money", "exp_income", "perception"]
            + ["time_" + m for m in ["walk", "bike", "car", "pt"]] + ["pt_switch"])

dw = (raw["beelineDist"] / raw.groupby("person")["beelineDist"].transform("sum")).fillna(1).to_numpy()
inc_f = (g_inc / raw["income"].to_numpy(dtype=float)) ** EXP0
lg = np.log(g_inc / raw["income"].to_numpy(dtype=float))
chosen = np.array(trip_modes)[raw["choice"].to_numpy(dtype=int) - 1]


def trip_x(m):
    x = np.zeros((len(raw), len(ALL_COLS)))
    h = raw[f"{m}_hours"].to_numpy(dtype=float)
    km = raw[f"{m}_km"].to_numpy(dtype=float)
    price = KM_COST.get(m, 0) * km + DAILY.get(m, 0) * dw * PERC0
    if "asc_" + m in ALL_COLS:
        x[:, ALL_COLS.index("asc_" + m)] = 1
    x[:, ALL_COLS.index("performing")] = -h * (2 if m == "ride" else 1)
    x[:, ALL_COLS.index("util_money")] = price * inc_f
    x[:, ALL_COLS.index("exp_income")] = UM0 * price * inc_f * lg
    x[:, ALL_COLS.index("perception")] = UM0 * inc_f * DAILY.get(m, 0) * dw
    if "time_" + m in ALL_COLS:
        x[:, ALL_COLS.index("time_" + m)] = h
    if m == "pt":
        x[:, ALL_COLS.index("pt_switch")] = raw["pt_switches"].to_numpy(dtype=float)
    return x


X = {m: trip_x(m) for m in trip_modes}
x_chosen = sum((chosen == m)[:, None] * X[m] for m in trip_modes)
diff_rows = []
for m in trip_modes:
    mask = (raw[f"{m}_valid"].astype(str).str.lower() == "true").to_numpy() & (chosen != m)
    diff_rows.append((X[m] - x_chosen)[mask])
D = np.vstack(diff_rows)

print(f"""
{D.shape[0]} differenced rows: x(alternative) - x(chosen) per (trip, valid
non-chosen mode); the nonlinear published money block is linearized at the
published estimates. uniqueness = share of a column's design variation no other
column in the scenario can imitate (low = ridge); condition number: <10
comfortable, >100 bad.""")


def report(title, note, cols):
    S = D[:, [ALL_COLS.index(c) for c in cols]]
    sv = np.linalg.svd(S, compute_uv=False)
    cond = sv[0] / sv[-1] if sv[-1] > 0 else np.inf
    print(f"\n--- {title} ---")
    print(note)
    print("condition number: %.1f   singular values: %s"
          % (cond, np.array2string(sv, precision=1)))
    if len(cols) > 2:
        Sn = S - S.mean(axis=0)
        norms = np.linalg.norm(Sn, axis=0)
        norms[norms == 0] = 1
        C = (Sn / norms).T @ (Sn / norms)
        print("pairwise design correlations:")
        print(pd.DataFrame(C, index=cols, columns=cols).round(3).to_string())
    print("uniqueness:")
    for jj, name in enumerate(cols):
        Xo = np.delete(S, jj, axis=1)
        y = S[:, jj]
        coef, *_ = np.linalg.lstsq(Xo, y, rcond=None)
        resid = y - Xo @ coef
        ss_tot = (y ** 2).sum()
        uniq = (resid ** 2).sum() / ss_tot if ss_tot > 0 else 0
        print("  %-10s %.3f" % (name, uniq))


# ---------------------------------------------------------------------------
# SPECS: the scenario list. Each entry: (title, note, columns from ALL_COLS).
# ---------------------------------------------------------------------------

SPECS = [
    ("published stage 1: ASCs + performing + money block",
     "money split over UTIL_MONEY x (income)^EXP_INCOME x PERCEPTION on the\n"
     "daily-cost share -- three parameters multiplying the same euro amounts.\n"
     "(BETA_PT_SWITCHES was fixed at 1, so it is no column here.)",
     ["asc_bike", "asc_ride", "asc_car", "asc_pt",
      "performing", "util_money", "exp_income", "perception"]),

    ("our stage 1: ASCs + mode-specific time offsets + switches",
     "performing fixed at 6, money fixed at full published prices, income exp 0;\n"
     "ride time tied to performing (x2), no free ride column.",
     ["asc_bike", "asc_ride", "asc_car", "asc_pt",
      "time_walk", "time_bike", "time_car", "time_pt", "pt_switch"]),
]

for spec in SPECS:
    report(*spec)
