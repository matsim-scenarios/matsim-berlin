#!/usr/bin/env python
# -*- coding: utf-8 -*-

from collections import defaultdict

import argparse

from xlogit import MixedLogit
from xlogit.utils import wide_to_long


import numpy as np
import pandas as pd
from scipy.special import softmax

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate the trip choice model")
    parser.add_argument("--input", help="Path to the input file", type=str, default="../../../trip-choices.csv")

    args = parser.parse_args()

    df_wide = pd.read_csv(args.input)

    #df_wide = df_wide.sample(frac=0.1)

    modes = list(df_wide.columns.str.extract(r"([a-zA-z]+)_valid", expand=False).dropna().unique())
    print("Modes:", modes)
    print("Number of choices:", len(df_wide))

    df_wide['custom_id'] = np.arange(len(df_wide))  # Add unique identifier
    df_wide['choice'] = df_wide['choice'].map(dict(enumerate(modes, 1)))

    km_costs = defaultdict(lambda: 0.0, car=-0.149, ride=-0.149)

    for mode in modes:
        # Base asc
        df_wide[f"{mode}_costs"] = df_wide[f"{mode}_km"] * km_costs[mode]

        # Add the time costs
        if mode == "ride":
            df_wide[f"{mode}_costs"] += -6.88 * df_wide[f"{mode}_hours"]

        df_wide = df_wide.drop(columns=[f"{mode}_walk_km"])

    df_wide["p_id"] = df_wide["p_id"].str.replace(r"_\d+$", "", regex=True)
    df_wide["p_id"] = df_wide["p_id"].astype('category').cat.codes

    varying = list(df_wide.columns.str.extract(r"walk_([a-zA-z]+)", expand=False).dropna().unique())

    print("Varying:", varying)

    df = wide_to_long(df_wide, id_col='custom_id', alt_name='alt', sep='_',
                      alt_list=modes, empty_val=0,
                      varying=varying, alt_is_prefix=True)

    for mode in modes:
        df[f'asc_{mode}'] = np.ones(len(df))*(df['alt'] == mode)

    MixedLogit.check_if_gpu_available()

    model = MixedLogit()

    varnames=['asc_car', 'asc_pt', 'asc_bike', 'asc_ride']

    model.fit(X=df[varnames], y=df['choice'], varnames=varnames,
              alts=df['alt'], ids=df['custom_id'], avail=df['valid'],
              panels=df["p_id"], randvars={'asc_car': 'n'}, n_draws=1500,
              optim_method='L-BFGS-B')

    model.summary()