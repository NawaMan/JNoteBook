package nawaman.jcompiler.code;

import static functionalj.list.FuncList.listOf;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.IntStream.rangeClosed;
import static nawaman.jcompiler.code.RuleTwoLine.bottomTwoLineRuler;
import static nawaman.jcompiler.code.RuleTwoLine.topTwoLineRuler;
import static nawaman.jcompiler.code.RulerOneLine.oneLineRuler;

import java.util.List;

import functionalj.function.Func2;

/**
 * Code Segment which displays with VT100 code.
 */
public class CodeSegmentPlainTextFormatter extends CodeSegmentFormatter {
    
    public static final Func2<Code, Boolean, CodeSegmentFormatter> segmentCreator 
                = (code, isOneLineRuler) -> new CodeSegmentPlainTextFormatter(code, isOneLineRuler != Boolean.FALSE);
                
    public static final Func2<Code, Boolean, CodeSegmentFormatter> plainTextSegmentCreator = segmentCreator;
                
    private final Ruler topRuler;
    private final Ruler bottomRuler;
    
    public CodeSegmentPlainTextFormatter(Code code) {
        this(code, false);
    }
    
    public CodeSegmentPlainTextFormatter(Code code, boolean isOneLineRuler) {
        super(code);
        topRuler    = isOneLineRuler ? oneLineRuler : topTwoLineRuler;
        bottomRuler = isOneLineRuler ? oneLineRuler : bottomTwoLineRuler;
    }
    
    @Override
    public CharSequence byLines(int firstLine, int lastLine, List<CodeHighLight> highlights) {
        code.processToLineCount(lastLine);
        lastLine = min(lastLine, code.knownLineCount());
        
        int maxColumn = maxColumn(firstLine, lastLine);
        
        var output = new StringBuilder();
        
        var prefix = "    |";
        topRuler.createRuler(output, prefix, maxColumn);
        
        output.append("\n");
        for (int i = firstLine; i <= lastLine; i++) {
            if (i >= code.lineCount()) {
                break;
            }
            
            var lineNumber = " %2d |".formatted(i + 1); // 1-based index
            var codeLine   = code.line(i).replaceAll("\t", "                         ".substring(0, tabSize));
            
            output.append(lineNumber).append(codeLine).append("\n");
        }
        
        if ((lastLine - firstLine) >= 5) {
            bottomRuler.createRuler(output, prefix, maxColumn);
            output.append("\n");
        }
        return output;
    }
    
    private int maxColumn(int firstLine, int lastLine) {
        int maxColumn
                = rangeClosed(firstLine, lastLine)
                .map(lineNumber -> code.endOffset(lineNumber) - code.startOffset(lineNumber))
                .max()
                .orElse(80);
        maxColumn = (int) (ceil(max(maxColumn, 80) / 10.0) * 10);
        return maxColumn;
    }
    
    public static void main(String[] args) {
        var text = """
                private void ensureNewlineUpTo(int offset, int lines) {
                \tvar lineCount = currentNewlineCount();%s
                \tvar content   = content();
                    int length    = content.length();
                    offset = Math.min(offset, length);
                    for ( ; (processedOffset < offset) && (lineCount < lines); processedOffset++) {
                        var ch = content.charAt(processedOffset);
                        if (ch == '\\r') {
                            if (((processedOffset + 1) < length) && (content.charAt(processedOffset + 1) == '\\n')) {
                                processedOffset++;
                                addNewLine(-processedOffset);
                                lineCount++;
                            } else {
                                addNewLine(processedOffset);
                                lineCount++;
                            }
                        } else if (ch == '\\n') {
                            addNewLine(processedOffset);
                            lineCount++;
                        }
                    }
                }
                """;
        var code = new Code(text.formatted("\r"));
        System.out.println(new CodeSegmentPlainTextFormatter(code, true).byLines(0, 10, listOf(
                new CodeHighLight(13, 30, 11),
                new CodeHighLight(92, 94, 2),
                new CodeHighLight(94, 101, 3), 
                new CodeHighLight(200, 203, 4), 
                new CodeHighLight(250, 263, 5), 
                new CodeHighLight(304, 324, 6), 
                new CodeHighLight(350, 354, 7)
            ).toArrayList()));
        
        System.out.println();
        System.out.println();
        
        System.out.println(new CodeSegmentPlainTextFormatter(code, false).byLines(0, 10, listOf(
                new CodeHighLight(13, 30, 11),
                new CodeHighLight(92, 94, 2),
                new CodeHighLight(94, 101, 3), 
                new CodeHighLight(200, 203, 4), 
                new CodeHighLight(250, 263, 5), 
                new CodeHighLight(304, 324, 6), 
                new CodeHighLight(350, 354, 7)
            ).toArrayList()));
//        System.out.println(segment.byLines(0, 25));
    }
    
}
