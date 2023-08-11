#!/bin/bash

python3 -m venv env
source env/bin/activate

module load gcc/9.2.0

pip install --upgrade pip
pip install optuna geopandas rtree pygeos eclipse-sumo sumolib traci lxml optax
