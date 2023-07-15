

JAR := matsim-berlin-*.jar
V := v6.0
CRS := EPSG:25832

p := input/$V
germany := ../shared-svn/projects/matsim-germany
berlin := ../public-svn/matsim/scenarios/countries/de/berlin/berlin-$V

MEMORY ?= 20G
REGIONS := brandenburg
SHP_FILES=$(patsubst %, input/shp/%-latest-free.shp.zip, $(REGIONS))

osmosis := osmosis/bin/osmosis

# Scenario creation tool
sc := java -Xmx$(MEMORY) -XX:+UseParallelGC -cp $(JAR) org.matsim.prepare.RunOpenBerlinCalibration

.PHONY: prepare

$(JAR):
	mvn package

${SHP_FILES}:
	mkdir -p input/shp
	curl https://download.geofabrik.de/europe/germany/$(@:input/shp/%=%) -o $@

input/brandenburg.osm.pbf:
	curl https://download.geofabrik.de/europe/germany/brandenburg-230101.osm.pbf -o $@


$(germany)/RegioStaR-Referenzdateien.xlsx:
	curl https://mcloud.de/downloads/mcloud/536149D1-2902-4975-9F7D-253191C0AD07/RegioStaR-Referenzdateien.xlsx -o $@

input/landuse.shp: ${SHP_FILES}
	mkdir -p input/landuse
	$(sc) prepare create-landuse-shp $^\
	 --target-crs ${CRS}\
	 --output $@

input/facilities.shp: input/brandenburg.osm.pbf
	$(sc) prepare facility-shp\
	 --activity-mapping input/activity_mapping.json\
	 --input $<\
	 --output $@


input/PLR_2013_2020.csv:
	curl https://instantatlas.statistik-berlin-brandenburg.de/instantatlas/interaktivekarten/kommunalatlas/Kommunalatlas.zip --insecure -o atlas.zip
	unzip atlas.zip -d input
	rm atlas.zip

$(berlin)/input/shp/Planungsraum_EPSG_25833.shp:
	curl https://www.stadtentwicklung.berlin.de/planen/basisdaten_stadtentwicklung/lor/download/LOR_SHP_EPSG_25833.zip -o tmp.zip
	unzip tmp.zip -d $(berlin)/input
	rm tmp.zip


input/network.osm: input/brandenburg.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,living_street\
	 --bounding-polygon file="$p/area/area.poly"\
	 --used-node --wb input/network-detailed.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction\
	 --used-node --wb input/network-coarse.osm.pbf

	$(osmosis) --rb file=input/network-coarse.osm.pbf --rb file=input/network-detailed.osm.pbf\
  	 --merge\
  	 --tag-transform file=input/remove-railway.xml\
  	 --wx $@

	rm input/network-detailed.osm.pbf
	rm input/network-coarse.osm.pbf


input/sumo.net.xml: input/network.osm

	$(SUMO_HOME)/bin/netconvert --geometry.remove --ramps.guess --ramps.no-split\
	 --type-files $(SUMO_HOME)/data/typemap/osmNetconvert.typ.xml,$(SUMO_HOME)/data/typemap/osmNetconvertUrbanDe.typ.xml\
	 --tls.guess-signals true --tls.discard-simple --tls.join --tls.default-type actuated\
	 --junctions.join --junctions.corner-detail 5\
	 --roundabouts.guess --remove-edges.isolated\
	 --no-internal-links --keep-edges.by-vclass passenger\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian,bicycle\
	 --output.original-names --output.street-names\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --osm-files $< -o=$@


$p/berlin-v6.0-network.xml.gz:
	# Use 5.x network
	$(sc) prepare reproject-network\
	 --input $(berlin)/../berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz\
	 --transit-schedule $(berlin)/../berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz\
	 --output $@\
 	 --output-transit $p/berlin-v6.0-transitSchedule.xml.gz\
	 --input-crs EPSG:31468\
	 --target-crs $(CRS)


$p/berlin-v6.0-network-with-pt.xml.gz: $p/berlin-v6.0-network.xml.gz
	# Copy 5.x network stuff
	cp $< $@
	cp $(berlin)/../berlin-v5.5-10pct/input/berlin-v5.5-transit-vehicles.xml.gz $p/berlin-v6.0-transitVehicles.xml.gz


$p/berlin-v6.1-network.xml.gz: input/sumo.net.xml
	$(sc) prepare network-from-sumo $< --target-crs $(CRS) --output $@

	$(sc) prepare clean-network $@ --output $@ --modes car

	$(sc) prepare sample-network --network $@

	# To update features and params, running python code is necessary
	$(sc) prepare network-params --network $@ --input-features input/sumo.net-edges.csv.gz --output $@

	$(sc) prepare network-freespeed --network $@ --params input/network-params.json --output $@

$p/berlin-v6.1-network-with-pt.xml.gz: $p/berlin-v6.1-network.xml.gz
	$(sc) prepare transit-from-gtfs --network $< --output=$p\
	 --name berlin-$V --date "2023-06-07" --target-crs $(CRS) \
	 $(germany)/gtfs/complete-pt-2023-06-06.zip\
	 --shp $p/pt-area/pt-area.shp

$p/berlin-v6.1-counts-car-vmz.xml.gz: $p/berlin-v6.1-network.xml.gz
	$(sc) prepare counts-from-vmz\
	 --excel ../shared-svn/projects/matsim-berlin/berlin-v5.5/original_data/vmz_counts_2018/Datenexport_2018_TU_Berlin.xlsx\
	 --network $<\
	 --network-geometries $p/berlin-v6.0-network-linkGeometries.csv\
	 --output $p/\
	 --version berlin-$(V)\
	 --input-crs EPSG:31468\
	 --target-crs $(CRS)\
	 --ignored-counts input/ignored_counts.csv

input/v6.0/berlin-v6.0-counts-car-vmz.xml.gz:
	$(sc) prepare counts-from-vmz-old\
	 --csv ../shared-svn/projects/matsim-berlin/berlin-v5.5/original_data/vmz_counts_2018/CountsId_to_linkId.csv\
	 --excel ../shared-svn/projects/matsim-berlin/berlin-v5.5/original_data/vmz_counts_2018/Datenexport_2018_TU_Berlin.xlsx\
 	 --output $@

$p/berlin-$V-facilities.xml.gz: $p/berlin-$V-network.xml.gz input/facilities.shp
	$(sc) prepare facilities --network $< --shp $(word 2,$^)\
	 --output $@

$p/berlin-only-$V-25pct.plans.xml.gz: input/PLR_2013_2020.csv $(berlin)/input/shp/Planungsraum_EPSG_25833.shp input/landuse.shp
	$(sc) prepare berlin-population\
		--input $<\
		--shp $(word 2,$^) --shp-crs EPSG:25833\
		--landuse $(word 3,$^) --landuse-filter residential\
		--output $@


$p/brandeburg-only-$V-25pct.plans.xml.gz: input/landuse.shp
	$(sc) prepare brandenburg-population\
	 --shp $(germany)/vg5000/vg5000_ebenen_0101/VG5000_GEM.shp\
	 --population $(germany)/regionalstatistik/population.csv\
	 --employees $(germany)/regionalstatistik/employed.json\
 	 --landuse $< --landuse-filter residential\
 	 --output $@

$p/berlin-static-$V-25pct.plans.xml.gz: $p/berlin-only-$V-25pct.plans.xml.gz $p/brandeburg-only-$V-25pct.plans.xml.gz
	$(sc) prepare merge-populations $^\
	 --output $@

	$(sc) prepare lookup-regiostar --input $@ --output $@ --xls $(germany)/RegioStaR-Referenzdateien.xlsx


$p/berlin-activities-$V-25pct.plans.xml.gz: $p/berlin-static-$V-25pct.plans.xml.gz
	$(sc) prepare activity-sampling --seed 1 --input $< --output $@ --persons src/main/python/table-persons.csv --activities src/main/python/table-activities.csv

$p/berlin-initial-$V-25pct.plans.xml.gz: $p/berlin-activities-$V-25pct.plans.xml.gz $p/berlin-$V-facilities.xml.gz $p/berlin-$V-network.xml.gz
	$(sc) prepare init-location-choice\
	 --input $<\
	 --output $@\
	 --facilities $(word 2,$^)\
	 --network $(word 3,$^)\
	 --shp $(germany)/vg5000/vg5000_ebenen_0101/VG5000_GEM.shp\
	 --commuter $(germany)/regionalstatistik/commuter.csv\

	# For debugging and visualization
	$(sc) prepare downsample-population $@\
		 --sample-size 0.25\
		 --samples 0.1 0.01\


$p/berlin-longHaulFreight-$V-25pct.plans.xml.gz: $p/berlin-$V-network.xml.gz
	$(sc) prepare extract-freight-trips ../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz\
	 --network ../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz\
	 --input-crs $(CRS)\
	 --target-crs $(CRS)\
	 --shp $p/area/area.shp\
	 --cut-on-boundary\
	 --output $@

$p/berlin-commercialPersonTraffic-$V-25pct.plans.xml.gz:
	$(sc) prepare generate-small-scale-commercial-traffic\
	  input/commercialTraffic\
	 --sample 0.25\
	 --jspritIterations 1\
	 --creationOption createNewCarrierFile\
	 --landuseConfiguration useOSMBuildingsAndLanduse\
	 --smallScaleCommercialTrafficType commercialPersonTraffic\
	 --zoneShapeFileName $(berlin)/input/shp/berlinBrandenburg_Zones_VKZ_4326.shp\
	 --buildingsShapeFileName $(berlin)/input/shp/buildings_BerlinBrandenburg_4326.shp\
	 --landuseShapeFileName $(berlin)/input/shp/berlinBrandenburg_landuse_4326.shp\
	 --shapeCRS "EPSG:4326"\
	 --resistanceFactor "0.005"\
	 --numberOfPlanVariantsPerAgent 5\
	 --nameOutputPopulation $(notdir $@)\
	 --pathOutput output/commercialPersonTraffic

	mv output/commercialPersonTraffic/$(notdir $@) $@

$p/berlin-goodsTraffic-$V-25pct.plans.xml.gz:
	$(sc) prepare generate-small-scale-commercial-traffic\
	  input/commercialTraffic\
	 --sample 0.25\
	 --jspritIterations 1\
	 --creationOption createNewCarrierFile\
	 --landuseConfiguration useOSMBuildingsAndLanduse\
	 --smallScaleCommercialTrafficType goodsTraffic\
	 --zoneShapeFileName $(berlin)/input/shp/berlinBrandenburg_Zones_VKZ_4326.shp\
	 --buildingsShapeFileName $(berlin)/input/shp/buildings_BerlinBrandenburg_4326.shp\
	 --landuseShapeFileName $(berlin)/input/shp/berlinBrandenburg_landuse_4326.shp\
	 --shapeCRS "EPSG:4326"\
	 --resistanceFactor "0.005"\
	 --numberOfPlanVariantsPerAgent 5\
	 --nameOutputPopulation $(notdir $@)\
	 --pathOutput output/goodsTraffic

	mv output/goodsTraffic/$(notdir $@) $@

$p/berlin-cadyts-input-$V-25pct.plans.xml.gz: $p/berlin-initial-$V-25pct.plans.xml.gz $p/berlin-commercialPersonTraffic-$V-25pct.plans.xml.gz
	$(sc) prepare merge-populations $^\
	 --output $@

# This file requires eval runs
$p/berlin-initial-$V-25pct.experienced_plans.xml.gz:
	$(sc) prepare merge-plans output/exp-*/*.output_experienced_plans.xml.gz\
		--output $@

	# Only for debugging
	$(sc) prepare downsample-population $@\
     	 --sample-size 0.25\
     	 --samples 0.05 0.01\

ERROR_METRIC ?= log_error
eval-opt: $p/berlin-initial-$V-25pct.experienced_plans.xml.gz
	$(sc) prepare run-count-opt\
	 --input $<\
	 --network $p/berlin-$V-network-with-pt.xml.gz\
     --counts $p/berlin-$V-counts-car-vmz.xml.gz\
	 --output $p/berlin-$V-25pct.plans_selection_$(ERROR_METRIC).csv\
	 --metric $(ERROR_METRIC)

	$(sc) prepare select-plans-idx\
 	 --input $p/berlin-cadyts-input-$V-25pct.plans.xml.gz\
 	 --csv $p/berlin-$V-25pct.plans_selection_$(ERROR_METRIC).csv\
 	 --output $p/berlin-$V-25pct.plans_$(ERROR_METRIC).xml.gz

	$(sc) run --mode "eval" --all-car --output "output/eval-$(ERROR_METRIC)" --25pct --population "berlin-$V-25pct.plans_$(ERROR_METRIC).xml.gz"\
	 --config $p/berlin-$V-base-calib.config.xml


# These depend on the output of optimization runs
$p/berlin-$V-25pct.plans.xml.gz: $p/berlin-$V-facilities.xml.gz $p/berlin-$V-network.xml.gz $p/berlin-goodsTraffic-$V-25pct.plans.xml.gz $p/berlin-longHaulFreight-$V-25pct.plans.xml.gz
	$(sc) prepare filter-relevant-agents\
	 --input $p/berlin-$V-25pct.plans_log_error.xml.gz --output $@\
	 --shp input/v6.0/area/area.shp\
	 --facilities $<\
	 --network $(word 2,$^)

	$(sc) prepare split-activity-types-duration\
 	 --exclude commercial_start,commercial_end,freight_start,freight_end\
	 --input $@ --output $@

	$(sc) prepare set-car-avail --input $@ --output $@

	$(sc) prepare check-car-avail --input $@ --output $@ --mode walk

	$(sc) prepare fix-subtour-modes --input $@ --output $@

	$(sc) prepare merge-populations $@ $(word 3,$^) $(word 4,$^)\
		--output $@

	$(sc) prepare downsample-population $@\
		 --sample-size 0.25\
		 --samples 0.1 0.01 0.001\

prepare-calibration: $p/berlin-cadyts-input-$V-25pct.plans.xml.gz $p/berlin-$V-network-with-pt.xml.gz $p/berlin-$V-counts-car-vmz.xml.gz
	echo "Done"

prepare: $p/berlin-$V-25pct.plans.xml.gz $p/berlin-$V-network-with-pt.xml.gz
	echo "Done"