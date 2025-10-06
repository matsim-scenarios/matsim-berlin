package org.matsim.dashboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.dashboard.*;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;


@CommandLine.Command(
        name = "dashboard",
        description = "Run analysis and create SimWrapper dashboard for existing run output."
)
public class CreateEnergyConsumptionDashboard implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(CreateEnergyConsumptionDashboard.class);
    @CommandLine.Mixin
    private final ShpOptions shp = new ShpOptions();
    @CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which the dashboards is to be generated.")
    private List<Path> inputPaths;

    private CreateEnergyConsumptionDashboard() {
    }

    public static void main(String[] args) {
        new CreateEnergyConsumptionDashboard().execute(args);
    }

    @Override
    public Integer call() throws Exception {

        for (Path runDirectory : inputPaths) {

            Path configPath = ApplicationUtils.matchInput("config.xml", runDirectory);
            Config config = ConfigUtils.loadConfig(configPath.toString());
            SimWrapper sw = SimWrapper.create(config);

            SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

//            if (shp.isDefined()) {
//                //not sure if this is the best way to go, might be that the shape file would be automatically read by providing the --shp command line option
//                simwrapperCfg.defaultParams().setShp(shp.getShapeFile());
//            }

            simwrapperCfg.defaultParams().setShp("D:/git/matsim-berlin/input/v6.4/area/area.shp");
            SimWrapperConfigGroup.ContextParams gartenfeldParams = (SimWrapperConfigGroup.ContextParams) simwrapperCfg.getParameterSets().get("params").stream()
                    .filter(p -> p instanceof SimWrapperConfigGroup.ContextParams)
                    .filter(p -> ((SimWrapperConfigGroup.ContextParams) p).getContext().equals("gartenfeld"))
                    .findAny().orElseThrow();
            gartenfeldParams.setShp("D:/git/matsim-berlin/input/gartenfeld/DNG_area.gpkg");

            //skip default dashboards
            simwrapperCfg.setDefaultDashboards(SimWrapperConfigGroup.Mode.disabled);

            sw.addDashboard(Dashboard.customize(new EnergyConsumptionDashboard())
                    .context("gartenfeld"));

            try {
                //append dashboard to existing ones
                boolean append = true;
                sw.generate(runDirectory, append);
                sw.run(runDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return 0;
    }

}

