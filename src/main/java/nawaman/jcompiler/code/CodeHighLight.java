package nawaman.jcompiler.code;

/**
 * CodeHighLight represent a highlight in the code.
 */
public record CodeHighLight(int startOffset, int endOffset, int color) {
    
    public CodeHighLight {
        if (startOffset < 0) {
            var message = "Start offset must be greater than or equal to 0. startOffset=" + startOffset;
            throw new IllegalArgumentException(message);
        }
        if ((endOffset >= 0) && (endOffset < startOffset)) {
            var message 
            = "End offset must be greater than or equal to the start offset. "
                    + "startOffset=" + startOffset + ", endOffset=" + endOffset;
            throw new IllegalArgumentException(message);
        }
    }
    
    /** @return the endOffset. */
    public int endOffset() {
        return (endOffset < 0) ? startOffset : endOffset;
    }
    
    /** @return the color. */
    public int color() {
        return color;
    }
    
}