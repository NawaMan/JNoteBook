package notebook;

import static functionalj.lens.Access.theString;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import functionalj.function.Func1;

public class MavenRunner {
    
    public static void main(String[] args) {
        var mvnPath = getMavenRepositoryPath();
        System.out.println(mvnPath);
    }
    
    public static String getMavenRepositoryPath() {
        var commands = new String[] { "mvn", "help:evaluate", "-B", "-Dexpression=settings.localRepository" };
        var mvnPath  = runCommand(commands, lines -> {
            return lines
                    .filter(theString.trim().thatStartsWith("[").negate())
                    .collect(joining("\n"));
        });
        if (mvnPath.exitCode != 0) {
            throw new RuntimeException("ExistCode: " + mvnPath.exitCode);
        }
        
        return mvnPath.result;
    }
    
    static record CommandResult<T>(T result, int exitCode) {}
    
    public static <T> CommandResult<T> runCommand(String[] cmds, Func1<Stream<String>, T> processor) {
        var processBuilder = new ProcessBuilder(cmds);
        processBuilder.redirectErrorStream(true); // Redirect errors to standard output
        
        try {
            var process = processBuilder.start();
            var result = (T)null;
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                result = processor.apply(reader.lines());
            }
            int exitCode = process.waitFor();
            return new CommandResult<>(result, exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
