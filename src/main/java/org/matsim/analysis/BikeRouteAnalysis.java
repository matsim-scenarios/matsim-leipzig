package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyse bike routes.
 */
public final class BikeRouteAnalysis {

	private static final Logger log = LogManager.getLogger(BikeRouteAnalysis.class);

	private final Map<Id<Person>, List<PlanElement>> consideredPersonsWithCar = new HashMap<>();
	private final Map<Id<Person>, List<PlanElement>> consideredPersonsCarless = new HashMap<>();

	private final Geometry consideredArea;

	public BikeRouteAnalysis(Geometry consideredArea) {
		this.consideredArea = consideredArea;
	}

	public static void main(String[] args) {

		String inputPopWithCar = "C:/Users/Simon/Documents/VSP-Projects/matsim-leipzig/output/it-1pct/ITERS/it.0/leipzig-25pct.0.plans.xml.gz";
		String inputPopCarless = "C:/Users/Simon/Documents/VSP-Projects/matsim-leipzig/output/it-1pct_carFree/ITERS/it.0/leipzig-25pct.0.plans.xml.gz";
		String consideredAreaShpFile = "C:/Users/Simon/Documents/shared-svn/projects/NaMAV/data/carFree-scenario/Leipzig_autofreie_Zonen_utm32n/Leipzig_autofreie_Zonen_Innenstadt_utm32n.shp";
		String outputFile = "C:/Users/Simon/Desktop/bikeLegsOnlyComparison.csv";

		ShapeFileReader shpReader = new ShapeFileReader();
		Collection<SimpleFeature> features = shpReader.readFileAndInitialize(consideredAreaShpFile);

		Geometry consideredArea = null;

		for (SimpleFeature feature : features) {
			if (consideredArea == null) {
				consideredArea = (Geometry) feature.getDefaultGeometry();
			} else {
				consideredArea = consideredArea.union((Geometry) feature.getDefaultGeometry());
			}
		}

		Population popWithCar = PopulationUtils.readPopulation(inputPopWithCar);
		Population popCarless = PopulationUtils.readPopulation(inputPopCarless);

		BikeRouteAnalysis bikeRouteAnalysis = new BikeRouteAnalysis(consideredArea);
		bikeRouteAnalysis.analyzeBikeRoutes(popWithCar, popCarless);
		bikeRouteAnalysis.writeData(outputFile);
	}

	void analyzeBikeRoutes(Population pop1, Population pop2) {
		for (Person person : pop1.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();

			for (PlanElement element : selectedPlan.getPlanElements()) {
				if (!(element instanceof Activity)) {
					continue;
				}
				if (!((Activity) element).getType().equals("bike interaction")) {
					continue;
				}
				if (MGC.coord2Point(((Activity) element).getCoord()).within(consideredArea)) {
					this.consideredPersonsWithCar.put(person.getId(), selectedPlan.getPlanElements());
				}
			}
		}

		for (Id<Person> personId : consideredPersonsWithCar.keySet()) {
			consideredPersonsCarless.put(pop2.getPersons().get(personId).getId(),
					pop2.getPersons().get(personId).getSelectedPlan().getPlanElements());
		}
	}

	void writeData(String outputFile) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("PersonID;PlanElementWithCar;PlanElementCarless");
			writer.newLine();

			for (Id<Person> personId : this.consideredPersonsWithCar.keySet()) {
				for (PlanElement p : this.consideredPersonsWithCar.get(personId)) {
					if (p instanceof Leg) {
						if (((Leg) p).getMode().equals(TransportMode.bike)) {
							writer.write(personId + ";" + p + ";" +
									this.consideredPersonsCarless.get(personId).get(this.consideredPersonsWithCar.get(personId).indexOf(p)));
							writer.newLine();
						}
					}
				}
			}
			writer.close();
		} catch (IOException e) {
			log.error(e);
		}
	}
}
