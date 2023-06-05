package org.matsim.prepare.download;


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

@CommandLine.Command(name = "download-employment-statistics", description = "Download table 13312-01-05-4 (Erwerbstätige nach Wirtschaftszweigen) from regionalstatistik.de")
public class DownloadEmploymentStatistic implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(DownloadEmploymentStatistic.class);

	@CommandLine.Option(names = "--username", required = true, description = "Username for regionalstatistik.de")
	private String username;

	@CommandLine.Option(names = "--password", defaultValue = "${GENESIS_PW}", interactive = true, description = "Password for regionalstatistik.de")
	private String password;

	@CommandLine.Option(names = "--output", description = "Output csv", required = true)
	private Path output;

	@CommandLine.Mixin
	private CsvOptions csv;

	private RequestConfig config;

	public static void main(String[] args) {
		new DownloadEmploymentStatistic().execute(args);
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
			printer.printRecord("code", "employed");
			for (Row row : rows) {
				printer.printRecord(row.code, (int) row.total);
			}
		}

		return 0;
	}

	private String downloadResult(CloseableHttpClient client) throws IOException {

		HttpGet httpGet = new HttpGet(String.format("https://www.regionalstatistik.de/genesisws/rest/2020/data/table?username=%s&password=%s&name=%s&area=all&compress=true",
				username, password, "13312-01-05-4"));

		httpGet.setConfig(config);

		JsonNode tree = client.execute(httpGet, response -> new ObjectMapper().readTree(response.getEntity().getContent()));

		return tree.get("Object").get("Content").asText();
	}

	private List<Row> parseResult(String table) {

		List<Row> result = new ArrayList<>();
		List<String> lines = table.lines().skip(7).toList();

		for (String line : lines) {
			// End of data
			if (line.startsWith("___"))
				break;

			String[] split = line.split(";");

			try {
				result.add(new Row(split[1], Double.parseDouble(split[3].replace(",", ".")) * 1000));
			} catch (NumberFormatException e) {
				log.warn("Format error in {}", split[2]);
			}
		}

		return result;
	}

	private record Row(String code, double total) {
	}


}
