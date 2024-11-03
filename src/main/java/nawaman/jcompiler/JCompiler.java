package nawaman.jcompiler;

import static functionalj.function.Func.f;
import static functionalj.lens.Access.theString;
import static functionalj.types.DefaultValue.EMPTY;
import static functionalj.types.DefaultValue.NULL;
import static java.nio.file.Files.walk;
import static nullablej.nullable.Nullable.nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

import functionalj.function.Func;
import functionalj.function.Func1;
import functionalj.function.FuncUnit0;
import functionalj.function.FuncUnit1;
import functionalj.list.FuncList;
import functionalj.types.DefaultTo;
import functionalj.types.Nullable;
import functionalj.types.Required;
import functionalj.types.Struct;
import nawaman.jcompiler.code.Code;
import nawaman.jcompiler.code.CodeHighLight;
import nawaman.jcompiler.code.CodeSegmentFormatter;
import nawaman.jcompiler.steps.ReflectionUtility;


/**
 * The Java compiler.
 */
public interface JCompiler {
    
    /**
     * Represents the input for the compiler.
     */
    @Struct(name = "JCompilerInput")
    static interface Input {
        /** the package name */     @DefaultTo(NULL)  String           packageName();
        /** the class name */       @Required         String           className();
        /** the Java code */        @Required         String           javaCode();
        /** the compiler options */ @DefaultTo(EMPTY) FuncList<String> compilerOptions();
        /** the classpath */        @DefaultTo(NULL)  String           classpath();
        /** the target path */      @Required         String           targetDirectory();
        
        public default String fullClassName() {
            var packageName = packageName();
            var className   = className();
            var classPrefix = ((packageName != null) && !packageName.isBlank()) ? (packageName + ".") : "";
            return classPrefix + className;
        }
        
    }
    
    /**
     * Represents the source of a diagnostic.
     */
    @Struct(name = "DiagnosticSource")
    static interface Source {
            JCompilerInput input();
            String         classToString();
            String         sourceToString();
        
        public default String content() throws IOException {
            var classToString  = classToString();
            var sourceToString = sourceToString();
            var className      = input().className();
            var pkgName        = nullable(input().packageName()).orElse("");
            var expected       = "%s[string:///%s/%s.java]".formatted(classToString, pkgName, className);
            if (classToString.equals(InMemoryJavaFileObject.class.getCanonicalName())
             && sourceToString.equals(expected)) {
                return input().javaCode();
            }
            
            var targetDirectory = input().targetDirectory;
            var pattern         = "^DirectoryFileObject\\[([^:]+):([^.]+\\.java)\\]$".formatted(targetDirectory, pkgName);
            var matcher         = Pattern.compile(pattern).matcher(sourceToString);
            if (matcher.find()) {
                var pathName = matcher.group(1);
                var fileName = matcher.group(2);
                var file     = new File(pathName, fileName);
                var content  = Files.readString(file.toPath());
                return content;
            }
            
            throw new IllegalStateException("Unknown source: " + sourceToString + " + " + classToString);
        }
    }
    
    /**
     * Represents the kind of a diagnostic.
     */
    enum DiagnosticKind {
        /** Problem which prevents the tool's normal completion. */
        ERROR,
        
        /** Problem which does not usually prevent the tool from completing normally. */
        WARNING,
        
        /**
         * Problem similar to a warning, but is mandated by the tool's specification. 
         * For example, the Java Language Specification mandates warnings on certain unchecked operations 
         *   and the use of deprecated methods.
         */
        MANDATORY_WARNING,
        
        /** Informative message from the tool. */
        NOTE,
        
        /** Diagnostic which does not fit within the other kinds. */
        OTHER,
    }
    
    /**
     * Represents a diagnostic.
     */
    @Struct(name = "JCompilerDiagnostic")
    static interface Diagnostic {
        /** the input of the compilation */                   Input            input();
        /** the diagnostic kind */                            DiagnosticKind   kind();
        /** the source of the diagnostic */                   DiagnosticSource source();
        /** the position of the diagnostic */                 Long             position();
        /** the start position of the diagnostic */ @Nullable Long             startPosition();
        /** the end position of the diagnostic */   @Nullable Long             endPosition();
        /** the line number of the diagnostic */              Long             lineNumber();
        /** the column number of the diagnostic */            Long             columnNumber();
        /** the code of the diagnostic */                     String           code();
        /** the message of the diagnostic */                  String           message();
        
        public default boolean isIgnore() {
//            var isGenerated 
//                =  source().classToString.contains("com.sun.tools.javac.file.PathFileObject")
//                && source().sourceToString.contains("DirectoryFileObject");
//            var isUncheckedCast
//                = (kind() + ": " + message()).startsWith("MANDATORY_WARNING: unchecked cast");
//            return isGenerated && isUncheckedCast;
            return false;
        }
        
        public default String display() throws IOException {
            var content      = source().content();
            int position     = (position()    != null) ? position().intValue()    : 0;
            int endPosition  = (endPosition() != null) ? endPosition().intValue() : 0;
            var codeLocation = new CodeLocation(content, position, endPosition);
            var snippet      = codeLocation.snippet();
            return snippet;
        }
    }
    
    /**
     * Converts a javax.tools.Diagnostic to a Diagnostic.
     */
    @Struct(name = "JCompilerOutput")
    static interface Output {
        @Required         JCompilerInput                input();
        @Required         Boolean                       success();
        @DefaultTo(EMPTY) FuncList<JCompilerDiagnostic> diagnostics();
        
        public default ClassLoader classLoader() throws MalformedURLException {
            var targetDirectory = new File(input().targetDirectory());
            return URLClassLoader.newInstance(new URL[] { targetDirectory.toURI().toURL() });
        }
        
        public default Class<?> compiledClass() throws MalformedURLException, ClassNotFoundException {
            var loader   = classLoader();
            var fullName = input().fullClassName();
            var clazz    = Class.forName(fullName, true, loader);
            return clazz;
        }
        
        public default FuncUnit0 main() {
            return Func.f(()-> {
                run(__ -> {});
            });
        }
        
        public default void run() throws Exception {
            run(__ -> {});
        }
        
        public default void run(FuncUnit1<Class<?>> prerun) throws Exception {
            var clazz = compiledClass();
            prerun.accept(clazz);
            
            var mainMethod = clazz.getDeclaredMethod("main", String[].class);
            mainMethod.invoke(null, (Object)new String[0]);
        }
        
        public default String toDetail(Func1<Code, CodeSegmentFormatter> segmentCreator, int prefixOffset, int codeLength) {
            var string = new StringBuilder();
            
            var output = this;
            if (!output.success()) {
                string.append("Compilation failed.\n");
            } else {
                string.append("Compilation success.\n");
            }
            
            var javaCode = input().javaCode();
            prefixOffset = (prefixOffset < 0) ? 0                 : prefixOffset;
            codeLength   = (codeLength   < 0) ? javaCode.length() : codeLength;
            
            var actualCode = javaCode.substring(prefixOffset, prefixOffset + codeLength);
            
            var code     = new Code(actualCode);
            var segment  = segmentCreator.apply(code);
            
            if (!output.diagnostics().isEmpty()) {
                var diags = new ArrayList<String>();
                
                for (var diagnostic : output.diagnostics()) {
                    if (diagnostic.isIgnore())
                        continue;
                    
                    var diag = diag(diagnostic, segment, prefixOffset);
                    diags.add(diag);
                }
                
                for (var diag : diags) {
                    string
                    .append(diag)
                    .append("\n");
                }
            }
            return string.toString();
        }
        
        public default FuncList<String> importableClasses() throws MalformedURLException {
            var targetDirectory = new File(input().targetDirectory());
            
            int pathPrefixLength  = (targetDirectory.getAbsolutePath() + "/").length();
            var isImportableClass = f(ReflectionUtility::isPublicFirstLevelClass).apply2(classLoader()).toPredicate();
            return FuncList.from(f(() -> walk(targetDirectory.toPath())))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .map(theString.substring(pathPrefixLength))
                    .map(ReflectionUtility::toImportFormat)
                    .filter(isImportableClass);
        }
    }
    
    /**
     * Represents the input for the compiler.
     */
    @Struct
    public interface CodeLocationSpec {
        
        /** the code */
        String  code();
        
        /** the position */
        int position();
        
        @Nullable
        Integer endPosition();
        
        /**
         * Returns the snippet of the code.
         * 
         * @return the snippet of the code.
         */
        public default String snippet() {
            var code        = code();
            var position    = position();
            var endPosition = endPosition();
            
            if (position < 0) {
                position = 0;
            }
            if (position > code.length()) {
                position = code.length();
            }
            if ((endPosition != null) && (position > endPosition)) {
                endPosition = position;
            }
            
            int lineStart = position;
            for (; (lineStart > 0) && (code.charAt(lineStart) != '\n'); lineStart--) {}
            
            int lineEnd = (endPosition == null) ? position : endPosition;
            for (; (lineEnd < code.length()) && (code.charAt(lineEnd) != '\n'); lineEnd++) {}
            
            var codeSnippet  = code.substring(lineStart, lineEnd);
            var offsetOnLine = position - lineStart;
            var cursor       = (offsetOnLine == 0) ? "^" : ("%" + offsetOnLine + "s^").formatted(" ");
            
            return codeSnippet + "\n" + cursor;
        }
    }
    
    
    /**
     * Creates a new compiler with the default implementation.
     * 
     * @return the new compiler.
     */
    public static JCompiler newCompiler() {
        return new JCompilerImpl();
    }
    
    /**
     * Compiles the given input and returns the output.
     * 
     * @param  input               the input
     * @param  targetDirectory     the target directory
     * @return                     the output
     * @throws JCompilerException  if an error occurs
     */
    public JCompilerOutput compile(JCompilerInput input, File targetDirectory) throws JCompilerException;
    
    
    /**
     * Creates a new compiler with the default implementation.
     * 
     * @return the new compiler.
     */
    public static String diag(JCompilerDiagnostic diagnostic, CodeSegmentFormatter segmentFormatter, int prefixOffset) {
        var code        = segmentFormatter.code();
        
        var position    = diagnostic.position;
        var endPosition = diagnostic.endPosition;
        var message     = diagnostic.kind + " - " + diagnostic.message;
        
        int startOffset = Long.valueOf(position).intValue() - prefixOffset;
        int endOffset   = Long.valueOf(endPosition).intValue() - prefixOffset;
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
        
        diag.append(segmentFormatter.byOffsets(startOffset, endOffset, highlight));
        
        diag.append("============================================================================================")
            .append("\n");
        
        diag.append("\n");
        return diag.toString();
    }
    
}
