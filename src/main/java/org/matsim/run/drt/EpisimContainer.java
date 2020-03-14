package org.matsim.run.drt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;

import java.util.*;

class EpisimContainer<T> {
        private final Id<T> containerId;

        private Map<Id<Person>, EpisimPerson> persons = new LinkedHashMap<>();
        private List<EpisimPerson> personsAsList = new ArrayList<>();
        // I need them as list, to make the "randomly draw persons within container" a bit cheaper.  kai, mar'20
        // one might be able to combine these two, e.g. by using binarySearch.  kai, mar'20

        private Map<Id<Person>,Double> containerEnterTimes = new LinkedHashMap<>();

        EpisimContainer( Id<T> containerId ) {
                this.containerId = containerId;
        }
        void addPerson( EpisimPerson person, double now ) {
                persons.put( person.getPersonId(), person );
                personsAsList.add( person );
                containerEnterTimes.put(  person.getPersonId(), now );
        }
        /** @noinspection UnusedReturnValue*/
        EpisimPerson removePerson( Id<Person> personId ) {
                containerEnterTimes.remove( personId );
                EpisimPerson personWrapper = persons.remove( personId );
                boolean wasRemoved = personsAsList.remove( personWrapper );
                Gbl.assertIf( wasRemoved );
                return personWrapper;
        }
        Id<T> getContainerId(){
                return containerId;
        }
//                Map<Id<Person>,PersonWrapper> getPersons(){
//                        return Collections.unmodifiableMap( persons );
//                }
//                Map<Id<Person>,Double> getContainerEnteringTimes(){
//                        return Collections.unmodifiableMap( containerEnterTimes );
//                }
        // we are not giving out internals of data structures! kai, mar'20

        void clearPersons() {
                this.persons.clear();
                this.personsAsList.clear();
                this.containerEnterTimes.clear();
        }
        Double getContainerEnteringTime( Id<Person> personId ){
                return containerEnterTimes.get( personId );
        }
        EpisimPerson getPerson( Id<Person> personId ){
                return persons.get( personId );
        }
        public List<EpisimPerson> getPersons(){
                return Collections.unmodifiableList( personsAsList );
        }
}
