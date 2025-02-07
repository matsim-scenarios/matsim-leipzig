package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
		name = "person-events",
		description = "Extract all events related to one specific person from events file."
)
public class ExtractPersonEvents implements MATSimAppCommand, BasicEventHandler {

	private static final Logger log = LogManager.getLogger(ExtractPersonEvents.class);

	@CommandLine.Parameters(paramLabel = "INPUT", description = "Input events file", arity = "1")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Optional path to output file")
	private Path output;

	@CommandLine.Option(names = "--person", description = "Person id", required = true)
	private String person;

	private BufferedWriter writer;

	public static void main(String[] args) {
		new ExtractPersonEvents().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("File {} does not exists.", input);
			return 2;
		}

		if (output != null)
			writer = Files.newBufferedWriter(output);

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this);

		manager.initProcessing();

		EventsUtils.readEvents(manager, input.toString());

		manager.finishProcessing();

		if (writer != null)
			writer.close();

		return 0;
	}

	@Override
	public void handleEvent(Event event) {

		boolean relevant = false;

		if (person.equals(event.getAttributes().get("person")))
			relevant = true;
		else if (event.getAttributes().containsKey("vehicle") && event.getAttributes().get("vehicle").startsWith(person))
			relevant = true;

		if (relevant) {

			log.info(event);

			if (writer != null) {
				try {
					writer.write(event.toString());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
	}
}
