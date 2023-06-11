#!/usr/bin/env python
# @author  Angelo Banse, Ronald Nippold, Christian Rakow

import os
import sys
import shutil
from os.path import join, basename

from utils import init_env, init_workload, create_args, write_scenario, filter_network

init_env()

import sumolib.net
import traci  # noqa
from sumolib import checkBinary  # noqa
import lxml.etree as ET

import pandas as pd
import numpy as np

sumoBinary = checkBinary('sumo')
netconvert = checkBinary('netconvert')


def capacity_estimate(v):
    tT = 1.2
    lL = 7.0
    Qc = v / (v * tT + lL)

    return 3600 * Qc


def writeRouteFile(f_name, departLane, arrivalLane, edges, veh, qCV, qAV, qACV):
    text = """<?xml version="1.0" encoding="UTF-8"?>
<routes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://sumo.dlr.de/xsd/routes_file.xsd">

    <vTypeDistribution id="vDist">
        <vType id="vehCV" probability="{qCV}" color="1,0,0" vClass="passenger"/>
        <vType id="vehACV" probability="{qACV}" color="0,0,1" vClass="passenger" minGap="0.5" accel="2.6" decel="3.5" sigma="0" tau="0.6" speedFactor="1" speedDev="0"/>
        <vType id="vehAV" probability="{qAV}" color="0,1,0" vClass="passenger" decel="3.0" sigma="0.1" tau="1.5" speedFactor="1" speedDev="0"/>
    </vTypeDistribution>

    <flow id="veh" begin="0" end= "600" vehsPerHour="{veh}" type="vDist" departLane="{departLane}" arrivalLane="{arrivalLane}" departSpeed="max">
        <route edges="{edges}"/>
    </flow>

</routes>
"""
    # departSpeed="speedLimit" ?
    context = {
        "departLane": departLane,
        "arrivalLane": arrivalLane,
        "edges": edges,
        "veh": veh,
        "qCV": qCV,
        "qAV": qAV,
        "qACV": qACV
    }

    with open(f_name, "w") as f:
        f.write(text.format(**context))


def writeDetectorFile(f_name, output, lane, laneNr, scale):
    text = """<?xml version="1.0" encoding="UTF-8"?>

	<additional xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://sumo.dlr.de/xsd/additional_file.xsd">
	        %s
	</additional>

	""" % "\n".join(
        """<e1Detector id="detector_%d" lane="{lane}_%d" pos="-15" friendlyPos="true" freq="10.00" file="{output_file}_%d.xml"/>""" % (i, i, i)
        for i in
        range(laneNr))

    context = {
        "lane": lane,
        "laneNr": laneNr,
        "output_file": join(output, scale, "lane")
    }

    with open(f_name, 'w') as f:
        f.write(text.format(**context))


def read_result(folder, edge, scale):
    data = []

    for f in os.listdir(folder):
        if not f.endswith(".xml"):
            continue

        total = 0
        end = 0

        for _, elem in ET.iterparse(join(folder, f), events=("end",),
                                    tag=('interval',),
                                    remove_blank_text=True):

            begin = float(elem.attrib["begin"])
            end = float(elem.attrib["end"])
            if begin < 60:
                continue

            total += float(elem.attrib["nVehContrib"])

        data.append({
            "edgeId": edge,
            "laneId": f.replace(".xml", ""),
            "flow": total * (3600 / (end - 60)),
            "scale": float(scale),
            "count": total
        })

    return data


def run(args, edges):
    # saveToFile(edges_ids,"junctions.json")
    i = 0

    total = args.cv + args.av + args.acv

    qCV = (args.cv / total)
    qAV = (args.av / total)
    qACV = (args.acv / total)

    print("Running vehicle shares cv: %.2f, av: %.2f, acv: %.2f" % (qCV, qAV, qACV))

    if args.to_index <= 0:
        args.to_index = len(edges)

    for x in range(args.from_index, args.to_index):
        edge = edges[x]
        i += 1
        print("Edge id: ", edge._id)
        print("Number of lanes: ", edge.getLaneNumber(), "speed:", edge.getSpeed())

        laneNr = edge.getLaneNumber()  # nr of lanes

        cap = capacity_estimate(edge.getSpeed()) * 0.9 * laneNr

        print("Capacity estimate:", cap)

        p_network = join(args.runner, "filtered.net.xml")
        p_routes = join(args.runner, "route.rou.xml")
        p_detector = join(args.runner, "detector.add.xml")

        filter_network(netconvert, args.network, edge, p_network)
        writeRouteFile(p_routes, "best", "current", edge._id, cap, qCV, qAV, qACV)
        p_scenario = join(args.runner, "scenario.sumocfg")

        write_scenario(p_scenario, basename(p_network), basename(p_routes), basename(p_detector), args.step_length)

        go(p_scenario, p_network, edge, p_detector, args)
        print("####################################################################")
        print("[" + str(i) + " / " + str(args.to_index - args.from_index) + "]")


def go(scenario, network, edge, p_detector, args):
    # while traci.simulation.getMinExpectedNumber() > 0:

    end = int(600 * (1 / args.step_length))

    res = []

    folder = join(args.runner, "detector")

    # Clean old data
    shutil.rmtree(folder, ignore_errors=True)
    os.makedirs(folder, exist_ok=True)

    traci.start([sumoBinary, "-n", network], port=args.port)

    xr = ["%.2f" % s for s in np.arange(1, 2.1, 0.05)]

    # Simulate different scales
    for scale in xr:

        #print("Running scale", scale)

        os.makedirs(join(folder, scale), exist_ok=True)
        writeDetectorFile(p_detector, "detector", edge._id, edge.getLaneNumber(), scale)

        # Load scenario with desired traffic scaling
        traci.load(["-c", scenario, "--scale", scale])

        try:
            for step in range(0, end):
                traci.simulationStep()
        except Exception as e:
            print(e)

    traci.close()

    for scale in xr:
        res.extend(read_result(join(folder, scale), edge._id, scale))

    df = pd.DataFrame(res)
    df.to_csv(join(args.output, "%s.csv" % edge._id), index=False)

    sys.stdout.flush()


if __name__ == "__main__":

    args = create_args("Determine edge volumes with SUMO")

    net = sumolib.net.readNet(args.network, withConnections=False, withInternal=False, withFoes=False)

    allEdges = net.getEdges()  # all type of edges

    selection = set(pd.read_csv(args.input[0]).edgeId)

    # select if edges in net file
    edges = [edge for edge in allEdges if edge._id in selection]

    init_workload(args, edges)

    print("Total number of edges:", len(edges))
    print("Processing: ", args.from_index, ' to ', args.to_index)

    run(args, edges)
