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
		name = "freight-events",
		description = "Extract all events related to freight_id(s) from events file, looking on specified type"
)
public class ExtractFreightEvents implements MATSimAppCommand, BasicEventHandler {
	private static final Logger log = LogManager.getLogger(ExtractPersonEvents.class);

	@CommandLine.Parameters(paramLabel = "INPUT", description = "Input events file", arity = "1")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Optional path to output file")
	private Path output;

//	@CommandLine.Option(names = "--freight", description = "Freight id", required = true)
	@CommandLine.Option(names = "--freight", description = "filter by (1) '' - all id (2) 'freight' - all freight id (3) '[id]' specific freight id")
	private String person;

	@CommandLine.Option(names = "--type", description = "Optional filter by type")
	private String type;

	private BufferedWriter writer;
	private boolean lookupHeadline = true;

	public static void main(String[] args) {
		new ExtractFreightEvents().execute(args);
	}


	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("File {} does not exists.", input);
			return 2;
		}

		if (output != null) {
			writer = Files.newBufferedWriter(output);
//			writer.write("time;type;person;link;x;y;actType");
//			writer.newLine();
		}

		if (person.contentEquals("all"))
			person = "all";
		else if (person == null)
			person = "freight_";
		else
			person = "freight_"+person;

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

//		if (person == null)
//			person = "freight_";
//		else
//			person = "freight_"+person;

//		if (person.equals(event.getAttributes().get("person")) && event.getAttributes().get("type").contains("actend"))
//			relevant = true;
//		else if (event.getAttributes().containsKey("vehicle") && event.getAttributes().get("vehicle").startsWith(person) && event.getAttributes().get("type").contains("actend"))
//			relevant = true;


		if (person.contentEquals("all")){
			if (event.getAttributes().get("type").toString().equals(type))
				relevant = true;
		} else if (event.getAttributes().get("type").toString().equals(type) && event.getAttributes().get("person").startsWith(person))
			relevant = true;

		if (relevant) {
			if (lookupHeadline){
				String headline = "";
				for (String title :event.getAttributes().keySet()){
					headline = headline+";"+title;
				}
				headline = headline.replaceFirst(";", "");
					try {
						writer.write(headline);
						writer.newLine();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					log.info("Set headline: "+ headline);
				lookupHeadline = false;
			}

			log.info(event);

			if (writer != null) {
				String line = "";
				try {
//					writer.write(event.toString());
					for (String valueTitle : event.getAttributes().keySet()){
						line = line+";"+event.getAttributes().get(valueTitle);
					}
					line = line.replaceFirst(";", "");
					writer.write(line);
//					writer.write(String.valueOf(event.getTime())+";"
//							+event.getEventType()+";"
//							+event.getAttributes().get("person")+";"
//							+event.getAttributes().get("link")+";"
//							+event.getAttributes().get("x")+";"
//							+event.getAttributes().get("y")+";"
//							+event.getAttributes().get("actType"));
					writer.newLine();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
	}
}
