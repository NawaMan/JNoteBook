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

public class StatementRunner {
    
    private static final String templatePrefix = """
            package %1$s;
            
            %2$s
            
            public class %3$s {
            
            public static void execute() {
            
            
            
            
            
            """;
    private static final String templateSuffix = """
            
            
            
            
            
            }
            }
            """;
    
    public static void main(String[] args) throws IOException, InterruptedException, JCompilerException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        var defaultCompilerOptions = FuncList.of("-proc:full", "-Xlint:unchecked", "-XDdiags:verbose");
        
        var dependencies    = new DependencyManager();
        
        
        var packageName = "pkg00001";
        var className   = "Clazz";
        var indent      = "    ";
        var expression  = "System.out.println(\"Hello World!\")";
        var imports     = FuncList.<String>emptyList();
        
        var javaCodePrefix = templatePrefix.formatted(packageName, imports.join("\n"), className);
        var javaCodeField  = expression + (expression.trim().endsWith(";") ? "" : ";");
        var javaCodeSuffix = templateSuffix;
        var javaCode       = javaCodePrefix + javaCodeField + javaCodeSuffix;
        
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
        
        if (output.success) {
            var compiledClass = output.compiledClass();
            var result        = compiledClass.getDeclaredMethod("execute").invoke(args);
            System.out.println("result.value: " + result);
            if (result != null) {
                System.out.println("result.class: " + result.getClass());
                System.out.println("result.type: " + result.getClass().getTypeName());
            }
        }
    }
    
    static File prepareTargetDirectory(String tempDirectoryPrefix, boolean deleteOnExit) throws IOException {
        var targetDirectory = Files.createTempDirectory(tempDirectoryPrefix).toFile();
        if (deleteOnExit) {
            targetDirectory.deleteOnExit();
        }
        return targetDirectory;
    }
    
}
