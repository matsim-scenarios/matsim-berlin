

JAR := matsim-berlin-*.jar
V := v6.5
CRS := EPSG:25832

p := input/$V
germany := ../shared-svn/projects/matsim-germany
berlin := ../public-svn/matsim/scenarios/countries/de/berlin/berlin-$V

MEMORY ?= 20G
REGIONS := brandenburg

osmosis := osmosis/bin/osmosis

# Scenario creation tool
sc := java -Xmx$(MEMORY) -XX:+UseParallelGC -cp $(JAR) org.matsim.prepare.RunOpenBerlinCalibration

.PHONY: prepare

$(JAR):
	mvn package

input/brandenburg.osm.pbf:
	curl https://download.geofabrik.de/europe/germany/brandenburg-230101.osm.pbf -o $@

input/facilities.osm.pbf:
	# Same OSM version as reference visitations
	curl https://download.geofabrik.de/europe/germany/brandenburg-210101.osm.pbf -o $@

$(germany)/RegioStaR-Referenzdateien.xlsx:
	curl https://mcloud.de/downloads/mcloud/536149D1-2902-4975-9F7D-253191C0AD07/RegioStaR-Referenzdateien.xlsx -o $@


input/facilities.gpkg: input/brandenburg.osm.pbf
	$(sc) prepare facility-shp\
	 --activity-mapping input/activity_mapping.json\
	 --input $<\
	 --output $@

# This facility file is using an older version that matches the reference visitations more closely
input/ref_facilities.gpkg: input/facilities.osm.pbf
	$(sc) prepare facility-shp\
	 --activity-mapping input/activity_mapping.json\
	 --input $<\
	 --output $@

input/PLR_2013_2020.csv:
	curl https://instantatlas.statistik-berlin-brandenburg.de/instantatlas/interaktivekarten/kommunalatlas/Kommunalatlas.zip --insecure -o atlas.zip
	unzip atlas.zip -d input
	rm atlas.zip

$(berlin)/input/shp/Planungsraum_EPSG_25833.shp:
	# This link is broken, the file is available in the public svn
	curl https://www.stadtentwicklung.berlin.de/planen/basisdaten_stadtentwicklung/lor/download/LOR_SHP_EPSG_25833.zip -o tmp.zip
	unzip tmp.zip -d $(berlin)/input
	rm tmp.zip


input/network.osm: input/brandenburg.osm.pbf

	# Detailed network includes bikes as well
	$(osmosis) --rb file=$<\
	 --tf accept-ways bicycle=designated highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,living_street,unclassified,cycleway\
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
	 --no-internal-links --keep-edges.by-vclass passenger,truck,bicycle\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian\
	 --output.original-names --output.street-names\
	 --osm.lane-access false --osm.bike-access false\
	 --osm.all-attributes\
	 --osm.extra-attributes smoothness,surface,crossing,tunnel,traffic_sign,bus:lanes,bus:lanes:forward,bus:lanes:backward,cycleway,cycleway:right,cycleway:left,bicycle\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --osm-files $< -o=$@


$p/berlin-$V-network.xml.gz: input/sumo.net.xml
	$(sc) prepare network-from-sumo $< --target-crs $(CRS) --lane-restrictions REDUCE_CAR_LANES --output $@

	$(sc) prepare clean-network $@  --output $@ --modes car,bike,ride,truck --remove-turn-restrictions

	$(sc) prepare reproject-network\
	 --input $@	--output $@\
	 --input-crs $(CRS) --target-crs $(CRS)\
	 --mode truck=freight\

	$(sc) prepare apply-network-params freespeed capacity\
 	  --network $@ --output $@\
	  --input-features $p/berlin-$V-network-ft.csv.gz\
	  --model org.matsim.prepare.network.BerlinNetworkParams

	$(sc) prepare apply-network-params capacity\
 	  --network $@ --output $@\
	  --input-features $p/berlin-$V-network-ft.csv.gz\
	  --road-types residential,living_street\
	  --capacity-bounds 0.3\
	  --model org.matsim.application.prepare.network.params.hbs.HBSNetworkParams\
	  --decrease-only


$p/berlin-$V-network-with-pt.xml.gz: $p/berlin-$V-network.xml.gz
	$(sc) prepare transit-from-gtfs --network $< --output=$p\
	 --name berlin-$V --date "2024-11-19" --target-crs $(CRS) \
	 $(germany)/gtfs/complete-pt-2024-10-27.zip\
	 --copy-late-early\
	 --transform-stops org.matsim.prepare.pt.CorrectStopLocations\
	 --transform-routes org.matsim.prepare.pt.CorrectRouteTypes\
	 --transform-schedule org.matsim.application.prepare.pt.AdjustSameDepartureTimes\
	 --pseudo-network withLoopLinks\
	 --merge-stops mergeToParentAndRouteTypes\
	 --shp $p/pt-area/pt-area.shp

	$(sc) prepare endless-circle-line\
 	  --network $p/berlin-$V-network-with-pt.xml.gz\
 	  --transit-schedule $p/berlin-$V-transitSchedule.xml.gz\
 	  --transit-vehicles $p/berlin-$V-transitVehicles.xml.gz\
 	  --output-transit-schedule $p/berlin-$V-transitSchedule.xml.gz\
	  --output-transit-vehicles $p/berlin-$V-transitVehicles.xml.gz

$p/berlin-$V-counts-vmz.xml.gz: $p/berlin-$V-network.xml.gz
	$(sc) prepare counts-from-vmz\
	 --excel ../shared-svn/projects/matsim-berlin/berlin-v5.5/original_data/vmz_counts_2018/Datenexport_2018_TU_Berlin.xlsx\
	 --network $<\
	 --network-geometries $p/berlin-$V-network-linkGeometries.csv\
	 --output $@\
	 --input-crs EPSG:31468\
	 --target-crs $(CRS)\
	 --counts-mapping input/counts_mapping.csv

$p/berlin-$V-facilities.xml.gz: $p/berlin-$V-network.xml.gz input/facilities.gpkg $(berlin)/input/shp/Planungsraum_EPSG_25833.shp
	$(sc) prepare facilities --network $< --shp $(word 2,$^)\
	 --facility-mapping input/facility_mapping.json\
	 --zones-shp $(word 3,$^)\
	 --output $@

$p/berlin-only-$V-100pct.plans.xml.gz: input/PLR_2013_2020.csv $(berlin)/input/shp/Planungsraum_EPSG_25833.shp input/facilities.gpkg
	$(sc) prepare berlin-population\
		--input $<\
		--sample 1.0\
		--shp $(word 2,$^) --shp-crs EPSG:25833\
		--facilities $(word 3,$^) --facilities-attr resident\
		--output $@

$p/berlin-only-$V-25pct.plans.xml.gz: input/PLR_2013_2020.csv $(berlin)/input/shp/Planungsraum_EPSG_25833.shp input/facilities.gpkg
	$(sc) prepare berlin-population\
		--input $<\
		--shp $(word 2,$^) --shp-crs EPSG:25833\
		--facilities $(word 3,$^) --facilities-attr resident\
		--output $@


$p/brandenburg-only-$V-25pct.plans.xml.gz: input/facilities.gpkg
	$(sc) prepare brandenburg-population\
	 --shp $(germany)/vg5000/vg5000_ebenen_0101/VG5000_GEM.shp\
	 --population $(germany)/regionalstatistik/population.csv\
	 --employees $(germany)/regionalstatistik/employed.json\
 	 --facilities $< --facilities-attr resident\
 	 --output $@

$p/berlin-static-$V-25pct.plans.xml.gz: $p/berlin-only-$V-25pct.plans.xml.gz $p/brandenburg-only-$V-25pct.plans.xml.gz
	$(sc) prepare merge-populations $^\
	 --output $@

	$(sc) prepare lookup-regiostar --input $@ --output $@ --xls $(germany)/RegioStaR-Referenzdateien.xlsx


$p/berlin-activities-$V-25pct.plans.xml.gz: $p/berlin-static-$V-25pct.plans.xml.gz $p/berlin-$V-facilities.xml.gz $p/berlin-$V-network.xml.gz
	$(sc) prepare activity-sampling --seed 1 --input $< --output $@ --persons src/main/python/table-persons.csv --activities src/main/python/table-activities.csv

	$(sc) prepare assign-reference-population --population $@ --output $@\
	 --persons src/main/python/table-persons.csv\
  	 --activities src/main/python/table-activities.csv\
  	 --shp $(germany)/../matsim-berlin/data/SrV/zones/zones.shp\
  	 --shp-crs $(CRS)\
	 --facilities $(word 2,$^)\
	 --network $(word 3,$^)\

$p/berlin-initial-$V-25pct.plans.xml.gz: $p/berlin-activities-$V-25pct.plans.xml.gz $p/berlin-$V-facilities.xml.gz $p/berlin-$V-network.xml.gz
	$(sc) prepare init-location-choice\
	 --input $<\
	 --output $@\
	 --facilities $(word 2,$^)\
	 --network $(word 3,$^)\
	 --shp $(germany)/vg5000/vg5000_ebenen_0101/VG5000_GEM.shp\
	 --commuter $(germany)/regionalstatistik/commuter.csv\
	 --berlin-commuter input/berlin-work-commuter.csv

	# For debugging and visualization
	$(sc) prepare downsample-population $@\
		 --sample-size 0.25\
		 --samples 0.1 0.03 0.01\


$p/berlin-longHaulFreight-$V-25pct.plans.xml.gz: $p/berlin-$V-network.xml.gz
	$(sc) prepare extract-freight-trips ../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz\
	 --network ../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz\
	 --input-crs $(CRS)\
	 --target-crs $(CRS)\
	 --shp $p/area/area.shp\
	 --cut-on-boundary\
	 --output $@

$p/commercialFacilities.xml.gz:
	$(sc) prepare create-data-distribution-of-structure-data\
	 --outputFacilityFile $@\
	 --outputDataDistributionFile $p/dataDistributionPerZone.csv\
	 --landuseConfiguration useOSMBuildingsAndLanduse\
 	 --regionsShapeFileName $(berlin)/input/shp/region_4326.shp\
	 --regionsShapeRegionColumn "GEN"\
	 --zoneShapeFileName $(berlin)/input/shp/berlinBrandenburg_Zones_VKZ_4326.shp\
	 --zoneShapeFileNameColumn "id"\
	 --buildingsShapeFileName $(berlin)/input/shp/buildings_BerlinBrandenburg_4326.shp\
	 --shapeFileBuildingTypeColumn "type"\
	 --landuseShapeFileName $(berlin)/input/shp/berlinBrandenburg_landuse_4326.shp\
	 --shapeFileLanduseTypeColumn "fclass"\
	 --shapeCRS "EPSG:4326"\
	 --pathToInvestigationAreaData input/commercialTrafficAreaData.csv

$p/berlin-small-scale-commercialTraffic-$V-25pct.plans.xml.gz: $p/berlin-$V-network.xml.gz $p/commercialFacilities.xml.gz
	$(sc) prepare generate-small-scale-commercial-traffic\
	  input/$V/berlin-$V.config.xml\
	 --pathToDataDistributionToZones $p/dataDistributionPerZone.csv\
	 --pathToCommercialFacilities $(notdir $(word 2,$^))\
	 --sample 0.25\
	 --jspritIterations 10\
	 --creationOption createNewCarrierFile\
	 --network $(notdir $<)\
	 --smallScaleCommercialTrafficType completeSmallScaleCommercialTraffic\
	 --zoneShapeFileName $(berlin)/input/shp/berlinBrandenburg_Zones_VKZ_4326.shp\
	 --zoneShapeFileNameColumn "id"\
	 --shapeCRS "EPSG:4326"\
	 --numberOfPlanVariantsPerAgent 5\
	 --nameOutputPopulation $(notdir $@)\
	 --pathOutput output/commercialPersonTraffic

	mv output/commercialPersonTraffic/$(notdir $@) $@


$p/berlin-cadyts-input-$V-25pct.plans.xml.gz: $p/berlin-initial-$V-25pct.plans.xml.gz $p/berlin-small-scale-commercialTraffic-$V-25pct.plans.xml.gz
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
     --counts $p/berlin-$V-counts-vmz.xml.gz\
	 --output $p/berlin-$V-25pct.plans_selection_$(ERROR_METRIC).csv\
	 --metric $(ERROR_METRIC)

	$(sc) prepare select-plans-idx\
 	 --input $p/berlin-cadyts-input-$V-25pct.plans.xml.gz\
 	 --csv $p/berlin-$V-25pct.plans_selection_$(ERROR_METRIC).csv\
 	 --output $p/berlin-$V-25pct.plans_$(ERROR_METRIC).xml.gz

	$(sc) run --mode "routeChoice" --iterations 20 --all-car --output "output/eval-$(ERROR_METRIC)" --25pct --population "berlin-$V-25pct.plans_$(ERROR_METRIC).xml.gz"\
	 --config $p/berlin-$V.config.xml

$p/berlin-$V-25pct.plans_cadyts.xml.gz:
	$(sc) prepare extract-plans-idx\
	 --input output/cadyts/cadyts.output_plans.xml.gz\
	 --output $p/berlin-$V-25pct.plans_selection_cadyts.csv

	$(sc) prepare select-plans-idx\
	 --input $p/berlin-cadyts-input-$V-25pct.plans.xml.gz\
	 --csv $p/berlin-$V-25pct.plans_selection_cadyts.csv\
	 --output $@

# These depend on the output of optimization runs
$p/berlin-$V-25pct.plans-initial.xml.gz: $p/berlin-$V-facilities.xml.gz $p/berlin-$V-network.xml.gz $p/berlin-longHaulFreight-$V-25pct.plans.xml.gz
	$(sc) prepare scenario-cutout\
	 --population $p/berlin-$V-25pct.plans_cadyts.xml.gz\
	 --facilities $<\
	 --network $(word 2,$^)\
	 --output-population $@\
	 --output-network $p/network-cutout.xml.gz\
	 --output-facilities $p/facilities-cutout.xml.gz\
	 --input-crs $(CRS)\
	 --shp input/$V/area/area.shp

	$(sc) prepare split-activity-types-duration\
 	 --exclude commercial_start,commercial_end,freight_start,freight_end\
	 --input $@ --output $@

	$(sc) prepare set-car-avail --input $@ --output $@

	$(sc) prepare check-car-avail --input $@ --output $@ --mode walk

	$(sc) prepare fix-subtour-modes --input $@ --output $@ --coord-dist 100

	$(sc) prepare merge-populations $@ $(word 3,$^)\
		--output $@

	$(sc) prepare downsample-population $@\
		 --sample-size 0.25\
		 --samples 0.1 0.03 0.01 0.001\

$p/berlin-$V-10pct.plans.xml.gz:
	$(sc) prepare clean-population\
	 --plans mode-choice-10pct-baseline/runs/008/008.output_plans.xml.gz\
	 --remove-unselected-plans\
	 --output $@

	$(sc) prepare downsample-population $@\
		--sample-size 0.1\
		--samples 0.01 0.001\

	$(sc) prepare clean-population\
	 	--plans choice-experiments/baseline/runs/008/008.output_plans.xml.gz\
	 	--remove-unselected-plans\
	 	--output $(subst 10pct,3pct,$@)

$p/inner-city/berlin-downtown-$V-3pct.xml.gz:

	mkdir -p $p/inner-city

	$(sc) prepare scenario-cutout\
	 --population $p/berlin-$V-3pct.plans.xml.gz\
	 --facilities $p/berlin-$V-facilities.xml.gz\
	 --network $p/berlin-$V-network.xml.gz\
	 --output-population $@\
	 --output-network $p/inner-city/berlin-downtown-$V-network.xml.gz\
	 --output-facilities $p/inner-city/berlin-downtown-$V-facilities.xml.gz\
	 --input-crs $(CRS)\
	 --shp "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/shp/berlin_inner_city.gpkg"

$p/berlin-$V.drt-by-rndLocations-10000vehicles-4seats.xml.gz: $p/berlin-$V-network.xml.gz
	$(sc) prepare create-drt-vehicles\
	 --network $<\
	 --shp "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-$V/input/shp/Berlin_25832.shp"\
	 --output $p/berlin-$V.\
	 --vehicles 10000\
	 --seats 4

	$(sc) prepare create-drt-vehicles\
	 --network $<\
	 --shp "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-$V/input/shp/berlin_inner_city.gpkg"\
	 --output $p/berlin-$V.\
	 --vehicles 500\
	 --seats 4


prepare-calibration: $p/berlin-activities-$V-25pct.plans.xml.gz $p/berlin-$V-network-with-pt.xml.gz $p/berlin-$V-counts-vmz.xml.gz
	echo "Done"

prepare-initial: $p/berlin-$V-25pct.plans-initial.xml.gz $p/berlin-$V-network-with-pt.xml.gz
	echo "Done"

prepare-drt: $p/berlin-$V.drt-by-rndLocations-10000vehicles-4seats.xml.gz
	echo "Done"

prepare: $p/berlin-$V-10pct.plans.xml.gz
	echo "Done"