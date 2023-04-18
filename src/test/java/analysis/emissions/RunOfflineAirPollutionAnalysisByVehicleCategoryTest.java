package analysis.emissions;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.matsim.analysis.emissions.RunOfflineAirPollutionAnalysisByVehicleCategory;

class RunOfflineAirPollutionAnalysisByVehicleCategoryTest {

    // to run this test an environment variable needs to be set in your IDE and on the server...
	@Ignore("Can only run when setup ENV correctly")
    @Test
    void run() {
        try {
            String runDirectory = "output/it-1pct/";
            String runId = "leipzig-25pct";
            String hbefaFileWarm = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";
            String hbefaFileCold = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/ColdStart_Vehcat_2020_Average_withHGVetc.csv.enc";
            String analysisOutputDirectory = "src/test/java/analysis/emissions/emission-analysis-offline-test";
            RunOfflineAirPollutionAnalysisByVehicleCategory analysis = new RunOfflineAirPollutionAnalysisByVehicleCategory(runDirectory, runId, hbefaFileWarm, hbefaFileCold, analysisOutputDirectory);
            analysis.call();

        } catch ( Exception ee ) {
            throw new RuntimeException(ee) ;
        }
    }
}
