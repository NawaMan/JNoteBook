package livecode;

import javax.tools.*;
import java.net.URI;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LombokCompilerExample {

    public static void main(String[] args) throws Exception {
        // Compile the first class
        String sourceCode1 = 
            "package pkg;\n" +
            "\n" +
            "import lombok.Value;\n" +
            "\n" +
            "@Value\n" +
            "public class MyLombokClass {\n" +
            "    private String message;\n" +
            "}";
        String classFullName = "pkg.MyLombokClass";
        
        // Compile and load MyLombokClass
        Map<String, byte[]> classBytes = compileClass(classFullName, sourceCode1, new HashMap<>());

        // Compile the second class that uses MyLombokClass
        String sourceCode2 = 
            "package pkg2;\n" +
            "\n" +
            "public class MyLombokClass2 {\n" +
            "    pkg.MyLombokClass message = null;\n" +
            "}";

        classBytes.putAll(compileClass("pkg2.MyLombokClass2", sourceCode2, classBytes));

        // Load and run MyLombokClass2
        InMemoryClassLoader classLoader = new InMemoryClassLoader(classBytes);
        Class<?> cls = classLoader.loadClass("pkg2.MyLombokClass2");
        System.out.println("Successfully compiled and loaded: " + cls.getName());
    }

    private static Map<String, byte[]> compileClass(String className, String sourceCode, Map<String, byte[]> existingClassBytes) throws IOException {
        // Write source code to a Java file object
        JavaFileObject javaFileObject = new InMemoryJavaFileObject(className, sourceCode);

        // Get the Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // Create an in-memory file manager
        InMemoryFileManager fileManager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null), existingClassBytes);

        // Add Lombok jar to the classpath and include previously compiled classes
        String lombokJarPath = System.getProperty("user.home") + "/.m2/repository/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar";
        InMemoryClassLoader inMemoryClassLoader = new InMemoryClassLoader(existingClassBytes);
        String classpath = lombokJarPath + System.getProperty("path.separator") + createClassPath(existingClassBytes);

        Iterable<String> options = Arrays.asList("-classpath", classpath, "-proc:none");

        // Compile the source file
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, Arrays.asList(javaFileObject));
        Boolean result = task.call();

        if (result) {
            System.out.println("Compilation successful for " + className);
            return fileManager.getClassBytes();
        } else {
            throw new RuntimeException("Compilation failed for " + className);
        }
    }

    private static String createClassPath(Map<String, byte[]> classBytes) {
        StringBuilder classPathBuilder = new StringBuilder();
        for (String className : classBytes.keySet()) {
            if (classPathBuilder.length() > 0) {
                classPathBuilder.append(System.getProperty("path.separator"));
            }
            classPathBuilder.append(className.replace('.', '/')).append(".class");
        }
        return classPathBuilder.toString();
    }
}

// A JavaFileObject implementation that holds the Java source code in memory
class InMemoryJavaFileObject extends SimpleJavaFileObject {
    private final String sourceCode;

    protected InMemoryJavaFileObject(String className, String sourceCode) {
        super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
        this.sourceCode = sourceCode;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return sourceCode;
    }
}

// A custom file manager that stores compiled class files in memory
class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    private final Map<String, ByteArrayJavaFileObject> classFileObjects = new HashMap<>();
    private final Map<String, byte[]> existingClassBytes;

    protected InMemoryFileManager(StandardJavaFileManager fileManager, Map<String, byte[]> existingClassBytes) {
        super(fileManager);
        this.existingClassBytes = existingClassBytes;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        System.out.println("getJavaFileForOutput: location=" + location + ", className" + className + ", sibling=" + sibling);
        
        ByteArrayJavaFileObject fileObject = new ByteArrayJavaFileObject(className, kind);
        classFileObjects.put(className, fileObject);
        return fileObject;
    }

    @Override
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        System.out.println("getClassLoader: location=" + location);
        
        return new InMemoryClassLoader(existingClassBytes);
    }

    public Map<String, byte[]> getClassBytes() {
        Map<String, byte[]> classBytes = new HashMap<>(existingClassBytes);
        for (Map.Entry<String, ByteArrayJavaFileObject> entry : classFileObjects.entrySet()) {
            classBytes.put(entry.getKey(), entry.getValue().getBytes());
        }
        return classBytes;
    }
}

// A JavaFileObject implementation that holds the compiled class bytes in memory
class ByteArrayJavaFileObject extends SimpleJavaFileObject {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    protected ByteArrayJavaFileObject(String name, JavaFileObject.Kind kind) {
        super(URI.create("bytes:///" + name.replace('.', '/') + kind.extension), kind);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return outputStream;
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }
}

// A custom class loader that loads classes from in-memory byte arrays
class InMemoryClassLoader extends ClassLoader {
    private final Map<String, byte[]> classBytes;

    public InMemoryClassLoader(Map<String, byte[]> classBytes) {
        this.classBytes = classBytes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classBytes.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }
}
