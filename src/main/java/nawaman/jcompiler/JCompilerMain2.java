package nawaman.jcompiler;

import static nawaman.jcompiler.JCompiler.newCompiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import functionalj.list.FuncList;

public class JCompilerMain2 {
    
    public static void main(String[] args) throws JCompilerException, IOException, InterruptedException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        var dependencies = new DependencyManager();
        dependencies.addDependency("!MVN:io.functionalj:functionalj-all:1.0.18-12-SNAPSHOT");
        
        var packageName = "pkg";
//        var packageName = (String)null;
        var className = "HelloWorld";
        var javaCode = """
                package pkg;
                
                public class HelloWorld {
                    
                    @functionalj.types.Struct(name = "Point")
                    static record PointSpec(int x, int y) {
                        public static double absolute(Point self, double factor) {
                            return factor * Math.sqrt(self.x()*self.x() + self.y()*self.y());
                        }
                    }
                    
                    public static void main(String[] args) {
                        System.out.println("Hello World!");
                    }
                }
                """;
        
        var target = prepareTargetDirectory("compiled-classes", false);
        var input  = new JCompilerInput.Builder()
                .packageName(packageName)
                .className(className)
                .javaCode(javaCode)
                .compilerOptions(FuncList.of("-proc:full", "-Xlint:unchecked"))
                .classpath(dependencies.classpath())
                .targetDirectory(target.getAbsolutePath())
                .build();
        var compiler = newCompiler();
        
        var output = compiler.compile(input, target);
        if (!output.success) {
            System.out.println("Compilation failed.");
        }
        
        System.out.println();
        System.out.println("Target directory: " + target.getAbsolutePath());
        System.out.println("Compiled classes:");
        listFilesRecursively(target.getAbsolutePath() + "/", target);
        System.out.println();
        
        for (var diagnostic : output.diagnostics) {
            if (diagnostic.isIgnore())
                continue;
            
            System.out.println(diagnostic.kind + ": " + diagnostic.message);
            System.out.println("Line Number:   " + diagnostic.lineNumber);
            System.out.println("Source:        " + diagnostic.source);
            System.out.println("Code:          " + diagnostic.code);
            System.out.println("ColumnNumber:  " + diagnostic.columnNumber);
            System.out.println("Message:       " + diagnostic.message);
            System.out.println("Kind:          " + diagnostic.kind);
            System.out.println("Position:      " + diagnostic.position);
            System.out.println("StartPosition: " + diagnostic.startPosition);
            System.out.println("EndPosition:   " + diagnostic.endPosition);
            System.out.println("Snippet:       " + diagnostic.display());
            System.out.println();
        }
        
        var content = Files.readAllLines(Path.of(target.getAbsolutePath() + "/pkg/Point.java"));
        System.out.println();
        System.out.println("== Point.java ==");
        content.forEach(System.out::println);
        
//        output.main().run();
    }
    
    static File prepareTargetDirectory(String tempDirectoryPrefix, boolean deleteOnExit) throws IOException {
        var targetDirectory = Files.createTempDirectory(tempDirectoryPrefix).toFile();
        if (deleteOnExit) {
            targetDirectory.deleteOnExit();
        }
        return targetDirectory;
    }
    
    static void listFilesRecursively(String base, File directory) {
        for (var file : directory.listFiles()) {
            System.out.println((directory + "/" + file.getName()).replaceFirst(base, ""));
            if (file.isDirectory()) {
                listFilesRecursively(base, file); // Recursive call
            }
        }
    }
    
}
