package notebook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class MavenDependencyManager {

    public static void main(String[] args) throws IOException, InterruptedException {
        // Example Maven dependencies
        List<String> dependencies = List.of(
            "<dependency><groupId>io.functionalj</groupId><artifactId>functionalj-all</artifactId><version>1.0.16</version></dependency>",
            "<dependency><groupId>io.defaultj</groupId><artifactId>defaultj-api</artifactId><version>2.2.0.5</version></dependency>",
            "<dependency><groupId>io.defaultj</groupId><artifactId>defaultj-annotations</artifactId><version>2.2.0.5</version></dependency>",
            "<dependency><groupId>io.defaultj</groupId><artifactId>defaultj-core</artifactId><version>2.2.0.5</version></dependency>"
        );
        
        // Create a temporary directory
        File tempDir = Files.createTempDirectory("maven-project").toFile();
        System.out.println("Temporary directory created at: " + tempDir.getAbsolutePath());

        // Create a pom.xml file in the temporary directory
        File pomFile = new File(tempDir, "pom.xml");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pomFile))) {
            writer.write(generatePomXml(dependencies));
        }
        System.out.println("pom.xml created at: " + pomFile.getAbsolutePath());

        // Use ProcessBuilder to run Maven commands
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(tempDir);

        // Install all dependencies
        builder.command("mvn", "clean", "install");
        executeMavenCommand(builder);

        // Ask Maven to output the classpath
        builder.command("mvn", "dependency:build-classpath", "-Dmdep.outputFile=cp.txt");
        executeMavenCommand(builder);
        
        File cpFile = new File(tempDir, "cp.txt");
        Files.readAllLines(cpFile.toPath())
        .forEach(System.out::println);
    }

    private static String generatePomXml(List<String> dependencies) {
        String basePomXml = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>temp</groupId>
                <artifactId>temp-project</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                    %s
                </dependencies>
            </project>
            """;
        String dependencyXml = String.join("\n", dependencies);
        return String.format(basePomXml, dependencyXml);
    }

    private static void executeMavenCommand(ProcessBuilder builder) throws IOException, InterruptedException {
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Maven command execution failed with exit code " + exitCode);
        }
    }
}
