package nawaman.jcompiler.code;

import static functionalj.list.intlist.IntFuncList.infinite;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import functionalj.list.FuncList;
import functionalj.list.intlist.IntFuncList;

/**
 * This class represents a specification for code.
 * 
 * This class processes the code in the line, column as well as the offset.
 * The processing of the line is done lazily.
 * The intention is that most of the code should not need to be processed.
 * 
 * Note: All index and offset are 0-based.
 * 
 * Note: To account for the two character new line (i.e. '\r\n'), the new line offset is stored as negative.
 * 
 * Note: Although once processed, the processing might not be the most optimize,
 *         the code only be processed when needed.
 *       And the space used is optimized. (as much as I could)
 */
public class Code {
    
    /** The default tab size. */
    public static final int DEFAULT_TAB_SIZE = 4;
    
    private final int ARRAY_LENGTH = 32;
    
    private final String content;
    private final int    tabSize;
    
    private final List<int[]> newLines = new ArrayList<>(16);
    private int latestInArray   = 0;
    private int processedOffset = 0;
    
    /**
     * Constructs a new instance of Code with the given content with default tabSize.
     *
     * @param content  The content of the code with new lines.
     */
    public Code(String content) {
        this(content, DEFAULT_TAB_SIZE);
    }
    
    /**
     * Constructs a new instance of Code with the given content and tab size.
     * 
     * If the tab size is less than 0, it will be set to the default tab size.
     * If the tab size is 0, it will be set to 1.
     * 
     * @param content  the content of the code with new lines.
     * @param tabSize  the tab size.
     */
    public Code(String content, int tabSize) {
        this.content = content;
        this.tabSize = (tabSize < 0) ? DEFAULT_TAB_SIZE : max(1, tabSize);
    }
    
    /** @return  the content of the code. */
    public final String content() {
        return content;
    }
    
    /** @return  the tab size. */
    public final int tabSize() {
        return tabSize;
    }
    
    /** @return  the length of the content. */
    public final int length() {
        return content.length();
    }
    
    //== Terminate ==
    
    /** @return  the number of lines in the content. */
    public final int lineCount() {
        processAllLines();
        int newlineCount = knownLineCount();
        return newlineCount + 1;
    }
    
    /**
     * Ensures that all the lines are processed.
     */
    public void processAllLines() {
        processToLineCount(Integer.MAX_VALUE);
    }
    
    //== Step ==
    
    /**
     * Ensures that the code is processed up to the given offset.
     * If the offset is beyond the length of the content, it will process up to the end of the content.
     *
     * @param offset  the offset up to which the code should be processed.
     */
    public final void processLinesToOffset(int offset) {
        processLinesUpTo(offset, Integer.MAX_VALUE);
    }
    
    /**
     * Ensures that the code is processed up to the given line count.
     * If the line count is beyond the number of lines in the content, it will process all lines.
     *
     * @param lineCount  the number of lines up to which the code should be processed.
     */
    public final void processToLineCount(int lineCount) {
        processLinesUpTo(Integer.MAX_VALUE, lineCount);
    }
    
    private void processLinesUpTo(int offset, int lines) {
        var lineCount = knownLineCount();
        var content   = content();
        int length    = content.length();
        offset = Math.min(offset, length);
        for ( ; (processedOffset < offset) && (lineCount < lines); processedOffset++) {
            var ch = content.charAt(processedOffset);
            if (ch == '\r') {
                if (((processedOffset + 1) < length) && (content.charAt(processedOffset + 1) == '\n')) {
                    processedOffset++;
                    addNewLine(-processedOffset);
                    lineCount++;
                } else {
                    addNewLine(processedOffset);
                    lineCount++;
                }
            } else if (ch == '\n') {
                addNewLine(processedOffset);
                lineCount++;
            }
        }
    }
    
    private void addNewLine(int newLineOffset) {
        if (latestInArray >= ARRAY_LENGTH) {
            latestInArray = 0;
            newLines.add(new int[ARRAY_LENGTH]);
        }
        
        int lastLine = newLines.size();
        if (lastLine == 0) {
            newLines.add(new int[ARRAY_LENGTH]);
            lastLine = 1;
        }
        
        var array = newLines.get(lastLine - 1);
        array[latestInArray] = newLineOffset;
        
        latestInArray++; 
    }
    
    /** @return  the currently known number of lines. */
    public final int knownLineCount() {
        if (latestInArray == 0) {
            return 0;
        }
        
        int currentNewlineCount = (newLines.size() - 1)*ARRAY_LENGTH + latestInArray;
        return currentNewlineCount;
    }
    
    /** @return  the offset up to which the code is processed. */
    final int processedOffset() {
        return processedOffset;
    }
    
    /** @return  if all the line has been processed. */
    final boolean isProcessedAll() {
        return processedOffset >= content.length();
    }
    
    /** @return  a list of newline offsets. */
    public final IntFuncList newlineOffsets() {
        processAllLines();
        var lineCount = knownLineCount();
        return IntFuncList.from(newLines.stream().flatMapToInt(array -> IntStream.of(array)))
                .map(Math::abs)
                .limit(lineCount);
    }
    
    /**
     * Returns the start offset of the line at the given line number.
     *
     * @param lineNumber  the line number.
     * @return            the start offset of the line at the given line number.
     */
    public final int startOffset(int lineNumber) {
        if (lineNumber < 0) {
            var message = "Line number must be greater than or equal to 0: lineNumber=" + lineNumber;
            throw new IndexOutOfBoundsException(message);
        }
        if (lineNumber == 0) {
            return 0;
        }
        if (isProcessedAll()) {
            int lineCount = lineCount();
            if (lineNumber >= lineCount) {
                var message 
                        = "Line number must be less than the line count: lineNumber=%s, lineCount=%s"
                        .formatted(lineNumber, lineCount);
                throw new IndexOutOfBoundsException(message);
            }
        }
        
        int end = rawEndOffset(lineNumber - 1);
        return ((end < 0) ? -end : end) + 1;
    }
    
    /**
     * Returns the end offset of the line at the given line number.
     *
     * @param lineNumber  the line number.
     * @return            the end offset of the line at the given line number.
     */
    public final int endOffset(int lineNumber) {
        int end = rawEndOffset(lineNumber);
        return (end < 0) ? (-end - 1) : end;
    }
    
    final int rawEndOffset(int lineNumber) {
        if (lineNumber < 0) {
            var message = "Line number must be greater than or equal to 0: lineNumber=" + lineNumber;
            throw new IndexOutOfBoundsException(message);
        }
        
        processToLineCount(lineNumber + 1);
        if (isProcessedAll()) {
            int lineCount = lineCount();
            if (lineNumber >= lineCount) {
                var message 
                        = "Line number must be less than the line count: lineNumber=%s, lineCount=%s"
                        .formatted(lineNumber, lineCount);
                throw new IndexOutOfBoundsException(message);
            }
        }
        
        int end          = content.length();
        int newlineCount = knownLineCount();
        if (lineNumber != newlineCount) {
            var arrayIndex = lineNumber / ARRAY_LENGTH;
            var lineIndex  = lineNumber % ARRAY_LENGTH;
            var array      = newLines.get(arrayIndex);
            end            = array[lineIndex];
        }
        return end;
    }
    
    /**
     * Returns the line at the given line number.
     *
     * @param lineNumber  the line number.
     * @return            the line at the given line number.
     */
    public final String line(int lineNumber) {
        var start   = startOffset(lineNumber);
        var end     = endOffset(lineNumber);
        var content = content();
        return content.substring(start, end);
    }
    
    /**
     * Returns the line at the given line number including the newline.
     * 
     * @param lineNumber  the line number.
     * @return            the line at the given line number including the newline.
     */
    public final String lineLn(int lineNumber) {
        int knownLineCount = knownLineCount();
        if (lineNumber < knownLineCount - 1) {
            processToLineCount(knownLineCount + 1);
        }
        
        var start   = startOffset(lineNumber);
        var end     = (lineNumber < knownLineCount - 1)
                    ? startOffset(lineNumber + 1)
                    : min(endOffset(lineNumber) + 1, length());
        var content = content();
        return content.substring(start, end);
    }
    
    /** @return  a stream of lines in the content. */
    public final FuncList<String> lines() {
        return infinite()
                .acceptUntil(index -> isProcessedAll() && (index > knownLineCount()))
                .mapToObj(this::line);
    }
    
    /** @return  a stream of lines with newline in the content. */
    public final FuncList<String> lineLns() {
        return infinite()
                .acceptUntil(index -> isProcessedAll() && (index > knownLineCount()))
                .mapToObj(this::lineLn);
    }
    
    /**
     * Returns the line number at the given offset.
     *
     * @param offset  the offset.
     * @return        the line number at the given offset.
     */
    public int lineNumberAtOffset(int offset) {
        processLinesToOffset(offset);
        if (newLines.size() == 0) {
            return 0;
        }
        if (offset >= length()) {
            return lineCount() - 1;
        }
        
        var arrayIndex = findArrayIndex(i -> abs(newLines.get(i)[0]), newLines.size(), offset);
        if (arrayIndex >= newLines.size()) {
            return knownLineCount();
        }
        
        var array    = newLines.get(arrayIndex);
        var endIndex = (arrayIndex == newLines.size() - 1) ? latestInArray : ARRAY_LENGTH;
        return findLineNumber(i -> abs(array[i]), endIndex, offset);
    }
    
    // Binary search for the best index of a newline array where the offset is located.
    private int findArrayIndex(IntUnaryOperator newlineOffsets, int stopIndex, int offset) {
        return findIndex(newlineOffsets, stopIndex, offset, true);
    }
    
    // Binary search for the best index of a newline offset -- use to find the best line number.
    private int findLineNumber(IntUnaryOperator newlineOffsets, int lastNewLineIndex, int offset) {
        return findIndex(newlineOffsets, lastNewLineIndex, offset, false);
    }
    
    private int findIndex(IntUnaryOperator newlineOffsets, int stopIndex, int offset, boolean returnSteps) {
        int left = 0;
        int right = stopIndex - 1;
        int steps = 0;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            steps++;
            
            int midValue = newlineOffsets.applyAsInt(mid);
            if (midValue == offset) {
                return returnSteps ? 0 : mid;
            }
            
            if (midValue < offset) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return returnSteps ? steps - 1 : left;
    }
    
    /**
     * Returns the line at the given offset.
     *
     * @param offset  the offset.
     * @return        the line at the given offset.
     */
    public String lineAtOffset(int offset) {
        int lineNumber = lineNumberAtOffset(offset);
        var line       = line(lineNumber);
        return line;
    }
    
}
