#!/usr/bin/env bash

python3 -m venv env
source env/bin/activate

module load gcc/9.2.0

pip install --upgrade pip
pip install optuna geopandas rtree pygeos

# Download latest calibration script
wget "https://github.com/matsim-vsp/matsim-python-tools/raw/master/matsim/calibration.py"
wget "https://github.com/matsim-vsp/matsim-python-tools/raw/master/matsim/analysis.py"