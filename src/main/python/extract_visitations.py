#!/usr/bin/env python
# -*- coding: utf-8 -*-

from matsim.scenariogen.data.formats import netcheck

if __name__ == "__main__":

    df = netcheck.read_visitations("../../../../shared-svn/projects/DiTriMo/data/netcheck-visitation-data-2023/data_2023_berlin")

    print(df)

    df.to_csv("visitations.csv.gz", index=False)