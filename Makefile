
JAR := matsim-leipzig-*.jar
V := v1.2
CRS := EPSG:25832
MEMORY ?= 20G

export SUMO_HOME ?= $(abspath ../../sumo-1.8.0/)
osmosis := osmosis/bin/osmosis

NETWORK := germany-220327.osm.pbf
germany := ../shared-svn/projects/matsim-germany
shared := ../shared-svn/projects/NaMAV

.PHONY: prepare

# Scenario creation tool
sc := java -Xmx$(MEMORY) -jar $(JAR)

$(JAR):
	mvn package


# Required files
$(germany)/maps/$(NETWORK):
	curl https://download.geofabrik.de/europe/$(NETWORK)\
	  -o scenarios/input/network.osm.pbf

input/gtfs-lvb.zip:
	curl https://opendata.leipzig.de/dataset/8803f612-2ce1-4643-82d1-213434889200/resource/b38955c4-431c-4e8b-a4ef-9964a3a2c95d/download/gtfsmdvlvb.zip\
	  -o $@

input/network.osm: $(germany)/maps/$(NETWORK)

	$(osmosis) --rb file=$<\
	 --tf accept-ways bicycle=yes highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,unclassified,living_street\
	 --bounding-box top=51.457 left=12.137 bottom=51.168 right=12.703\
	 --used-node --wb network-detailed.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction\
	 --bounding-box top=51.92 left=11.45 bottom=50.83 right=13.36\
	 --used-node --wb network-coarse.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,motorway_junction,trunk,trunk_link,primary,primary_link\
	 --used-node --wb network-germany.osm.pbf

	$(osmosis) --rb file=network-germany.osm.pbf --rb file=network-coarse.osm.pbf --rb file=network-detailed.osm.pbf\
  	 --merge --merge\
  	 --tag-transform file=input/remove-railway.xml\
  	 --wx $@

	rm network-detailed.osm.pbf
	rm network-coarse.osm.pbf
	rm network-germany.osm.pbf


input/sumo.net.xml: input/network.osm

	$(SUMO_HOME)/bin/netconvert --geometry.remove --ramps.guess --ramps.no-split\
	 --type-files $(SUMO_HOME)/data/typemap/osmNetconvert.typ.xml,$(SUMO_HOME)/data/typemap/osmNetconvertUrbanDe.typ.xml\
	 --tls.guess-signals true --tls.discard-simple --tls.join --tls.default-type actuated\
	 --junctions.join --junctions.corner-detail 5\
	 --roundabouts.guess --remove-edges.isolated\
	 --no-internal-links --keep-edges.by-vclass passenger,bicycle --remove-edges.by-type highway.track,highway.services,highway.unsurfaced\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian\
	 --output.original-names --output.street-names\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --osm-files $< -o=$@


input/$V/leipzig-$V-network.xml.gz: input/sumo.net.xml
	$(sc) prepare network-from-sumo $< --output $@
	$(sc) prepare fix-network $@ --output $@
	$(sc) prepare clean-network $@ --output $@ --modes bike

input/$V/leipzig-$V-network-with-pt.xml.gz: input/$V/leipzig-$V-network.xml.gz input/gtfs-lvb.zip
	$(sc) prepare transit-from-gtfs --network $< $(filter-out $<,$^)\
	 --name leipzig-$V --date "2019-06-05" --target-crs $(CRS)\
	 --output input/$V

	$(sc) prepare prepare-transit-schedule\
	 --input input/$V/leipzig-$V-transitSchedule.xml.gz\
	 --output input/$V/leipzig-$V-transitSchedule.xml.gz\
	 --shp $(shared)/data/shapefiles/leipzig-utm32n/leipzig-utm32n.shp

input/plans-longHaulFreight.xml.gz: input/$V/leipzig-$V-network.xml.gz
	$(sc) prepare extract-freight-trips ../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz\
	 --network ../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz\
	 --input-crs $(CRS)\
	 --target-crs $(CRS)\
	 --shp ../shared-svn/projects/NaMAV/data/shapefiles/freight-area/freight-area.shp\
	 --output $@

input/plans-commercialTraffic.xml.gz:
	$(sc) prepare generate-small-scale-commercial-traffic\
	  input/commercialTraffic\
	 --sample 0.25\
	 --jspritIterations 1\
	 --creationOption createNewCarrierFile\
	 --landuseConfiguration useOSMBuildingsAndLanduse\
	 --trafficType commercialTraffic\
	 --zoneShapeFileName $(berlin)/data/input-commercialTraffic/leipzig_zones_25832.shp\
	 --buildingsShapeFileName $(berlin)/data/input-commercialTraffic/leipzig_buildings_25832.shp\
	 --landuseShapeFileName $(berlin)/data/input-commercialTraffic/leipzig_landuse_25832.shp\
	 --shapeCRS "EPSG:25832"\
	 --resistanceFactor "0.005"\
	 --nameOutputPopulation $(notdir $@)\
	 --PathOutput output/commercialTraffic

	mv output/commercialTraffic/$(notdir $@) $@

input/$V/leipzig-$V-25pct.plans-initial.xml.gz: input/plans-longHaulFreight.xml.gz input/plans-commercialTraffic.xml.gz
	$(sc) prepare trajectory-to-plans\
	 --name prepare --sample-size 0.25\
	 --max-typical-duration 0\
	 --output input/\
	 --population $(shared)/matsim-input-files/senozon/20210520_leipzig/population.xml.gz\
	 --attributes $(shared)/matsim-input-files/senozon/20210520_leipzig/personAttributes.xml.gz

	$(sc) prepare population input/prepare-25pct.plans.xml.gz\
	 --output input/prepare-25pct.plans.xml.gz

	$(sc) prepare resolve-grid-coords input/prepare-25pct.plans.xml.gz\
	 --input-crs $(CRS)\
	 --grid-resolution 300\
	 --landuse $(germany)/landuse/landuse.shp\
	 --output input/prepare-25pct.plans.xml.gz

	$(sc) prepare generate-short-distance-trips\
 	 --population input/prepare-25pct.plans.xml.gz\
 	 --input-crs $(CRS)\
 	 --shp $(shared)/data/shapefiles/leipzig-utm32n/leipzig-utm32n.shp --shp-crs $(CRS)\
 	 --num-trips 67395

	$(sc) prepare adjust-activity-to-link-distances input/prepare-25pct.plans-with-trips.xml.gz\
	 --shp $(shared)/data/shapefiles/leipzig-utm32n/leipzig-utm32n.shp --shp-crs $(CRS)\
	 --scale 1.15\
	 --input-crs $(CRS)\
	 --network input/$V/leipzig-$V-network.xml.gz\
	 --output input/prepare-25pct.plans-adj.xml.gz

	$(sc) prepare split-activity-types-duration\
		--input input/prepare-25pct.plans-with-trips.xml.gz\
		--output $@

	$(sc) prepare fix-subtour-modes --input $@ --coord-dist 100 --output $@

	$(sc) prepare merge-populations $@ $< --output $@

	$(sc) prepare extract-home-coordinates $@ --csv input/$V/leipzig-$V-homes.csv

	$(sc) prepare downsample-population $@\
    	 --sample-size 0.25\
    	 --samples 0.1 0.01\

counts:
	java -cp $(JAR) org.matsim.run.prepare.CreatingCountsFromZaehldaten\
		--network input/leipzig-$V-network.xml.gz\
		--excel $(shared)/NaMAV/data/Zaehldaten/Zaehldaten.xlsx\
		-i input/ignored_counts.csv -m input/manuallyMatsimLinkShift.csv\
		--output input/$V/leipzig-$V-counts

check: input/$V/leipzig-$V-25pct.plans-initial.xml.gz
	$(sc) analysis check-population $<\
 	 --input-crs $(CRS)\
 	 --attribute carAvail\
 	 --shp ../shared-svn/projects/NaMAV/data/leipzig-utm32n/leipzig-utm32n.shp\

# Aggregated target
prepare: input/$V/leipzig-$V-25pct.plans-initial.xml.gz input/$V/leipzig-$V-network-with-pt.xml.gz
	echo "Done"