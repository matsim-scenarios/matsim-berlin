/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis.emissions;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class RunOfflineAirPollutionAnalysisByEngineInformationTest{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	// to run this test an environment variable needs to be set in your IDE and on the server...
	// FIXME
	@Ignore
	@Test
	public final void test1() {
		try {
			String runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-0.1pct/output-berlin-v5.4-0.1pct/";
			String runId = "berlin-v5.4-0.1pct";
			String hbefaFileWarm = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/f5b276f41a0531ed740a81f4615ec00f4ff7a28d.enc";
			String hbefaFileCold = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/b63f949211b7c93776cdce8a7600eff4e36460c8.enc";
			
			RunOfflineAirPollutionAnalysisByEngineInformation analysis = new RunOfflineAirPollutionAnalysisByEngineInformation(runDirectory, runId, hbefaFileWarm, hbefaFileCold, utils.getOutputDirectory());
			analysis.run();
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

}
