package livecode;

public class PlayWithPerson {
    
    public static void main(String[] args) {
        System.out.println(generateGreeting(new Person("1", "Bruce", "Wayne", "Batman")));
        System.out.println(generateGreeting(new Person("2", "Clark", "Kent", "Superman")));
    }
    
    static String generateGreeting(Object obj) {
        return switch (obj) {
            case Person(String id, String firstName, String lastName, String nickName) 
                 when "Bruce".equals(firstName) && "Wayne".equals(lastName)
                 ->   "Hello, Batman! Your ID is %s.".formatted(id);
            case Person(String id, String firstName, String lastName, String nickName)
                 -> "Hello, %s %s (a.k.a. %s)! Your ID is %s.".formatted(firstName, lastName, nickName, id);
            default -> "Unknown person.";
        };
    }
    
}
