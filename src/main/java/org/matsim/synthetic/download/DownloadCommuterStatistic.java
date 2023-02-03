package org.matsim.synthetic.download;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "download-commuter", description = "Download commuter statistic for all Gemeinden.")
public class DownloadCommuterStatistic implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(DownloadCommuterStatistic.class);

	@CommandLine.Option(names = "--username", required = true, description = "Username for regionalstatistik.de")
	private String username;

	@CommandLine.Option(names = "--password", defaultValue = "${GENESIS_PW}", interactive = true, description = "Password for regionalstatistik.de")
	private String password;

	@CommandLine.Option(names = "--gemeinden", required = true, description = "Path to gemeinden CSV.")
	private Path gemeinden;

	@CommandLine.Option(names = "--output", description = "Output csv", required = true)
	private Path output;

	@CommandLine.Mixin
	private CsvOptions csv;

	private RequestConfig config;
	private ObjectMapper mapper;

	private Path tmp;

	public static void main(String[] args) {
		new DownloadCommuterStatistic().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		config = RequestConfig.custom()
				.setConnectionRequestTimeout(5, TimeUnit.MINUTES)
				.setResponseTimeout(5, TimeUnit.MINUTES)
				.build();

		mapper = new ObjectMapper();

		tmp = Path.of(output.toAbsolutePath() + "_tmp");
		Files.createDirectories(tmp);

		if (password.isBlank()) {
			log.error("No password given, either set GENESIS_PW or use --password to enter it.");
			return 1;
		}

		List<OD> results = new ArrayList<>();

		List<CSVRecord> records;
		try (CSVParser parser = csv.createParser(gemeinden)) {
			records = parser.getRecords();
		}

		try (CloseableHttpClient client = HttpClients.createDefault()) {

			for (CSVRecord gemeinde : records) {

				String result;
				try {
					result = downloadGemeinde(client, gemeinde);
				} catch (IOException e) {
					log.error("Error retrieving stats", e);
					Thread.sleep(15_000);
					continue;
				}

				List<OD> parsed = parseResult(result, gemeinde);

				log.info("Parsed {} relations", parsed.size());

				results.addAll(parsed);
			}
		}


		try (CSVPrinter printer = csv.createPrinter(output)) {

			printer.printRecord("from", "to", "n");

			for (OD od : results) {

				// commuter codes (pendler) are prefixed with a P which we don't need
				printer.print(od.origin.replace("P", ""));
				printer.print(od.destination);
				printer.print(od.n);
				printer.println();

			}
		}

		return 0;
	}

	private String downloadGemeinde(CloseableHttpClient client, CSVRecord gemeinde) throws IOException, InterruptedException {

		String code = gemeinde.get("code");
		Path path = tmp.resolve(code + ".csv");

		// Return cached result
		if (Files.exists(path)) {
			return Files.readString(path);
		}

		HttpGet httpGet = new HttpGet(String.format("https://www.regionalstatistik.de/genesisws/rest/2020/data/table?username=%s&password=%s&name=19321-Z-21&area=all&compress=true&regionalvariable=PGEMEIN&regionalkey=%s",
				username, password, code));
		httpGet.setConfig(config);

		log.info("Processing {}: {}", code, gemeinde.get("name"));

		JsonNode tree = client.execute(httpGet, response -> mapper.readTree(response.getEntity().getContent()));

		String result = tree.get("Object").get("Content").asText();

		Files.writeString(path, result, StandardCharsets.UTF_8, StandardOpenOption.CREATE);

		Thread.sleep(5_000);

		return result;
	}

	private List<OD> parseResult(String table, CSVRecord gemeinde) {

		List<String> lines = table.lines().toList();

		List<OD> result = new ArrayList<>();
		String from = gemeinde.get("code").intern();

		boolean parse = false;
		for (String line : lines) {

			// Find beginning of data
			if (line.startsWith(";;Anzahl")) {
				parse = true;
				continue;
			}

			if (!parse)
				continue;

			// End of data
			if (line.startsWith("___"))
				break;

			String[] split = line.split(";");
			try {
				result.add(new OD(from, split[0].intern(), Integer.parseInt(split[2])));
			} catch (NumberFormatException e) {
				// Ignore parse error
			}

		}

		return result;

	}

	private record OD(String origin, String destination, int n) {
	}

}
