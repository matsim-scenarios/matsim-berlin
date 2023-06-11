#!/usr/bin/env python

import os
import re
import sklearn.svm
import sklearn.ensemble
import statsmodels
import xgboost as xgb

import m2cgen as m2c
import pandas as pd
import numpy as np
import seaborn as sns


import matplotlib.pyplot as plt

#%%

plt.rcParams['figure.dpi'] = 300
plt.rcParams['savefig.dpi'] = 300


#%%

df = pd.read_csv("../../R/acv.csv")

df.speed /= 3.6
df["veh"] = 1 - df.cv/100

#%%

sns.scatterplot(data=df, x="veh", y="rel", hue="speed")

#%%

#model = sklearn.ensemble.ExtraTreesRegressor(max_depth=5, min_samples_split=3)
#model = sklearn.ensemble.RandomForestRegressor()
#model = sklearn.tree.ExtraTreeRegressor()
model = sklearn.tree.DecisionTreeRegressor()
#model = sklearn.linear_model.RidgeCV()
#model = sklearn.svm.SVR(kernel="poly")
#model = xgb.XGBRegressor(tree_method="hist", early_stopping_rounds=True)

#%%

X = df[["speed", "veh"]].to_numpy()
y = df.rel.to_numpy()

model.fit(X, y)

#%%

speed = pd.DataFrame({"speed": np.arange(0, 140) / 3.6})
cv = pd.DataFrame({"veh" :np.arange(0, 100) / 100})

test = speed.merge(cv, how='cross')

test["pred"] = model.predict(test[["speed", "veh"]].to_numpy())


#%%

sns.scatterplot(data=test, x="veh", y="pred", hue="speed")


#%%

code = m2c.export_to_java(model, package_name="org.matsim.analysis", class_name="ACVModel")

with open("../../java/org/matsim/analysis/ACVModel.java", "w") as f:
    f.write(code)

#%%

if __name__ == "__main__":
    pass