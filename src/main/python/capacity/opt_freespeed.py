#!/usr/bin/env python

import optuna
from requests import post

URL = "http://localhost:9090"


def objective(trial):
    r = {
        "b30": trial.suggest_float('b30', 0.5, 1),
        "b50": trial.suggest_float('b50', 0.5, 1),
        "b90": trial.suggest_float('b90', 0.5, 1),
    }

    r = post(URL, json=r)

    if r.status_code != 200:
        raise Exception(r.text)

    return float(r.text)


if __name__ == "__main__":
    # FreeSpeed Optimizer service must be running in parallel

    # Params
    # 2023-07-02T17:56:50,707  INFO FreeSpeedOptimizer:194 Request{b30=0.8976534120085969, b50=0.8755181598302809, b90=0.9083000320117319}, rmse: 2.7693593577106586, mse: 3.995433187788609


    study = optuna.create_study(sampler=optuna.samplers.TPESampler(seed=42))
    study.optimize(objective, n_trials=400)

    # Save best parameter
    post(URL + "/save", json=study.best_params)
