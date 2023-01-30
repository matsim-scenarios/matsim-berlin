

JAR := matsim-berlin-*.jar
V := v6.0
CRS := EPSG:25832

p := input/$V

REGIONS := brandenburg
SHP_FILES=$(patsubst %, scenarios/shp/%-latest-free.shp.zip, $(REGIONS))

.PHONY: prepare

${SHP_FILES}:
	mkdir -p scenarios/shp
	curl https://download.geofabrik.de/europe/germany/$(@:scenarios/shp/%=%) -o $@

input/landuse.shp: ${SHP_FILES}
	mkdir -p scenarios/input/landuse
	java -Xmx20G -jar $(JAR) prepare create-landuse-shp $^\
	 --target-crs ${CRS}\
	 --output $@

input/PLR_2013_2020.csv:
	curl https://instantatlas.statistik-berlin-brandenburg.de/instantatlas/interaktivekarten/kommunalatlas/Kommunalatlas.zip --insecure -o atlas.zip
	unzip atlas.zip -d input
	rm atlas.zip

input/Planungsraum_EPSG_25833.shp:
	curl https://www.stadtentwicklung.berlin.de/planen/basisdaten_stadtentwicklung/lor/download/LOR_SHP_EPSG_25833.zip -o tmp.zip
	unzip tmp.zip -d input
	rm tmp.zip


$p/berlin-only-$V-25pct.plans.xml.gz: input/PLR_2013_2020.csv input/Planungsraum_EPSG_25833.shp input/landuse.shp
	java -jar $(JAR) prepare create-population\
		--input $<\
		--shp $(word 2,$^) --shp-crs EPSG:25833\
		--landuse $(word 3,$^) --landuse-filter residential\
		--output $@

prepare: $p/berlin-only-$V-25pct.plans.xml.gz
	echo "Done"