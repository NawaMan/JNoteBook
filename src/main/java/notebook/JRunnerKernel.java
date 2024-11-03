package notebook;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.tools.StandardJavaFileManager;

import jdk.jshell.JShell;
import jdk.jshell.JShellConsole;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.JShell.Builder;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;

public class JRunnerKernel implements Closeable {
    
    private final JShell jshell;
    
    public JRunnerKernel() {
        jshell = JShell.builder()
//                .in(InputStream in)
//                .out(PrintStream out)
//                .err(PrintStream err)
//                .console(JShellConsole console)
                .tempVariableNameGenerator(this::generateTempVariableName)
                .idGenerator(this::generateId)
//                .remoteVMOptions(String... options)
//                .compilerOptions(String... options)
//                .executionEngine(String executionControlSpec)
//                .executionEngine(ExecutionControlProvider executionControlProvider, Map<String,String> executionControlParameters)
                .fileManager(this::fileManager)
                .build();
    }
    
    private List<SnippetEvent> eval(String code) {
        return jshell.eval(code);
    }
    
    private String generateTempVariableName() {
        // When `null` is returned, a default one will be provided by JShell.
        return null;
    }
    
    private String generateId(Snippet snippet, Integer snippetKeyIndex) {
        return "" + snippetKeyIndex;
    }
    
    private StandardJavaFileManager fileManager(StandardJavaFileManager fileManager) {
        return fileManager;
    }
    
    @Override
    public void close() throws IOException {
        jshell.close();
    }
    
    private void evaluateSnippet(String code) {
        var output = new StringBuilder();
        List<SnippetEvent> events = eval(code);
        for (SnippetEvent e : events) {
            if (e.causeSnippet() == null) {  // Only display output if the snippet is original
                System.out.println("Snippet: " + e.snippet().source());
                if (e.value() != null && !e.value().isEmpty()) {
                    output.append("Result: " + e.value() + "\n");
                }
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
    
    public static void main(String[] args) throws IOException {
        var code1 = "var x = 10;";
        var code2 = "var y = 100;";
        var code3 = "System.out.println(\"x + y: \" + (x + y + z));";
        
        try (var kernel = new JRunnerKernel()) {
            kernel.evaluateSnippet(code1);
            kernel.evaluateSnippet(code2);
            kernel.evaluateSnippet(code3);
        }
    }
    
}
