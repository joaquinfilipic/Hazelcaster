package ar.edu.itba.pod.hazelcaster.client;

import ar.edu.itba.pod.hazelcaster.abstractions.Airport;
import ar.edu.itba.pod.hazelcaster.abstractions.Movement;
import ar.edu.itba.pod.hazelcaster.abstractions.outputObjects.InternationalPercentageOutput;
import ar.edu.itba.pod.hazelcaster.abstractions.outputObjects.LandingMoveCountOutput;
import ar.edu.itba.pod.hazelcaster.abstractions.outputObjects.MoveCountOutput;
import ar.edu.itba.pod.hazelcaster.abstractions.outputObjects.MovesBetweenAirportsOutput;
import ar.edu.itba.pod.hazelcaster.abstractions.outputObjects.SameMovesPairOutput;
import ar.edu.itba.pod.hazelcaster.client.config.ClientConfiguration;
import ar.edu.itba.pod.hazelcaster.client.config.ClientProperties;
import ar.edu.itba.pod.hazelcaster.interfaces.CSVSerializer;
import ar.edu.itba.pod.hazelcaster.interfaces.QueryService;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class Client {

	private static final Logger logger
		= LoggerFactory.getLogger(Client.class);

	protected final HazelcastInstance hazelcast;
	protected final ClientProperties properties;
	protected final CSVSerializer csv;
	protected final QueryService qService;

	@Inject
	public Client(
			@Nullable final HazelcastInstance hazelcast,
			final ClientProperties properties,
			final CSVSerializer csv,
			final QueryService qService) {
		this.hazelcast = hazelcast;
		this.properties = properties;
		this.csv = csv;
		this.qService = qService;
	}

	public static void main(final String ... arguments) {
		try {
			logger.info("(2018) Hazelcaster Client v1.0.");
			SpringApplication.run(ClientConfiguration.class, arguments)
				.getBean(Client.class)
				.run();
		}
		catch (final Exception exception) {
			logger.error("Excepción inesperada: '{}'.", exception.getClass().getName());
			logger.error("Mensaje: {}.", exception.getMessage());
			exception.printStackTrace();
		}
		logger.info("Hazelcaster client exiting...");
	}

	private static final Logger timeLogger = LoggerFactory.getLogger("time-logger");

	public void run() {
		if (hazelcast == null) {
			logger.error("No se pudo desplegar la instancia de Hazelcast.");
			return;
		}
		try {
			timeLogger.info("Inicio de lectura de archivos CSV de entrada.");
			final List<Airport> airports = csv.read(Airport.class, properties.getAirportsFilename());
			final List<Movement> movements = csv.read(Movement.class, properties.getMovementsFilename());
			timeLogger.info("Fin de lectura de archivos CSV de entrada.");
			int queryId = properties.getQueryID();
			
			IList<Airport> airportsIList = hazelcast.getList(properties.getClusterName() + "-airports");
			IList<Movement> movementsIList = hazelcast.getList(properties.getClusterName() + "-movements"); 

			airportsIList.clear();
			airportsIList.addAll(airports);
			
			movementsIList.clear();
			movementsIList.addAll(movements);
			
			switch (queryId) {
				case 1:
					List<MoveCountOutput> result1 = qService.getAirportsMovements();
					csv.write(result1, properties.getResultFilename());
					break;
				case 2:
					List<SameMovesPairOutput> result2 = qService.getAirportsPairsWithSameMovements();
					csv.write(result2, properties.getResultFilename());
					break;
				case 3:
					List<MovesBetweenAirportsOutput> result3 = qService.getMovementsBetweenAirports();
					csv.write(result3, properties.getResultFilename());
					break;
				case 4:
					List<LandingMoveCountOutput> result4 = qService.getAirportsWithMostLandings(
							properties.getOACI(),
							properties.getN());
					csv.write(result4, properties.getResultFilename());
					break;
				case 5:
					List<InternationalPercentageOutput> result5 = qService.getAirportsWithMostInternationalLandings(
							properties.getN());
					csv.write(result5, properties.getResultFilename());
					break;
			}
			
			// En 'properties' están todas las properties.
			// Cargarlas en el cluster.
		}
		catch (final IOException exception) {
			exception.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		hazelcast.shutdown();
	}
}
