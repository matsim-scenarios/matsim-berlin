package org.matsim.analysis.linkAnalysis;

//import org.matsim.analysis.linkAnalysis.LinkAnalysisEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/*public class RunEventsHandler {
    public static void main(String args[]) throws IOException, ParserConfigurationException, SAXException {


        //-------------For Input (and Output) Files-------------
        // BaseCase
        //String inputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/berlin-v5.5-10pct.output_events.xml";

        // PlanA25
        //String inputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/ScenarioA25.output_events.xml.gz";

        // PlanA50
        //String inputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/ScenarioA50.output_events.xml";

        // PlanA75
        //String inputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/ScenarioA75.output_events.xml";

        // PlanA100
        //String inputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/ScenarioA100.output_events.xml";

        // PlanC25
        String inputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/ScenarioC25.output_events.xml";

        // PlanC50
        //String inputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/ScenarioC50.output_events.xml";

        // PlanC75
        //String inputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/ScenarioC75.output_events.xml";

        // PlanC100
        //String inputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/ScenarioC100.output_events.xml";
        //-------------For Input (and Output) Files-------------//

        EventsManager eventsManager = EventsUtils.createEventsManager();

        LinkAnalysisEventHandler eventHandler = new LinkAnalysisEventHandler();
        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);

        eventHandler.printResults();

    }
}*/
