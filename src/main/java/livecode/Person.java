package livecode;

import functionalj.lens.core.LensSpec;
import functionalj.lens.lenses.ObjectLensImpl;
import functionalj.lens.lenses.StringLens;
import functionalj.pipeable.Pipeable;
import functionalj.types.Generated;
import functionalj.types.IPostConstruct;
import functionalj.types.IStruct;
import functionalj.types.struct.generator.Getter;
import java.lang.Exception;
import java.lang.Object;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Generated(value = "FunctionalJ",date = "2024-10-19T00:05:59.797250905", comments = "javaelmexample.services.PersonService.PersonService.PersonModel")

@SuppressWarnings("all")

public record Person(String id, String firstName, String lastName, String nickName) implements PersonService.PersonModel,IStruct,Pipeable<Person> {
    
    public static final Person.PersonLens<Person> thePerson = new Person.PersonLens<>("thePerson", LensSpec.of(Person.class));
    public static final Person.PersonLens<Person> eachPerson = thePerson;
    
    public Person(String firstName, String lastName) {
        this(null, firstName, lastName, null);
    }
    public Person(String id, String firstName, String lastName, String nickName) {
        this.id = java.util.Optional.ofNullable(id).orElseGet(()->null);
        this.firstName = $utils.notNull(firstName);
        this.lastName = $utils.notNull(lastName);
        this.nickName = java.util.Optional.ofNullable(nickName).orElseGet(()->null);
//        if (this instanceof IPostConstruct) ((IPostConstruct)this).postConstruct();
    }
    
    public Person __data() throws Exception  {
        return this;
    }
    public String id() {
        return id;
    }
    public String firstName() {
        return firstName;
    }
    public String lastName() {
        return lastName;
    }
    public String nickName() {
        return nickName;
    }
    public Person withId(String id) {
        return new Person(id, firstName, lastName, nickName);
    }
    public Person withId(Supplier<String> id) {
        return new Person(id.get(), firstName, lastName, nickName);
    }
    public Person withId(Function<String, String> id) {
        return new Person(id.apply(this.id), firstName, lastName, nickName);
    }
    public Person withId(BiFunction<Person, String, String> id) {
        return new Person(id.apply(this, this.id), firstName, lastName, nickName);
    }
    public Person withFirstName(String firstName) {
        return new Person(id, firstName, lastName, nickName);
    }
    public Person withFirstName(Supplier<String> firstName) {
        return new Person(id, firstName.get(), lastName, nickName);
    }
    public Person withFirstName(Function<String, String> firstName) {
        return new Person(id, firstName.apply(this.firstName), lastName, nickName);
    }
    public Person withFirstName(BiFunction<Person, String, String> firstName) {
        return new Person(id, firstName.apply(this, this.firstName), lastName, nickName);
    }
    public Person withLastName(String lastName) {
        return new Person(id, firstName, lastName, nickName);
    }
    public Person withLastName(Supplier<String> lastName) {
        return new Person(id, firstName, lastName.get(), nickName);
    }
    public Person withLastName(Function<String, String> lastName) {
        return new Person(id, firstName, lastName.apply(this.lastName), nickName);
    }
    public Person withLastName(BiFunction<Person, String, String> lastName) {
        return new Person(id, firstName, lastName.apply(this, this.lastName), nickName);
    }
    public Person withNickName(String nickName) {
        return new Person(id, firstName, lastName, nickName);
    }
    public Person withNickName(Supplier<String> nickName) {
        return new Person(id, firstName, lastName, nickName.get());
    }
    public Person withNickName(Function<String, String> nickName) {
        return new Person(id, firstName, lastName, nickName.apply(this.nickName));
    }
    public Person withNickName(BiFunction<Person, String, String> nickName) {
        return new Person(id, firstName, lastName, nickName.apply(this, this.nickName));
    }
    public static Person fromMap(Map<String, ? extends Object> map) {
        Map<String, Getter> $schema = getStructSchema();
        Person obj = new Person(
                    (String)$utils.extractPropertyFromMap(Person.class, String.class, map, $schema, "id"),
                    (String)$utils.extractPropertyFromMap(Person.class, String.class, map, $schema, "firstName"),
                    (String)$utils.extractPropertyFromMap(Person.class, String.class, map, $schema, "lastName"),
                    (String)$utils.extractPropertyFromMap(Person.class, String.class, map, $schema, "nickName")
                );
        return obj;
    }
    public Map<String, Object> __toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", $utils.toMapValueObject(id));
        map.put("firstName", $utils.toMapValueObject(firstName));
        map.put("lastName", $utils.toMapValueObject(lastName));
        map.put("nickName", $utils.toMapValueObject(nickName));
        return map;
    }
    public Map<String, Getter> __getSchema() {
        return getStructSchema();
    }
    public static Map<String, Getter> getStructSchema() {
        java.util.Map<String, functionalj.types.struct.generator.Getter> map = new java.util.HashMap<>();
        map.put("id", new functionalj.types.struct.generator.Getter("id", new functionalj.types.Type("java.lang", null, "String", java.util.Collections.emptyList()), true, functionalj.types.DefaultValue.NULL));
        map.put("firstName", new functionalj.types.struct.generator.Getter("firstName", new functionalj.types.Type("java.lang", null, "String", java.util.Collections.emptyList()), false, functionalj.types.DefaultValue.REQUIRED));
        map.put("lastName", new functionalj.types.struct.generator.Getter("lastName", new functionalj.types.Type("java.lang", null, "String", java.util.Collections.emptyList()), false, functionalj.types.DefaultValue.REQUIRED));
        map.put("nickName", new functionalj.types.struct.generator.Getter("nickName", new functionalj.types.Type("java.lang", null, "String", java.util.Collections.emptyList()), true, functionalj.types.DefaultValue.NULL));
        map.put("cape", new functionalj.types.struct.generator.Getter("cape", new functionalj.types.Type("javaelmexample.services", null, "Cape", java.util.Collections.emptyList()), false, functionalj.types.DefaultValue.REQUIRED));
        return map;
    }
    public String toString() {
        return "Person[" + "id: " + id() + ", " + "firstName: " + firstName() + ", " + "lastName: " + lastName() + ", " + "nickName: " + nickName() + "]";
    }
    public int hashCode() {
        return toString().hashCode();
    }
    public boolean equals(Object another) {
        return (another == this) || ((another != null) && (getClass().equals(another.getClass())) && java.util.Objects.equals(toString(), another.toString()));
    }
    
    public static class PersonLens<HOST> extends ObjectLensImpl<HOST, Person> {
        
        public final StringLens<HOST> id = createSubLens("id", Person::id, Person::withId, StringLens::of);
        public final StringLens<HOST> firstName = createSubLens("firstName", Person::firstName, Person::withFirstName, StringLens::of);
        public final StringLens<HOST> lastName = createSubLens("lastName", Person::lastName, Person::withLastName, StringLens::of);
        public final StringLens<HOST> nickName = createSubLens("nickName", Person::nickName, Person::withNickName, StringLens::of);
        
        public PersonLens(String name, LensSpec<HOST, Person> spec) {
            super(name, spec);
        }
        
    }
    public static final class Builder {
        
        public final PersonBuilder_withoutFirstName id(String id) {
            return (String firstName)->{
            return (String lastName)->{
            return (String nickName)->{
            return ()->{
                return new Person(
                    id,
                    firstName,
                    lastName,
                    nickName
                );
            };
            };
            };
            };
        }
        
        public static interface PersonBuilder_withoutFirstName {
            
            public PersonBuilder_withoutLastName firstName(String firstName);
            
        }
        public static interface PersonBuilder_withoutLastName {
            
            public PersonBuilder_withoutNickName lastName(String lastName);
            
        }
        public static interface PersonBuilder_withoutNickName {
            
            public PersonBuilder_ready nickName(String nickName);
            
        }
        public static interface PersonBuilder_ready {
            
            public Person build();
            
        }
        
        
        public PersonBuilder_withoutLastName firstName(String firstName){
            return id(null).firstName(firstName);
        }
        
    }
    
}

