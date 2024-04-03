package org.matsim.analysis;

import org.matsim.application.analysis.population.TripAnalysis;

public class TripAnalysisCarFree {

	public static void main(String[] args) {

		new TripAnalysis().execute(
				"--input-trips", "/Users/gregorr/Volumes/math-cluster/matsim-leipzig/v1.3.1policyRuns/baseCase/output/output-leipzig-10pct/leipzig-10pct.output_trips.csv.gz",
				"--input-persons", "/Users/gregorr/Volumes/math-cluster/matsim-leipzig/v1.3.1policyRuns/baseCase/output/output-leipzig-10pct/leipzig-10pct.output_persons.csv.gz",
				"--shp", "/Users/gregorr/Volumes/math-cluster/matsim-leipzig/input/shp/leipzig_carfree_area_large/Zonen90_update.shp"
		);



	}

}
