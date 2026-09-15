#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Calibrate the alternative specific constants (ASC) of the mode choice against the SrV mode shares.

This is the outer loop: each trial runs the scenario with one set of constants, reads the resulting
mode shares, and derives the constants of the next trial from them. It is driven from the Makefile
(`make prepare-asc-calibration`), which builds the config, points it at the population of the
requested sample size and gives every sample size its own run directory, so that the ensembles of
different sample sizes can live in the tree next to each other.

Needs the calibration extra of the matsim python tools, see src/main/sh/setup.sh.
"""

import argparse
import glob
import os
import shutil
import sys

from matsim.calibration import create_calibration, ASCCalibrator, utils

modes = ["walk", "car", "pt", "bike", "ride"]
fixed_mode = "walk"

# Starting point of the calibration; the same values are in the config, this is what the first
# trial uses. Taken over from the last calibrated release, so the calibration starts warm.
initial = {
    "bike": -1.0925403188187748,
    "pt": -1.1827952556051875,
    "car": -2.0901447661509565,
    "ride": -5.783899879391965
}

# modal split according to SrV 2018 (shared-svn/projects/matsim-berlin/data/SrV)
# Also see https://www.berlin.de/sen/uvk/presse/pressemitteilungen/2020/pressemitteilung.906382.php
# The calibration only considers persons living in Berlin
target = {
    "walk": 0.296769,
    "bike": 0.177878,
    "pt": 0.265073,
    "car": 0.200673,
    "ride": 0.059607
}


def filter_persons(persons):
    df = persons[persons.person.str.startswith("berlin")]
    print("Filtered %s persons" % len(df))
    return df


def filter_modes(df):
    return df[df.main_mode.isin(modes)]


def resolve_jar(pattern):
    """The Makefile refers to the jar by a glob, because its name carries the git revision."""
    jars = glob.glob(pattern)
    if len(jars) != 1:
        raise ValueError("Expected exactly one jar matching %s, found: %s" % (pattern, jars))
    return os.path.abspath(jars[0])


def write_calibrated_params(study, output):
    """Copy the parameters of the best trial to where the Makefile expects them."""
    completed = utils.completed_trials(study)
    if not completed:
        raise RuntimeError("No trial of the study completed, no parameters to write.")

    # multi objective study: one error per mode, so rank by their sum
    best = min(completed, key=lambda t: sum(abs(v) for v in t.values))
    params = os.path.join("params", "run%d.yaml" % best.number)

    print("Best trial is %d, total absolute error %.4f" % (best.number, sum(abs(v) for v in best.values)))
    for mode in modes:
        print("  %-5s share %.4f (target %.4f)" % (mode, best.user_attrs["%s_share" % mode], target[mode]))

    shutil.copyfile(params, output)
    print("Calibrated mode parameters written to %s" % output)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--jar", required=True, help="Path of the scenario jar, may be a glob.")
    parser.add_argument("--config", required=True, help="Config of the runs, see the Makefile.")
    parser.add_argument("--run-dir", required=True,
                        help="Directory holding the study of this calibration: its database, the "
                             "parameters and the output of every run. One per sample size.")
    parser.add_argument("--output", required=True,
                        help="Where to write the parameters of the best trial.")
    parser.add_argument("--trials", type=int, default=10,
                        help="Number of runs to add to the study. A study that is already in "
                             "--run-dir is continued, not restarted.")
    parser.add_argument("--jvm-args", default="-Xmx60G -Xms60G -XX:+AlwaysPreTouch -XX:+UseParallelGC")
    parser.add_argument("--args", default="", help="Further arguments for the scenario.")
    parser.add_argument("--base-params", default=None,
                        help="Yaml with the estimated parameters of the choice model, used as the "
                             "base of every trial. Without it the config carries them.")
    parser.add_argument("--study-name", default="calib")
    parser.add_argument("--quiet", action="store_true",
                        help="Swallow the output of the scenario runs instead of passing it through.")

    args = parser.parse_args()

    jar = resolve_jar(args.jar)
    config = os.path.abspath(args.config)
    base_params = os.path.abspath(args.base_params) if args.base_params else None
    output = os.path.abspath(args.output)

    # create_calibration puts the study database, the parameters and the runs into the working
    # directory, so the working directory is what separates the sample sizes from each other.
    run_dir = os.path.abspath(args.run_dir)
    os.makedirs(run_dir, exist_ok=True)
    os.chdir(run_dir)

    study, obj = create_calibration(
        args.study_name,
        ASCCalibrator(modes, initial, target, fixed_mode=fixed_mode,
                      lr=utils.linear_scheduler(start=0.8, end=1.4, interval=15)),
        jar,
        config,
        args=args.args,
        jvm_args=args.jvm_args,
        transform_persons=filter_persons,
        transform_trips=filter_modes,
        base_params=base_params,
        chain_runs=utils.default_chain_scheduler,
        debug=not args.quiet
    )

    study.optimize(obj, args.trials)

    write_calibrated_params(study, output)


if __name__ == "__main__":
    sys.exit(main())
