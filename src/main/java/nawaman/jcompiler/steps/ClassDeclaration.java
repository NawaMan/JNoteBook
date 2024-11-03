package nawaman.jcompiler.steps;

import static nawaman.jcompiler.JCompiler.newCompiler;
import static nawaman.jcompiler.code.CodeSegmentVT100Formatter.vt100SegmentCreator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;

import functionalj.list.FuncList;
import nawaman.jcompiler.DependencyManager;
import nawaman.jcompiler.JCompilerException;
import nawaman.jcompiler.JCompilerInput;
import nawaman.jcompiler.JCompilerOutput;
import nawaman.jcompiler.ShellProcess;


public class ClassDeclaration {
    
    private static final String templatePrefix = """
            package %1$s;
            
            %2$s
            
            
            
            
            
            """;
    
    public static void main(String[] args) throws IOException, InterruptedException, JCompilerException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        var defaultCompilerOptions = FuncList.of("-proc:full", "-Xlint:unchecked", "-XDdiags:verbose");
        
        var dependencies    = new DependencyManager();
        
        
        var packageName = "pkg00001";
        var indent      = "    ";
        var classCode   = "@SuppressWarnings(\"documentation\")\nclass Main {\npublic static class M {}\n    public void print() {\n        System.out.println(\"Hello World!\");\n    }\n}";
        var imports     = FuncList.<String>emptyList();
        
        var className = extractClassName(classCode);
        classCode     = makePublic(classCode);
        
        var javaCodePrefix = templatePrefix.formatted(packageName, imports.join("\n"), className);
        var javaCodeField  = classCode;
        var javaCode       = javaCodePrefix + javaCodeField;
        
        int prefixOffset = javaCodePrefix.length();
        int codeLength   = javaCodeField.length();
        
        var onelineRuler    = true;
        var segmentCreator  = vt100SegmentCreator.apply2(onelineRuler);
        var compilerOptions = defaultCompilerOptions;
        
        
        var target = prepareTargetDirectory("compiled-classes", false);
        var input  = new JCompilerInput.Builder()
                .packageName(packageName)
                .className(className)
                .javaCode(javaCode.formatted(indent))
                .compilerOptions(compilerOptions)
                .classpath(dependencies.classpath())
                .targetDirectory(target.getAbsolutePath())
                .build();
        var compiler = newCompiler();
        
        var output = compiler.compile(input, target);
        System.out.println(output.toDetail(segmentCreator, prefixOffset, codeLength));
        
        System.out.println("Result classpath");
        System.out.println(output.input.targetDirectory);
        System.out.println();
        
        System.out.println("Importable classes");
        output
        .importableClasses()
        .forEach(System.out::println);
        
        
        
        
        
        
        
        
        javap(output);
    }
    
    private static void javap(JCompilerOutput output) throws IOException, InterruptedException {
        var clssFilePath = output.input.targetDirectory + "/pkg00001/Main.class";
        var shellOutput  = new ShellProcess().run("javap", "-c", clssFilePath);
        System.out.println(shellOutput.output());
    }
    
    private static String makePublic(String classCode) {
        var isPublic = classCode.matches("(?s).*?\\bpublic\\s+class\\s+(\\w+).*");
        System.out.println("isPublic: " + isPublic);
        
        if (!isPublic) {
            classCode = classCode.replaceFirst("(?s)([ \t\n])(class)([ \t\n])", "$1public class$3");
        }
        return classCode;
    }
    
    private static String extractClassName(String classCode) {
        return classCode.replaceFirst("(?s).*?(?:class|interface|enum|record)\\s+(\\w+).*", "$1");
    }
    
    static File prepareTargetDirectory(String tempDirectoryPrefix, boolean deleteOnExit) throws IOException {
        var targetDirectory = Files.createTempDirectory(tempDirectoryPrefix).toFile();
        if (deleteOnExit) {
            targetDirectory.deleteOnExit();
        }
        return targetDirectory;
    }
    
}
