

JAR := matsim-berlin-*.jar
V := v6.0
CRS := EPSG:25832

p := input/$V
germany := ../shared-svn/projects/matsim-germany/
berlin := ../public-svn/matsim/scenarios/countries/de/berlin/berlin-$V

REGIONS := brandenburg
SHP_FILES=$(patsubst %, input/shp/%-latest-free.shp.zip, $(REGIONS))

osmosis := osmosis/bin/osmosis

.PHONY: prepare

${SHP_FILES}:
	mkdir -p input/shp
	curl https://download.geofabrik.de/europe/germany/$(@:input/shp/%=%) -o $@

input/brandenburg.osm.pbf:
	curl https://download.geofabrik.de/europe/germany/brandenburg-230101.osm.pbf -o $@

input/brandenburg_facilities.osm.pbf: input/brandenburg.osm.pbf
	$(osmosis) --rb file=$<\
	 --tf accept-ways landuse=* building=* shop=* office=* sport=* amenity=* leisure=* tourism=* industrial=*\
     --tf reject-relations\
	 --used-node --wb $@

input/brandenburg_epsg_25832.geojson: input/brandenburg_facilities.osm.pbf
	osmox run input/facilities.json $< brandenburg -crs epsg:25832

$(germany)/RegioStaR-Referenzdateien.xlsx:
	curl https://mcloud.de/downloads/mcloud/536149D1-2902-4975-9F7D-253191C0AD07/RegioStaR-Referenzdateien.xlsx -o $@

input/landuse.shp: ${SHP_FILES}
	mkdir -p input/landuse
	java -Xmx20G -jar $(JAR) prepare create-landuse-shp $^\
	 --target-crs ${CRS}\
	 --output $@

input/PLR_2013_2020.csv:
	curl https://instantatlas.statistik-berlin-brandenburg.de/instantatlas/interaktivekarten/kommunalatlas/Kommunalatlas.zip --insecure -o atlas.zip
	unzip atlas.zip -d input
	rm atlas.zip

$(berlin)/input/shp/Planungsraum_EPSG_25833.shp:
	curl https://www.stadtentwicklung.berlin.de/planen/basisdaten_stadtentwicklung/lor/download/LOR_SHP_EPSG_25833.zip -o tmp.zip
	unzip tmp.zip -d $(berlin)/input
	rm tmp.zip


$p/berlin-only-$V-25pct.plans.xml.gz: input/PLR_2013_2020.csv $(berlin)/input/shp/Planungsraum_EPSG_25833.shp input/landuse.shp
	java -jar $(JAR) prepare berlin-population\
		--input $<\
		--shp $(word 2,$^) --shp-crs EPSG:25833\
		--landuse $(word 3,$^) --landuse-filter residential\
		--output $@

$p/brandeburg-only-$V-25pct.plans.xml.gz: input/landuse.shp
	java -jar $(JAR) prepare brandenburg-population\
	 --shp $(germany)/vg5000/vg5000_ebenen_0101/VG5000_GEM.shp\
	 --population $(germany)/regionalstatistik/population.csv\
 	 --landuse $< --landuse-filter residential\
 	 --output $@

$p/berlin-$V-25pct.plans.xml.gz: $p/berlin-only-$V-25pct.plans.xml.gz $p/brandeburg-only-$V-25pct.plans.xml.gz
	java -jar $(JAR) prepare merge-populations $^\
	 --output $@

	java -jar $(JAR) prepare lookup-regiostar --input $@ --output $@ --xls $(germany)/RegioStaR-Referenzdateien.xlsx


prepare: $p/berlin-$V-25pct.plans.xml.gz
	echo "Done"