#!/usr/bin/env python3
"""
Reads the phase durations per iteration in milliseconds from a MATSim logfile (<runId>.logfile.log).

The stopwatch (<runId>.stopwatch.csv) contains the same phases, but only with a resolution of one second. The logfile has a
timestamp in milliseconds at every phase boundary:

    replanning    end of the iteration starts listeners -> all replanning listeners called (from iteration 1)
    before_mobsim end of replanning                     -> all before mobsim listeners called
    mobsim        end of before mobsim listeners        -> after mobsim event (prepareForMobsim + mobsim)
    after_mobsim  after mobsim event                    -> scoring event
    scoring       scoring event                         -> all scoring listeners called
    iteration     iteration begins                      -> iteration ends

Usage: python3 dsim_phase_times.py <logfile> [<output.csv>]
"""

import csv
import re
import sys
from datetime import datetime

MARKERS = {
    "begin": re.compile(r"### ITERATION (\d+) BEGINS"),
    "starts_done": re.compile(r"\[it\.(\d+)\] all ControllerIterationStartsListeners called"),
    "replanning_done": re.compile(r"\[it\.(\d+)\] all ControllerReplanningListeners called"),
    "before_mobsim_done": re.compile(r"\[it\.(\d+)\] all ControlerBeforeMobsimListeners called"),
    "after_mobsim": re.compile(r"### ITERATION (\d+) fires after mobsim event"),
    "scoring": re.compile(r"### ITERATION (\d+) fires scoring event"),
    "scoring_done": re.compile(r"\[it\.(\d+)\] all ControllerScoringListeners called"),
    "end": re.compile(r"### ITERATION (\d+) ENDS"),
}

PHASES = [
    ("replanning", "starts_done", "replanning_done"),
    ("before_mobsim", "replanning_done", "before_mobsim_done"),
    ("mobsim", "before_mobsim_done", "after_mobsim"),
    ("after_mobsim", "after_mobsim", "scoring"),
    ("scoring", "scoring", "scoring_done"),
    ("iteration", "begin", "end"),
]


def read_markers(path):
    markers = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            if "ITERATION" not in line and "[it." not in line:
                continue
            for name, pattern in MARKERS.items():
                m = pattern.search(line)
                if m:
                    time = datetime.strptime(line[:23], "%Y-%m-%dT%H:%M:%S,%f")
                    markers.setdefault(int(m.group(1)), {})[name] = time
                    break
    return markers


def phase_times(markers):
    rows = []
    for iteration in sorted(markers):
        m = markers[iteration]
        # without replanning (first iteration), the before mobsim listeners start after the iteration starts listeners
        if "replanning_done" not in m and "starts_done" in m:
            m = {**m, "replanning_done": m["starts_done"]}
            no_replanning = True
        else:
            no_replanning = False

        row = {"iteration": iteration}
        for phase, start, end in PHASES:
            if start in m and end in m and not (phase == "replanning" and no_replanning):
                row[phase + "_ms"] = round((m[end] - m[start]).total_seconds() * 1000)
            else:
                row[phase + "_ms"] = ""
        rows.append(row)
    return rows


def main():
    if len(sys.argv) not in (2, 3):
        print(__doc__)
        sys.exit(1)

    rows = phase_times(read_markers(sys.argv[1]))
    fields = ["iteration"] + [p + "_ms" for p, _, _ in PHASES]

    out = open(sys.argv[2], "w", newline="") if len(sys.argv) == 3 else sys.stdout
    writer = csv.DictWriter(out, fieldnames=fields)
    writer.writeheader()
    writer.writerows(rows)
    if out is not sys.stdout:
        out.close()


if __name__ == "__main__":
    main()
