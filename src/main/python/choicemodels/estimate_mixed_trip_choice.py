#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
from collections import defaultdict

import numpy as np
from xlogit import MixedLogit, MultinomialLogit
from xlogit.utils import wide_to_long

from prepare import read_trip_choices

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate the trip choice model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../../trip-choices.csv")
    parser.add_argument("--seed", help="Random seed", type=int, default=42)
    parser.add_argument("--n-draws", help="Number of draws for the estimation", type=int, default=1500)
    parser.add_argument("--mxl-modes", help="Modes to use mixed logit for", nargs="+", type=set,
                        default=["pt", "bike", "ride"])
    parser.add_argument("--est-performing", help="Estimate the beta for performing", action="store_true")
    parser.add_argument("--mnl", help="Use MNL instead of mixed logit", action="store_true")

    args = parser.parse_args()

    ds = read_trip_choices(args.input)

    df_wide = ds.df

    df_wide['custom_id'] = np.arange(len(df_wide))  # Add unique identifier
    df_wide['choice'] = df_wide['choice'].map(dict(enumerate(ds.modes, 1)))

    km_costs = defaultdict(lambda: 0.0, car=-0.149, ride=-0.149)

    for mode in ds.modes:
        # Base asc
        df_wide[f"{mode}_costs"] = df_wide[f"{mode}_km"] * km_costs[mode]

        df_wide[f"{mode}_time"] = df_wide[f"{mode}_hours"]

        # Ride opportunity costs is doubled
        if mode == "ride":
            df_wide[f"{mode}_time"] += df_wide[f"{mode}_hours"]

    ds.varying.extend(["costs", "time"])

    df = wide_to_long(df_wide, id_col='custom_id', alt_name='alt', sep='_',
                      alt_list=ds.modes, empty_val=0,
                      varying=ds.varying, alt_is_prefix=True)

    for mode in ds.modes:
        df[f'asc_{mode}'] = np.ones(len(df)) * (df['alt'] == mode)

    MixedLogit.check_if_gpu_available()

    varnames = ['asc_car', 'asc_pt', 'asc_bike', 'asc_ride']
    randvars = {'asc_' + mode: 'n' for mode in args.mxl_modes}

    print("Random variables", randvars)

    fixedvars = {}
    addit = df['costs'] * df['util_money']

    if args.est_performing:
        varnames += ['time']
    else:
        addit += -6.88 * df['time']

    if not args.mnl:
        model = MixedLogit()
        model.fit(X=df[varnames], y=df['choice'], weights=df['weight'], varnames=varnames,
                  alts=df['alt'], ids=df['custom_id'], avail=df['valid'], random_state=args.seed, addit=addit,
                  panels=df['person'], skip_std_errs=True, randvars=randvars, fixedvars=fixedvars, n_draws=args.n_draws,
                  optim_method='BFGS')

    else:
        model = MultinomialLogit()
        model.fit(X=df[varnames], y=df['choice'], weights=df['weight'], varnames=varnames, addit=addit,
                  alts=df['alt'], ids=df['custom_id'], avail=df['valid'], random_state=args.seed)

    model.summary()
