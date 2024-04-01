package org.matsim.run.drt_post_simulation;

import org.apache.commons.collections.ArrayStack;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.matsim.application.ApplicationUtils.globFile;

public class DetourAnalysis {
	public static void main(String[] args) throws IOException {
		Path outputDirectory = Path.of(args[0]);
		DetourAnalysisEventsHandler detourAnalysis = new DetourAnalysisEventsHandler(outputDirectory);
		detourAnalysis.readEvents();
		detourAnalysis.writeAnalysis();
	}

	public static class DetourAnalysisEventsHandler implements PassengerRequestSubmittedEventHandler,
		PassengerRequestScheduledEventHandler, PassengerRequestRejectedEventHandler, PassengerPickedUpEventHandler,
		PassengerDroppedOffEventHandler {
		private final Path outputDirectory;
		private final Network network;
		private final TravelTime travelTime;
		private final LeastCostPathCalculator router;
		private final Map<Id<Person>, Double> submissionTimeMap = new LinkedHashMap<>();
		private final Map<Id<Person>, Double> directTripTimeMap = new HashMap<>();
		private final Map<Id<Person>, Double> scheduledPickupTimeMap = new HashMap<>();
		private final Map<Id<Person>, Double> actualPickupTimeMap = new HashMap<>();
		private final Map<Id<Person>, Double> arrivalTimeMap = new HashMap<>();
		private final List<Id<Person>> rejectedPersons = new ArrayList<>();
		private final Map<Id<Person>, Double> waitTimeMap = new HashMap<>();
		private final Map<Id<Person>, Double> euclideanDistanceMap = new HashMap<>();

		public DetourAnalysisEventsHandler(Path outputDirectory) {
			this.outputDirectory = outputDirectory;
			Path networkPath = globFile(outputDirectory, "*output_network.xml.gz*");
			this.network = NetworkUtils.readNetwork(networkPath.toString());
			this.travelTime = new QSimFreeSpeedTravelTime(1);
			this.router = new SpeedyALTFactory().createPathCalculator(network, new TimeAsTravelDisutility(travelTime), travelTime);
		}

		@Override
		public void handleEvent(PassengerRequestSubmittedEvent event) {
			double submissionTime = event.getTime();
			Link fromLink = network.getLinks().get(event.getFromLinkId());
			Link toLink = network.getLinks().get(event.getToLinkId());
			double directTripTime = VrpPaths.calcAndCreatePath
				(fromLink, toLink, submissionTime, router, travelTime).getTravelTime();
			double euclideanDistance = CoordUtils.calcEuclideanDistance(fromLink.getToNode().getCoord(), toLink.getToNode().getCoord());
			submissionTimeMap.put(event.getPersonIds().get(0), submissionTime);
			directTripTimeMap.put(event.getPersonIds().get(0), directTripTime);
			euclideanDistanceMap.put(event.getPersonIds().get(0), euclideanDistance);
		}

		@Override
		public void handleEvent(PassengerRequestScheduledEvent event) {
			double scheduledPickupTime = Math.ceil(event.getPickupTime());
			scheduledPickupTimeMap.put(event.getPersonIds().get(0), scheduledPickupTime);
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			rejectedPersons.add(event.getPersonIds().get(0));
		}

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			double actualPickupTime = event.getTime();
			actualPickupTimeMap.put(event.getPersonId(), actualPickupTime);
			double waitTime = actualPickupTime - submissionTimeMap.get(event.getPersonId());
			waitTimeMap.put(event.getPersonId(), waitTime);
		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			double arrivalTime = event.getTime();
			arrivalTimeMap.put(event.getPersonId(), arrivalTime);
		}

		@Override
		public void reset(int iteration) {
			PassengerRequestScheduledEventHandler.super.reset(iteration);
		}

		public void writeAnalysis() throws IOException {
			// Write information about simulated trips. (In the lists below, we only store completed trips)
			List<Double> directTripDurations = new ArrayList<>();
			List<Double> actualRideDurations = new ArrayList<>();
			List<Double> waitTimes = new ArrayList<>();
			List<Double> euclideanDistances = new ArrayList<>();

			{
				String detourAnalysisOutput = outputDirectory.toString() + "/simulated-drt-trips.tsv";
				CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(detourAnalysisOutput), CSVFormat.TDF);
				csvPrinter.printRecord(Arrays.asList("person_id", "submission", "scheduled_pickup", "actual_pickup",
					"arrival", "direct_trip_duration", "total_wait_time", "delay_since_scheduled_pickup",
					"actual_ride_duration", "total_travel_time", "euclidean_distance"));

				for (Id<Person> personId : submissionTimeMap.keySet()) {
					if (rejectedPersons.contains(personId)) {
						continue;
					}

					double submissionTime = submissionTimeMap.get(personId);
					double scheduledPickupTime = scheduledPickupTimeMap.get(personId);
					double actualPickupTime = actualPickupTimeMap.get(personId);
					double arrivalTime = arrivalTimeMap.get(personId);
					double directTripDuration = directTripTimeMap.get(personId);
					double euclideanDistance = euclideanDistanceMap.get(personId);

					double waitTime = actualPickupTime - submissionTime;
					double delay = actualPickupTime - scheduledPickupTime;
					double actualRideDuration = arrivalTime - actualPickupTime;
					double actualTotalTravelTime = arrivalTime - submissionTime;

					csvPrinter.printRecord(Arrays.asList(
						personId.toString(),
						Double.toString(submissionTime),
						Double.toString(scheduledPickupTime),
						Double.toString(actualPickupTime),
						Double.toString(arrivalTime),
						Double.toString(directTripDuration),
						Double.toString(waitTime),
						Double.toString(delay),
						Double.toString(actualRideDuration),
						Double.toString(actualTotalTravelTime),
						Double.toString(euclideanDistance)
					));

					actualRideDurations.add(actualRideDuration);
					directTripDurations.add(directTripDuration);
					waitTimes.add(waitTime);
					euclideanDistances.add(euclideanDistance);
				}
				csvPrinter.close();
			}

			// Write out summary data
			{
				String tripsSummary = outputDirectory + "/simulated-drt-summary.tsv";
				CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(tripsSummary), CSVFormat.TDF);
				csvPrinter.printRecord(Arrays.asList("direct_trip_duration", "actual_ride_duration", "wait_time",
					"total_travel_time", "euclidean_distance"));

				double meanDirectTripDuration = directTripDurations.stream().mapToDouble(v -> v).average().orElseThrow();
				double meanActualRideDuration = actualRideDurations.stream().mapToDouble(v -> v).average().orElseThrow();
				double meanWaitTime = waitTimes.stream().mapToDouble(v -> v).average().orElseThrow();
				double meanTotalTravelTime = meanActualRideDuration + meanWaitTime;
				double meanEuclideanDistance = euclideanDistances.stream().mapToDouble(v -> v).average().orElseThrow();

				csvPrinter.printRecord(Arrays.asList(
					Double.toString(meanDirectTripDuration),
					Double.toString(meanActualRideDuration),
					Double.toString(meanWaitTime),
					Double.toString(meanTotalTravelTime),
					Double.toString(meanEuclideanDistance)
				));
				csvPrinter.close();
			}
		}

		public List<Id<Person>> getRejectedPersons() {
			return rejectedPersons;
		}

		public void readEvents() {
			Path outputEventsPath = globFile(outputDirectory, "*output_events.xml.gz*");
			EventsManager eventManager = EventsUtils.createEventsManager();
			eventManager.addHandler(this);
			eventManager.initProcessing();
			MatsimEventsReader matsimEventsReader = DrtEventsReaders.createEventsReader(eventManager);
			matsimEventsReader.readFile(outputEventsPath.toString());
		}

		public double get95pctWaitTime() {
			List<Double> waitingTimes = waitTimeMap.values().stream().sorted().toList();
			int idx = (int) Math.min(Math.ceil(waitingTimes.size() * 0.95), waitingTimes.size() - 1);
			return waitingTimes.get(idx);
		}
	}
}
