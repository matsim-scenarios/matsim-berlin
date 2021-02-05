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

package org.matsim.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class RunOfflineNoiseAnalysisTest{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	// to run this test an environment variable needs to be set in your IDE and on the server...
	@Test
	public final void test1() {
		try {
			String runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-0.1pct/output-berlin-v5.4-0.1pct/";
			String runId = "berlin-v5.4-0.1pct";
			
			RunOfflineNoiseAnalysis analysis = new RunOfflineNoiseAnalysis(runDirectory, runId, utils.getOutputDirectory());
			analysis.run();
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

}
