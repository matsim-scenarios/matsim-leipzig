package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;
import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.application.ApplicationUtils.globFile;

/**
 *Analysis class for printing a distribution of age groups in a given population.
 * The analysis can be performed for agents who live in a given shp (optional).
 */
public class AgeDistributionAnalysis implements MATSimAppCommand {

	@CommandLine.Option(names = "--directory", description = "path to the directory of the simulation output", required = true)
	private Path directory;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	private static final Logger log = LogManager.getLogger(AgeDistributionAnalysis.class);

	private Integer ageGroupBelow18 = 0;
	private Integer ageGroupBetween18And70 = 0;
	private Integer ageGroupGreater70 = 0;

	public static void main(String[] args) {
		new AgeDistributionAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Path outputPlansFilePath = globFile(directory, "*output_plans.*");
		Path outputFolder = Path.of(directory.toString() + "/analysis/analysis-age-distribution");

		if (!Files.exists(outputFolder)) {
			Files.createDirectory(outputFolder);
		}

		Geometry geometry = null;
		Map<Id<Person>, String> persons = new HashMap<>();

		if (shp.isDefined()) {
			geometry = shp.getGeometry();
		}

		Population population = PopulationUtils.readPopulation(outputPlansFilePath.toString());

		for (Person person : population.getPersons().values()) {
			for (PlanElement el : person.getSelectedPlan().getPlanElements()) {
				if (el instanceof Activity) {
					if (((Activity) el).getType().contains(ActivityTypes.HOME)) {
						if (shp.isDefined()) {
							boolean linkInShp = MGC.coord2Point(((Activity) el).getCoord()).within(geometry);

							if (linkInShp) {
								persons.put(person.getId(), person.getAttributes().getAttribute("age").toString());
							}
						} else {
							persons.put(person.getId(), person.getAttributes().getAttribute("age").toString());
						}
						continue;
					}
				}
			}
		}

		for (String age : persons.values()) {
			if (Integer.parseInt(age) < 18) {
				ageGroupBelow18++;
			} else if (Integer.parseInt(age) >= 18 && Integer.parseInt(age) < 70) {
				ageGroupBetween18And70++;
			} else if (Integer.parseInt(age) >= 70) {
				ageGroupGreater70++;
			}
		}

		writeOutput(outputFolder + "/age-distribution.csv");



		return 0;
	}

	private void writeOutput(String outputFile) {

		try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.TDF)){


			List<String> header = new ArrayList<>();
			header.add("<=18");
			header.add("18-70");
			header.add(">70");
			header.add("dataset");

			List<String> data = new ArrayList<>();
			data.add(ageGroupBelow18.toString());
			data.add(ageGroupBetween18And70.toString());
			data.add(ageGroupGreater70.toString());
			data.add("simulationOutput");

			printer.printRecord(header);
			printer.printRecord(data);

		} catch (IOException e) {
			log.error("Writing of analysis output not successful!");
		}
	}
}
