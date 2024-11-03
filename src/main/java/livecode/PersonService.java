package livecode;

import functionalj.types.Nullable;
import functionalj.types.Required;
import javaelmexample.server.RestData;

public class PersonService {
    
    static interface PersonModel extends RestData {
        @Nullable String id();
        @Required String firstName();
        @Required String lastName();
        @Nullable String nickName();
    }
    
}
