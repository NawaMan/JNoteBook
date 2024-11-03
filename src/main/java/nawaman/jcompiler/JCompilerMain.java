package nawaman.jcompiler;

import static nawaman.jcompiler.JCompiler.newCompiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;

import functionalj.list.FuncList;

public class JCompilerMain {
    
    public static void main(String[] args) throws JCompilerException, IOException, InterruptedException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        var dependencies = new DependencyManager();
        dependencies.addDependency("!MVN:io.functionalj:functionalj-all:1.0.18-01-SNAPSHOT");
        
        var packageName = "pkg";
//        var packageName = (String)null;
        var className = "HelloWorld";
        var javaCode = """
                package pkg;
                
                public class HelloWorld {
                    
                    @functionalj.types.Struct(toStringTemplate = "Staff:${name}")
                    void Staff(String name, String position, int salary) {
                    }
                    
                    @functionalj.types.Choice
                    static interface TemperatureSpec {
                        
                        void Celsius(double celsius);
                        
                        void Fahrenheit(double fahrenheit);
                        
                        default Temperature.Fahrenheit toFahrenheit(functionalj.types.choice.Self self) {
                            Temperature temp = self.unwrap();
                            return temp.match().celsius(c -> Temperature.Fahrenheit(c.celsius() * 1.8 + 32.0)).fahrenheit(f -> f);
                        }
                        
                        default Temperature.Celsius toCelsius(functionalj.types.choice.Self self) {
                            Temperature temp = self.unwrap();
                            return temp.match().celsius(c -> c).fahrenheit(f -> Temperature.Celsius((f.fahrenheit() - 32.0) / 1.8));
                        }
                    }
                    
                    @functionalj.types.Rule
                    static String Int255(int intValue) {
                        return ((intValue >= 0) && (intValue <= 255)) ? null : ("Int value must be between 0 to 255 inclusively: " + intValue);
                    }
                    
                    static  <T> void thing(T ... things) {
                        for (var thing : things) {
                            System.out.println(thing);
                        }
                    }
                    
                    public static void main(String[] args) {
                        var r = functionalj.function.Func.f(() -> {
                            System.out.println("Hello, World!! " + new Staff("John", "Manager", 1000));
                            System.out.println(Temperature.Celsius(0.0).toFahrenheit());
                            System.out.println(Int255.from(254));
                            System.out.println(Int255.from(260));
                        });
                        r.run();
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
