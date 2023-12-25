#!/usr/bin/env bash

python3.9 -m venv env
source env/bin/activate

pip install --upgrade pip

pip install "matsim-tools[calibration]==0.0.16"