package notebook;

import java.util.Arrays;

import jdk.jshell.JShell;



public class NoteBook {
    
    public static void main(String[] args) {
        try (JShell jshell = JShell.create()) {
//            String[] snippets = {
//                "int x = 100;" +
//                "int y = 20;",
//                "int z = x + y;",
//                "var s = \"Hello, \" + \"World!\";", // This should cause an error
//                "var p = (java.util.function.Predicate<String>)((String s) -> s.isEmpty());", // This should cause an error
//                "x = 200;", // Correct reassignment
//                "p.test(s)",
//                "p.negate().test(s)"
//            };
//            
//            var sourceCodeAnalysis = jshell.sourceCodeAnalysis();
//            var analyzeCompletion  = sourceCodeAnalysis.analyzeCompletion("var x = 100; var y = 100;");
//            
//            var anchor = new int[1];
//            var suggestions = sourceCodeAnalysis.completionSuggestions("System.", 3, anchor);
//            System.out.println(Arrays.toString(anchor));
//            for (int i = 0; i < suggestions.size(); i++) {
//                var suggestion = suggestions.get(i);
//                System.out.println(i + ": " + suggestion.continuation());
//            }
//            System.out.println();
//            
//            suggestions = sourceCodeAnalysis.completionSuggestions("System.", "System.".length(), anchor);
//            System.out.println(Arrays.toString(anchor));
//            for (int i = 0; i < suggestions.size(); i++) {
//                var suggestion = suggestions.get(i);
//                System.out.println(i + ": " + suggestion.continuation());
//            }
//            System.out.println();
//            
////            sourceCodeAnalysis.
//            
//            for (int i = 0; i < "10*5;".length(); i++) {
//                System.out.println(i + ": " + sourceCodeAnalysis.analyzeType("10*5;", i));
//            }
//            
//            System.out.println(analyzeCompletion);
//            
            
//            
//            for (String snippet : snippets) {
//                System.out.println("Executing: " + snippet);
//                jshell.eval(snippet).forEach(event -> {
//                    if (event.status() == Status.VALID && event.value() != null) {
//                        System.out.println("Result: " + event.value());
//                        System.out.println();
//                    } else if (event.status() == Status.REJECTED) {
////                        System.out.println("Error: " + event.snippet().source() + " -> " +  );
//                        System.out.println("Error: " + event.snippet().source() + " -> " + jshell.diagnostics(event.snippet()).findFirst().get().getMessage(Locale.CANADA));
//                        System.out.println();
//                    } else if (event.status() == Status.VALID) {
//                        System.out.println("Executed successfully without output.");
//                        System.out.println();
//                    }
//                });
//            }
//            
//            System.out.println("Variables after execution:");
//            jshell.variables().forEach(var -> {
//                var value = jshell.eval(var.name()).get(0).value();
//                System.out.println(var.name() + " = " + value);
//            });
//            String[] snippets = {
//                    "int x = 100;" + "int y = 20;",
//                    "System.out.println(\"x: \" + x);",
//                    "System.out.println(\"y: \" + y);"
//                };
//            
//            for (String snippet : snippets) {
//                jshell.eval(snippet).forEach(event -> {
//                    if (event.status() == Status.VALID && event.value() != null) {
//                        System.out.println("Result: " + event.value());
//                        System.out.println();
//                    } else if (event.status() == Status.REJECTED) {
//                        System.out.println("Error: " + event.snippet().source() + " -> " + jshell.diagnostics(event.snippet()).findFirst().get().getMessage(Locale.CANADA));
//                        System.out.println();
//                    } else if (event.status() == Status.VALID) {
//                        System.out.println("Executed successfully without output.");
//                        System.out.println();
//                    }
//                });
//            }
            
            var sourceCodeAnalysis = jshell.sourceCodeAnalysis();
            var code = """
                    var x = 10;
                    var y = 10;
                    System.out.println("x + y: " + (x + y));
                    """;
//          class C {
//          int y = 100;
//      }
//      void __run() {
//          var y = new C().y;
//          System.out.println(\"x + y: \" + (x + y));
//      }
//      System.out.println(__run());
//            var code = """
//                    var x = 10;
//                    var y = 100;
//                    System.out.println(\"x + y: \" + (x + y + z));
//                    """;
//            List<SnippetEvent> events = jshell.eval(code);
//            for (SnippetEvent event : events) {
//                Snippet snippet = event.snippet();
//                System.out.println("Snippet kind: " + snippet.kind());
//                System.out.println("Code executed: " + snippet.source());
//                if (event.value() != null) {
//                    System.out.println("Result: " + event.value());
//                }
//                System.out.println("Status: " + event.status());
//                System.out.println();
//            }
            
            while (!code.isBlank()) {
                var completion = sourceCodeAnalysis.analyzeCompletion(code);
                var source     = completion.source();
                var remaining  = completion.remaining();
                
                var snippets = sourceCodeAnalysis.sourceToSnippets(source);
                for (var snippet : snippets) {
                    System.out.println("Snippet kind  : " + snippet.kind());
                    System.out.println("Snippet source: " + snippet.source());
                    System.out.println("eval: " + jshell.eval(source));
                    System.out.println();
                }
                code = remaining;
            }
            
//            while (!code.isBlank()) {
//                var completion   = sourceCodeAnalysis.analyzeCompletion(code);
//                var source       = completion.source();
//                var remaining    = completion.remaining();
//                var completeness = completion.completeness();
//                System.out.println("source      : `" + source + "`");
//                System.out.println("remaining   : `" + remaining + "`");
//                System.out.println("completeness: `" + completeness + "`");
//                System.out.println();
                
//                code = remaining;
//            }
//            System.out.println("eval: " + jshell.eval(code));
            System.out.println("DONE!");
        }
    }
    
}
