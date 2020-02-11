/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.run.drt.ptRoutingModes;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author vsp-gleich
 */
public class PtIntermodalRoutingModesConfigGroup extends ReflectiveConfigGroup {

	private static final Logger log = Logger.getLogger(PtIntermodalRoutingModesConfigGroup.class);
    public static final String GROUP = "ptIntermodalRoutingModes";

    private final Map<String, PtIntermodalRoutingModeParameterSet> routingMode2PtIntermodalRoutingModeParameterSet = new HashMap<>();

    public PtIntermodalRoutingModesConfigGroup() {
        super(GROUP);
    }
    
    @Override
    public ConfigGroup createParameterSet(String type) {
        if (PtIntermodalRoutingModeParameterSet.TYPE.equals(type)) {
            return new PtIntermodalRoutingModeParameterSet();
        } else {
            throw new IllegalArgumentException("Unsupported parameterset-type: " + type);
        }
    }

    @Override
    public void addParameterSet(ConfigGroup set) {
        if (set instanceof PtIntermodalRoutingModeParameterSet) {
            addPtIntermodalRoutingModeParameterSet((PtIntermodalRoutingModeParameterSet) set);
        } else {
            throw new IllegalArgumentException("Unsupported parameterset: " + set.getClass().getName());
        }
    }
    
    public void addPtIntermodalRoutingModeParameterSet(PtIntermodalRoutingModeParameterSet paramSet) {
        this.routingMode2PtIntermodalRoutingModeParameterSet.put(paramSet.getRoutingMode(), paramSet);
        super.addParameterSet(paramSet);
    }

    public PtIntermodalRoutingModeParameterSet getPtIntermodalRoutingModeParameterSet(String routingMode) {
        return this.routingMode2PtIntermodalRoutingModeParameterSet.get(routingMode);
    }

    public Collection<PtIntermodalRoutingModeParameterSet> getPtIntermodalRoutingModeParameterSets() {
        return this.routingMode2PtIntermodalRoutingModeParameterSet.values();
    }

    public static class PtIntermodalRoutingModeParameterSet extends ReflectiveConfigGroup {

        private static final String TYPE = "ptIntermodalRoutingMode";

        private static final String PARAM_ROUTING_MODE = "routingMode";
        private static final String PARAM_DELEGATE_MODE = "delegateMode";

        private String routingMode;
        private String delegateMode;
        private List<PersonAttribute2ValuePair> personAttribute2ValuePairs = new ArrayList<>();

        public PtIntermodalRoutingModeParameterSet() {
            super(TYPE);
        }

        @StringGetter(PARAM_ROUTING_MODE)
        public String getRoutingMode() {
            return routingMode;
        }

        @StringSetter(PARAM_ROUTING_MODE)
        public void setRoutingMode(String routingMode) {
            this.routingMode = routingMode;
        }
        
        @StringGetter(PARAM_DELEGATE_MODE)
        public String getDelegateMode() {
            return delegateMode;
        }

        @StringSetter(PARAM_DELEGATE_MODE)
        public void setDelegateMode(String delegateMode) {
            this.delegateMode = delegateMode;
        }
        
        @Override
        public ConfigGroup createParameterSet(String type) {
            if (PersonAttribute2ValuePair.SET_TYPE.equals(type)) {
                return new PersonAttribute2ValuePair();
            } else {
                throw new IllegalArgumentException("Unsupported parameterset-type: " + type);
            }
        }

        @Override
        public void addParameterSet(ConfigGroup set) {
            if (set instanceof PersonAttribute2ValuePair) {
                addPersonAttribute2ValuePair((PersonAttribute2ValuePair) set);
            } else {
                throw new IllegalArgumentException("Unsupported parameterset: " + set.getClass().getName());
            }
        }
        
        public void addPersonAttribute2ValuePair(PersonAttribute2ValuePair paramSet) {
            this.personAttribute2ValuePairs.add(paramSet);
            super.addParameterSet(paramSet);
        }

        public List<PersonAttribute2ValuePair> getPersonAttribute2ValuePairs() {
            return this.personAttribute2ValuePairs;
        }

        @Override
        public Map<String, String> getComments() {
            Map<String, String> map = super.getComments();
            map.put(PARAM_ROUTING_MODE, "Routing mode to be installed.");
            map.put(PARAM_DELEGATE_MODE, "The routing mode to which is delegated (typically TransportMode.pt).");
            return map;
        }
    }
    
	public static class PersonAttribute2ValuePair extends ReflectiveConfigGroup {

		public final static String SET_TYPE = "personAttribute2ValuePair";
        private static final String PARAM_PERSON_FILTER_ATTRIBUTE = "personFilterAttribute";
        private static final String PARAM_PERSON_FILTER_VALUE = "personFilterValue";
        
        private String personFilterAttribute;
        private String personFilterValue;
		
		public PersonAttribute2ValuePair() {
			super(SET_TYPE);
		}
		
        @StringGetter(PARAM_PERSON_FILTER_ATTRIBUTE)
        public String getPersonFilterAttribute() {
            return this.personFilterAttribute;
        }

        @StringSetter(PARAM_PERSON_FILTER_ATTRIBUTE)
        public void setPersonFilterAttribute(String personFilterAttribute) {
            this.personFilterAttribute = personFilterAttribute;
        }
		
        @StringGetter(PARAM_PERSON_FILTER_VALUE)
        public String getPersonFilterValue() {
            return this.personFilterValue;
        }

        @StringSetter(PARAM_PERSON_FILTER_VALUE)
        public void setPersonFilterValue(String personFilterValue) {
            this.personFilterValue = personFilterValue;
        }
		
        @Override
        public Map<String, String> getComments() {
            Map<String, String> map = super.getComments();
            map.put(PARAM_PERSON_FILTER_ATTRIBUTE, "Name of the person attribute added before calling the routing module of the delegate mode. The attribute is removed immediately after routing. Should be the same attribute as in the IntermodalAccessEgressParameterSet of the SwissRailRaptorConfigGroup.");
            map.put(PARAM_PERSON_FILTER_VALUE, "Value of the person attribute added before calling the routing module of the delegate mode.");
          return map;
        }
	}
}
