package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonWarmEventHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class KnEmissionsAnalysis{
	private static final Logger log = LogManager.getLogger( KnEmissionsAnalysis.class );

	public static void main( String[] args ){
		final String file = "/Users/kainagel/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/v1.3.1/base-case/analysis/analysis-emissions/short.xml";
//		final String file = "/Users/kainagel/public-svn/matsim/scenarios/countries/de/leipzig/projects/namav/v1.3.1/base-case/analysis/analysis-emissions/emission.events.offline.xml.gz";
		// yy replace by relative file name

		EventsManager events = EventsUtils.createEventsManager();

		final MyHandler emissionsPerPersonWarmEventHandler = new MyHandler();
		events.addHandler( emissionsPerPersonWarmEventHandler );

		final Map<String,Double> co2ByMode = new LinkedHashMap<>();
		events.addHandler( new BasicEventHandler(){
			@Override public void handleEvent( Event event ){
				if ( !"warmEmissionEvent".equals( event.getEventType() ) ) {
					return;
				}
//				log.info( "event=" + event );
				double co2 = Double.parseDouble( event.getAttributes().get( "CO2_TOTAL" ) );
				String vehicleId = event.getAttributes().get( "vehicleId" );
				if ( vehicleId.contains( "car" ) ) {
					co2ByMode.merge( "car", co2, Double::sum );
//					log.info( "car; co2=" + co2 + "; newSum=" + co2ByMode.get("car") );
				} else if ( vehicleId.contains( "freight" ) ) {
					co2ByMode.merge( "freight", co2, Double::sum );
//					log.info( "freight; co2=" + co2 + "; newSum=" + co2ByMode.get("freight") );
				}

			}
		} );

		EventsUtils.readEvents( events, file );

		for( Map.Entry<String, Double> entry : co2ByMode.entrySet() ){
			log.info( "vehType=" + entry.getKey() + "; co2=" + entry.getValue() );
		}

	}


	static class MyHandler implements WarmEmissionEventHandler{
		private final Map<Id<Person>, Map<Pollutant, Double>> warmEmissionsTotal = new HashMap<>();

		public MyHandler() {
		}

		@Override
		public void handleEvent( WarmEmissionEvent event ) {
			log.info( "event=" + event );

			// TODO person id statt vehicleid??? woher?
			Id<Person> personId = Id.create(event.getVehicleId(), Person.class);
			Map<Pollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

			if(warmEmissionsTotal.get(personId) != null){
				Map<Pollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(personId );
				for( Map.Entry<Pollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
					Pollutant pollutant = entry.getKey();
					Double eventValue = entry.getValue();

					Double previousValue = warmEmissionsSoFar.get(pollutant);
					Double newValue = previousValue + eventValue;
					warmEmissionsSoFar.put(pollutant, newValue);
				}
				warmEmissionsTotal.put(personId, warmEmissionsSoFar);
			} else {
				warmEmissionsTotal.put(personId, warmEmissionsOfEvent);
			}
		}

		public Map<Id<Person>, Map<Pollutant, Double>> getWarmEmissionsPerPerson() {
			return warmEmissionsTotal;
		}

		@Override
		public void reset(int iteration) {
			warmEmissionsTotal.clear();
		}
	}



}
