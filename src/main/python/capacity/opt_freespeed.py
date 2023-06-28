#!/usr/bin/env python

import optuna
from requests import post

URL = "http://localhost:9090"


def objective(trial):
    r = {
        "b50": trial.suggest_float('b50', 0.5, 1),
        "b90": trial.suggest_float('b90', 0.5, 1),
    }

    r = post(URL, json=r)

    if r.status_code != 200:
        raise Exception(r.text)

    return float(r.text)


if __name__ == "__main__":
    # FreeSpeed Optimizer service must be running in parallel

    # [I 2023-06-28 16:39:44,946] Trial 112 finished with value: 3.207233505432148 and parameters: {'tl50': 0.7515425810122633, 'prio50': 0.9901237166939723, 'rbl30': 0.8302898194867332, 'rbl50': 0.695230249055693}. Best is trial 112 with value: 3.207233505432148.

    #[I 2023-06-28 17:28:49,268] Trial 363 finished with value: 2.9352693132304144 and parameters: {'b50': 0.9998624055291735, 'b90': 0.8180051891789177}. Best is trial 363 with value: 2.9352693132304144.


    study = optuna.create_study()
    study.optimize(objective, n_trials=500)

    # TODO: command for server to write network
