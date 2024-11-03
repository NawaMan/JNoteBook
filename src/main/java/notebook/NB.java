package notebook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class NB {
    public static void main(String[] args) {
        ProcessBuilder processBuilder = new ProcessBuilder("jshell");
        try {
            Process process = processBuilder.start();
            
            // Get the input and output streams
            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();
            
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            
            // Commands to run in jshell
            String[] commands = {
//                "/set feedback silent", // reduce verbosity
                "int x = 10;\n",
                "int y = 20;\n",
                "int z = x + y\n",
                "var s = \"Hello World!\"\n",
                "System.out.println(z);\n",
                "try { System.out.println(10 / 0); } catch(Exception exception) { exception.printStackTrace(System.out); }\n",
                "/exit\n"
            };
            
            try { System.out.println(10 / 0); } catch(Exception exception) { exception.printStackTrace(); }
            
            // Sending commands to jshell
            for (String command : commands) {
                writer.write(command);
                writer.newLine();
                writer.flush();
            }
            
            // Reading output from jshell
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            process.waitFor(); // Wait for the process to finish
            System.out.println("JShell exited with code: " + process.exitValue());
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
