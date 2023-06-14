#!/usr/bin/env python
# -*- coding: utf-8 -*-

from argparse import ArgumentParser

import lxml.etree as ET
import pandas as pd
from shapely.geometry import LineString


def parse_ls(el):
    shape = el.attrib['shape']
    coords = [tuple(map(float, l.split(","))) for l in shape.split(" ")]
    return LineString(coords)


def combine_bitset(a, b):
    return "".join("1" if x[0] == "1" or x[1] == "1" else "0" for x in zip(a, b))


def read_network(sumo_network):
    """ Read sumo network from xml file. """

    edges = {}
    junctions = {}

    # Aggregated connections, for outgoing edge
    connections = {}

    # count the indices of connections, assuming they are ordered
    # this seems to be the case according to sumo doc. there is no further index attribute
    idx = {}

    data_conns = []

    for _, elem in ET.iterparse(sumo_network, events=("end",),
                                tag=('edge', 'junction', 'connection'),
                                remove_blank_text=True):

        if elem.tag == "edge":
            edges[elem.attrib["id"]] = elem
            continue

        elif elem.tag == "junction":
            junctions[elem.attrib["id"]] = elem
            idx[elem.attrib["id"]] = 0
            continue

        if elem.tag != "connection":
            continue

        # Rest is parsing connection        
        conn = elem.attrib

        fromEdge = edges[conn["from"]]
        fromLane = fromEdge.find("lane", {"index": conn["fromLane"]})

        toEdge = edges[conn["to"]]
        toLane = toEdge.find("lane", {"index": conn["toLane"]})

        junction = junctions[fromEdge.attrib["to"]]
        request = junction.find("request", {"index": str(idx[fromEdge.attrib["to"]])})

        # increase request index
        idx[fromEdge.attrib["to"]] += 1

        from_edge_id = fromEdge.attrib["id"]

        if from_edge_id not in connections:
            connections[from_edge_id] = {
                "dirs": {conn["dir"]},
                "response": request.attrib["response"],
                "foes": request.attrib["foes"],
                "conns": 1
            }
        else:
            connections[from_edge_id]["dirs"].add(conn["dir"])
            connections[from_edge_id]["response"] = combine_bitset(connections[from_edge_id]["response"],
                                                                   request.attrib["response"])
            connections[from_edge_id]["foes"] = combine_bitset(connections[from_edge_id]["foes"],
                                                               request.attrib["foes"])
            connections[from_edge_id]["conns"] += 1

        data_conns.append({
            "junctionId": junction.attrib["id"],
            "fromEdgeId": from_edge_id,
            "toEdgeId": toEdge.attrib["id"],
            "fromLaneId": fromLane.attrib["id"],
            "toLaneId": toLane.attrib["id"],
            "dir": conn["dir"],
            "connDistance": round(parse_ls(fromLane).distance(parse_ls(toLane)), 2)
        })

    data = []

    for edge in edges.values():
        junction = junctions[edge.attrib["to"]]

        conn = connections.get(edge.attrib["id"], {})

        # speed and length should be the same on all lanes
        lane = edge.find("lane", {"index": "0"})

        d = {
            "edgeId": edge.attrib["id"],
            "edgeType": edge.attrib["type"],
            "priority": int(edge.attrib["priority"]),
            "speed": float(lane.attrib["speed"]),
            "fromLength": float(lane.attrib["length"]),
            "numLanes": len(edge.findall("lane")),
            "numConns": conn.get("conns", 0),
            "numResponse": conn.get("response", "").count("1"),
            "numFoes": conn.get("foes", "").count("1"),
            "dirs": "".join(sorted(conn.get("dirs", ""))),
            "junctionType": junction.attrib["type"],
            "junctionSize": len(junction.findall("request"))
        }

        data.append(d)

    return pd.DataFrame(data), pd.DataFrame(data_conns)


def read_edges(sumo_network):
    data = []

    edges = {}
    junctions = {}

    for _, elem in ET.iterparse(sumo_network, events=("end",),
                                tag=('edge', 'junction'),
                                remove_blank_text=True):

        if elem.tag == "edge":
            edges[elem.attrib["id"]] = elem
            continue
        elif elem.tag == "junction":
            junctions[elem.attrib["id"]] = elem
            continue

    for k, v in edges.items():
        lane = v.find("lane")

        f = junctions[v.attrib["from"]]
        t = junctions[v.attrib["to"]]

        data.append({
            "edgeId": k,
            "name": v.attrib.get("name", ""),
            "from": v.attrib["from"],
            "to": v.attrib["to"],
            "type": v.attrib["type"],
            "speed": lane.attrib["speed"],
            "length": float(lane.attrib["length"]),
            "numLanes": len(v.findall("lane")),
            "fromType": f.attrib["type"],
            "toType": t.attrib["type"]
        })

    return pd.DataFrame(data)


if __name__ == "__main__":
    parser = ArgumentParser(description="Util to convert data to csv")

    parser.add_argument("mode", nargs='?', help="Convert result file that create with one of the run scripts")
    parser.add_argument("--network", help="Path to sumo network")
    parser.add_argument("--input", help="Path to input file for conversion")

    args = parser.parse_args()

    if args.network:
        print("Extracting network features")
        edges, conns = read_network(args.network)

        edges.to_csv(args.network.replace(".xml", "-edges.csv.gz"), index=False)
        conns.to_csv(args.network.replace(".xml", "-conns.csv.gz"), index=False)

    # TODO: collect aggregated results, replace the result txt