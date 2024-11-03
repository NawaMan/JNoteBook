package nawaman.jcompiler;

import static java.nio.file.Files.lines;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Utility class to resolve Maven dependencies.
 */
public class MavenDependencyResolver {
    
    private static final String pomPrefix = """
            <project 
                xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>temp-project</artifactId>
                <version>1.0-SNAPSHOT</version>
                
                <dependencies>
            """;
    
    private static final String pomSuffix = """
                </dependencies>
            </project>
            """;
    
    private static final String dependencyTemplate = """
            <dependency>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
            </dependency>
            """;
    
    static void createPomFile(List<String> mavenDependencies, File pomFile) throws IOException {
        try (FileWriter writer = new FileWriter(pomFile)) {
            writer.write(pomPrefix);
            for (String dependency : mavenDependencies) {
                var parts = dependency.split(":");
                writer.write(dependencyTemplate.formatted(parts[1], parts[2], parts[3]));
            }
            writer.write(pomSuffix);
        }
    }
    
    private final ShellProcess shellProcess;
    
    public MavenDependencyResolver(ShellProcess shellProcess) {
        this.shellProcess = shellProcess;
    }
    
    /**
     * Resolves the Maven dependencies and returns the JAR files.
     * 
     * @param  mavenDependencies    the list of Maven dependencies
     * @return                      the list of JAR files
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    public List<File> resolve(List<String> mavenDependencies) throws IOException, InterruptedException {
        return resolve(null, mavenDependencies);
    }
    
    /**
     * Resolves the Maven dependencies and returns the JAR files.
     * 
     * @param  pomDir               the directory to store the temporary POM file
     * @param  mavenDependencies    the list of Maven dependencies
     * @return                      the list of JAR files
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    public List<File> resolve(File pomDir, List<String> mavenDependencies) throws IOException, InterruptedException {
        if (pomDir == null) {
            pomDir = Files.createTempDirectory("mvnproj").toFile();
            pomDir.deleteOnExit();
        }
        
        var pomFile = new File(pomDir, "pom.xml");
        createPomFile(mavenDependencies, pomFile);
        
        var classpathFile = new File(pomFile.getParentFile(), "classpath.txt");
        var result = shellProcess.run(
                "/home/linuxbrew/.linuxbrew/bin/mvn", 
                "dependency:build-classpath", 
                "-Dmdep.outputFile=" + classpathFile.getAbsolutePath(), 
                "-f", pomFile.getAbsolutePath()
        );
        
        var output   = result.output();
        var error    = result.error();
        int exitCode = result.exitCode();
        if (exitCode != 0) {
            var template 
                    = "Failed with exit code: %d\n"
                    + "    Output:\n%s\n"
                    + "    Error:\n%s\n";
            var message = template.formatted(exitCode, output, error);
            throw new RuntimeException(message);
        }
        
        return readMvnJarFiles(classpathFile);
    }
    
    private static List<File> readMvnJarFiles(File classpathFile) throws IOException {
        try (var lines = lines(classpathFile.toPath())) {
            var mvnFiles = lines
                    .flatMap(line -> List.of(line.split(File.pathSeparator)).stream())
                    .map    (File::new)
                    .toList ();
            return mvnFiles;
        }
    }
    
}
