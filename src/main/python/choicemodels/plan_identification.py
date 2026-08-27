#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Ex-ante identification analysis for the stage-2 (plan-level) model -- the
companion of plan_model.py, computed directly from a plan-choices CSV with no
estimation and no draws. All scenarios are reported; the SPECS list at the
bottom IS the menu -- add an entry to analyze another parameter combination.

Variance parameters (Walker/Ben-Akiva/Bolduc 2007 style, NECLM):
Each variance parameter theta contributes theta^2 * f_theta(a,b) to the
covariance of plan utilities, where the "design" f depends only on the
candidate structure:
  taste_m: f(a,b) = n_m(a) * n_m(b)          (person-level draw, usage loading)
  ec_m:    f(a,b) = overlap_m(a,b)           (draw per (mode,slot), same-slot only)
  ec:      sum of ec_m over all modes        (the single pooled scale EC_S)
Only utility *differences* are estimable, so the identifiable design for a pair
(a,b) of non-reference alternatives is
  g(a,b) = f(a,b) - f(a,r) - f(b,r) + f(r,r)         (r = chosen plan).
Stacking g over all persons and pairs gives the design matrix; a variance
parameter is identified iff its column is not (nearly) in the span of the
others. Also counted: "separating cells" per mode (same-mode-different-slot
pairs), which is where the taste-vs-EC separation formally lives.

Mean parameters: what happens if the stage-1 parameters (time offsets, pt
switches, money, ASCs) are opened up inside stage 2 on the same data. Mean
parameters enter the utility differences linearly, so the relevant matrix is
simply the differenced regressors x(a) - x(chosen) stacked over persons and
candidates; the Fisher information is a probability-weighted Gram matrix of
these rows, so severe collinearity here is decisive ex ante. (The trip-level
stage-1 counterpart of this analysis is trip_identification.py.)

Reported per scenario: singular value spectrum, condition number, pairwise
design correlations, and "uniqueness" = share of a column's design variation
that no other column can imitate (low = ridge).

Usage: python plan_identification.py <plan-choices-*.csv>
"""

import sys

import numpy as np
import pandas as pd

MODES = ["walk", "bike", "ride", "car", "pt"]
TASTE_MODES = ["car", "pt", "bike", "ride"]
MAX_TRIPS = 7

path = sys.argv[1] if len(sys.argv) > 1 else "../../../../plan-choices-subtour_9.csv"

raw = pd.read_csv(path, comment="#")
k = raw.columns.str.extract(r"plan_(\d+)", expand=False).dropna().astype(int).max()
print(f"{len(raw)} persons, k={k}")

# ---------------------------------------------------------------------------
# design columns
# ---------------------------------------------------------------------------

# variance parameters: taste sds, per-mode EC scales, pooled EC scale
VAR_COLS = ([f"taste_{m}" for m in TASTE_MODES]
            + [f"ec_{m}" for m in MODES] + ["ec"])

# stage-1 mean parameters and the plan-level regressors they multiply
MEAN_COLS = (["asc_" + m for m in ["bike", "ride", "car", "pt"]]      # walk base
             + ["time_" + m for m in MODES] + ["pt_switch", "money"])


def mean_x(person, i):
    x = [person[f"plan_{i}_{m}_usage"] for m in ["bike", "ride", "car", "pt"]]
    x += [person[f"plan_{i}_{m}_hours"] for m in MODES]
    x += [person[f"plan_{i}_transfers"]]
    money = -0.149 * (person[f"plan_{i}_car_km"] + person[f"plan_{i}_ride_km"])
    money += -14.30 * (person[f"plan_{i}_car_usage"] > 0) - 3.00 * (person[f"plan_{i}_pt_usage"] > 0)
    x += [money]
    return np.array(x, dtype=float)


var_rows = []
mean_rows = []
separating_cells = {m: 0 for m in TASTE_MODES}

for _, person in raw.iterrows():
    # collect valid, deduplicated plans as mode sequences
    plans = []
    plan_ids = []
    seen = set()
    for i in range(1, k + 1):
        if person[f"plan_{i}_valid"] != 1:
            continue
        seq = tuple(person[f"plan_{i}_trip_{j}_mode"] for j in range(MAX_TRIPS))
        if seq in seen:
            continue
        seen.add(seq)
        plans.append(seq)
        plan_ids.append(i)
    if len(plans) < 2:
        continue

    x_ref = mean_x(person, plan_ids[0])
    for i in plan_ids[1:]:
        mean_rows.append(mean_x(person, i) - x_ref)

    ref = plans[0]  # chosen plan is candidate 1

    def counts(seq):
        return {m: sum(1 for x in seq if x == m) for m in TASTE_MODES}

    def f_vec(a, b):
        ca, cb = counts(a), counts(b)
        v = [ca[m] * cb[m] for m in TASTE_MODES]
        per_mode = [sum(1 for x, y in zip(a, b) if x == y == m) for m in MODES]
        v += per_mode + [sum(per_mode)]
        return np.array(v, dtype=float)

    others = plans[1:]
    f_rr = f_vec(ref, ref)
    f_r = {id(p): f_vec(p, ref) for p in others}
    for ai in range(len(others)):
        a = others[ai]
        for bi in range(ai, len(others)):
            b = others[bi]
            g = f_vec(a, b) - f_r[id(a)] - f_r[id(b)] + f_rr
            var_rows.append(g)

    # separating cells: same-mode, different-slot pairs across any two plans
    for a in plans:
        for b in plans:
            if a is b:
                continue
            for m in TASTE_MODES:
                cross = counts(a)[m] * counts(b)[m]
                same_slot = sum(1 for x, y in zip(a, b) if x == y == m)
                if cross > same_slot:
                    separating_cells[m] += 1

G = np.array(var_rows)      # variance designs, VAR_COLS
M = np.array(mean_rows)     # mean designs, MEAN_COLS

# derived column: one uniform utility of time (performing) = -(total hours,
# all modes alike) -- for the "one time + one money coefficient" scenarios.
# MEAN_COLS_BASE stays without it: the stage-1-means scenarios below must not
# silently include performing (the runs they describe did not have it free).
MEAN_COLS_BASE = MEAN_COLS
performing = -sum(M[:, MEAN_COLS.index(f"time_{m}")] for m in MODES)
M = np.column_stack([M, performing])
MEAN_COLS = MEAN_COLS + ["performing"]

print(f"""
variance design matrix: {G.shape[0]} plan-pair rows; mean design matrix: {M.shape[0]} candidate rows.

variance columns (covariance designs over plan pairs):
  taste_m : person-level taste sd of mode m (the modeTasteVariations attributes;
            published values car 1.507 / pt 1.738 / bike 0.880 / ride 2.861).
            walk has no taste column -- the published model normalizes it out.
  ec      : the single pooled situational error scale EC_S (one draw per
            (mode, slot); estimated 2.213 in specs C/D).
  ec_m    : PROSPECTIVE per-mode EC scales, incl. walk -- not estimated yet.

mean columns (differenced regressors x(candidate) - x(chosen)):
  asc_m     : delta trip count of mode m (walk is the base alternative)
  time_m    : delta door-to-door hours of mode m
  pt_switch : delta pt transfers (PT_SWITCHES, fixed at -1 in the published stage 2)
  money     : delta money at published prices (fuel 0.149 EUR/km on car+ride km,
              daily 14.30/3.00 EUR if car/pt used) -- UTIL_MONEY's regressor

reading guide: uniqueness = share of a column's design variation that no other
column in the scenario can imitate; near 1 = independently identified, near 0 =
the likelihood cannot tell this parameter from a combination of the others (the
ridge/'siphoning' situation). condition number: <10 comfortable, >100 bad.
watch asc_m <-> time_m: substituting one trip to mode m moves the count by 1 AND
the hours by ~a typical trip duration, so separation rests only on duration
variation across trips. money vs car columns inherits corr(km, hours) ~ 0.94.""")


def report(title, note, cols, matrix, all_cols):
    idx = [all_cols.index(c) for c in cols]
    S = matrix[:, idx]
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
        X = np.delete(S, jj, axis=1)
        y = S[:, jj]
        coef, *_ = np.linalg.lstsq(X, y, rcond=None)
        resid = y - X @ coef
        ss_tot = (y ** 2).sum()
        uniq = (resid ** 2).sum() / ss_tot if ss_tot > 0 else 0
        print("  %-10s %.3f" % (name, uniq))


# ---------------------------------------------------------------------------
# SPECS: the scenario list. Each entry: (title, note, columns, matrix, all_cols)
# with matrix G/VAR_COLS for variance scenarios, M/MEAN_COLS for mean scenarios.
# ---------------------------------------------------------------------------

SPECS = [
    ("published spec: 4 taste sds, no EC",
     "the taste-variation structure as shipped in v6.4/v7 (plus the sigma-vs-\n"
     "variance plan-Gumbel split, which is a fixed pattern, not a free column).",
     [f"taste_{m}" for m in TASTE_MODES], G, VAR_COLS),

    ("spec D: 4 taste sds + EC_S (the 2000-draw ridge run)",
     "if conditioning is fine here, the empirical ridge we saw was information\n"
     "scarcity / draw noise / mean-sd coupling -- not a broken design.",
     [f"taste_{m}" for m in TASTE_MODES] + ["ec"], G, VAR_COLS),

    ("spec C: ride_s + EC_S (the winning re-estimated spec)",
     "the pair that stayed stable between nDraws 300 and 2000.",
     ["taste_ride", "ec"], G, VAR_COLS),

    ("prospective spec: per-mode EC scales (+ the 4 taste sds)",
     "not estimated yet; watch the taste_m <-> ec_m pairs -- within one mode,\n"
     "person-level sd and per-slot scale can only be told apart by the\n"
     "separating cells below.",
     [f"taste_{m}" for m in TASTE_MODES] + [f"ec_{m}" for m in MODES], G, VAR_COLS),

    ("stage-1 means opened in stage 2: all free (the run that failed)",
     "ASCs, all times, switches -- money stays fixed, as it did: that money\n"
     "must be 'guessed' was decided out of band, before any estimation.\n"
     "observed: PT_TIME past +5, WALK_TIME/PT_SWITCHES insignificant, ASC_car -> 0.",
     [c for c in MEAN_COLS_BASE if c != "money"], M, MEAN_COLS),

    ("stage-1 means opened in stage 2: money opened too (never actually run)",
     "what the out-of-band decision to fix money spared us.",
     MEAN_COLS_BASE, M, MEAN_COLS),

    ("normalization check: performing + all five mode times (deliberately rank-deficient)",
     "performing = -(sum of the five time columns) exactly, so only the five\n"
     "sums (-performing + time_m) are identified: expect one zero singular value\n"
     "and uniqueness 0.000 throughout. the normalization must be spent once --\n"
     "fix performing (our anchor at 6) or fix one time offset (value arbitrary);\n"
     "published stage 1 instead fixed ALL five offsets: four extra restrictions,\n"
     "not a normalization.",
     ["performing"] + [f"time_{m}" for m in MODES], M, MEAN_COLS),

    ("fundamental: one utility of time + one utility of money, nothing else",
     "the classical-grounding question: performing (uniform -hours, all modes\n"
     "alike) and util_money (full daily cost if mode used, fuel on car+ride km);\n"
     "no ASCs, no mode-specific times, no tastes, no error components.",
     ["performing", "money"], M, MEAN_COLS),

    ("fundamental: time + money with ASCs (any deployable spec keeps constants)",
     "same two coefficients, but mode constants present -- the known money-eaters:\n"
     "does the money column survive next to asc_car?",
     ["asc_bike", "asc_ride", "asc_car", "asc_pt", "performing", "money"],
     M, MEAN_COLS),

    ("published stage 2: ASCs + time_ride + time_bike free",
     "the shipped division of labor: walk/pt/car times, switches and money\n"
     "frozen from stage 1; ride and bike time re-estimated at plan level.",
     [c for c in MEAN_COLS if c.startswith("asc_")] + ["time_ride", "time_bike"],
     M, MEAN_COLS),
]

for spec in SPECS:
    report(*spec)

print("\nseparating cells (same-mode-different-slot plan pairs): the only design\n"
      "feature that distinguishes taste_m (loads on n_a*n_b, slot-blind) from an\n"
      "EC in mode m (loads on same-slot overlap only). zero here would mean\n"
      "taste-vs-EC separation is impossible for that mode:")
for m, c in separating_cells.items():
    print("  %-6s %d" % (m, c))
