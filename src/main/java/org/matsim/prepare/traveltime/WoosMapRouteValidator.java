package org.matsim.prepare.traveltime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.matsim.api.core.v01.Coord;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Validator for woos map.
 */
public class WoosMapRouteValidator implements RouteValidator {

	private static final String URL = "https://api.woosmap.com/distance/route/json";

	private final String apiKey;

	private final CloseableHttpClient httpClient;
	private final ObjectMapper mapper;


	public WoosMapRouteValidator(String apiKey) {
		this.apiKey = apiKey;
		this.httpClient = HttpClients.createDefault();
		this.mapper = new ObjectMapper();
	}

	@Override
	public String name() {
		return "woosmap";
	}

	@Override
	public Result calculate(Coord from, Coord to, int hour) {

		// Rate limit of 10 request per seconds
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// https://developers.woosmap.com/products/distance-api/route-endpoint/

		// TODO
		ClassicHttpRequest req = ClassicRequestBuilder.get(URL)
			.addParameter("key", apiKey)
			.build();

		try {
			httpClient.execute(req, resp -> mapper.readTree(resp.getEntity().getContent()));

			// TODO
			return null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void close() throws Exception {
		httpClient.close();
	}
}
