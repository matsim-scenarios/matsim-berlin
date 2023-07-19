package org.matsim.policies;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobilityBudgetEventHandler implements PersonDepartureEventHandler, AfterMobsimListener, PersonStuckEventHandler {


    private final Map<Id<Person>, Double> person2MobilityBudget;
    private final Map<Id<Person>, Double> currentIterationMobilityBudget = new HashMap<>();
    private final List<Id<Person>> personWhoAreStuck= new ArrayList<>();

    public MobilityBudgetEventHandler(Map<Id<Person>, Double> personsEligibleForMobilityBudget2MoneyValue) {
        this.person2MobilityBudget = personsEligibleForMobilityBudget2MoneyValue;
    }

    @Override
    public void reset(int iteration) {
        currentIterationMobilityBudget.clear();
        currentIterationMobilityBudget.putAll(person2MobilityBudget);
        personWhoAreStuck.clear();
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        Id<Person> personId = personDepartureEvent.getPersonId();
        if (this.currentIterationMobilityBudget.containsKey(personId) && personDepartureEvent.getLegMode().equals(TransportMode.car)) {
            this.currentIterationMobilityBudget.replace(personId, 0.0);
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {

        double totalSumMobilityBudget = 0.;
        for (Map.Entry<Id<Person>, Double> idDoubleEntry : currentIterationMobilityBudget.entrySet()) {
            Id<Person> person = idDoubleEntry.getKey();
            Double mobilityBudget = idDoubleEntry.getValue();
            event.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, mobilityBudget, "mobilityBudget", null));
            totalSumMobilityBudget = totalSumMobilityBudget + mobilityBudget;
        }

        for (Id<Person> personId: personWhoAreStuck) {
            event.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, personId, -10000, "punishmentForBeingStuck", null));
        }
    }

    /*
    This is here to add an extra punishment to people who get stuck and benefit from getting the mob budget and thus obtaining a high score
     */
    @Override
    public void handleEvent(PersonStuckEvent personStuckEvent) {
        personWhoAreStuck.add(personStuckEvent.getPersonId());

    }
}