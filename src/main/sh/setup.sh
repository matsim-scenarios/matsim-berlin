#!/usr/bin/env bash

python3.9 -m venv env
source env/bin/activate

pip install --upgrade pip

#pip install "matsim-tools[calibration]==0.0.19"
pip install --force-reinstall "matsim-tools[calibration] @ git+https://github.com/matsim-vsp/matsim-python-tools.git@dist-calibration"