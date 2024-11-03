package nawaman.jcompiler;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility class to run shell commands.
 */
public class ShellProcess {

    /**
     * The output of a shell command.
     */
    public static record Output(
            int    exitCode,
            String output,
            String error) {
        
    }
    
    /**
     * Runs a shell command and returns the output.
     * 
     * @param  commands  the command and its arguments
     * @return           the output of the command
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    public Output run(String ... commands) throws IOException, InterruptedException {
        var processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        
        var process = processBuilder.start();
        
        var outputStream = new ByteArrayOutputStream();
        var errorStream  = new ByteArrayOutputStream();
        try (var inputStream = process.getInputStream()) {
            inputStream.transferTo(outputStream);
        }
        try (var inputStream = process.getErrorStream()) {
            inputStream.transferTo(errorStream);
        }
        var output   = outputStream.toString(UTF_8);
        var error    = errorStream.toString(UTF_8);
        int exitCode = process.waitFor();
        
        return new Output(exitCode, output, error);
    }
    
}
