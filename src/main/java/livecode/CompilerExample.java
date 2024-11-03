package livecode;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.function.Supplier;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class CompilerExample {

    public static void main(String[] args) throws Exception {
        // Java source code as a string
        String sourceCode = 
            "package pkg;\n" +
            "\n" +
            "public class MyClass implements java.util.function.Supplier<String> {\n" +
            "    @Override public String get() {\n" +
            "        return \"Hello World!\";\n" +
            "    }\n" +
            "}\n";

        // Write source code to a Java file object
        JavaFileObject javaFileObject = new InMemoryJavaFileObject("pkg.MyClass", sourceCode);

        // Get the Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // Get a standard file manager
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // Set the output directory for the compiled class
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(outputDir));

        // Compile the source file
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, Arrays.asList(javaFileObject));
        Boolean result = task.call();
        
        if (result) {
            System.out.println("Compilation successful.");
            
            // Load and use the compiled class from the output directory
            URL[] urls = { outputDir.toURI().toURL() };
            URLClassLoader classLoader = new URLClassLoader(urls);
            Class<?> cls = classLoader.loadClass("pkg.MyClass");
            Supplier<String> instance = (Supplier<String>) cls.getDeclaredConstructor().newInstance();
            System.out.println(instance.get()); // Output: Hello World!
        } else {
            System.out.println("Compilation failed.");
        }
        
        // Close the file manager
        fileManager.close();
    }
}

//// A JavaFileObject implementation that holds the Java source code in memory
//class InMemoryJavaFileObject extends SimpleJavaFileObject {
//    private final String sourceCode;
//
//    protected InMemoryJavaFileObject(String className, String sourceCode) {
//        super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
//        this.sourceCode = sourceCode;
//    }
//
//    @Override
//    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
//        return sourceCode;
//    }
//}
