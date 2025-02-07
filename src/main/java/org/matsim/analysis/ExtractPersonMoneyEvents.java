package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
		name = "person-money-events",
		description = "Extract all events which involve payments from events file."
)

public class ExtractPersonMoneyEvents implements MATSimAppCommand, PersonMoneyEventHandler {

	private static final Logger log = LogManager.getLogger(ExtractPersonMoneyEvents.class);

	@CommandLine.Option(names = "--events", description = "Input events file")
	private Path inputEvents;

	@CommandLine.Option(names = "--output", description = "Path to output file")
	private Path output;

	@CommandLine.Option(names = "--purpose", description = "Optional filter for payment purpose. Use quotation marks if your purpose contains blanks")
	private String purpose;

	private BufferedWriter writer;

	public static void main(String[] args) {
		new ExtractPersonMoneyEvents().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		if (!Files.exists(inputEvents)) {
			log.error("File {} does not exists.", inputEvents);
			return 2;
		}

		if (output == null) {
			log.error("No output file defined!");
			return 2;
		}

		writer = Files.newBufferedWriter(output);

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this);

		manager.initProcessing();

		EventsUtils.readEvents(manager, inputEvents.toString());

		manager.finishProcessing();

		if (writer != null)
			writer.close();

		return 0;
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {

		boolean relevant = false;

		if (purpose != null) {
			if (event.getPurpose().contains(purpose)) {
				relevant = true;
			}
		} else {
			relevant = true;
		}

		if (relevant) {
			if (writer != null) {
				try {
					writer.write(event.toString());
					writer.newLine();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}

	}
}
