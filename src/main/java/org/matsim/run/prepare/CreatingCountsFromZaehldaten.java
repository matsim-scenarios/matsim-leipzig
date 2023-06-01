package org.matsim.run.prepare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@CommandLine.Command(
		name = "createLeipzigCounts",
		description = "Create vehicle counts from Leipzig count data"
)
public class CreatingCountsFromZaehldaten implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreatingCountsFromZaehldaten.class);

	private static final Logger logger = LogManager.getLogger(CreatingCountsFromZaehldaten.class);

	private static final CoordinateTransformation CT = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:25832");

	@CommandLine.Option(names = {"-e", "--excel"}, description = "Input count excel file", required = true)
	private Path excel;

	@CommandLine.Option(names = {"-n", "--network"}, description = "Input network file", required = true)
	private Path network;

	@CommandLine.Option(names = {"-o", "--output"}, description = "Output count file", required = true)
	private Path count;

	@CommandLine.Option(names = {"-i", "--ignoredCountsFile"}, description = "Ignored counts file", required = true)
	private Path ignoredCount;

	@CommandLine.Option(names = {"-m", "--manuallyMatsimLinkShift"}, description = "new manually matsim links shift file", required = true)
	private Path manuallyMatsimLinkShift;

	public static void main(String[] args) {
		new CreatingCountsFromZaehldaten().execute(args);
	}

	@Override
	public Integer call() {

		List<LeipzigCounts> leipzigCountsList = new ArrayList<>();

		try (XSSFWorkbook wb = new XSSFWorkbook(excel.toFile())) {
			Sheet sheet = wb.getSheetAt(0);
			handleSheet(sheet, leipzigCountsList);
		} catch (IOException | InvalidFormatException e) {
			log.error(e);
		}

		creatingCounts(leipzigCountsList);

		return null;
	}

	/**
	 * Reads in the given excel sheet and creates a list for with all the information from that file.
	 *
	 * @param sheet given excel sheet from the hamburg government with the count data
	 * @param list  a list where the count data from the government is saved to work with
	 */
	@SuppressWarnings("IllegalCatch")
	private static void handleSheet(Sheet sheet, List<LeipzigCounts> list) {
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			try {
				LeipzigCounts leipzigCounts = new LeipzigCounts();
				leipzigCounts.routeNr = (int) sheet.getRow(i).getCell(0).getNumericCellValue();
				leipzigCounts.startNodeID = (int) sheet.getRow(i).getCell(1).getNumericCellValue();
				leipzigCounts.endNodeID = (int) sheet.getRow(i).getCell(2).getNumericCellValue();
				if (sheet.getRow(i).getCell(3) != null && sheet.getRow(i).getCell(4) != null) {
					leipzigCounts.startName = sheet.getRow(i).getCell(3).getStringCellValue();
					leipzigCounts.endName = sheet.getRow(i).getCell(4).getStringCellValue();
				}
				leipzigCounts.year = (int) sheet.getRow(i).getCell(5).getNumericCellValue();
				leipzigCounts.kfz = (int) sheet.getRow(i).getCell(6).getNumericCellValue();
				leipzigCounts.lkw = (int) sheet.getRow(i).getCell(7).getNumericCellValue();
				leipzigCounts.startNodeCoordX = sheet.getRow(i).getCell(10).getNumericCellValue();
				leipzigCounts.startNodeCoordY = sheet.getRow(i).getCell(11).getNumericCellValue();
				leipzigCounts.endNodeCoordX = sheet.getRow(i).getCell(12).getNumericCellValue();
				leipzigCounts.endNodeCoordY = sheet.getRow(i).getCell(13).getNumericCellValue();
				if (leipzigCounts.year > 2017) {
					list.add(leipzigCounts);
				}
			} catch (Exception e) {
				logger.error("Error when reading excel data", e);
			}
		}
	}

	/**
	 * Creates the count file for matsim-simulations.
	 *
	 * @param leipzigCountsList the list with the information from the excel file
	 */
	private void creatingCounts(List<LeipzigCounts> leipzigCountsList) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(network.toString());
		Network network = scenario.getNetwork();
		LeastCostPathCalculator router = generateRouter(network);
		Map<String, Link> map = new HashMap<>();
		ArrayList<LeipzigCounts> countList = new ArrayList<>();

		Counts<Link> countsPkw = new Counts<>();
		countsPkw.setYear(2018);
		countsPkw.setDescription("data from leipzig to matsim counts");
		Counts<Link> countsLkw = new Counts<>();
		countsLkw.setYear(2018);
		countsLkw.setDescription("data from leipzig to matsim counts");

		List<String> ignoredCounts = readIgnoredCountFile(ignoredCount);
		Map<String, String> shiftLinks = readNewManuallyMastimLinkShift(manuallyMatsimLinkShift);

		for (LeipzigCounts leipzigCounts : leipzigCountsList) {
			Coord fromCoordOldSys = new Coord(leipzigCounts.startNodeCoordX, leipzigCounts.startNodeCoordY);
			Coord toCoordOldSys = new Coord(leipzigCounts.endNodeCoordX, leipzigCounts.endNodeCoordY);
			Coord fromCoord = CT.transform(fromCoordOldSys);
			Coord toCoord = CT.transform(toCoordOldSys);
			Node fromNode = NetworkUtils.getNearestNode(network, fromCoord);
			Node toNode = NetworkUtils.getNearestNode(network, toCoord);
			LeastCostPathCalculator.Path route = router.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
			String leipzigName = leipzigCounts.startNodeID + "_" + leipzigCounts.endNodeID;
			Link countLink = null;

			if (shiftLinks.get(leipzigName) != null) {
				countLink = network.getLinks().get(Id.createLinkId(shiftLinks.get(leipzigName)));
				countList.add(leipzigCounts);
				map.put(leipzigCounts.routeNr + "_" + leipzigName, countLink);
				fillingCounts(leipzigCounts, requireNonNull(countLink), countsPkw, countsLkw);
			} else if (route.links.size() > 0 && !(ignoredCounts.contains(leipzigName))) {
				for (Link link : route.links) {
					if (countLink == null) {
						countLink = link;
					} else {
						if (countLink.getLength() < link.getLength()) {
							countLink = link;
						}
					}
				}
				if (countLink != null) {
					if (leipzigCounts.endName == null || leipzigCounts.startName == null) {
						if (calculateAngle(leipzigCounts, countLink)) {
							countList.add(leipzigCounts);
							map.put(leipzigCounts.routeNr + "_" + leipzigName, countLink);
							fillingCounts(leipzigCounts, requireNonNull(countLink), countsPkw, countsLkw);
						}
					} else {
						countList.add(leipzigCounts);
						map.put(leipzigCounts.routeNr + "_" + leipzigName, countLink);
						fillingCounts(leipzigCounts, requireNonNull(countLink), countsPkw, countsLkw);
					}
				}
			}
		}
		writeSafetyFile(countList, network, map);

		CountsWriter writerPkw = new CountsWriter(countsPkw);
		CountsWriter writerLkw = new CountsWriter(countsLkw);
		writerPkw.write(count.toString() + "_Pkw.xml");
		writerLkw.write(count.toString() + "_Lkw.xml");
	}

	/**
	 * calculates the angel between the street from the excel data and the paired matsim-link, if the angel is to high the count will be ignored.
	 *
	 * @param leipzigCounts the list with the information from the excel file
	 * @param countLink     the link that angel is currently checked
	 * @return ture if the angel is lower then 30° and false if it is higher
	 */
	private boolean calculateAngle(LeipzigCounts leipzigCounts, Link countLink) {

		Coord fromCoordOldSys = new Coord(leipzigCounts.startNodeCoordX, leipzigCounts.startNodeCoordY);
		Coord toCoordOldSys = new Coord(leipzigCounts.endNodeCoordX, leipzigCounts.endNodeCoordY);
		Coord fromCoord = CT.transform(fromCoordOldSys);
		Coord toCoord = CT.transform(toCoordOldSys);

		double[] v1 = new double[2];
		double[] v2 = new double[2];

		v1[0] = toCoord.getX() - fromCoord.getX();
		v1[1] = toCoord.getY() - fromCoord.getY();

		v2[0] = countLink.getToNode().getCoord().getX() - countLink.getFromNode().getCoord().getX();
		v2[1] = countLink.getToNode().getCoord().getY() - countLink.getFromNode().getCoord().getY();

		double scalarP = v1[0] * v2[0] - v1[1] * v2[1];
		double amountV1 = Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
		double amountV2 = Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);

		double angel = Math.acos(scalarP / (amountV1 * amountV2));
		// 30°
		return angel < Math.PI / 6;

	}

	/**
	 * creates a file to manually check the quality of the data and the matching.
	 *
	 * @param leipzigCounts the list with the information from the excel file
	 * @param network       the matsim-network on which the counts are located
	 * @param map           a map with ids of the counts as key and the links as values
	 */
	private void writeSafetyFile(List<LeipzigCounts> leipzigCounts, Network network, Map<String, Link> map) {
		try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("Output/allPoints.csv"), CSVFormat.DEFAULT)) {
			csvPrinter.printRecord("originalId", "oCoordStartX", "oCoordStartY", "oCoordEndX", "oCoordEndY", "CoordStartX", "nCoordStartY", "nCoordEndX", "nCoordEndY");
			for (LeipzigCounts lc : leipzigCounts) {
				Coord originalFrom = CT.transform(new Coord(lc.startNodeCoordX, lc.startNodeCoordY));
				Coord originalTo = CT.transform(new Coord(lc.endNodeCoordX, lc.endNodeCoordY));
				Node fromNode = NetworkUtils.getNearestNode(network, originalFrom);
				Node toNode = NetworkUtils.getNearestNode(network, originalTo);
				csvPrinter.printRecord(lc.routeNr + "_" + lc.startNodeID + "_" + lc.endNodeID, originalFrom.getX(), originalFrom.getY(), originalTo.getX(), originalTo.getY(), fromNode.getCoord().getX(), fromNode.getCoord().getY(), toNode.getCoord().getX(), toNode.getCoord().getY());
				csvPrinter.flush();
			}
		} catch (IOException e) {
			logger.error("Error when writing points", e);
		}

		try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("Output/lines.csv"), CSVFormat.DEFAULT)) {
			csvPrinter.printRecord("id", "line");
			for (Map.Entry<String, Link> entry : map.entrySet()) {
				String line = "LINESTRING (" + entry.getValue().getFromNode().getCoord().getX() + " " + entry.getValue().getFromNode().getCoord().getY() + ", " + entry.getValue().getToNode().getCoord().getX() + " " + entry.getValue().getToNode().getCoord().getY() + ")";
				csvPrinter.printRecord(entry.getKey(), line);
				csvPrinter.flush();
			}
		} catch (IOException e) {
			logger.error("Error when writing link ids", e);
		}
	}

	/**
	 * Creates the actual counts and fills them.
	 *
	 * @param leipzigCounts the list with the information from the excel file
	 * @param countLink     link for the count
	 * @param countsPkw     counts were the information for the pkw-counts is saved
	 * @param countsLkw     counts were the information for the lkw-counts is saved
	 */
	private void fillingCounts(LeipzigCounts leipzigCounts, Link countLink, Counts<Link> countsPkw, Counts<Link> countsLkw) {

		if (countsPkw.getCount(countLink.getId()) != null) {
			Volume x = countsPkw.getCount(countLink.getId()).getVolume(1);
			double c = (x.getValue() + leipzigCounts.kfz) / 2;
			countsPkw.getCount(Id.createLinkId(countLink.getId())).createVolume(1, c);
			Volume y = countsLkw.getCount(countLink.getId()).getVolume(1);
			double l = (y.getValue() + leipzigCounts.kfz) / 2;
			countsLkw.getCount(Id.createLinkId(countLink.getId())).createVolume(1, l);
			return;
		}
		countsPkw.createAndAddCount(countLink.getId(), leipzigCounts.startNodeID + "_" + leipzigCounts.endNodeID);
		countsPkw.getCount(Id.createLinkId(countLink.getId())).createVolume(1, leipzigCounts.kfz);
		countsLkw.createAndAddCount(countLink.getId(), leipzigCounts.startNodeID + "_" + leipzigCounts.endNodeID);
		countsLkw.getCount(Id.createLinkId(countLink.getId())).createVolume(1, leipzigCounts.lkw);
	}

	/**
	 * Creates a route generator with only time as a decision variable.
	 *
	 * @param network the matsim-network on which the counts are located
	 * @return a least cost path router
	 */
	private LeastCostPathCalculator generateRouter(Network network) {
		FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathCalculatorFactory fastAStarLandmarksFactory = new SpeedyALTFactory();
		TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
		return fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility,
				travelTime);
	}

	/**
	 * Reads in a file with information about witch counts should be ignored.
	 *
	 * @param ignoredCountsFile path to the file with the information
	 * @return a list with ids of ignored counts
	 */
	private List<String> readIgnoredCountFile(Path ignoredCountsFile) {
		ArrayList<String> ignoredCountList = new ArrayList<>();
		try (CSVParser csvReader = new CSVParser(new FileReader(ignoredCountsFile.toFile()), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
			for (CSVRecord count : csvReader) {
				if (count.size() == 1) {
					ignoredCountList.add(count.get(0));
				} else {
					throw new IllegalArgumentException("Something is wrong with the ignored counts file, count[] should has length 1 but has " + count.size());
				}
			}
		} catch (IOException e) {
			logger.error("Error when reading ignored counts file", e);
		}
		return ignoredCountList;
	}

	private Map<String, String> readNewManuallyMastimLinkShift(Path manuallyMatsimLinkShift) {
		Map<String, String> shiftLinks = new HashMap<>();
		try (CSVParser csvReader = new CSVParser(new FileReader(manuallyMatsimLinkShift.toFile()), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
			for (CSVRecord idLinks : csvReader) {
				if (idLinks.size() == 2) {
					shiftLinks.put(idLinks.get(0), idLinks.get(1));
				} else {
					throw new IllegalArgumentException("Something is wrong with the new manually matsim link shift file, id_link[] should has length 1 but has " + idLinks.size());
				}
			}
		} catch (IOException e) {
			logger.error("Error when reading manually matsim links shift", e);
		}
		return shiftLinks;
	}

	/**
	 * A inner class to save the data from the excel file.
	 */
	private static final class LeipzigCounts {
		private int routeNr;
		private int kfz;
		private int lkw;
		private int startNodeID;
		private int endNodeID;
		private int year;
		private double startNodeCoordX;
		private double startNodeCoordY;
		private double endNodeCoordX;
		private double endNodeCoordY;
		private String startName = null;
		private String endName = null;
	}
}
