package notebook;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import java.util.List;

public class JShellExample {
    public static void main(String[] args) {
        // Create a JShell instance
        try (JShell jshell = JShell.create()) {
            // Define the snippets of code
            String code1 = "var x = 10;";
            String code2 = "var y = 100;";
//            String code3 = "System.out.println(\"x + y: \" + (x + y + z));";
            String code3 = "System.out.println(functionalj.lens.Access.theInteger);";
            String code4 = "System.out.println(defaultj.annotations.Nullable.class);";
            
            
            
            // Evaluate the snippets one by one
            evaluateSnippet(jshell, code1);
            evaluateSnippet(jshell, code2);
            evaluateSnippet(jshell, code3);
            evaluateSnippet(jshell, code4);
        }
    }
    
    private static void evaluateSnippet(JShell jshell, String code) {
        var output = new StringBuilder();
        List<SnippetEvent> events = jshell.eval(code);
        
        jshell.addToClasspath("/home/nawa/.m2/repository/io/functionalj/functionalj-all/1.0.16/functionalj-all-1.0.16.jar:/home/nawa/.m2/repository/io/functionalj/functionalj-core/1.0.16/functionalj-core-1.0.16.jar:/home/nawa/.m2/repository/io/functionalj/functionalj-types/1.0.16/functionalj-types-1.0.16.jar:/home/nawa/.m2/repository/io/functionalj/functionalj-elm/1.0.16/functionalj-elm-1.0.16.jar:/home/nawa/.m2/repository/io/functionalj/functionalj-store/1.0.16/functionalj-store-1.0.16.jar:/home/nawa/.m2/repository/io/defaultj/defaultj-api/2.2.0.5/defaultj-api-2.2.0.5.jar:/home/nawa/.m2/repository/io/defaultj/defaultj-annotations/2.2.0.5/defaultj-annotations-2.2.0.5.jar:/home/nawa/.m2/repository/io/defaultj/defaultj-core/2.2.0.5/defaultj-core-2.2.0.5.jar:/home/nawa/.m2/repository/io/nullablej/nullablej/4.0.6.0/nullablej-4.0.6.0.jar");
        
        for (SnippetEvent e : events) {
            if (e.causeSnippet() == null) {  // Only display output if the snippet is original
//                System.out.println("Snippet: " + e.snippet().source());
//                if (e.value() != null && !e.value().isEmpty()) {
//                    output.append("Result: " + e.value() + "\n");
//                }
                if (e.exception() != null) {
                    output.append("Exception: " + e.exception() + "\n");
                }
                if (e.status() == jdk.jshell.Snippet.Status.REJECTED) {
                    output.append("Error: \n");
                    jshell.diagnostics(e.snippet()).forEach(d -> {
                        output.append(d.getMessage(null) + "\n");
                        output.append("    " + e.snippet().source() + "\n");
                        output.append(" ".repeat("    ".length() + (int)d.getPosition()) + "^" + "\n");
                        output.append("Pos  : " + d.getPosition() + "\n");
                        output.append("Start: " + d.getStartPosition() + "\n");
                        output.append("End  : " + d.getEndPosition() + "\n");
                    });
                }
            }
        }
        System.out.println(output);
    }
}
