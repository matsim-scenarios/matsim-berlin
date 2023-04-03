#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import argparse

import pandas as pd
import numpy as np

from data import read_all_srv

#%%

d = os.path.expanduser("~/Development/matsim-scenarios/shared-svn/projects/matsim-berlin/data/SrV/")

all_hh, all_persons, all_trips = read_all_srv([d + "Berlin+Umland", d + "Brandenburg"])

#%%

print(trips)

#%%

# Filter person ad trips for area

df = all_persons.join(all_hh, on="hh_id")

persons = df[df.present_on_day & 
                      (df.reporting_day <= 4) &
                      (df.region_type == 1)]


persons["age_group"] = pd.cut(persons.age, [0, 18, 66, np.inf], labels=["0 - 17", "18 - 65", "65+"], right=False)

invalid = set(all_trips[~all_trips.valid].p_id)

persons = persons[~persons.index.isin(invalid)]

trips = all_trips.drop(columns=["hh_id"]).join(persons, on="p_id", how="inner")


#%%

labels = ["0 - 1000", "1000 - 2000", "2000 - 5000", "5000 - 10000", "10000 - 20000", "20000+"]
bins = [0, 1000, 2000, 5000, 10000, 20000, np.inf]

trips["dist_group"] = pd.cut(trips.gis_length * 1000, bins, labels=labels, right=False)


#%%

def weighted(x):
    data = dict(n=x.t_weight.sum(), mean_dist=np.average(x.gis_length * 1000, weights=x.t_weight))           
    return pd.Series(data=data)


aggr = trips.groupby(["age_group", "dist_group", "main_mode"]).apply(weighted)

aggr["share"] = aggr.n / aggr.n.sum()
aggr["share"].fillna(0, inplace=True)

aggr = aggr.drop(columns=["n"])


aggr.to_csv("mode_share_ref.csv")


#%%
def mode_usage(mode):
    
    def f(x):
        return (x == mode).any()
    
    return f
    

def summarize(x):
    
    total = x.p_weight.sum()
    total_mobile = x[x.mobile_on_day].p_weight.sum()
    
    mobile = total_mobile / total
    
    
    args = {"%s_user" % k.value: ("main_mode", mode_usage(k)) for k in set(trips.main_mode)}
    
    p_trips = trips[trips.p_id.isin(x.index)]
    
    mode_user = p_trips.groupby(["p_id"]).agg(**args)    
    joined = x.join(mode_user, how="inner")
    
    
    data = {}
    for c in mode_user.columns:
        share = joined[joined[c]].p_weight.sum() / total_mobile        
        data[c] = share    
     
    data.update({"mobile": mobile, "n": x.p_weight.sum(), "avg_trips": np.average(x.n_trips, weights=x.p_weight)})
    
    return pd.Series(data=data)



aggr = persons.groupby(["age_group"]).apply(summarize)

aggr["population_share"] = aggr.n / aggr.n.sum()

aggr = aggr.drop(columns=["n"])


print(aggr)


aggr.to_csv("pupulation_stats_ref.csv")



#%%





