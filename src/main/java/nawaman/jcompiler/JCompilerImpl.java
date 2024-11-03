package nawaman.jcompiler;

import static functionalj.list.FuncList.empty;
import static functionalj.list.FuncList.listOf;
import static java.util.Collections.singletonList;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

import java.io.File;
import java.io.IOException;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.ToolProvider;

import functionalj.function.Func;
import functionalj.list.FuncList;
import nullablej.nullable.Nullable;

/**
 * The implementation of the Java compiler.
 */
public class JCompilerImpl implements JCompiler {
    
    @Override
    public JCompilerOutput compile(JCompilerInput input, File targetDirectory) throws JCompilerException {
        var packageName = input.packageName();
        var className   = input.className();
        var fullName    = (((packageName != null) && !packageName.isBlank()) ? packageName + "." : "") + className;
        var classpath   = Nullable.of(input.classpath()).map(cp -> listOf("-classpath", cp)).orElse(empty());
        var options     = input.compilerOptions().appendAll(classpath);
        var javaCode    = input.javaCode();
        var javaFile    = new InMemoryJavaFileObject(fullName, javaCode);
        
        var compiler    = prepareCompiler();
        var diagnostics = new DiagnosticCollector<>();
        var fileManager = prepareJavaFileManager(targetDirectory, diagnostics, compiler);
        try {
            var task      = compiler.getTask(null, fileManager, diagnostics, options, null, listOf(javaFile));
            var isSuccess = task.call();
            
            var toDiagnotic     = Func.f(JCompilerImpl::toDiagnotic).apply(input);
            var diagnosticsList = FuncList.from(diagnostics.getDiagnostics().stream().map(toDiagnotic));
            return new JCompilerOutput.Builder()
                    .input(input)
                    .success(isSuccess)
                    .diagnostics(diagnosticsList)
                    .build();
            
        } finally {
            closeJavaFileManager(fileManager);
        }
    }
    
    static JavaFileManager prepareJavaFileManager(
            File                        targetDirectory,
            DiagnosticCollector<Object> diagnostics,
            JavaCompiler                compiler) 
            throws JCompilerException {
        try {
            var fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            fileManager.setLocation(CLASS_OUTPUT, singletonList(targetDirectory));
            return fileManager;
            
        } catch (IOException exception) {
            throw new JCompilerException(JCompilerException.Failure.CreateJavaFileManager, exception);
        }
    }
    
    static void closeJavaFileManager(JavaFileManager fileManager) throws JCompilerException {
        try {
            fileManager.close();
        } catch (IOException exception) {
            throw new JCompilerException(JCompilerException.Failure.CloseJavaFileManager, exception);
        }
    }
    
    static JavaCompiler prepareCompiler() throws JCompilerException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new JCompilerException(JCompilerException.Failure.GetCompiler);
        }
        return compiler;
    }
    
    static JCompilerDiagnostic toDiagnotic(JCompilerInput input, javax.tools.Diagnostic<? extends Object> diagnostic) {
        var kind         = DiagnosticKind.valueOf(diagnostic.getKind().name());
        var sourceClass  = diagnostic.getSource().getClass().getCanonicalName();
        var sourceString = diagnostic.getSource().toString();
        var source       = new DiagnosticSource(input, sourceClass, sourceString);
        return new JCompilerDiagnostic(
                input,
                kind,
                source,
                toLong(diagnostic.getPosition()),
                toLong(diagnostic.getStartPosition()),
                toLong(diagnostic.getEndPosition()),
                toLong(diagnostic.getLineNumber()),
                toLong(diagnostic.getColumnNumber()),
                diagnostic.getCode(),
                diagnostic.getMessage(null));
    }
    
    static Long toLong(long value) {
        return (value == javax.tools.Diagnostic.NOPOS) ? null : Long.valueOf(value);
    }
    
}
