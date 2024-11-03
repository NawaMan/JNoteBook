package nawaman.jcompiler.steps;

import static nawaman.jcompiler.JCompiler.newCompiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;

import functionalj.list.FuncList;
import nawaman.jcompiler.DependencyManager;
import nawaman.jcompiler.JCompilerException;
import nawaman.jcompiler.JCompilerInput;
import nawaman.jcompiler.code.Code;
import nawaman.jcompiler.code.CodeHighLight;
import nawaman.jcompiler.code.CodeSegmentFormatter;
import nawaman.jcompiler.code.CodeSegmentVT100Formatter;

public class JCompilerMain {
    
    public static void main(String[] args) throws JCompilerException, IOException, InterruptedException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        var dependencies = new DependencyManager();
        dependencies.addDependency("!MVN:io.functionalj:functionalj-all:1.0.18-SNAPSHOT");
        
//        var indent = "    ";
        var indent = "\t";
      
        var packageName = "pkg";
//        var packageName = (String)null;
        var className = "HelloWorld";
        var javaCode = """
                package pkg;
                public class HelloWorld {
                %1$spublic static void main(String[ args) {
                %1$s%1$sSystem.out.println("Hello, World!!);
                %1$s}
                }
                """;
        
        var target = prepareTargetDirectory("compiled-classes", false);
        var input  = new JCompilerInput.Builder()
                .packageName(packageName)
                .className(className)
                .javaCode(javaCode.formatted(indent))
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
        
        var code        = new Code(javaCode.formatted(indent));
        var segment     = new CodeSegmentVT100Formatter(code, true);
//        var segment     = new CodeSegmentPlainTextFormatter(code, false);
        
        var diags = new ArrayList<String>();
        
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
            
            var position    = diagnostic.position;
            var endPosition = diagnostic.endPosition;
            var message     = diagnostic.kind + " - " + diagnostic.message;
            var diag        = diag(position, endPosition, message, code, segment);
            diags.add(diag);
        }
        
        for (var diag : diags) {
            System.out.println(diag);
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
    
    static String diag(long position, long endPosition, String message, Code code, CodeSegmentFormatter segment) {
        int startOffset = Long.valueOf(position).intValue();
        int endOffset   = Long.valueOf(endPosition).intValue();
        var highlight   = new CodeHighLight(startOffset, ((endOffset != startOffset) ? 0 : 1) + endOffset, 1);
        
        var startLineNumber = code.lineNumberAtOffset(startOffset);
        var startLineOffset = code.startOffset(startLineNumber);
        var column          = startOffset - startLineOffset;
        
        var diag = new StringBuilder();
        diag.append("============================================================================================")
            .append("\n");
        
        diag.append("Line: ")
            .append(startLineNumber)
            .append(", Column: ")
            .append(column)
            .append(" (Offset: ")
            .append(position)
            .append(")")
            .append("\n");
        
        diag.append("Message: ")
            .append(message)
            .append("\n");
        
        diag.append("\n");
        
        diag.append(segment.byOffset(startOffset, highlight));
        
        diag.append("============================================================================================")
            .append("\n");
        
        diag.append("\n");
        return diag.toString();
    }
    
}
