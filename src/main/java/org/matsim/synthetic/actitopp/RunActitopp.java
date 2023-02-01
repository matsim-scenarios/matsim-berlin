package org.matsim.synthetic.actitopp;

import edu.kit.ifv.mobitopp.actitopp.*;
import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

@CommandLine.Command(
		name = "actitopp",
		description = "Run actiTopp activity generation"
)
public class RunActitopp implements MATSimAppCommand {

	private static ModelFileBase fileBase = new ModelFileBase();
	private static RNGHelper randomgenerator = new RNGHelper(1234);
	private static DebugLoggers debugloggers = new DebugLoggers();

	public static void main(String[] args) throws InvalidPatternException {

		//Configuration.parameterset = "mopv14_nopkwhh";

		ActitoppPerson testperson = new ActitoppPerson(
				10,    // PersIndex
				0,        // number of children 0-10
				1,        // number of children below 18
				55,    // age
				1,        // employment status
				1,        // gender
				2,        // area of living
				2            // number of cars in household
		);

		testperson.generateSchedule(fileBase, randomgenerator);

		testperson.getWeekPattern().printAllActivitiesList();


	}


	@Override
	public Integer call() throws Exception {
		return null;
	}
}
