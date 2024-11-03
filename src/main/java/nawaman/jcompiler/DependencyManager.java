package nawaman.jcompiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to manage dependencies.
 * 
 * The dependencies can be of the following types:
 * - A Maven dependency in the form "!MVN:groupId:artifactId:version"
 * - A JAR file in the form "!JAR:/path/to/jar"
 * - A directory in the form "!DIR:/path/to/dir"
 * 
 * The classpath can be obtained by calling the {@link #classpath()} method.
 * Once the dependencies are added or removed, the classpath is automatically updated.
 */
public class DependencyManager {

    private Set<String> dependencies;
    private String      cachedClasspath;
    
    public DependencyManager() {
        this.dependencies = new HashSet<>();
        this.cachedClasspath = null;
    }
    
    /**
     * Adds a dependency to the list of dependencies.
     * 
     * @param dependency the dependency to add
     */
    public void addDependency(String dependency) {
        if (dependencies.add(dependency)) {
            cachedClasspath = null;
        }
    }
    
    /**
     * Removes a dependency from the list of dependencies.
     * 
     * @param dependency the dependency to remove
     */
    public void removeDependency(String dependency) {
        if (dependencies.remove(dependency)) {
            cachedClasspath = null;
        }
    }
    
    /**
     * Returns the classpath constructed from the list of dependencies.
     * 
     * @return the classpath as a string.
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    public String classpath() throws IOException, InterruptedException {
        if (cachedClasspath == null) {
            cachedClasspath = ClasspathUtil.classpath(new ArrayList<>(dependencies));
        }
        return cachedClasspath;
    }
    
    public static void main(String[] args) {
        try {
            var manager = new DependencyManager();
            
            manager.addDependency("!MVN:io.functionalj:functionalj-all:1.0.17");
            manager.addDependency("!JAR:/path/to/local.jar");
            manager.addDependency("!DIR:/path/to/classes/dir");
            
            System.out.println("Classpath: " + manager.classpath());
            
            manager.removeDependency("JAR:/path/to/local.jar");
            System.out.println("Updated Classpath: " + manager.classpath());
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
