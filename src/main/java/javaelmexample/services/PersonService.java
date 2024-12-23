package javaelmexample.services;

import static java.lang.String.format;
import static nullablej.nullable.Nullable.nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import functionalj.list.FuncList;
import functionalj.promise.Promise;
import functionalj.stream.StreamPlus;
import functionalj.types.Choice;
import functionalj.types.Nullable;
import functionalj.types.Required;
import functionalj.types.Struct;
import javaelmexample.server.RestData;
import javaelmexample.server.RestService;
import javaelmexample.server.WithDemoMode;

/**
 * This is an example Rest service that deal with a Person objects.
 **/
public class PersonService implements RestService<Person>, WithDemoMode {
    
    @Struct
    static interface PersonModel extends RestData {
        @Nullable String id();
        @Required String firstName();
        @Required String lastName();
        @Nullable String nickName();
        @Required Cape   cape();
    }
    
    @Choice
    static interface CapeModel {
        void Color(String color);
        void None();
    }
    
    
    private final Map<String, Person> persons  = new ConcurrentHashMap<>();
    
    public Class<Person> dataClass() {
        return Person.class;
    }
    
    private final Map<String, Person> snapshot = new ConcurrentHashMap<>();
    
    @Override
    public Promise<Person> get(String id) {
        var person = persons.get(id);
        return Promise.ofValue(person);
    }
    
    @Override
    public Promise<FuncList<Person>> list() {
        var streamPlus = StreamPlus.from(persons.values().stream());
        var funcList   = streamPlus.toFuncList();
        return Promise.ofValue(funcList);
    }
    
    @Override
    public Promise<Person> post(Person person) {
        if (person == null) {
            return Promise.ofValue(null);
        }
        
        var newPersonId = nullable(person.id).orElseGet(()->UUID.randomUUID().toString());
        var newPerson   = person.withId(newPersonId);
        persons.put(newPersonId, newPerson);
        return Promise.ofValue(newPerson);
    }
    
    @Override
    public Promise<Person> put(String id, Person person) {
        if (person == null) {
            return Promise.ofValue(null);
        }
        if (!Objects.equals(id, person.id)) {
            var errorMessage = format("ID mismatch: id=[%s] vs item.id=[%s]", id, person.id);
            throw new IllegalArgumentException(errorMessage);
        }
        
        persons.put(person.id, person);
        return Promise.ofValue(person);
    }
    
    @Override
    public Promise<Person> delete(String id) {
        var person = persons.remove(id);
        return Promise.ofValue(person);
    }
    
    //== Demo mode ==
    
    @Override
    public void takeSnapshot() {
        snapshot.putAll(persons);
    }
    
    @Override
    public void resetToSnapshot() {
        persons.clear();
        persons.putAll(snapshot);
    }
    
}
