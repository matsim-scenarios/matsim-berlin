#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

from xlogit import MixedLogit, MultinomialLogit
from xlogit.utils import wide_to_long

from prepare import read_plan_choices

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Estimate the plan choice mixed logit model")
    parser.add_argument("--input", help="Path to the input file", type=str,
                        default="../../../plan-choices-bestK_9-tt-only.csv")
    parser.add_argument("--n-draws", help="Number of draws for the estimation", type=int, default=1500)
    parser.add_argument("--batch-size", help="Batch size for the estimation", type=int, default=None)
    parser.add_argument("--sample", help="Use sample of choice data", type=float, default=1)
    parser.add_argument("--seed", help="Random seed", type=int, default=0)
    parser.add_argument("--mnl", help="Use MNL instead of mixed logit", action="store_true")

    args = parser.parse_args()

    ds = read_plan_choices(args.input, sample=args.sample, seed=args.seed)

    df = wide_to_long(ds.df, id_col='custom_id', alt_name='alt', sep='_',
                      alt_list=[f"plan_{i}" for i in range(1, ds.k + 1)], empty_val=0,
                      varying=ds.varying, alt_is_prefix=True)

    MixedLogit.check_if_gpu_available()

    # ASC is present as mode_usage
    varnames = [f"{mode}_usage" for mode in ds.modes if mode != "walk" and mode != "car"] + ["car_used"] + ["tt_hours"]
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
                  randvars={"car_used": "tn", "tt_hours": "tn", "bike_usage": "n", "pt_usage": "n", "ride_usage": "n"},
                  fixedvars={"car_used": None, "tt_hours": None},
                  n_draws=args.n_draws, batch_size=args.batch_size, halton=True, skip_std_errs=True,
                  optim_method='L-BFGS-B')

    else:
        # varnames += ["car_usage"]

        model = MultinomialLogit()
        model.fit(X=df[varnames], y=df['choice'], weights=df['weight'], varnames=varnames,
                  alts=df['alt'], ids=df['custom_id'], avail=df['valid'], random_state=args.seed,
                  addit=addit)

    model.summary()
