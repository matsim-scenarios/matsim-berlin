package org.matsim.prepare.traveltime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.matsim.api.core.v01.Coord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Fetch route information from Google Maps api.
 */
public class GoogleRouteValidator implements RouteValidator {

	private static final String URL = "https://routes.googleapis.com/directions/v2:computeRoutes";

	private final String apiKey;
	private final ObjectMapper mapper;
	private final CloseableHttpClient httpClient;

	public GoogleRouteValidator(String apiKey) {
		this.apiKey = apiKey;

		mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		httpClient = HttpClients.createDefault();
	}

	@Override
	public String name() {
		return "Google";
	}

	@Override
	public Result calculate(Coord from, Coord to, int hour) {

		try {

			ClassicRequestBuilder post = ClassicRequestBuilder.post(URL);

			post.addHeader("Content-Type", "application/json");
			post.addHeader("X-Goog-Api-Key", apiKey);
			post.addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters");

			String request = mapper.writeValueAsString(new Request(from, to, hour));

			post.setEntity(request, ContentType.APPLICATION_JSON);

			JsonNode data = httpClient.execute(post.build(), response -> {

				if (response.getCode() != 200) {
					response.getEntity().writeTo(System.err);
					throw new IllegalStateException("Non-success response:");
				}

				return mapper.readValue(response.getEntity().getContent(), JsonNode.class);
			});

			JsonNode route = data.get("routes").get(0);
			String duration = route.get("duration").asText().replace("s", "");

			return new Result(hour, (int) Double.parseDouble(duration), (int) route.get("distanceMeters").asDouble());

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void close() throws Exception {
		httpClient.close();
	}

	private static final class Request {

		Location origin;
		Location destination;
		String travelMode = "DRIVE";
		String routingPreference = "TRAFFIC_AWARE_OPTIMAL";
		ZonedDateTime departureTime;
		String units = "METRIC";

		public Request(Coord from, Coord to, int hour) {
			origin = new Location(from);
			destination = new Location(to);
			departureTime = RouteValidator.createDateTime(hour);
		}
	}

	private final static class Location {
		final Map<String, Object> location;

		public Location(Coord coord) {

			location = Map.of("latLng", Map.of(
				"latitude", coord.getY(),
				"longitude", coord.getX()
			));

		}
	}

}
