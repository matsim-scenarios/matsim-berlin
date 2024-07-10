#!/usr/bin/env bash

python3.9 -m venv env
source env/bin/activate

pip install --upgrade pip
pip install scikit-learn pandas "numpy<2.0" biogeme