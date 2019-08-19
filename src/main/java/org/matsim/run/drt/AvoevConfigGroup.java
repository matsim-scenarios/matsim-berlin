package org.matsim.run.drt;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

public class AvoevConfigGroup extends ReflectiveConfigGroup {
	private static final String NAME = "avoev" ;

	private static final Logger log = Logger.getLogger(AvoevConfigGroup.class) ;

	public AvoevConfigGroup( ){
		super( NAME );
	}

	private String drtServiceAreaShapeFileName ;

	public String getDrtServiceAreaShapeFileName(){
		// yyyy needs to be moved to multimodal drt config group of drt contrib.  kai, jul'19
		return drtServiceAreaShapeFileName;
	}

	public void setDrtServiceAreaShapeFileName( String drtServiceAreaShapeFileName ){
		// yyyy needs to be moved to multimodal drt config group of drt contrib.  kai, jul'19
		this.drtServiceAreaShapeFileName = drtServiceAreaShapeFileName;
	}
}
