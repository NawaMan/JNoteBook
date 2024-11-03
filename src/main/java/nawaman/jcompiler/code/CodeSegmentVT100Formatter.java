package nawaman.jcompiler.code;

import static functionalj.list.FuncList.listOf;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.IntStream.rangeClosed;
import static nawaman.jcompiler.code.RulerOneLine.oneLineRuler;
import static nawaman.jcompiler.code.RuleTwoLine.bottomTwoLineRuler;
import static nawaman.jcompiler.code.RuleTwoLine.topTwoLineRuler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import functionalj.function.Func2;
import functionalj.list.FuncList;

/**
 * Code Segment which displays with VT100 code.
 */
public class CodeSegmentVT100Formatter extends CodeSegmentFormatter {
    
    public static final Func2<Code, Boolean, CodeSegmentFormatter> segmentCreator 
                = (code, isOneLineRuler) -> new CodeSegmentVT100Formatter(code, isOneLineRuler != Boolean.FALSE);
                
    public static final Func2<Code, Boolean, CodeSegmentFormatter> vt100SegmentCreator = segmentCreator;
    
    private static final String           VT100_HIGHLIGHT_END    = "\u001B[0m";
    private static final FuncList<String> VT100_HIGHLIGHT_STARTS = FuncList.of(
                "\u001B[41;1;3;37m",
                "\u001B[42;1;3;37m",
                "\u001B[44;1;3;37m",
                "\u001B[45;1;3;37m",
                "\u001B[46;1;3;37m",
                "\u001B[100;1;3;37m",
                "\u001B[43;1;3;37m",
                "\u001B[101;1;3;200m",
                "\u001B[102;1;3;200m",
                "\u001B[104;1;3;200m",
                "\u001B[105;1;3;200m",
                "\u001B[106;1;3;200m",
                "\u001B[47;1;3;200m",
                "\u001B[103;1;3;200m"
            );
    
    private final Ruler topRuler;
    private final Ruler bottomRuler;
    
    public CodeSegmentVT100Formatter(Code code) {
        this(code, false);
    }
    
    public CodeSegmentVT100Formatter(Code code, boolean isOneLineRuler) {
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
            var codeLine   = code.lineLn(i);
            
            int lineStartOffset = code.startOffset(i);
            int lineEndOffset =   (i < code.knownLineCount() - 1) ? code.startOffset(i + 1) : code.endOffset(i);
            
            codeLine = highLightLine(highlights, codeLine, lineStartOffset, lineEndOffset);
            
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
    
    private String highLightLine(List<CodeHighLight> codehighlights, String codeLine, int lineStartOffset, int lineEndOffset) {
        var highlights = new ArrayList<>(codehighlights);
        highlights.sort(Comparator.<CodeHighLight>comparingInt(h -> h.startOffset()).thenComparingInt(h -> h.endOffset()));
        
        var segments = new ArrayList<CodeHighLight>();
        int currentPos = 0;
        
        for (int h = 0; h < highlights.size(); h++) {
            var highlight = highlights.get(h);
            int startOffset = max(highlight.startOffset() - lineStartOffset, 0);
            int endOffset   = min(highlight.endOffset()   - lineStartOffset, codeLine.length());
            
            if (startOffset >= codeLine.length() || endOffset <= 0) continue;
            
            if (currentPos < startOffset) {
                segments.add(new CodeHighLight(currentPos, startOffset, -1)); // -1 for no highlight
            }
            segments.add(new CodeHighLight(startOffset, endOffset, h));
            currentPos = Math.max(currentPos, endOffset);
        }
        
        if (currentPos < codeLine.length()) {
            segments.add(new CodeHighLight(currentPos, codeLine.length(), -1));
        }
        
        var result = new StringBuilder();
        for (var segment : segments) {
            var part = codeLine.substring(segment.startOffset(), segment.endOffset());
            if (segment.color() == -1) {
                result.append(highlightLineSegment(part));
            } else {
                result.append(highlightCodeSegment(part, 0, part.length(), segment.color()));
            }
        }
        
        return result.toString();
    }
        
    private String highlightCodeSegment(String codeLine, int start, int end, int color) {
        return highlightLineSegment(codeLine.substring(0, start))
            + VT100_HIGHLIGHT_STARTS.get(color % VT100_HIGHLIGHT_STARTS.size()) 
            + codeLine
                .substring(start, end)
                .replaceAll(" ", "·")
                .replaceAll("\t", "—————————————————".substring(0, tabSize - 1) + "→")
                .replaceAll("\r", "↵")
                .replaceAll("\n", "¶")
            + VT100_HIGHLIGHT_END 
            + highlightLineSegment(codeLine.substring(end));
    }
    
    private String highlightLineSegment(String codeLine) {
        codeLine = codeLine.replaceAll("(\bclass\b)",  "\u001B[1m$1\u001B[0m");
        return codeLine
                .replaceAll("([\\(\\)\\{\\}\\[\\]\\\"\\\'\\.;,+-]+)", "\u001B[1m$1\u001B[0m")
                .replaceAll("(\\bprivate\\b|\\bstatic\\b|\\bpublic\\b|\\bvoid\\b|\\bvar\\b|\\bint\\b|\\bfor\\b|\\bif\\b|\\bclass\\b)", "\u001B[1m$1\u001B[0m")
                .replaceAll(" ",  "\u001B[38;2;200;200;200m·\u001B[0m")
                .replaceAll("\t", "\u001B[38;2;200;200;200m" + "—————————————————".substring(0, tabSize - 1) + "→\u001B[0m")
                .replaceAll("\r", "\u001B[38;2;200;200;200m↵\u001B[0m")
                .replaceAll("\n", "\u001B[38;2;200;200;200m¶\u001B[0m");
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
        System.out.println(new CodeSegmentVT100Formatter(code, true).byLines(0, 10, listOf(
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
        
        System.out.println(new CodeSegmentVT100Formatter(code, false).byLines(0, 10, listOf(
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
