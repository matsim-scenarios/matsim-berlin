
###################################
########### SETUP #################
###################################
JAR := matsim-berlin-*.jar
VERSION := v7.1
CRS := EPSG:25832
MAKE_XMX ?= 20G

## we assume SUMO is installed locally via pip
## use either the agimo-digital-twin-workflow or 
## install via pip install eclipse-sumo==[VERSION]
#SUMO_VERSION := 1.20.0

## if you want to override thes variables set them as environment-variables and run make -e
## make will then use the environment-variable instead what you defined here.
SVN_PATH := ..
OUTPUT := output/
## either use the global installation via, e.g. apt-get, or define where this is comming from
OSMOSIS := osmosis
## we use a tmp-dir because on the cluster the default-tmp-dir is to small
TMP_DIR := ./tmp
# Scenario creation tool
JAVA_APP := java -Xmx$(MAKE_XMX) -XX:+UseParallelGC -Dorg.geotools.referencing.forceXY=true -Djava.io.tmpdir=$(TMP_DIR) -cp $(JAR) org.matsim.prepare.RunOpenBerlinCalibration

.PHONY: setup prepare prepare-network-and-counts prepare-calibration prepare-run-cadyts prepare-initial prepare-drt
.DELETE_ON_ERROR:

###################################
######## INPUT ####################
###################################

GERMANY := $(SVN_PATH)/shared-svn/projects/matsim-germany
BERLINSHARED := $(SVN_PATH)/shared-svn/projects/matsim-berlin
BERLINPUBLIC := $(SVN_PATH)/public-svn/matsim/scenarios/countries/de/berlin

## Freight-Processing currrently does not work. Should probably move to a separate makefile
#GERMAN_FREIGHT_25PCT := $(SVN_PATH)/public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz
#GERMAN_FREIGHT_NETWORK := $(SVN_PATH)/public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz

## For the time being, use the old version
BERLIN_BRANDENBURG_LONGHAULFREIGHT_25PCT := $(BERLINPUBLIC)/berlin-v7.0/input/berlin-longHaulFreight-v7.0-25pct.plans.xml.gz
BERLIN_SMALLSCALE_COMMERCIAL_25PCT := $(BERLINPUBLIC)/berlin-v7.0/input/berlin-small-scale-commercialTraffic-v7.0-25pct.plans.xml.gz

AREA_POLY := input/v7.0/area/area.poly
AREA_SHP := input/v7.0/area/area.shp
PT_AREA := input/v7.0/pt-area/pt-area.shp
REMOVE_RAILWAY := input/remove-railway.xml
COUNTS_UNDERESTIMATED := input/counts_underestimated.csv
COUNTS_MAPPING := input/counts_mapping.csv
FACILITY_MAPPING := input/facility_mapping.json
COMMERCIAL_TRAFFIC_AREA_DATA := input/commercialTrafficAreaData.csv
ACTIVITY_MAPPING := input/activity_mapping.json


#SUMO_OSM_NETCONVERT_URL := https://raw.githubusercontent.com/eclipse-sumo/sumo/refs/tags/v1_20_0/data/typemap/osmNetconvert.typ.xml
SUMO_OSM_NETCONVERT := $(BERLINSHARED)/data/sumo/osmNetconvert.typ.xml
#SUMO_OSM_NETCONVERT_URBAN_DE_URL  := https://raw.githubusercontent.com/eclipse-sumo/sumo/refs/tags/v1_20_0/data/typemap/osmNetconvertUrbanDe.typ.xml
SUMO_OSM_NETCONVERT_URBAN_DE := $(BERLINSHARED)/data/sumo/osmNetconvertUrbanDe.typ.xml

#BRANDENBURG_OSM_URL := https://download.geofabrik.de/europe/germany/brandenburg-230101.osm.pbf 
BRANDENBURG_OSM_LOCAL := $(BERLINSHARED)/data/osm/brandenburg-230101.osm.pbf 

PLANUNGSRAUM_25833 := $(BERLINPUBLIC)/berlin-v7.0/input/shp/Planungsraum_EPSG_25833.shp
## link no longer working
#PLANUNGSRAUM_25833_URL := https://www.stadtentwicklung.berlin.de/planen/basisdaten_stadtentwicklung/lor/download/LOR_SHP_EPSG_25833.zip
REGION_4326 := $(BERLINPUBLIC)/berlin-v7.0/input/shp/region_4326.shp
BB_ZONES_4326 := $(BERLINPUBLIC)/berlin-v7.0/input/shp/berlinBrandenburg_Zones_VKZ_4326.shp
BB_BUILDINGS_4326 := $(BERLINPUBLIC)/berlin-v7.0/input/shp/buildings_BerlinBrandenburg_4326.shp
BERLIN_LANDUSE_4326 := $(BERLINPUBLIC)/berlin-v7.0/input/shp/berlinBrandenburg_landuse_4326.shp
BB_ZONES_VKZ_4326 := $(BERLINPUBLIC)/berlin-v7.0/input/shp/berlinBrandenburg_Zones_VKZ_4326.shp
BERLIN_INNER_CITY_GPKG := $(BERLINPUBLIC)/berlin-v6.4/input/shp/berlin_inner_city.gpkg
BERLIN_SHP_25832 := $(BERLINPUBLIC)/berlin-v7.0/input/shp/Berlin_25832.shp

COUNTS_BERLIN_2018 := $(BERLINSHARED)/berlin-v5.5/original_data/vmz_counts_2018/Datenexport_2018_TU_Berlin.xlsx
PLR_2013_2020 := $(BERLINSHARED)/data/statistik-berlin-brandenburg/PLR_2013_2020.csv
SRV_PERSONS := $(BERLINSHARED)/data/SrV/2018/converted/table-persons.csv
SRV_ACTS := $(BERLINSHARED)/data/SrV/2018/converted/table-activities.csv
BERLIN_COMMUTER := $(BERLINSHARED)/data/SrV/2018/converted/berlin-work-commuter.csv
SRV_ZONES := $(BERLINSHARED)/data/SrV/2018/zones/zones.shp

GTFS_DAY_TO_CONVERT := "2024-11-19"
GTFS_DATA := $(GERMANY)/gtfs/complete-pt-2024-10-27.zip 
VG5000_GEM := $(GERMANY)/vg5000/vg5000_ebenen_0101/VG5000_GEM.shp
REGIONALSTAT_POP := $(GERMANY)/regionalstatistik/population.csv
REGIONALSTAT_EMPL := $(GERMANY)/regionalstatistik/employed.json
REGIONALSTAT_COMMUTER := $(GERMANY)/regionalstatistik/commuter.csv
## (link no longer working; in general, mcloud no longer exists; RegioStar = spatial planning categories)
#REGIOSTAR_URL := https://mcloud.de/downloads/mcloud/536149D1-2902-4975-9F7D-253191C0AD07/RegioStaR-Referenzdateien.xlsx
REGIOSTAR := $(GERMANY)/RegioStaR-Referenzdateien.xlsx
VEHICLESFILE_IN := input/v7.0/berlin-v7.0-vehicleTypes.xml

###################################
######## OUTPUT ###################
###################################

FACILITIES_GPKG := $(OUTPUT)/facilities.gpkg

NETWORK_OSM := $(OUTPUT)/network.osm
NETWORK_SUMO := $(OUTPUT)/sumo.net.xml
NETWORK_MATSIM := $(OUTPUT)/berlin-$(VERSION)-network.xml.gz
NETWORK_MATSIM_PT := $(OUTPUT)/berlin-$(VERSION)-network-with-pt.xml.gz

VMZ_COUNTS := $(OUTPUT)/berlin-$(VERSION)-counts-vmz.xml.gz
LINK_GEOMETRIES := $(OUTPUT)/berlin-$(VERSION)-network-linkGeometries.csv
FACILITIES_XML := $(OUTPUT)/berlin-$(VERSION)-facilities.xml.gz
BERLIN_ONLY_100PCT := $(OUTPUT)/berlin-only-$(VERSION)-100pct.plans.xml.gz
BERLIN_ONLY_25PCT := $(OUTPUT)/berlin-only-$(VERSION)-25pct.plans.xml.gz
BRANDENBURG_ONLY_25PCT := $(OUTPUT)/brandenburg-only-$(VERSION)-25pct.plans.xml.gz
BERLIN_BRANDENBURG_STATIC_25PCT := $(OUTPUT)/berlin-static-$(VERSION)-25pct.plans.xml.gz
BERLIN_BRANDENBURG_ACTS_25PCT := $(OUTPUT)/berlin-activities-$(VERSION)-25pct.plans.xml.gz
BERLIN_BRANDENBURG_INITIAL_25PCT := $(OUTPUT)/berlin-initial-$(VERSION)-25pct.plans.xml.gz
BERLIN_CADYTS_INPUT_25PCT := $(OUTPUT)/berlin-cadyts-input-$(VERSION)-25pct.plans.xml.gz
BERLIN_CADYTS_OUTPUT_25PCT := $(OUTPUT)/cadyts/cadyts.output_plans.xml.gz
BERLIN_CADYTS_FINAL_25PCT := $(OUTPUT)/berlin-$(VERSION)-25pct.plans_cadyts.xml.gz
BERLIN_BRANDENBURG_INITIAL_25PCT_AFTER_CADYTS := $(OUTPUT)/berlin-$(VERSION)-25pct.plans-initial.xml.gz
BERLIN_10PCT_AFTER_CHOICE_EXPERIMENTS := $(OUTPUT)/berlin-$(VERSION)-10pct.plans.xml.gz
BERLIN_DOWNTOWN_3PCT_PLANS := $(OUTPUT)/inner-city/berlin-downtown-$(VERSION)-3pct.xml.gz
BERLIN_3PCT_PLANS := $(OUTPUT)/berlin-$(VERSION)-3pct.plans.xml.gz
# this is coming from an external process. You can set it via environment-variable. For more info see comment 
## below where this file is used. 
MODECHOICE_10PCT_BASELINE_PLANS := ""

## this currrently does not work. Should probably move to a separate makefile
#BERLIN_BRANDENBURG_LONGHAULFREIGHT_25PCT := $(OUTPUT)/berlin-longHaulFreight-$(VERSION)-25pct.plans.xml.gz
#COMMERCIAL_FACILITIES := $(OUTPUT)/commercialFacilities.xml.gz
#BERLIN_SMALLSCALE_COMMERCIAL_25PCT := $(OUTPUT)/berlin-small-scale-commercialTraffic-$(VERSION)-25pct.plans.xml.gz

RANDOM_DRT_FLEET_10K := $(OUTPUT)/berlin-$(VERSION).drt-by-rndLocations-10000vehicles-4seats.xml.gz

## this is produced together with BERLIN_CADYTS_FINAL_25PCT, it has an own target now
BERLIN_CADYTS_SELECTION_25PCT := $(OUTPUT)/berlin-$(VERSION)-25pct.plans_selection_cadyts.csv
## its produced together with the commercial-facilities and has an own target now
DATA_DISTR_PER_ZONE := $(OUTPUT)/dataDistributionPerZone.csv

VEHICLESFILE_OUT := $(OUTPUT)/berlin-$(VERSION)-vehicleTypes.xml

## TODO where is this comming from
NETWORK_FT := $(OUTPUT)/berlin-$(VERSION)-network-ft.csv.gz


###################################
######## OUTPUT ###################
###################################

$(JAR):
	./mvnw clean package -DskipTests=true

# Preprocessing and cleaning of raw osm data to geo-referenced activity facilities.
$(FACILITIES_GPKG): $(BRANDENBURG_OSM_LOCAL) $(ACTIVITY_MAPPING)
	$(JAVA_APP) prepare facility-shp\
	 --activity-mapping $(word 2,$^)\
	 --input $<\
	 --output $@

# filtering for those parts of the osm data that we need for the network:
$(NETWORK_OSM): $(BRANDENBURG_OSM_LOCAL) $(AREA_POLY) $(REMOVE_RAILWAY)

	# Detailed network includes bikes as well
	 # hard-coded because we delete within this step
	$(OSMOSIS) --rb file=$<\
	 --tf accept-ways bicycle=designated highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,living_street,unclassified,cycleway\
	 --bounding-polygon file="$(word 2,$^)"\
	 --used-node --wb input/network-detailed.osm.pbf

	$(OSMOSIS) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction\
	 --used-node --wb input/network-coarse.osm.pbf

	$(OSMOSIS) --rb file=input/network-coarse.osm.pbf --rb file=input/network-detailed.osm.pbf\
  	 --merge\
  	 --tag-transform file=$(word 3,$^)\
  	 --wx $@

	rm input/network-detailed.osm.pbf
	rm input/network-coarse.osm.pbf

# converting the network from OSM format to SUMO format:
$(NETWORK_SUMO): $(NETWORK_OSM) $(SUMO_OSM_NETCONVERT) $(SUMO_OSM_NETCONVERT_URBAN_DE)
	netconvert --geometry.remove --ramps.guess --ramps.no-split\
	 --type-files $(word 2,$^),$(word 3,$^)\
	 --tls.guess-signals true --tls.discard-simple --tls.join --tls.default-type actuated\
	 --junctions.join --junctions.corner-detail 5\
	 --roundabouts.guess --remove-edges.isolated\
	 --no-internal-links --keep-edges.by-vclass passenger,truck,bicycle\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian\
	 --output.original-names --output.street-names\
	 --osm.lane-access false --osm.bike-access false\
	 --osm.all-attributes\
	 --osm.extra-attributes smoothness,surface,crossing,tunnel,traffic_sign,bus:lanes,bus:lanes:forward,bus:lanes:backward,cycleway,cycleway:right,cycleway:left,bicycle\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --ignore-errors --ignore-errors.connections\
	 --osm-files $< -o=$@


# converting the network from SUMO format to MATSim format:
$(NETWORK_MATSIM): $(NETWORK_SUMO)
	$(JAVA_APP) prepare network-from-sumo $< --target-crs $(CRS) --lane-restrictions REDUCE_CAR_LANES --output $@

	$(JAVA_APP) prepare clean-network $@ --output $@ --modes car,bike,ride,truck --remove-turn-restrictions

	$(JAVA_APP) prepare reproject-network\
	 --input $@	--output $@\
	 --input-crs $(CRS) --target-crs $(CRS)\
	 --mode truck=freight\

	$(JAVA_APP) prepare apply-network-params freespeed capacity\
	  --network $@ --output $@\
	  --input-features $(NETWORK_FT)\
	  --model org.matsim.prepare.network.BerlinNetworkParams

	$(JAVA_APP) prepare apply-network-params capacity\
	  --network $@ --output $@\
	  --input-features $(NETWORK_FT)\
	  --road-types residential,living_street\
	  --capacity-bounds 0.3\
	  --model org.matsim.application.prepare.network.params.hbs.HBSNetworkParams\
	  --decrease-only

# add the PT network. Generates MATSim transit schedule as a side effect.  Note that this uses "complete-pt-2024-10-27.zip" as hardcoded input.
$(NETWORK_MATSIM_PT): $(NETWORK_MATSIM) $(GTFS_DATA) $(PT_AREA) $(VMZ_COUNTS) $(COUNTS_UNDERESTIMATED)
	$(JAVA_APP) prepare transit-from-gtfs --network $< --output=$(OUTPUT)\
	 --name berlin-$(VERSION) --date $(GTFS_DAY_TO_CONVERT) --target-crs $(CRS) \
	 $(word 2,$^)\
	 --copy-late-early\
	 --transform-stops org.matsim.prepare.pt.CorrectStopLocations\
	 --transform-routes org.matsim.prepare.pt.CorrectRouteTypes\
	 --transform-schedule org.matsim.application.prepare.pt.AdjustSameDepartureTimes\
	 --pseudo-network withLoopLinks\
	 --merge-stops mergeToParentAndRouteTypes\
	 --shp $(word 3,$^)

	$(JAVA_APP) prepare endless-circle-line\
	  --network $(NETWORK_MATSIM_PT)\
	  --transit-schedule $(OUTPUT)/berlin-$(VERSION)-transitSchedule.xml.gz\
	  --transit-vehicles $(OUTPUT)/berlin-$(VERSION)-transitVehicles.xml.gz\
	  --output-transit-schedule $(OUTPUT)/berlin-$(VERSION)-transitSchedule.xml.gz\
	  --output-transit-vehicles $(OUTPUT)/berlin-$(VERSION)-transitVehicles.xml.gz

  # Very last step depends on counts and the network to set better capacities
	$(JAVA_APP) prepare link-capacity-from-measurements\
	 	--network $@\
	 	--counts $(word 4,$^)\
	 	--under-estimated $(word 5,$^)\
	 	--output $@

# register the VMZ counts (from 2018; see filename below) onto the network:
$(VMZ_COUNTS): $(NETWORK_MATSIM) $(COUNTS_BERLIN_2018) $(LINK_GEOMETRIES) $(COUNTS_MAPPING)
	$(JAVA_APP) prepare counts-from-vmz\
	 --network $<\
	 --excel $(word 2,$^)\
	 --network-geometries $(word 3,$^)\
	 --output $@\
	 --input-crs EPSG:31468\
	 --target-crs $(CRS)\
	 --counts-mapping $(word 4, $^)

# convert the gpkg facilities (for activity locations) into MATSim format.
$(FACILITIES_XML): $(NETWORK_MATSIM) $(FACILITIES_GPKG) $(FACILITY_MAPPING) $(PLANUNGSRAUM_25833)
	$(JAVA_APP) prepare facilities --network $< --shp $(word 2,$^)\
	 --facility-mapping $(word 3,$^)\
	 --zones-shp $(word 4,$^)\
	 --output $@

$(BERLIN_ONLY_100PCT): $(PLR_2013_2020) $(PLANUNGSRAUM_25833) $(FACILITIES_GPKG)
	$(JAVA_APP) prepare berlin-population\
		--input $<\
		--sample 1.0\
		--shp $(word 2,$^) --shp-crs EPSG:25833\
		--facilities $(word 3,$^) --facilities-attr resident\
		--output $@

# (presumably generates a synthetic population for Berlin from the "PLR" data, i.e. the population attribute marginals at LOR500 level)
$(BERLIN_ONLY_25PCT): $(PLR_2013_2020) $(PLANUNGSRAUM_25833) $(FACILITIES_GPKG)
	$(JAVA_APP) prepare berlin-population\
		--input $<\
		--shp $(word 2,$^) --shp-crs EPSG:25833\
		--facilities $(word 3,$^) --facilities-attr resident\
		--output $@

$(BRANDENBURG_ONLY_25PCT): $(FACILITIES_GPKG) $(VG5000_GEM) $(REGIONALSTAT_POP) $(REGIONALSTAT_EMPL)
	$(JAVA_APP) prepare brandenburg-population\
	 --shp $(word 2,$^)\
	 --population $(word 3,$^)\
	 --employees $(word 4,$^)\
 	 --facilities $< --facilities-attr resident\
 	 --output $@

# (merges the two population, and joins spatial category into each person)
$(BERLIN_BRANDENBURG_STATIC_25PCT): $(BERLIN_ONLY_25PCT) $(BRANDENBURG_ONLY_25PCT) $(REGIOSTAR)
	$(JAVA_APP) prepare merge-populations $< $(word 2, $^)\
	 --output $@

	$(JAVA_APP) prepare lookup-regiostar --input $@ --output $@ --xls $(word 3, $^)

$(BERLIN_BRANDENBURG_ACTS_25PCT): $(BERLIN_BRANDENBURG_STATIC_25PCT) $(SRV_PERSONS) $(SRV_ACTS) $(SRV_ZONES) $(FACILITIES_XML) $(NETWORK_MATSIM)
	$(JAVA_APP) prepare activity-sampling --seed 1 --input $< --output $@ --persons $(word 2, $^) --activities $(SRV_ACTS)

	$(JAVA_APP) prepare assign-reference-population --population $@ --output $@\
	 --persons $(word 2, $^)\
	 --activities $(word 3, $^)\
	 --shp $(word 4,$^)\
	 --shp-crs $(CRS)\
	 --facilities $(word 5,$^)\
	 --network $(word 6,$^)\

# ("reference population" = population taken from SrV; used to assign activity chains. SrV records have to be processed (manually, not automatically done here) by extract_population_data.py to create src/main/python/table-....csv as input.
# Input tables can also be found on shared-svn (restricted access): https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/matsim-berlin/data/SrV/converted/
# Assign activity locations to agents (except home, which is set before).
$(BERLIN_BRANDENBURG_INITIAL_25PCT): $(BERLIN_BRANDENBURG_ACTS_25PCT) $(FACILITIES_XML) $(NETWORK_MATSIM) $(VG5000_GEM) $(REGIONALSTAT_COMMUTER) $(BERLIN_COMMUTER)
	$(JAVA_APP) prepare init-location-choice\
	 --input $<\
	 --output $@\
	 --facilities $(word 2,$^)\
	 --network $(word 3,$^)\
	 --shp $(word 4,$^)\
	 --commuter $(word 5,$^)\
	 --berlin-commuter $(word 6,$^)

	# For debugging and visualization
	$(JAVA_APP) prepare downsample-population $@\
		 --sample-size 0.25\
		 --samples 0.1 0.03 0.01\

## Freight-Processing currrently does not work. Should probably move to a separate makefile
#$(BERLIN_BRANDENBURG_LONGHAULFREIGHT_25PCT): $(GERMAN_FREIGHT_25PCT) $(GERMAN_FREIGHT_NETWORK) $(AREA_SHP)
#
# $(NETWORK_MATSIM) was defined as input but never used?!
#	$(JAVA_APP) prepare extract-freight-trips $<\
#	 --network $(word 2,$^)\
#	 --input-crs $(CRS)\
#	 --target-crs $(CRS)\
#	 --shp $(word 3,$^)\
#	 --cut-on-boundary\
#	 --output $@

#$(COMMERCIAL_FACILITIES): $(REGION_4326) $(BB_ZONES_4326) $(BB_BUILDINGS_4326) $(BERLIN_LANDUSE_4326) $(COMMERCIAL_TRAFFIC_AREA_DATA)
#	$(JAVA_APP) prepare create-data-distribution-of-structure-data\
#	 --outputFacilityFile $@\
#	 --outputDataDistributionFile $(DATA_DISTR_PER_ZONE)\
#	 --landuseConfiguration useOSMBuildingsAndLanduse\
#	 --regionsShapeFileName $<\
#	 --regionsShapeRegionColumn "GEN"\
#	 --zoneShapeFileName $(word 2,$^)\
#	 --zoneShapeFileNameColumn "id"\
#	 --buildingsShapeFileName $(word 3,$^)\
#	 --shapeFileBuildingTypeColumn "type"\
#	 --landuseShapeFileName $(word 4,$^)\
#	 --shapeFileLanduseTypeColumn "fclass"\
#	 --shapeCRS "EPSG:4326"\
#	 --pathToInvestigationAreaData $(word 5,$^)
#	 
#$(DATA_DISTR_PER_ZONE): $(COMMERCIAL_FACILITIES)
#	echo "this is only here because $(DATA_DISTR_PER_ZONE) is created together with $(COMMERCIAL_FACILITIES)"
#
#$(BERLIN_SMALLSCALE_COMMERCIAL_25PCT): $(NETWORK_MATSIM) $(COMMERCIAL_FACILITIES) $(DATA_DISTR_PER_ZONE) $(BB_ZONES_VKZ_4326)
#	$(JAVA_APP) prepare generate-small-scale-commercial-traffic\
#	  input/$(VERSION)/berlin-$(VERSION).config.xml\
#	 --pathToDataDistributionToZones $(abspath $(word 3,$^))\
#	 --pathToCommercialFacilities $(abspath $(word 2,$^))\
#	 --sample 0.25\
#	 --jspritIterations 10\
#	 --creationOption createNewCarrierFile\
#	 --network $(abspath $<)\
#	 --smallScaleCommercialTrafficType completeSmallScaleCommercialTraffic\
#	 --zoneShapeFileName $(abspath $(word 4,$^))\
#	 --zoneShapeFileNameColumn "id"\
#	 --shapeCRS "EPSG:4326"\
#	 --numberOfPlanVariantsPerAgent 5\
#	 --nameOutputPopulation $(notdir $@)\
#	 --pathOutput output/commercialPersonTraffic
#
#	mv output/commercialPersonTraffic/$(notdir $@) $@
#	#rm -r output/commercialPersonTraffic delete or keep?

$(BERLIN_CADYTS_INPUT_25PCT): $(BERLIN_BRANDENBURG_INITIAL_25PCT) $(BERLIN_SMALLSCALE_COMMERCIAL_25PCT)
	$(JAVA_APP) prepare merge-populations $^\
	 --output $@

$(VEHICLESFILE_OUT): $(VEHICLESFILE_IN)
	cp $(VEHICLESFILE_IN) $(VEHICLESFILE_OUT)

$(BERLIN_CADYTS_OUTPUT_25PCT): $(BERLIN_CADYTS_INPUT_25PCT) $(VEHICLESFILE_OUT)
	cat input/cadyts-config-template.xml | sed -e "s/==VERSION==/$(VERSION)/g" > ${OUTPUT}/cadyts.config.xml
	./src/main/sh/cadyts.sh ${OUTPUT}/cadyts.config.xml $(VERSION)

$(BERLIN_CADYTS_FINAL_25PCT): $(BERLIN_CADYTS_OUTPUT_25PCT) $(BERLIN_CADYTS_INPUT_25PCT) 
	$(JAVA_APP) prepare extract-plans-idx\
	 --input $<\
	 --output $(BERLIN_CADYTS_SELECTION_25PCT)

	$(JAVA_APP) prepare select-plans-idx\
	 --input $(word 2,$^)\
	 --csv $(BERLIN_CADYTS_SELECTION_25PCT)\
	 --output $@
 
$(BERLIN_CADYTS_SELECTION_25PCT): $(BERLIN_CADYTS_FINAL_25PCT)
	echo "check if $(BERLIN_CADYTS_SELECTION_25PCT) was produced" 

# These depend on the output of cadyts calibration runs
# should we really use NETWORK_MATSIM here or not maybe NETWORK_MATSIM_PT
$(BERLIN_BRANDENBURG_INITIAL_25PCT_AFTER_CADYTS): $(FACILITIES_XML) $(NETWORK_MATSIM) $(BERLIN_BRANDENBURG_LONGHAULFREIGHT_25PCT) $(BERLIN_CADYTS_FINAL_25PCT) $(AREA_SHP)
	$(JAVA_APP) prepare scenario-cutout\
	 --population $(word 4,$^)\
	 --facilities $<\
	 --network $(word 2,$^)\
	 --output-population $@\
	 --output-network $(OUTPUT)/network-cutout.xml.gz\
	 --output-facilities $(OUTPUT)/facilities-cutout.xml.gz\
	 --input-crs $(CRS)\
	 --shp $(word 5,$^)

	$(JAVA_APP) prepare split-activity-types-duration\
 	 --exclude commercial_start,commercial_end,freight_start,freight_end\
	 --input $@ --output $@

	$(JAVA_APP) prepare set-car-avail --input $@ --output $@

	$(JAVA_APP) prepare check-car-avail --input $@ --output $@ --mode walk

	$(JAVA_APP) prepare fix-subtour-modes --input $@ --output $@ --coord-dist 100

	$(JAVA_APP) prepare merge-populations $@ $(word 3,$^)\
		--output $@

	$(JAVA_APP) prepare downsample-population $@\
		 --sample-size 0.25\
		 --samples 0.1 0.03 0.01 0.001\

#  According to a discussion with CR: The following step is based on the output of the constant-calibration and is executed 
# manually with src/main/sh/runCalib.sh and src/main/python/calibrate.py. In the following step only the first two sub-steps 
# are necessary and should be repeated for every sample you are interested in. The both input-files in the original-version 
# are the output of the constant- calibration. 
#$(BERLIN_10PCT_AFTER_CHOICE_EXPERIMENTS): $(MODECHOICE_10PCT_BASELINE_PLANS) $(CHOICE_EXPERIMENTS_10PCT_BASELINE_PLANS)
$(BERLIN_10PCT_AFTER_CHOICE_EXPERIMENTS): $(MODECHOICE_10PCT_BASELINE_PLANS) 
	$(JAVA_APP) prepare clean-population\
	 --plans $<\
	 --remove-unselected-plans\
	 --output $@
	 
	# TODO read from and write into the same file?
	$(JAVA_APP) prepare taste-variations\
	 --input $@ --output $@

#	$(JAVA_APP) prepare downsample-population $@\
#		--sample-size 0.1\
#		--samples 0.01 0.001\
#
#	$(JAVA_APP) prepare clean-population\
#	 	--plans $(word 2,$^)\
#	 	--remove-unselected-plans\
#	 	--output $(subst 10pct,3pct,$@)

$(BERLIN_DOWNTOWN_3PCT_PLANS): $(BERLIN_INNER_CITY_GPKG) $(BERLIN_3PCT_PLANS) $(FACILITIES_XML) $(NETWORK_MATSIM)

	mkdir -p $(OUTPUT)/inner-city

	$(JAVA_APP) prepare scenario-cutout\
	 --population $(word 2,$^)\
	 --facilities $(word 3,$^)\
	 --network $(word 4,$^)\
	 --output-population $@\
	 --output-network $(OUTPUT)/inner-city/berlin-downtown-$(VERSION)-network.xml.gz\
	 --output-facilities $(OUTPUT)/inner-city/berlin-downtown-$(VERSION)-facilities.xml.gz\
	 --input-crs $(CRS)\
	 --shp "$<"

$(RANDOM_DRT_FLEET_10K): $(NETWORK_MATSIM) $(BERLIN_SHP_25832) $(BERLIN_INNER_CITY_GPKG)
	$(JAVA_APP) prepare create-drt-vehicles\
	 --network $<\
	 --shp "$(word 2,$^)"\
	 --output $(OUTPUT)/berlin-$(VERSION).\
	 --vehicles 10000\
	 --seats 4

	$(JAVA_APP) prepare create-drt-vehicles\
	 --network $<\
	 --shp "$(word 3,$^)"\
	 --output $(OUTPUT)/berlin-$(VERSION).\
	 --vehicles 500\
	 --seats 4
	 
setup: 
	echo "setup $(OUTPUT)"
	mkdir -p $(OUTPUT)
	mkdir -p $(TMP_DIR)
	
	
prepare-network-and-counts: $(NETWORK_MATSIM_PT) $(VMZ_COUNTS)
	echo done

prepare-calibration: $(BERLIN_CADYTS_INPUT_25PCT) $(NETWORK_MATSIM_PT) $(VMZ_COUNTS)
	#make -Bndri prepare-calibration | make2graph | gv2gml -o prepare-calibration_graph.gml
	echo "Done"
	
prepare-run-cadyts: $(BERLIN_CADYTS_OUTPUT_25PCT) $(NETWORK_MATSIM_PT) $(VMZ_COUNTS)
	echo "done"

prepare-initial: $(BERLIN_BRANDENBURG_INITIAL_25PCT_AFTER_CADYTS) $(NETWORK_MATSIM_PT)
	#make -Bndri prepare-initial | make2graph | gv2gml -o prepare-initial_graph.gml
	echo "Done"

prepare-drt: $(RANDOM_DRT_FLEET_10K)
	#make -Bndri prepare-drt | make2graph | gv2gml -o prepare-drt_graph.gml
	echo "Done"

prepare: $(BERLIN_10PCT_AFTER_CHOICE_EXPERIMENTS)
	#make -Bndri prepare | make2graph | gv2gml -o prepare_graph.gml
	echo "Done"
