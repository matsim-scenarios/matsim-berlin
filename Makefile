
###################################
########### SETUP #################
###################################
JAR := matsim-berlin-*.jar
VERSION := v7.0
CRS := EPSG:25832
MEMORY ?= 20G
## either use the global installation via, e.g. apt-get, or define where this is comming from
osmosis := osmosis
# Scenario creation tool
JAVA_APP := java -Xmx$(MEMORY) -XX:+UseParallelGC -cp $(JAR) org.matsim.prepare.RunOpenBerlinCalibration
SVN_PATH := ../../..
OUTPUT := output/$(VERSION)

# you need SUMO (set $(SUMO_HOME) )to run this script in version 1.20.0 (or greater ?), either build it yourself 
# or use https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/matsim-germany/sumo/sumo_1.20.0/

###################################
######## INPUT ####################
###################################

GERMANY := $(SVN_PATH)/shared-svn/projects/matsim-germany
BERLINSHARED := $(SVN_PATH)/shared-svn/projects/matsim-berlin
BERLINPUBLIC := $(SVN_PATH)/public-svn/matsim/scenarios/countries/de/berlin/berlin-$(VERSION)

GERMAN_FREIGHT_25PCT := ../../../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz
GERMAN_FREIGHT_NETWORK := ../../../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz

AREA_POLY := input/$(VERSION)/area/area.poly
AREA_SHP := input/$(VERSION)/area/area.shp
PT_AREA := input/$(VERSION)/pt-area/pt-area.shp
REMOVE_RAILWAY := input/remove-railway.xml
COUNTS_UNDERESTIMATED := input/counts_underestimated.csv
COUNTS_MAPPING := input/counts_mapping.csv
FACILITY_MAPPING := input/facility_mapping.json
COMMERCIAL_TRAFFIC_AREA_DATA := input/commercialTrafficAreaData.csv
ACTIVITY_MAPPING := input/activity_mapping.json

SUMO_OSM_NETCONVERT := $(SUMO_HOME)/data/typemap/osmNetconvert.typ.xml
SUMO_OSM_NETCONVERT_URBAN_DE := $(SUMO_HOME)/data/typemap/osmNetconvertUrbanDe.typ.xml

## TODO this should be store in shared-svn
BRANDENBURG_OSM_LOCAL := input/brandenburg.osm.pbf
BRANDENBURG_OSM_URL := https://download.geofabrik.de/europe/germany/brandenburg-230101.osm.pbf 

PLANUNGSRAUM_25833 := $(BERLINPUBLIC)/input/shp/Planungsraum_EPSG_25833.shp
## link no longer working
PLANUNGSRAUM_25833_URL := https://www.stadtentwicklung.berlin.de/planen/basisdaten_stadtentwicklung/lor/download/LOR_SHP_EPSG_25833.zip
REGION_4326 := $(BERLINPUBLIC)/input/shp/region_4326.shp
BB_ZONES_4326 := $(BERLINPUBLIC)/input/shp/berlinBrandenburg_Zones_VKZ_4326.shp
BB_BUILDINGS_4326 := $(BERLINPUBLIC)/input/shp/buildings_BerlinBrandenburg_4326.shp
BERLIN_LANDUSE_4326 := $(BERLINPUBLIC)/input/shp/berlinBrandenburg_landuse_4326.shp
BB_ZONES_VKZ_4326 := $(BERLINPUBLIC)/input/shp/berlinBrandenburg_Zones_VKZ_4326.shp
BERLIN_INNER_CITY_GPKG := $(BERLINPUBLIC)/../berlin-v6.4/input/shp/berlin_inner_city.gpkg
BERLIN_INNER_CITY_GPKG2 := $(BERLINPUBLIC)/berlin-$(VERSION)/input/shp/berlin_inner_city.gpkg
BERLIN_SHP_25832 := $(BERLINPUBLIC)/berlin-$(VERSION)/input/shp/Berlin_25832.shp

COUNTS_BERLIN_2018 := $(BERLINSHARED)/berlin-v5.5/original_data/vmz_counts_2018/Datenexport_2018_TU_Berlin.xlsx
PLR_2013_2020 := $(BERLINSHARED)/data/statistik-berlin-brandenburg/PLR_2013_2020.csv
SRV_PERSONS := $(BERLINSHARED)/data/SrV/2018/converted/table-persons.csv
SRV_ACTS := $(BERLINSHARED)/data/SrV/2018/converted/table-activities.csv
SRV_ZONES := $(BERLINSHARED)/data/SrV/2018/zones/zones.shp

GTFS_DAY_TO_CONVERT := "2024-11-19"
GTFS_DATA := $(GERMANY)/gtfs/complete-pt-2024-10-27.zip 
VG5000_GEM := $(GERMANY)/vg5000/vg5000_ebenen_0101/VG5000_GEM.shp
REGIONALSTAT_POP := $(GERMANY)/regionalstatistik/population.csv
REGIONALSTAT_EMPL := $(GERMANY)/regionalstatistik/employed.json
REGIONALSTAT_COMMUTER := $(GERMANY)/regionalstatistik/commuter.csv
## (link no longer working; in general, mcloud no longer exists; RegioStar = spatial planning categories)
REGIOSTAR_URL := https://mcloud.de/downloads/mcloud/536149D1-2902-4975-9F7D-253191C0AD07/RegioStaR-Referenzdateien.xlsx
REGIOSTAR := $(GERMANY)/RegioStaR-Referenzdateien.xlsx

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
BERLIN_BRANDENBURG_LONGHAULFREIGHT_25PCT := $(OUTPUT)/berlin-longHaulFreight-$(VERSION)-25pct.plans.xml.gz
COMMERCIAL_FACILITIES := $(OUTPUT)/commercialFacilities.xml.gz
DATA_DISTR_PER_ZONE := $(OUTPUT)/dataDistributionPerZone.csv
BERLIN_SMALLSCALE_COMMERCIAL_25PCT := $(OUTPUT)/berlin-small-scale-commercialTraffic-$(VERSION)-25pct.plans.xml.gz
BERLIN_CADYTS_INPUT_25PCT := $(OUTPUT)/berlin-cadyts-input-$(VERSION)-25pct.plans.xml.gz
BERLIN_CADYTS_OUTPUT_25PCT := $(OUTPUT)/cadyts/cadyts.output_plans.xml.gz
BERLIN_CADYTS_FINAL_25PCT := $(OUTPUT)/berlin-$(VERSION)-25pct.plans_cadyts.xml.gz
BERLIN_CADYTS_SELECTION_25PCT := $(OUTPUT)/berlin-$(VERSION)-25pct.plans_selection_cadyts.csv
BERLIN_BRANDENBURG_INITIAL_25PCT_AFTER_CADYTS := $(OUTPUT)/berlin-$(VERSION)-25pct.plans-initial.xml.gz
BERLIN_10PCT_AFTER_CHOICE_EXPERIMENTS := $(OUTPUT)/berlin-$(VERSION)-10pct.plans.xml.gz
BERLIN_DOWNTOWN_3PCT_PLANS := $(OUTPUT)/inner-city/berlin-downtown-$(VERSION)-3pct.xml.gz
BERLIN_3PCT_PLANS := $(OUTPUT)/berlin-$(VERSION)-3pct.plans.xml.gz

RANDOM_DRT_FLEET_10K := $(OUTPUT)/berlin-$(VERSION).drt-by-rndLocations-10000vehicles-4seats.xml.gz

## TODO where is this comming from
NETWORK_FT := $(OUTPUT)/berlin-$(VERSION)-network-ft.csv.gz
BERLIN_COMMUTER := $(OUTPUT)/berlin-work-commuter.csv
MODECHOICE_10PCT_BASELINE_PLANS := mode-choice-10pct-baseline/runs/008/008.output_plans.xml.gz
CHOICE_EXPERIMENTS_10PCT_BASELINE_PLANS := choice-experiments/baseline/runs/008/008.output_plans.xml.gz

.PHONY: prepare prepare-calibration prepare-initial prepare-drt
.DELETE_ON_ERROR:

###################################
######## OUTPUT ###################
###################################

$(JAR):
	./mvnw clean package -DskipTests=true

$(BRANDENBURG_OSM_LOCAL):
	curl $(BRANDENBURG_OSM_URL) -o $@
# (Brandenburg OSM, presumably from 2023-01-01)

$(FACILITIES_OSM_LOCAL):
	curl $(FACILITIES_OSM_URL) -o $@


$(REGIOSTAR):
	curl $(REGIOSTAR_URL) -o $@

# Preprocessing and cleaning of raw osm data to geo-referenced activity facilities.
$(FACILITIES_GPKG): $(BRANDENBURG_OSM_LOCAL) $(ACTIVITY_MAPPING)
	$(JAVA_APP) prepare facility-shp\
	 --activity-mapping $(word 2,$^)\
	 --input $<\
	 --output $@

# NEVER used ?!?
#Same OSM version as reference visitations
# (Brandenburg OSM, presumably from 2021-01-01; for "reference visitations" which are used in covid project. Not necessary for transport planning purposes.
#FACILITIES_OSM_LOCAL := input/facilities.osm.pbf
#FACILITIES_OSM_URL := https://download.geofabrik.de/europe/germany/brandenburg-210101.osm.pbf
# The reference visitations used in the covid project refer to this older osm data version.
#input/ref_facilities.gpkg: $(FACILITIES_OSM_LOCAL) $(ACTIVITY_MAPPING)
#	$(JAVA_APP) prepare facility-shp\
#	 --activity-mapping $(word 2,$^)\
#	 --input $<\
#	 --output $@

$(PLR_2013_2020):
	#curl https://instantatlas.statistik-berlin-brandenburg.de/instantatlas/interaktivekarten/kommunalatlas/Kommunalatlas.zip --insecure -o atlas.zip
	#unzip atlas.zip -d input
	#rm atlas.zip
	echo "URL for Kommunalatlas.zip does no longer exist. Use local PLR_2013_2020.csv."
# (Kommunalatlas = kleinräumiges Datenangebot.  "PLR" is the file name after expanding the zipfile; it may mean "Planungsraum".  Contains attributes of LOR zones (at 500 zones level).)

$(PLANUNGSRAUM_25833):
	curl $(PLANUNGSRAUM_25833_URL) -o tmp.zip
	unzip tmp.zip -d $(OUTPUT)
	rm tmp.zip
# (shapefiles LORs = Berlin local system of zones)

# filtering for those parts of the osm data that we need for the network:
$(NETWORK_OSM): $(BRANDENBURG_OSM_LOCAL) $(AREA_POLY) $(REMOVE_RAILWAY)

	# Detailed network includes bikes as well
	 # hard-coded because we delete within this step
	$(osmosis) --rb file=$<\
	 --tf accept-ways bicycle=designated highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,living_street,unclassified,cycleway\
	 --bounding-polygon file="$(word 2,$^)"\
	 --used-node --wb input/network-detailed.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction\
	 --used-node --wb input/network-coarse.osm.pbf

	$(osmosis) --rb file=input/network-coarse.osm.pbf --rb file=input/network-detailed.osm.pbf\
  	 --merge\
  	 --tag-transform file=$(word 3,$^)\
  	 --wx $@

	rm input/network-detailed.osm.pbf
	rm input/network-coarse.osm.pbf

# converting the network from OSM format to SUMO format:
$(NETWORK_SUMO): $(NETWORK_OSM) $(SUMO_OSM_NETCONVERT) $(SUMO_OSM_NETCONVERT_URBAN_DE)

	$(SUMO_HOME)/bin/netconvert --geometry.remove --ramps.guess --ramps.no-split\
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
	 --shp $(word 3,$^)\
	 --commuter $(word 4,$^)\
	 --berlin-commuter $(word 5,$^)

	# For debugging and visualization
	$(JAVA_APP) prepare downsample-population $@\
		 --sample-size 0.25\
		 --samples 0.1 0.03 0.01\

$(BERLIN_BRANDENBURG_LONGHAULFREIGHT_25PCT): $(GERMAN_FREIGHT_25PCT) $(GERMAN_FREIGHT_NETWORK) $(AREA_SHP)

# $(NETWORK_MATSIM) was defined as input but never used?!
	$(JAVA_APP) prepare extract-freight-trips $<\
	 --network $(word 2,$^)\
	 --input-crs $(CRS)\
	 --target-crs $(CRS)\
	 --shp $(word 3,$^)\
	 --cut-on-boundary\
	 --output $@

$(COMMERCIAL_FACILITIES): $(DATA_DISTR_PER_ZONE) $(REGION_4326) $(BB_ZONES_4326) $(BB_BUILDINGS_4326) $(BERLIN_LANDUSE_4326) $(COMMERCIAL_TRAFFIC_AREA_DATA)
	$(JAVA_APP) prepare create-data-distribution-of-structure-data\
	 --outputFacilityFile $@\
	 --outputDataDistributionFile $<\
	 --landuseConfiguration useOSMBuildingsAndLanduse\
 	 --regionsShapeFileName $(word 2$^)\
	 --regionsShapeRegionColumn "GEN"\
	 --zoneShapeFileName $(word 3,$^)\
	 --zoneShapeFileNameColumn "id"\
	 --buildingsShapeFileName $(word 4,$^)\
	 --shapeFileBuildingTypeColumn "type"\
	 --landuseShapeFileName $(word 5,$^)\
	 --shapeFileLanduseTypeColumn "fclass"\
	 --shapeCRS "EPSG:4326"\
	 --pathToInvestigationAreaData $(word 6,$^)

$(BERLIN_SMALLSCALE_COMMERCIAL_25PCT): $(NETWORK_MATSIM) $(COMMERCIAL_FACILITIES) $(DATA_DISTR_PER_ZONE) $(BB_ZONES_VKZ_4326)
	$(JAVA_APP) prepare generate-small-scale-commercial-traffic\
	  input/$(VERSION)/berlin-$(VERSION).config.xml\
	 --pathToDataDistributionToZones $(word 3,$^)\
	 --pathToCommercialFacilities $(notdir $(word 2,$^))\
	 --sample 0.25\
	 --jspritIterations 10\
	 --creationOption createNewCarrierFile\
	 --network $(notdir $<)\
	 --smallScaleCommercialTrafficType completeSmallScaleCommercialTraffic\
	 --zoneShapeFileName $(word 4,$^)\
	 --zoneShapeFileNameColumn "id"\
	 --shapeCRS "EPSG:4326"\
	 --numberOfPlanVariantsPerAgent 5\
	 --nameOutputPopulation $(notdir $@)\
	 --pathOutput output/commercialPersonTraffic

	mv output/commercialPersonTraffic/$(notdir $@) $@
	#rm -r output/commercialPersonTraffic delete or keep?

$(BERLIN_CADYTS_INPUT_25PCT): $(BERLIN_BRANDENBURG_INITIAL_25PCT) $(BERLIN_SMALLSCALE_COMMERCIAL_25PCT)
	$(JAVA_APP) prepare merge-populations $^\
	 --output $@
	 
$(BERLIN_CADYTS_OUTPUT_25PCT): $(BERLIN_CADYTS_INPUT_25PCT)
	echo "=== NOT YET IMPLEMENTED, HERE SHOULD PROBABLY RUN CADYTS === 
	
$(BERLIN_CADYTS_SELECTION_25PCT):$(BERLIN_CADYTS_OUTPUT_25PCT)
	echo "this is only a dummy-step, because CADYTS produces more than one file we need"

$(BERLIN_CADYTS_FINAL_25PCT): $(BERLIN_CADYTS_OUTPUT_25PCT) $(BERLIN_CADYTS_INPUT_25PCT) $(BERLIN_CADYTS_SELECTION_25PCT)
	$(JAVA_APP) prepare extract-plans-idx\
	 --input $<\
	 --output $@

	$(JAVA_APP) prepare select-plans-idx\
	 --input $(word 2,$^)\
	 --csv $(work 3,$^)\
	 --output $@

# These depend on the output of cadyts calibration runs
$(BERLIN_BRANDENBURG_INITIAL_25PCT_AFTER_CADYTS): $(FACILITIES_XML) $(NETWORK_MATSIM) $(BERLIN_BRANDENBURG_LONGHAULFREIGHT_25PCT) $(BERLIN_CADYTS_FINAL_25PCT) $(AREA_SHP)
	$(JAVA_APP) prepare scenario-cutout\
	 --population $(word 4,$^)\
	 --facilities $<\
	 --network $(word 2,$^)\
	 --output-population $@\
	 # TODO where is this comming from
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

$(BERLIN_10PCT_AFTER_CHOICE_EXPERIMENTS): $(MODECHOICE_10PCT_BASELINE_PLANS) $(CHOICE_EXPERIMENTS_10PCT_BASELINE_PLANS)
	$(JAVA_APP) prepare clean-population\
	 --plans $<\
	 --remove-unselected-plans\
	 --output $@
	 
	# TODO read from and write into the same file?
	$(JAVA_APP) prepare taste-variations\
 	 --input $@ --output $@

	$(JAVA_APP) prepare downsample-population $@\
		--sample-size 0.1\
		--samples 0.01 0.001\

	$(JAVA_APP) prepare clean-population\
	 	--plans $(word 2,$^)\
	 	--remove-unselected-plans\
	 	--output $(subst 10pct,3pct,$@)

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

$(RANDOM_DRT_FLEET_10K): $(NETWORK_MATSIM) $(BERLIN_SHP_25832) $(BERLIN_INNER_CITY_GPKG2)
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

prepare-calibration: $(BERLIN_BRANDENBURG_ACTS_25PCT) $(NETWORK_MATSIM_PT) $(VMZ_COUNTS)
	make -Bndri prepare-calibration | make2graph | gv2gml -o prepare-calibration_graph.gml
	echo "Done"

prepare-initial: $(BERLIN_BRANDENBURG_INITIAL_25PCT_AFTER_CADYTS) $(NETWORK_MATSIM_PT)
	make -Bndri prepare-initial | make2graph | gv2gml -o prepare-initial_graph.gml
	echo "Done"

prepare-drt: $(RANDOM_DRT_FLEET_10K)
	make -Bndri prepare-drt | make2graph | gv2gml -o prepare-drt_graph.gml
	echo "Done"

prepare: $(BERLIN_10PCT_AFTER_CHOICE_EXPERIMENTS)
	make -Bndri prepare | make2graph | gv2gml -o prepare_graph.gml
	echo "Done"
