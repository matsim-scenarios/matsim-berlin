#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

from enum import Enum, auto
from dataclasses import dataclass

import numpy as np
import pandas as pd


class StrEnum(str, Enum):
    pass


# this creates nice lowercase and JSON serializable names
# https://docs.python.org/3/library/enum.html#using-automatic-values
class AutoNameLower(StrEnum):
    def _generate_next_value_(name, start, count, last_values):
        return name.lower()


class AutoNameLowerStrEnum(AutoNameLower):
    pass


class ParkingPosition(AutoNameLowerStrEnum):
    PRIVATE = auto()
    PUBLIC = auto()
    DIFFERENT = auto()
    NA = auto()


class HouseholdType(AutoNameLowerStrEnum):
    MULTI_W_CHILDREN = auto()
    MULTI_WO_CHILDREN = auto()
    SINGLE = auto()


class EconomicStatus(AutoNameLowerStrEnum):
    VERY_LOW = auto()
    LOW = auto()
    MEDIUM = auto()
    HIGH = auto()
    VERY_HIGH = auto()
    UNKNOWN = auto()


class Gender(AutoNameLowerStrEnum):
    M = auto()
    F = auto()
    OTHER = auto()


class Employment(AutoNameLowerStrEnum):
    CHILD = auto()
    HOMEMAKER = auto()
    RETIREE = auto()
    UNEMPLOYED = auto()
    SCHOOL = auto()
    STUDENT = auto()
    TRAINEE = auto()

    JOB_FULL_TIME = auto()
    JOB_PART_TIME = auto()
    OTHER = auto()


class Availability(AutoNameLowerStrEnum):
    YES = auto()
    NO = auto()
    UNKNOWN = auto()


class Purpose(AutoNameLowerStrEnum):
    WORK = auto()
    WORK_BUSINESS = auto()
    EDU_KIGA = auto()
    """ Kinderkrippe/-garten  """
    EDU_PRIMARY = auto()
    """ Grundschule """
    EDU_SECONDARY = auto()
    """ Weiterführende Schule """
    EDU_HIGHER = auto()
    """ Berufs-, Fach-, Hochschule """
    EDU_OTHER = auto()
    """ Andere Bildungseinrichtung """

    SHOP_DAILY = auto()
    SHOP_OTHER = auto()
    PERSONAL_BUSINESS = auto()
    TRANSPORT = auto()
    LEISURE = auto()
    DINING = auto()
    OUTSIDE_RECREATION = auto()
    VISIT = auto()
    HOME = auto()
    OTHER = auto()


class TripMode(AutoNameLowerStrEnum):
    WALK = auto()
    BIKE = auto()
    CAR = auto()
    RIDE = auto()
    PT = auto()
    OTHER = auto()


class DistanceGroup(AutoNameLowerStrEnum):
    """ These distance groups are designed so that they are roughly equally populated. """

    ZERO = auto()
    G_500M = auto()
    G_1KM = auto()
    G_2KM = auto()
    G_3KM = auto()
    G_5KM = auto()
    G_10KM = auto()
    G_25KM = auto()
    G_50KM = auto()
    G_100KM = auto()
    OVER_100KM = auto()

    @staticmethod
    def cut(values):
        bins = [0, 0.5, 1, 2, 3, 5, 10, 25, 50, 100]
        values = np.asarray(values)

        idx = np.digitize(values, bins, right=True)
        # Set ZERO group manually
        idx[np.where(values <= 0)] = 0
        return np.take(np.asarray(DistanceGroup, dtype=object), idx, axis=0)


class DurationGroup(AutoNameLowerStrEnum):
    """ Most common duration groups, right side is inclusive e.g <= 5 min """

    G_5MIN = auto()
    G_15MIN = auto()
    G_30MIN = auto()
    G_60MIN = auto()
    G_120MIN = auto()
    G_180MIN = auto()
    G_300MIN = auto()
    G_420MIN = auto()
    G_480MIN = auto()
    G_510MIN = auto()
    G_570MIN = auto()
    G_660MIN = auto()
    G_750MIN = auto()
    REST_OF_DAY = auto()

    @staticmethod
    def cut(values):
        bins = [5, 15, 30, 60, 120, 180, 300, 420, 480, 510, 570, 660, 750]

        values = np.asarray(values)
        idx = np.digitize(values, bins, right=True)
        return np.take(np.asarray(DurationGroup, dtype=object), idx, axis=0)


class SourceDestinationGroup(AutoNameLowerStrEnum):
    HOME_WORK = auto()
    HOME_CHILDCARE = auto()
    HOME_EDU = auto()
    HOME_BUSINESS = auto()
    HOME_SHOP = auto()
    HOME_LEISURE = auto()
    HOME_OTHER = auto()
    WORK_HOME = auto()
    CHILDCARE_HOME = auto()
    EDU_HOME = auto()
    BUSINESS_HOME = auto()
    SHOP_HOME = auto()
    LEISURE_HOME = auto()
    OTHER_HOME = auto()
    OTHER_WORK = auto()
    WORK_OTHER = auto()
    OTHER_OTHER = auto()

    UNKNOWN = auto()

    def source(self):
        if self.name.startswith("HOME"):
            return Purpose.HOME
        elif self.name.startswith("WORK"):
            return Purpose.WORK

        return Purpose.OTHER


@dataclass
class Household:
    """ Universal definition of household attributes """
    hh_id: str
    h_weight: float
    n_persons: int
    n_cars: int
    n_bikes: int
    n_other_vehicles: int
    car_parking: ParkingPosition
    economic_status: EconomicStatus
    type: HouseholdType
    region_type: int
    location: str


@dataclass
class Person:
    """ Universal definition of person attributes."""
    p_id: str
    p_weight: float
    hh_id: str
    age: int
    gender: Gender
    employment: Employment
    restricted_mobility: bool
    driving_license: Availability
    car_avail: Availability
    bike_avail: Availability
    pt_abo_avail: Availability
    mobile_on_day: bool
    present_on_day: bool
    reporting_day: int
    n_trips: int


@dataclass
class Trip:
    """ Universal definition of trip attributes"""
    t_id: str
    t_weight: float
    p_id: str
    hh_id: str
    n: int
    day_of_week: int
    departure: int
    duration: int
    gis_length: float
    main_mode: TripMode
    purpose: Purpose
    sd_group: SourceDestinationGroup
    valid: bool


@dataclass
class Activity:
    """ Activity information (including leg) """
    a_id: str
    p_id: str
    n: int
    type: Purpose
    duration: int
    leg_dist: float
    leg_duration: float
    leg_mode: TripMode


def read_srv(household_file, person_file, trip_file):
    """ Read SrV into pandas format """

    hh = pd.read_csv(household_file, encoding="windows-1252", delimiter=";", decimal=",",
                     quotechar="\"", low_memory=False, quoting=2)

    p = pd.read_csv(person_file, encoding="windows-1252", delimiter=";", decimal=",",
                    quotechar="\"", low_memory=False, quoting=2)

    t = pd.read_csv(trip_file, encoding="windows-1252", delimiter=";", decimal=",",
                    quotechar="\"", low_memory=False, quoting=2)

    return hh, p, t


def _batch(iterable: list, max_batch_size: int):
    """ Batches an iterable into lists of given maximum size, yielding them one by one. """
    batch = []
    for element in iterable:
        batch.append(element)
        if len(batch) >= max_batch_size:
            yield batch
            batch = []
    if len(batch) > 0:
        yield batch


def read_all_srv(dirs, regio=None):
    """ Scan directories and read everything into one dataframe """

    hh = []
    pp = []
    tt = []

    for d in dirs:

        files = []

        # Collect all SrV files
        for f in os.scandir(d):
            fp = f.name
            if not f.is_file() or not f.path.endswith(".csv"):
                continue
            if "_HH" in fp or "_P" in fp or "_W" in fp or "H2018" in fp or "P2018" in fp or "W2018" in fp:
                files.append(f.path)

        files = sorted(files)

        if len(files) % 3 != 0:
            print(files)
            raise ValueError("File structure is wrong. Need exactly 3 files per region.")

        for h, p, t in _batch(files, 3):
            print("Reading", h, p, t)

            data = read_srv(h, p, t)
            df = srv_to_standard(data, regio)

            hh.append(df[0])
            pp.append(df[1])
            tt.append(df[2])

    hh = pd.concat(hh, axis=0)
    hh = hh[~hh.index.duplicated(keep='first')]
    print("Households: ", len(hh))

    pp = pd.concat(pp, axis=0)
    pp = pp[~pp.index.duplicated(keep='first')]
    print("Persons: ", len(pp))

    tt = pd.concat(tt, axis=0)
    tt = tt[~tt.index.duplicated(keep='first')]
    print("Trips: ", len(tt))

    return hh, pp, tt


def pint(x):
    """ Convert to positive integer"""
    return max(0, int(x))


def srv_to_standard(data: tuple, regio=None):
    """ Convert srv data to standardized survey format """

    # Needs to be importer late
    from converter import SrV2018

    (hh, pp, tt) = data

    if regio is not None:
        regio = pd.read_csv(regio)

    ps = []
    for p in pp.itertuples():
        ps.append(
            Person(
                str(int(p.HHNR)) + "_" + str(int(p.PNR)),
                p.GEWICHT_P,
                str(int(p.HHNR)),
                int(p.V_ALTER),
                SrV2018.gender(p.V_GESCHLECHT),
                SrV2018.employment(p.V_ERW),
                False if p.V_EINSCHR_NEIN else True,
                SrV2018.yes_no(p.V_FUEHR_PKW),
                SrV2018.veh_avail(p.V_PKW_VERFUEG),
                Availability.YES if SrV2018.veh_avail(p.V_RAD_VERFUEG) == Availability.YES or SrV2018.veh_avail(
                    p.V_ERAD_VERFUEG) == Availability.YES else SrV2018.veh_avail(p.V_RAD_VERFUEG),
                SrV2018.veh_avail(p.V_FK_VERFUEG),
                p.V_WOHNUNG == 1,
                p.V_WOHNORT == 1,
                int(p.STICHTAG_WTAG),
                int(p.E_ANZ_WEGE)
            )
        )

    ps = pd.DataFrame(ps).set_index("p_id")

    random_state = np.random.RandomState(0)

    hhs = []
    for h in hh.itertuples():

        # Invalid entries in certain files
        if np.isnan(h.HHNR):
            continue

        hh_id = str(int(h.HHNR))
        hhs.append(
            Household(
                hh_id,
                h.GEWICHT_HH,
                pint(h.V_ANZ_PERS),
                pint(h.V_ANZ_PKW_PRIV + h.V_ANZ_PKW_DIENST),
                pint(h.V_ANZ_RAD + h.V_ANZ_ERAD),
                pint(h.V_ANZ_MOT125 + h.V_ANZ_MOPMOT + h.V_ANZ_SONST),
                SrV2018.parking_position(h.V_STELLPL1),
                SrV2018.economic_status(h.E_OEK_STATUS if "E_OEK_STATUS" in hh.keys() else -1, h.V_EINK,
                                        ps[ps.hh_id == hh_id]),
                SrV2018.household_type(h.E_HHTYP),
                SrV2018.region_type(h, regio, random_state),
                h.ST_CODE_NAME,
            )
        )

    ts = []
    for t in tt.itertuples():
        # TODO: E_DAUER, E_GESCHW
        # E_ANKUNFT
        # TODO: double check
        ts.append(
            Trip(
                str(int(t.HHNR)) + "_" + str(int(t.PNR)) + "_" + str(int(t.WNR)),
                t.GEWICHT_W,
                str(int(t.HHNR)) + "_" + str(int(t.PNR)),
                str(int(t.HHNR)),
                int(t.WNR),
                int(t.STICHTAG_WTAG),
                int(t.E_BEGINN),
                int(t.E_DAUER),
                float(t.GIS_LAENGE),
                SrV2018.trip_mode(t.E_HVM),
                SrV2018.trip_purpose(t.V_ZWECK),
                SrV2018.sd_group(int(t.E_QZG_17)),
                t.E_WEG_GUELTIG != 0
            )
        )

    return pd.DataFrame(hhs).set_index("hh_id"), ps, pd.DataFrame(ts).set_index("t_id")


if __name__ == "__main__":
    import argparse

    from preparation import prepare_persons, create_activities

    parser = argparse.ArgumentParser(description="Converter for survey data")

    parser.add_argument("-d", "--directory", default=os.path.expanduser(
        "~/Development/matsim-scenarios/shared-svn/projects/matsim-berlin/data/SrV/"))
    parser.add_argument("--regiostar", default=os.path.expanduser(
        "~/Development/matsim-scenarios/shared-svn/projects/matsim-germany/zuordnung_plz_regiostar.csv"))

    parser.add_argument("--output", default="table", help="Output prefix")

    args = parser.parse_args()

    hh, persons, trips = read_all_srv([args.directory + "Berlin+Umland", args.directory + "Brandenburg"],
                                      regio=args.regiostar)

    hh.to_csv(args.output + "-households.csv")
    trips.to_csv(args.output + "-trips.csv")
    persons.to_csv(args.output + "-unscaled-persons.csv")

    print("Written survey csvs")

    df = prepare_persons(hh, persons, trips, augment=5)

    df.to_csv(args.output + "-persons.csv", index_label="idx")

    activities = create_activities(df, trips, include_person_context=False, cut_groups=False)
    activities.to_csv(args.output + "-activities.csv", index=False)
