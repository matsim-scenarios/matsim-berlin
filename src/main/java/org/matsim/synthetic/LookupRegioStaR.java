package org.matsim.synthetic;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(
		name = "lookup-regiostar",
		description = "RegioStaR7 lookup using gemeinde id."
)
public class LookupRegioStaR implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(LookupRegioStaR.class);

	@CommandLine.Option(names = "--input", required = true, description = "Input Population")
	private Path input;

	@CommandLine.Option(names = "--xls", required = true, description = "Path to RegioStar Excel sheet")
	private Path regiostar;

	@CommandLine.Option(names = "--output", required = true, description = "Output Population")
	private Path output;

	private Int2IntMap lookup;

	public static void main(String[] args) {
		new LookupRegioStaR().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(regiostar)) {
			log.error("File {} does not exist.", regiostar);
			return 2;
		}

		lookup = readXls();
		log.info("Read {} entries from xls.", lookup.size());

		Population population = PopulationUtils.readPopulation(input.toString());

		population.getPersons().values().forEach(this::run);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	@Override
	public void run(Person person) {
		int gem = (int) person.getAttributes().getAttribute(Attributes.GEM);

		if (!lookup.containsKey(gem))
			log.warn("Unknown Gemeinde {}", gem);

		person.getAttributes().putAttribute(Attributes.RegioStaR7, lookup.get(gem));
	}

	private Int2IntMap readXls() throws IOException, InvalidFormatException {

		Int2IntMap result = new Int2IntOpenHashMap();

		try (XSSFWorkbook workbook = new XSSFWorkbook(regiostar.toFile())) {

			XSSFSheet sheet = workbook.getSheet("ReferenzGebietsstand2020");

			XSSFRow first = sheet.getRow(0);
			Map<String, Integer> header = new HashMap<>();
			for (Cell cell : first) {
				header.put(cell.getStringCellValue(), cell.getColumnIndex());
			}

			for (int i = 1; i < sheet.getLastRowNum(); i++) {
				XSSFRow row = sheet.getRow(i);
				result.put(
						(int) row.getCell(header.get("gem_20")).getNumericCellValue(),
						(int) row.getCell(header.get(Attributes.RegioStaR7)).getNumericCellValue()
				);
			}
		}

		return result;
	}

}
