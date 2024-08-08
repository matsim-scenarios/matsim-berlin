#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import numpy as np
import pandas as pd
from xlogit import MixedLogit, MultinomialLogit
from xlogit.utils import wide_to_long

from estimate_plan_choice import calc_costs

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate the plan choice mixed logit model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../plan-choices.csv")
    parser.add_argument("--n-draws", help="Number of draws for the estimation", type=int, default=1500)
    parser.add_argument("--batch-size", help="Batch size for the estimation", type=int, default=None)
    parser.add_argument("--sample", help="Use sample of choice data", type=float, default=0.2)
    parser.add_argument("--seed", help="Random seed", type=int, default=0)
    parser.add_argument("--mnl", help="Use MNL instead of mixed logit", action="store_true")

    args = parser.parse_args()

    df_wide = pd.read_csv(args.input, comment="#")

    modes = list(df_wide.columns.str.extract(r"_([a-zA-z]+)_usage", expand=False).dropna().unique())
    print("Modes: ", modes)

    k = df_wide.columns.str.extract(r"plan_(\d+)", expand=False).dropna().to_numpy(int).max()
    print("Number of plans: ", len(df_wide))
    print("Number of choices for plan: ", k)

    # df_wide["p_id"] = df_wide["p_id"].str.replace(r"_\d+$", "", regex=True)
    # df_wide["person"] = df_wide["person"].astype('category').cat.codes

    # sample = set(df_wide.person.sample(frac=0.2))
    # df_wide = df_wide[df_wide.person.isin(sample)]
    if args.sample < 1:
        df_wide = df_wide.sample(frac=args.sample, random_state=args.seed)

    print("Modes:", modes)
    print("Number of choices:", len(df_wide))

    df_wide['custom_id'] = np.arange(len(df_wide))  # Add unique identifier
    df_wide['choice'] = df_wide['choice'].map({1: "plan_1"})

    df_wide = calc_costs(df_wide, k, modes)

    varying = list(df_wide.columns.str.extract(r"plan_1_([a-zA-z_]+)", expand=False).dropna().unique())

    print("Varying:", varying)

    df = wide_to_long(df_wide, id_col='custom_id', alt_name='alt', sep='_',
                      alt_list=[f"plan_{i}" for i in range(1, k + 1)], empty_val=0,
                      varying=varying, alt_is_prefix=True)

    MixedLogit.check_if_gpu_available()


    # ASC is present as mode_usage
    varnames = [f"{mode}_usage" for mode in modes if mode != "walk" and mode != "car"] + ["car_used"]
    # varnames += ["pt_ride_hours", "car_ride_hours", "bike_ride_hours"]
    # varnames = ["car_used", "car_usage"]

    # Additive costs
    # utils contains the price and opportunist costs
    addit = df["utils"] - df["pt_n_switches"]

    if not args.mnl:
        model = MixedLogit()
        model.fit(X=df[varnames], y=df['choice'], weights=df['weight'], varnames=varnames,
                  alts=df['alt'], ids=df['custom_id'], avail=df['valid'], random_state=args.seed,
                  addit=addit,
                  # randvars={"car_used": "tn"},
                  randvars={"car_used": "tn", "bike_usage": "n", "pt_usage": "n", "ride_usage": "n"},
                  fixedvars={"car_used": None},
                  n_draws=args.n_draws, batch_size=args.batch_size, halton=True, skip_std_errs=True,
                  optim_method='L-BFGS-B')

    else:
        #varnames += ["car_usage"]

        model = MultinomialLogit()
        model.fit(X=df[varnames], y=df['choice'], weights=df['weight'], varnames=varnames,
                  alts=df['alt'], ids=df['custom_id'], avail=df['valid'], random_state=args.seed,
                  addit=addit)

    model.summary()
