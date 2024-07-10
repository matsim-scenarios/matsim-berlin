package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Quick person money analysis handler
 */
public class MyPersonMoneyEventHandler {

	public static void main (String[] args) {
		String inputPath = args[0];
		String outputPath = args[1];
		EventsManager myEventsmanager = EventsUtils.createEventsManager();
		TestPersonMoneyEventHandler myEventHandler = new TestPersonMoneyEventHandler(new ArrayList<PersonMoneyEvent>());
		myEventsmanager.addHandler(myEventHandler);
		EventsUtils.readEvents(myEventsmanager, inputPath);
		writeAllPersonMoneyEvents(myEventHandler.personMoneyEventList, outputPath);
	}

	static void writeAllPersonMoneyEvents(List<PersonMoneyEvent> personMoneyEventList, String outputFilename) {
		try {
			CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(outputFilename), CSVFormat.DEFAULT);

			try {
				csvPrinter.printRecord("time", "person", "amount", "purpose", "transactionPartner", "reference");
				Iterator var3 = personMoneyEventList.iterator();

				while (var3.hasNext()) {
					PersonMoneyEvent personMoneyEvent = (PersonMoneyEvent) var3.next();
					csvPrinter.printRecord(personMoneyEvent.getTime(), personMoneyEvent.getPersonId(), personMoneyEvent.getAmount(), personMoneyEvent.getPurpose(), personMoneyEvent.getTransactionPartner(), personMoneyEvent.getReference());
				}
			} catch (Throwable var6) {
				try {
					csvPrinter.close();
				} catch (Throwable var5) {
					var6.addSuppressed(var5);
				}
				throw var6;
			}
			csvPrinter.close();
		} catch (IOException var7) {
			LogManager.getLogger("Could not write " + outputFilename + ".");
		}
	}

}

class TestPersonMoneyEventHandler implements org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler {

	List<PersonMoneyEvent> personMoneyEventList;
	TestPersonMoneyEventHandler( List<PersonMoneyEvent> personMoneyEventList) {
		this.personMoneyEventList = personMoneyEventList;
	}

	@Override
	public void handleEvent(PersonMoneyEvent personMoneyEvent) {
		personMoneyEventList.add(personMoneyEvent);
	}
}


