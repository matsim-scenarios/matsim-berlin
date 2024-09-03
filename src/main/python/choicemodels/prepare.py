#!/usr/bin/env python
# -*- coding: utf-8 -*-

from collections import namedtuple, defaultdict

import numpy as np
import pandas as pd
from scipy.stats import truncnorm

# Cost parameter from current berlin model
daily_costs = defaultdict(lambda: 0.0, car=-14.30, pt=-3)
km_costs = defaultdict(lambda: 0.0, car=-0.149, ride=-0.149)

TN = truncnorm(0, np.inf)

PlanChoice = namedtuple("PlanChoice", ["df", "modes", "varying", "k", "global_income"])
TripChoice = namedtuple("TripChoice", ["df", "modes", "varying", "global_income"])


def read_global_income(input_file: str) -> float:
    """ Read global income from input file """

    with open(input_file) as f:
        _, _, income = f.readline().rpartition(":")
        return float(income.strip())


def read_plan_choices(input_file: str, sample: float = 1, seed: int = 42) -> PlanChoice:
    """ Read plan choices from input file """

    df_wide = pd.read_csv(input_file, comment="#")

    modes = list(df_wide.columns.str.extract(r"_([a-zA-z]+)_usage", expand=False).dropna().unique())
    print("Modes: ", modes)

    k = df_wide.columns.str.extract(r"plan_(\d+)", expand=False).dropna().to_numpy(int).max()
    print("Number of plans: ", len(df_wide))
    print("Number of choices for plan: ", k)

    # df_wide["p_id"] = df_wide["p_id"].str.replace(r"_\d+$", "", regex=True)
    # df_wide["person"] = df_wide["person"].astype('category').cat.codes

    # sample = set(df_wide.person.sample(frac=0.2))
    # df_wide = df_wide[df_wide.person.isin(sample)]
    if sample < 1:
        df_wide = df_wide.sample(frac=sample, random_state=seed)

    print("Number of choices:", len(df_wide))

    df_wide['custom_id'] = np.arange(len(df_wide))  # Add unique identifier
    df_wide['choice'] = df_wide['choice'].map({1: "plan_1"})

    df_wide = calc_plan_variables(df_wide, k, modes, False, True)

    varying = list(df_wide.columns.str.extract(r"plan_1_([a-zA-z_]+)", expand=False).dropna().unique())

    return PlanChoice(df_wide, modes, varying, k, read_global_income(input_file))


def tn_generator(sample_size: int, number_of_draws: int) -> np.ndarray:
    """
    User-defined random number generator to the database.
    See the numpy.random documentation to obtain a list of other distributions.
    """
    return TN.rvs((sample_size, number_of_draws))


def calc_plan_variables(df, k, modes, use_util_money=False, add_util_performing=True):
    """ Calculate utility and costs variables for all alternatives in the dataframe"""

    util_performing = -6.88

    # Normalize activity utilities to be near zero
    # columns = [f"plan_{i}_act_util" for i in range(1, k + 1)]
    # for t in df.itertuples():
    #     utils = df.loc[t.Index, columns]
    #     df.loc[t.Index, columns] -= utils.max()

    # Marginal utility of money as factor
    util_money = df.util_money if use_util_money else 1

    for i in range(1, k + 1):

        # Price is only monetary costs
        df[f"plan_{i}_price"] = 0
        df[f"plan_{i}_car_price"] = 0
        df[f"plan_{i}_pt_price"] = 0
        df[f"plan_{i}_other_price"] = 0

        # Costs will also include time costs
        df[f"plan_{i}_utils"] = 0

        df[f"plan_{i}_tt_hours"] = 0

        for mode in modes:

            fixed_costs = (df[f"plan_{i}_{mode}_usage"] > 0) * daily_costs[mode]
            distance_costs = df[f"plan_{i}_{mode}_km"] * km_costs[mode]

            df[f"plan_{i}_{mode}_fixed_cost"] = fixed_costs
            df[f"plan_{i}_price"] += fixed_costs + distance_costs

            if mode == "car":
                df[f"plan_{i}_car_price"] += fixed_costs
            elif mode == "pt":
                df[f"plan_{i}_pt_price"] += fixed_costs
            else:
                df[f"plan_{i}_other_price"] += fixed_costs

            df[f"plan_{i}_other_price"] += distance_costs

            df[f"plan_{i}_{mode}_used"] = (df[f"plan_{i}_{mode}_usage"] > 0) * 1
            df[f"plan_{i}_tt_hours"] -= df[f"plan_{i}_{mode}_hours"]

            # Add configured time costs
            df[f"plan_{i}_utils"] += (fixed_costs + distance_costs) * util_money

            if add_util_performing:
                # Add time costs the overall costs
                df[f"plan_{i}_utils"] += util_performing * df[f"plan_{i}_{mode}_hours"]

                # Add additional ride time utils for the driver
                if mode == "ride":
                    df[f"plan_{i}_utils"] += util_performing * df[f"plan_{i}_{mode}_hours"]

        # Defragment df
        df = df.copy()

    return df


def read_trip_choices(input_file: str) -> TripChoice:
    """ Read trip choices from input file """

    df = pd.read_csv(input_file, comment="#")

    modes = list(df.columns.str.extract(r"([a-zA-z]+)_valid", expand=False).dropna().unique())
    print("Modes: ", modes)
    print("Number of choices: ", len(df))

    varying = list(df.columns.str.extract(r"walk_([a-zA-z]+)", expand=False).dropna().unique())

    print("Varying:", varying)

    return TripChoice(df, modes, varying, read_global_income(input_file))
