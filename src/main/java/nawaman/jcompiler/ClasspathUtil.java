package nawaman.jcompiler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Utility class to construct a classpath from a list of paths.
 */
class ClasspathUtil {
    
    /**
     * Constructs a classpath from a list of paths.
     * 
     * The paths can be of the following types:
     *  - A Maven dependency in the form "!MVN:groupId:artifactId:version"
     *  - A JAR file in the form "!JAR:/path/to/jar"
     *  - A directory in the form "!DIR:/path/to/dir"
     * 
     * @param dependencies  the list of paths
     * @return              the classpath as a string
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    static String classpath(List<String> dependencies) throws IOException, InterruptedException {
        var jarFiles = new ArrayList<File>();
        var mavenDependencies = new ArrayList<String>();
        
        for (var dependency : dependencies) {
            if (dependency.startsWith("!MVN:")) {
                if (!dependency.matches("!MVN:[^:]+:[^:]+:[^:]+")) {
                    throw new IllegalArgumentException("Invalid Maven dependency: " + dependency);
                }
                
                mavenDependencies.add(dependency);
            } else {
                var file = new File(dependency.substring(5));
                if (dependency.startsWith("!JAR:")) {
                    if (!dependency.matches("!JAR:.+\\.jar")) {
                        throw new IllegalArgumentException("Invalid JAR file: " + dependency);
                    }
                    
                    jarFiles.add(file);
                } else if (dependency.startsWith("!DIR:")) {
                    jarFiles.add(file);
                } else {
                    throw new IllegalArgumentException("Invalid dependency: " + dependency);
                }
            }
        }
        
        if (!mavenDependencies.isEmpty()) {
            var shellProcess          = new ShellProcess();
            var mvnDependencyResolver = new MavenDependencyResolver(shellProcess);
            jarFiles.addAll(mvnDependencyResolver.resolve(mavenDependencies));
        }
        
        var classpath = new StringJoiner(File.pathSeparator);
        for (var jarFile : jarFiles) {
            classpath.add(jarFile.getAbsolutePath());
        }
        
        return classpath.toString();
    }
}
