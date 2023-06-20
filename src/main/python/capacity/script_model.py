#!/usr/bin/env python

from os import makedirs
from os.path import join

import random
import sklearn.ensemble

from sklearn.model_selection import KFold
from sklearn.metrics import mean_absolute_error

import m2cgen as m2c
import pandas as pd
import numpy as np
import seaborn as sns

import optuna

from models import create_regressor, model_to_java
from features import build_datasets

#%%

d = "../../../../"

dfs = build_datasets(d + "input/sumo.net-edges.csv.gz",
                     d + "input/result_intersections_scenario-base.csv",
                     d + "input/result_routes_scenario-base.csv")


#%%

targets = dfs.keys()

def get(idx, t):
    
    if idx is not None:
        df = dfs[t].iloc[idx]        
    else:
        df = dfs[t]
    
    return df.drop(columns=["target"]), df.target.to_numpy()
    

scaler = {}

for t in targets:

    _scaler = sklearn.preprocessing.StandardScaler(with_mean=True)        
    
    df = get(None, t)[0]
    
    norm = ["length", "speed", "numFoes", "numLanes", "junctionSize"]
    
    scaler[t] = sklearn.compose.ColumnTransformer([
          ("scale", _scaler, [df.columns.get_loc(x) for x in norm]) # column indices
        ],
        remainder="passthrough"
    )
    
    scaler[t].fit(df)
    

print("Model targets", targets)

#%%

def best_model(ms, t):
    
    errors = []
    for m in ms:

        X, y = get(None, t)
        X = scaler[t].transform(X)

        pred = m.predict(X)
        err = mean_absolute_error(y, pred)
        
        errors.append((m, err))
        
    errors = sorted(errors, key=lambda m : m[1])
    
    return errors[0]

#%%

## Feature selection

from xgboost import XGBRegressor
from sklearn.feature_selection import RFECV

model = XGBRegressor(max_depth=6, n_estimators=150)

rfecv = RFECV(estimator=model, step=1, cv=KFold(n_splits=5, shuffle=True), scoring='neg_mean_absolute_error')

X, y = get(None, "capacity_traffic_light")
X = scaler[t].transform(X)

rfecv.fit(X, y)

#Selected features
#print(X.columns[rfecv.get_support()])
print("Optimal number of features : %d" % rfecv.n_features_)

#%%

fold = KFold(n_splits=6, shuffle=True)
n_trials = 150


classifier = {
     'mean',
     'XGBRFRegressor',
     'XGBRegressor',
     'RandomForestRegressor',
     'ExtraTreesRegressor',
     'LGBMRegressor',
     'DecisionTreeRegressor',
     'PassiveAggressiveRegressor',
    # More
    #   'SVR',
    #   'KernelSVC',
    #   'QLatticeRegressor',
    #   'LinearSVR',
    #   'Ridge',
    #   'SGDRegressor',
    #   'LogisticRegression',
    #   'AdaGradRegressor',
    #   'CDRegressor',
    #   'FistaRegressor',
    #   'SDCARegressor',
    #   'Lasso',
    #   'ElasticNet'
}


def objective(classifier_name, target):
    global model

    def _fn(trial):
        global model

        r = random.Random(42)

        random_state = r.getrandbits(31)

        seq = iter(fold.split(dfs[target]))

        error = 0
        i = 0

        candidates = []

        for train, test in seq:

            model = create_regressor(trial, classifier_name, random_state)
            
            candidates.append(model)


            X, y = get(train, target)
            X = scaler[t].transform(X)

            model.fit(X, y)

            Xval, yval = get(test, target)
            Xval = scaler[t].transform(Xval)

            pred = model.predict(Xval)

            error += mean_absolute_error(yval, pred)

            i += 1

        best = best_model(candidates, t)[0]

        return error / i

    return _fn

def callback(study, trial):
    global best
    global model
    if study.best_trial == trial:
        best = model

models = {}

for t in targets:

    print("Training", t)

    models[t] = {}

    for m in classifier:
        print("Running model", m)

        study = optuna.create_study(direction='minimize')
        study.optimize(objective(m, t), n_trials=n_trials, callbacks=[callback], show_progress_bar=True)

        models[t][m] = best

#%%


for t in targets:
    
    print("#### ", t)
    
    m = best_model(models[t].values(), t)
    
    print("Best model", m)
    
    code = model_to_java(t, m[0], scaler[t], get(None, t)[0])
    
    makedirs("code", exist_ok=True)

    with open(join("code", t.capitalize() + ".java"), "w") as f:
        f.write(code)


#%%

"""

# Current models

####  speedRelative_priority
Best model (XGBRegressor(alpha=0.016274331343132255, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.49738177270937495,
             eval_metric='mae', feature_types=None, gamma=0.010989774140675113,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.4808316830202329,
             learning_rate=0.497381777, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=2, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...), 0.03486538178476313)
####  speedRelative_right_before_left
Best model (LGBMRegressor(colsample_bytree=0.9, n_estimators=30, num_leaves=15,
              objective='regression', random_state=1373158606,
              reg_alpha=2.8781138265221893e-08, subsample=0.9,
              subsample_freq=10), 0.03110904840177184)
####  speedRelative_traffic_light
Best model (XGBRegressor(alpha=0.18112228287174761, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.44431894139221145,
             eval_metric='mae', feature_types=None, gamma=0.010592854880473093,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.02730064085187793,
             learning_rate=0.44431895, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=2, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...), 0.07547469940758773)
####  capacity_priority
Best model (XGBRegressor(alpha=0.10354942228390128, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.3219114758096538,
             eval_metric='mae', feature_types=None, gamma=0.04441440963458821,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.037683052815746965,
             learning_rate=0.321911484, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=8, missing=nan, monotone_constraints='()',
             n_estimators=25, n_jobs=0, ...), 63.271886462249334)
####  capacity_right_before_left
Best model (XGBRegressor(alpha=0.776746384838071, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.2968084020008939,
             eval_metric='mae', feature_types=None, gamma=0.011351481475457127,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.01634910363853723,
             learning_rate=0.296808392, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=2, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...), 36.30614042669991)
####  capacity_traffic_light
Best model (XGBRegressor(alpha=0.01571462151622278, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.4194660419570857,
             eval_metric='mae', feature_types=None, gamma=0.873878271331525,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.8022157011051211,
             learning_rate=0.419466048, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=5, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...), 133.36648901538405)

"""



