package nawaman.jcompiler.code;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.List;

import functionalj.list.FuncList;

/**
 * A segment of code.
 */
public abstract class CodeSegmentFormatter {
    
    protected final Code   code;
    protected final String content;
    protected final int    tabSize;
    
    public CodeSegmentFormatter(Code code) {
        this.code    = code;
        this.content = code.content();
        this.tabSize = code.tabSize();
    }
    
    /** @return  the code this segment */
    public final Code code() {
        return code;
    }
    
    /** @return the content of the code. */
    public final String content() {
        return content;
    }
    
    /** @return the tab size of the code. */
    public final int tabSize() {
        return tabSize;
    }
    
    /** @return  the start offset of this segment. */
    public final CharSequence byOffset(int startOffset, CodeHighLight ... highlights) {
        return byOffset(startOffset, FuncList.from(highlights));
    }
    
    /** @return  the start offset of this segment. */
    public final CharSequence byOffset(int startOffset, List<CodeHighLight> highlights) {
        return byOffsets(startOffset, startOffset, highlights);
    }
    
    /** @return  the segment from the start offset to the end offset. */
    public final CharSequence byOffsets(int startOffset, int endOffset, CodeHighLight ... highlights) {
        return byOffsets(startOffset, endOffset, FuncList.from(highlights));
    }
    
    /** @return  the segment from the start offset to the end offset. */
    public final CharSequence byOffsets(int startOffset, int endOffset, List<CodeHighLight> highlights) {
        if (startOffset < 0)
            startOffset = 0;
        if (endOffset < 0)
            endOffset = startOffset;
        if (endOffset < startOffset) {
            var temp = startOffset;
            startOffset = endOffset;
            endOffset = temp;
        }
        
        var code = code();
        var lastLine  = max(0, code.lineNumberAtOffset(endOffset) + 1);
        code.processLinesToOffset(lastLine);
        code.processToLineCount(code.knownLineCount() + 1);
        
        var firstLine = max(0, code.lineNumberAtOffset(startOffset) - 1);
        lastLine      = min(lastLine + 1, code.knownLineCount());
        
        return byLines(firstLine, lastLine, highlights);
    }
    
    /** @return  the segment from the start offset to the end offset. */
    public final CharSequence byLine(int firstLine, int lastLine) {
        if (firstLine < 0)
            firstLine = 0;
        if (lastLine < 0)
            lastLine = firstLine;
        if (lastLine < firstLine) {
            var temp = firstLine;
            firstLine = lastLine;
            lastLine = temp;
        }
        return byLines(firstLine, lastLine, FuncList.empty());
    }
    
    /** @return  the segment from the start offset to the end offset. */
    public final CharSequence byLines(int firstLine, int lastLine, CodeHighLight ... highlights) {
        return byLines(firstLine, lastLine, FuncList.from(highlights));
    }
    
    /** @return  the segment from the start offset to the end offset */
    public abstract CharSequence byLines(int firstLine, int lastLine, List<CodeHighLight> highlights);
    
}
