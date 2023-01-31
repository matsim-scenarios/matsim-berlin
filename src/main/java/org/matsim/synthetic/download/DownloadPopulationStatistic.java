package org.matsim.synthetic.download;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVPrinter;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommandLine.Command(name = "download-population-statistics", description = "Download table 12411-02-03-5 for existing job from regionalstatistik.de")
public class DownloadPopulationStatistic implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(DownloadPopulationStatistic.class);

	@CommandLine.Option(names = "--username", required = true, description = "Username for regionalstatistik.de")
	private String username;

	@CommandLine.Option(names = "--password", defaultValue = "${GENESIS_PW}", interactive = true, description = "Password for regionalstatistik.de")
	private String password;

	@CommandLine.Option(names = "--name", required = true, defaultValue = "12411-02-03-5", description = "Name of the saved result on regionalstatistik.de")
	private String name;

	@CommandLine.Option(names = "--output", description = "Output csv", required = true)
	private Path output;

	@CommandLine.Mixin
	private CsvOptions csv;

	private RequestConfig config;

	public static void main(String[] args) {
		new DownloadPopulationStatistic().execute(args);
	}

	/**
	 * Format string for age group.
	 */
	public static String formatAge(String input) {
		return input.replace("bis unter", "-")
				.replace("unter", "0 -").replace(" Jahre", "")
				.replace(" und mehr", " - inf");
	}

	@Override
	public Integer call() throws Exception {

		config = RequestConfig.custom()
				.setConnectionRequestTimeout(5, TimeUnit.MINUTES)
				.setResponseTimeout(5, TimeUnit.MINUTES)
				.build();
		if (password.isBlank()) {
			log.error("No password given, either set GENESIS_PW or use --password to enter it.");
			return 1;
		}

		List<Row> rows;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			String result = downloadResult(client);
			rows = parseResult(result);
		}

		try (CSVPrinter printer = csv.createPrinter(output)) {
			printer.printRecord("code", "age", "gender", "n");
			for (Row row : rows) {
				printer.printRecord(row.code, row.ageGroup, row.gender, row.n);
			}
		}

		return 0;
	}

	private String downloadResult(CloseableHttpClient client) throws IOException {

		HttpGet httpGet = new HttpGet(String.format("https://www.regionalstatistik.de/genesisws/rest/2020/data/result?username=%s&password=%s&name=%s&area=all&compress=true",
				username, password, name));

		httpGet.setConfig(config);

		log.info("Processing result {}", name);

		JsonNode tree = client.execute(httpGet, response -> new ObjectMapper().readTree(response.getEntity().getContent()));

		return tree.get("Object").get("Content").asText();
	}

	private List<Row> parseResult(String table) {

		List<Row> result = new ArrayList<>();
		List<String> lines = table.lines().skip(6).collect(Collectors.toList());

		// Normalize header lines
		String[] gender = lines.remove(0).split(";");
		for (int i = 0; i < gender.length; i++) {
			gender[i] = gender[i].length() > 0 ? gender[i].substring(0, 1) : gender[i];
		}

		lines.remove(0);

		String[] age = lines.remove(0).split(";");

		for (int i = 0; i < age.length; i++) {
			age[i] = formatAge(age[i]);
		}

		for (String line : lines) {
			// End of data
			if (line.startsWith("___"))
				break;

			String[] split = line.split(";");

			for (int i = 0; i < split.length; i++) {

				if (!gender[i].equals("m") && !gender[i].equals("w"))
					continue;

				if (age[i].equals("Insgesamt"))
					continue;

				if (split[i].equals("-") | split[i].equals("."))
					continue;

				try {
					result.add(new Row(
							split[0],
							age[i],
							gender[i],
							Integer.parseInt(split[i])
					));
				} catch (NumberFormatException e) {
					log.warn("Invalid entry in {}: {}", split[1], split[i]);
				}
			}
		}

		return result;
	}

	/**
	 * One row in the csv.
	 *
	 * @param code     location code
	 * @param ageGroup age
	 * @param gender   gender
	 * @param n        amount
	 */
	private record Row(String code, String ageGroup, String gender, int n) {
	}


}
