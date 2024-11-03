package notebook;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import jdk.jshell.JShell;
import jdk.jshell.tool.JavaShellToolBuilder;

public class JShellKinds {
    public static void main(String[] args) {
        var output = System.out;// new StringBuilder();
        var cmdout = new ByteArrayOutputStream();
        try (JShell jshell = JShell.builder().out(new PrintStream(output)).build()) {
            var code = """
                    class Test {
                        String things = doThing();
                    }
                    System.out.println("Yo!");
                    """;

//            String doThing() {
//                return "Hello World!";
//            }
            var sourceCodeAnalysis = jshell.sourceCodeAnalysis();
            while (!code.isBlank()) {
                var completion   = sourceCodeAnalysis.analyzeCompletion(code);
                var source       = completion.source();
                var remaining    = completion.remaining();
                System.out.println("Source: `" + source + "`");
                System.out.println("Completeness: `" + completion.completeness() + "`");
                var snippets = sourceCodeAnalysis.sourceToSnippets(source);
                snippets
                .forEach(snippet -> {
                    System.out.println("Kind:  " + snippet.kind());
                });
                
                var events = jshell.eval(source);
                System.out.println("Events:  " + events);
                events.forEach(event -> {
                    var e = event;
                    output.println("e.previousStatus   : " + e.previousStatus());
                    output.println("e.isSignatureChange: " + e.isSignatureChange());
                    output.println("e.status           : " + e.status());
                    
                    if (e.snippet() instanceof jdk.jshell.TypeDeclSnippet typeSnippet) {
                        output.println(typeSnippet.kind());
                    }
                    
//                    
//                    
//                    if (e.causeSnippet() == null) {  // Only display output if the snippet is original
//                        System.out.println("Snippet: " + e.snippet().source());
//                        if (e.value() != null && !e.value().isEmpty()) {
//                            output.append("Result: " + e.value() + "\n");
//                        }
//                        if (e.exception() != null) {
//                            output.append("Exception: " + e.exception() + "\n");
//                        }
//                        if (e.status() == jdk.jshell.Snippet.Status.REJECTED) {
//                            output.append("Error: \n");
//                            jshell.diagnostics(e.snippet()).forEach(d -> {
//                                output.append(d.getMessage(null) + "\n");
//                                output.append("    " + e.snippet().source() + "\n");
//                                output.append(" ".repeat("    ".length() + (int)d.getPosition()) + "^" + "\n");
//                                output.append("Pos  : " + d.getPosition() + "\n");
//                                output.append("Start: " + d.getStartPosition() + "\n");
//                                output.append("End  : " + d.getEndPosition() + "\n");
//                            });
//                        }
//                    }
//                    if (event.status() == Snippet.Status.VALID) {
//                        System.out.println("Snippet evaluated successfully.");
//                    } else if (event.status() == Snippet.Status.REJECTED) {
//                        System.out.println("Error: Snippet has errors and cannot be executed. event: " + event);
////                        System.out.println("Diagnostic: " + event.diagnostics());
////                        jshell.diagnostics(event.snippet()).findFirst().get().message());
//                    }
                });
                
                System.out.println();
                
                code = remaining;
            }
        }
        
        System.out.println(cmdout.toString());
    }
}
