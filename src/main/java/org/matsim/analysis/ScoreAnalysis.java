package org.matsim.analysis;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@CommandLine.Command(
		name = "score-analysis",
		description = "Read score details from plans and writes it to CSV."
)
public class ScoreAnalysis implements MATSimAppCommand {

	@CommandLine.Option(names = "--input", required = true, description = "Path to plans xml")
	private Path input;

	@CommandLine.Option(names = "--output", required = true, description = "Output csv path")
	private Path output;

	@CommandLine.Mixin
	private CsvOptions csv;

	public static void main(String[] args) {
		new ScoreAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		// Reads new scoreExplanation attribute, not yet merged into master
		Set<String> header = new LinkedHashSet<>();

		List<Holder> values = new ArrayList<>();

		for (Person p : population.getPersons().values()) {

			Plan plan = p.getSelectedPlan();

			String expl = (String) plan.getAttributes().getAttribute("scoreExplanation");

			if (expl == null)
				continue;

			String[] parts = expl.split(";");

			Object2DoubleOpenHashMap<String> entries = new Object2DoubleOpenHashMap<>();

			for (String part : parts) {
				String[] name_value = part.split("=");
				header.add(name_value[0]);
				entries.put(name_value[0], Double.parseDouble(name_value[1]));
			}

			values.add(new Holder(p.getId().toString(), plan.getScore(), entries));
		}

		try (CSVPrinter printer = csv.createPrinter(output)) {

			printer.print("person");
			printer.print("score");
			for (String s : header) {
				printer.print(s);
			}
			printer.println();

			for (Holder value : values) {
				printer.print(value.id);
				printer.print(value.score);
				for (String s : header) {
					printer.print(value.values.getDouble(s));
				}
				printer.println();
			}
		}

		return 0;
	}

	private record Holder(String id, double score, Object2DoubleMap<String> values) {
	}
}
