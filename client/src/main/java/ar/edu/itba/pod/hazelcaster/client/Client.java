package ar.edu.itba.pod.hazelcaster.client;

import ar.edu.itba.pod.hazelcaster.abstractions.Airport;
import ar.edu.itba.pod.hazelcaster.abstractions.Movement;
import ar.edu.itba.pod.hazelcaster.abstractions.outputObjects.MoveCountOutput;
import ar.edu.itba.pod.hazelcaster.client.config.ClientConfiguration;
import ar.edu.itba.pod.hazelcaster.client.config.ClientProperties;
import ar.edu.itba.pod.hazelcaster.interfaces.CSVSerializer;
import ar.edu.itba.pod.hazelcaster.interfaces.QueryService;

import com.hazelcast.core.HazelcastInstance;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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

	public void run() {
		if (hazelcast == null) {
			logger.error("No se pudo desplegar la instancia de Hazelcast.");
			return;
		}
		try {
			final List<Airport> airports = csv.read(Airport.class, properties.getAirportsFilename());
			final List<Movement> movements = csv.read(Movement.class, properties.getMovementsFilename());
			// En 'properties' están todas las properties.
			// Cargarlas en el cluster.
			
			List<MoveCountOutput> result = qService.getAirportsMovements(movements, airports);
			
			System.out.println("Query 1 answer:");
			for (MoveCountOutput output : result) {
				System.out.println(output.toString());
			}
			
			// Ejecutar query.
			// Transformar el resultado para poder usar csv.write(...).
			// Almacenar el resultado en un archivo.

			// Estos write son de ejemplo:
			//csv.write(airports, properties.getResultFilename());
			//csv.write(movements, properties.getResultFilename() + "M");
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
