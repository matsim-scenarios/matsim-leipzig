package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO @Simon: JavaDoc.
 */
public final class ActivitiesOnPtLinkAnalysis {

	private static final Logger log = LogManager.getLogger(ActivitiesOnPtLinkAnalysis.class);

	private final Map<Id<Link>, String> ptLinksWithActivities = new HashMap<>();
	private final Map<Id<Link>, String> followingLegs = new HashMap<>();

	public ActivitiesOnPtLinkAnalysis() {
	}

	public static void main(String[] args) {


		String inputPop = "C:/Users/Simon/Desktop/carFreeAreaTestLeipzig/010_linkChooser2.0_carOnlyNetwork_carfree/ITERS/it.0/MultimodalLinkChooserTest.0.plans.xml.gz";
		String outputFile = "C:/Users/Simon/Desktop/carFreeAreaTestLeipzig/010_linkChooser2.0_carOnlyNetwork_carfree/ITERS/it.0/activitiesOnPtLinks.csv";

		ActivitiesOnPtLinkAnalysis analysis = new ActivitiesOnPtLinkAnalysis();
		analysis.getActivitiesOnPtLinks(inputPop);
		analysis.writeCsv(outputFile);
		log.info("Activities on pt links written to {}", outputFile);
	}

	void getActivitiesOnPtLinks(String inputPop) {

		Population pop = PopulationUtils.readPopulation(inputPop);

		for (Person person : pop.getPersons().values()) {
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for (PlanElement element : planElements) {
				if (element instanceof Activity) {
					if (((Activity) element).getType().contains("interaction")) {
						continue;
					}

					int index = planElements.indexOf(element);

					if (((Activity) element).getLinkId().toString().contains("pt")) {
						this.ptLinksWithActivities.putIfAbsent(((Activity) element).getLinkId(), ((Activity) element).getType());
						Leg followingLeg;
						//check for last act on plan
						if (!(planElements.indexOf(element) >= planElements.size() - 1)) {
							if (planElements.get(planElements.indexOf(element) + 1) instanceof Leg) {
								followingLeg = (Leg) planElements.get(planElements.indexOf(element) + 1);
								this.followingLegs.putIfAbsent(((Activity) element).getLinkId(), followingLeg.getMode());
							} else {
								throw new IllegalStateException("PlanElement " + element + " is an Activity but is not followed by a Leg!");
							}
						} else {
							this.followingLegs.putIfAbsent(((Activity) element).getLinkId(), "lastAct");
						}

					}
				}
			}
		}
	}

	void writeCsv(String outputFile) {

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);

		//header
		try {
			writer.write("linkID;actType;followingMode");
			writer.newLine();

			for (Id<Link> id : this.ptLinksWithActivities.keySet()) {
				writer.write(id + ";" + this.ptLinksWithActivities.get(id) + ";" + this.followingLegs.get(id));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			log.error(e);
		}
	}
}
