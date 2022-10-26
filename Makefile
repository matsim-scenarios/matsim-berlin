

JAR := matsim-berlin-*.jar
V := v6.0
CRS := EPSG:25832

p := scenarios/synthetic-$V

REGIONS := brandenburg
SHP_FILES=$(patsubst %, scenarios/shp/%-latest-free.shp.zip, $(REGIONS))

.PHONY: synthetic

${SHP_FILES}:
	mkdir -p scenarios/shp
	curl https://download.geofabrik.de/europe/germany/$(@:scenarios/shp/%=%) -o $@

$p/input/landuse.shp: ${SHP_FILES}
	mkdir -p scenarios/input/landuse
	java -Xmx20G -jar $(JAR) prepare create-landuse-shp $^\
	 --target-crs ${CRS}\
	 --output $@

$p/input/PLR_2013_2020.csv:
	curl https://instantatlas.statistik-berlin-brandenburg.de/instantatlas/interaktivekarten/kommunalatlas/Kommunalatlas.zip --insecure -o tmp.zip
	unzip tmp.zip -d $p/input
	rm tmp.zip

$p/input/Planungsraum_EPSG_25833.shp:
	curl https://www.stadtentwicklung.berlin.de/planen/basisdaten_stadtentwicklung/lor/download/LOR_SHP_EPSG_25833.zip -o tmp.zip
	unzip tmp.zip -d $p/input
	rm tmp.zip


synthetic: $p/input/PLR_2013_2020.csv $p/input/Planungsraum_EPSG_25833.shp $p/input/landuse.shp
	java -jar $(JAR) prepare create-population\
		--input $<\
		--shp $(word 2,$^) --shp-crs EPSG:25833\
		--landuse $(word 3,$^) --landuse-filter residential\
		--output test.population.xml.gz
