#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import argparse

import pandas as pd
import numpy as np

from data import TripMode, read_all_srv
from preparation import _fill

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

_fill(trips, "main_mode", TripMode.OTHER)


#%%

labels = ["0 - 1000", "1000 - 2000", "2000 - 5000", "5000 - 10000", "10000 - 20000", "20000+"]
bins = [0, 1000, 2000, 5000, 10000, 20000, np.inf]


trips["dist_group"] = pd.cut(trips.gis_length * 1000, bins, labels=labels, right=False)


#%%

def weighted(x):
    data = dict(n=x.t_weight.sum(), mean_dist=np.average(x.gis_length * 1000, weights=x.t_weight))           
    return pd.Series(data=data)


aggr = trips.groupby(["dist_group", "main_mode"]).apply(weighted)

aggr["share"] = aggr.n / aggr.n.sum()
aggr["share"].fillna(0, inplace=True)

aggr = aggr.drop(columns=["n"])


aggr.to_csv("mode_share_ref.csv")


# Also normalize der distance group
for dist_group in aggr.index.get_level_values(0).categories:
    sub = aggr.loc[dist_group, :]
    sub.share /= sub.share.sum()


aggr.to_csv("mode_share_per_dist_ref.csv")



#%%

def summarize(x):
    
    data = {"d": 1}
    
    x["departure_h"] = x.departure // 60
    x["arrival_h"] = (x.departure + x.duration) // 60

    d = x.groupby(["purpose", "departure_h"]).agg(n=("t_weight", "sum"))    
    d["departure"] = d.n / d.n.sum()
    d = d.drop(columns=["n"])
    d.index.rename(names=["purpose", "h"], inplace=True)

    a = x.groupby(["purpose", "arrival_h"]).agg(n=("t_weight", "sum"))    
    a["arrival"] = a.n / a.n.sum()
    a = a.drop(columns=["n"]).rename(index={"arrival_h": "h"})
    a.index.rename(names=["purpose", "h"], inplace=True)

    
    m = pd.merge(a, d, left_index=True, right_index=True, how="outer")
    m.fillna(0, inplace=True)
    
    return m


aggr = summarize(trips)

aggr.to_csv("trip_purposes_by_hour_ref.csv")

#%%

def mode_usage(mode):
    
    def f(x):
        return (x == mode).any()
    
    return f

def summarize(x):
    
    total = x.p_weight.sum()
    total_mobile = x[x.mobile_on_day].p_weight.sum()
    
    mobile = total_mobile / total
    
    
    args = {k.value: ("main_mode", mode_usage(k)) for k in set(trips.main_mode)}
    
    p_trips = trips[trips.p_id.isin(x.index)]
    
    mode_user = p_trips.groupby(["p_id"]).agg(**args)    
    joined = x.join(mode_user, how="inner")
        
    data = {}
    for c in mode_user.columns:
        share = joined[joined[c]].p_weight.sum() / total_mobile        
        data[c] = share    
     
    
    return pd.DataFrame(data={"main_mode": data.keys(), "user": data.values()}).set_index("main_mode")


aggr = summarize(persons)
#aggr = aggr.to_frame(0).T


aggr.to_csv("mode_users_ref.csv")


#%%
    

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
     
    # avg_trips_per_mobile
    x_mobile = x[x.mobile_on_day] 
    data.update({"mobile": mobile, 
                 "n": x.p_weight.sum(), 
                 "avg_trips": np.average(x.n_trips, weights=x.p_weight),
                 "avg_trips_mobile": np.average(x_mobile.n_trips, weights=x_mobile.p_weight)
                 })
    
    return pd.Series(data=data)



aggr = persons.groupby(["age_group"]).apply(summarize)

aggr["population_share"] = aggr.n / aggr.n.sum()
aggr["n"] = aggr.population_share * 3645000


print(aggr)


aggr.to_csv("population_stats_ref.csv")

#%%

act = pd.read_csv("table-activities.csv")


aggr = act.groupby("p_id").max()

#%%
