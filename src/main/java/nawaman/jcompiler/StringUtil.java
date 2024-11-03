package nawaman.jcompiler;

/**
 * Utility class for string operations.
 */
public class StringUtil {
    
    public static final String INDENT = "    ";
    
    /**
     * Indents a string with a given indent string.
     * 
     * @param input   the string to indent
     * @return        the indented string
     */
    public static String indentString(String input) {
        return indentString(input, INDENT);
    }
    
    /**
     * Indents a string with a given indent string.
     * 
     * @param input   the string to indent
     * @param indent  the indent string
     * @return        the indented string
     */
    public static String indentString(String input, String indent) {
        var lines    = input.split("\n");
        var indented = new StringBuilder();
        
        for (var line : lines) {
            indented.append(indent).append(line).append("\n");
        }
        
        if (indented.length() > 0) {
            indented.setLength(indented.length() - 1);
        }
        
        return indented.toString();
    }
    
}
