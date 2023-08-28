package org.matsim.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.matsim.application.ApplicationUtils.globFile;

/**
 * Creates a tsv file donating the location of parking activities.
 * Can be used for visualizations.
 */
public class ParkingLocation implements MATSimAppCommand {

	@CommandLine.Option(names = "--directory", description = "path to matsim output directory", required = true)
	private Path directory;

	public static void main(String[] args) {
		new ParkingLocation().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Path eventsPath = globFile(directory, "*output_events.*");
		Path networkPath = globFile(directory, "*output_network.*");
		Path popPath = globFile(directory, "*output_plans.*");
		EventsManager manager = EventsUtils.createEventsManager();
		Network network = NetworkUtils.readNetwork(String.valueOf(networkPath));
		List<ParkingData> listOfParkingActivities = new ArrayList<>();
		List<Id<Person>> listOfRelevantPersons = new ArrayList<>();
		manager.addHandler(new ParkingActivities(listOfParkingActivities, network, listOfRelevantPersons));
		manager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(manager);
		matsimEventsReader.readFile(eventsPath.toString());
		manager.finishProcessing();
		writeResults(directory, listOfParkingActivities);

		if (listOfRelevantPersons.size() > 0) {
			Population population = PopulationUtils.readPopulation(String.valueOf(popPath));
			Population reducedPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			for (Person p : population.getPersons().values()) {
				if (listOfRelevantPersons.contains(p.getId())) {
					reducedPop.addPerson(p);
				}
			}
			PopulationUtils.writePopulation(reducedPop, directory.resolve("reducedPlans.xml").toString());
		}

		return null;
	}

	private static void writeResults(Path outputFolder, List<ParkingData> listOfParkingData) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder.resolve("parkingActivities.tsv").toString());
		writer.write("personId\tx\ty\tlinkId");
		writer.newLine();
		for (ParkingData pd : listOfParkingData) {
			writer.write(pd.personId + "\t" + pd.coord.getX() + "\t" + pd.coord.getY() + "\t" + pd.linkId);
			writer.newLine();
		}
		writer.close();

		Map<ParkingData, Integer> duplicateCountMap = listOfParkingData
				.stream()
				.collect(
						Collectors.toMap(Function.identity(), company -> 1, Math::addExact)
				);

		BufferedWriter writerWithCounts = IOUtils.getBufferedWriter(outputFolder.resolve("parkingActivitiesWithCount.tsv").toString());

		writerWithCounts.write(("x\ty\tlinkId\tcount"));
		writerWithCounts.newLine();
		for (ParkingData parkingData : duplicateCountMap.keySet()) {
			writerWithCounts.write(parkingData.coord.getX() + "\t" + parkingData.coord.getY() + "\t"
					+ parkingData.linkId + "\t" + duplicateCountMap.get(parkingData));
			writerWithCounts.newLine();
		}
		writerWithCounts.close();
	}

	private record ParkingActivities(List<ParkingData> listOfParkingData, Network network, List<Id<Person>> listOfRelevantPersons) implements ActivityStartEventHandler {

		@Override
		public void handleEvent(ActivityStartEvent activityStartEvent) {
			if (activityStartEvent.getActType().equals("parking interaction")) {
				Link l = network.getLinks().get(activityStartEvent.getLinkId());
				ParkingData pd = new ParkingData(activityStartEvent.getPersonId(), l.getCoord(), activityStartEvent.getLinkId());
				listOfParkingData.add(pd);

				if (l.getAttributes().getAttribute("linkParkingType") != null) {
					listOfRelevantPersons.add(activityStartEvent.getPersonId());
				}
			}
		}
	}

	private record ParkingData(Id<Person> personId, Coord coord, Id<Link> linkId) {
	}
}
