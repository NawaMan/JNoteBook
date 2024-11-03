package nawaman.jcompiler;

/**
 * Exception thrown by the JCompiler.
 */
public class JCompilerException extends Exception {
    
    private static final long serialVersionUID = -797502479383421434L;
    
    /**
     * The failures that can occur.
     */
    public static enum Failure {
        GetCompiler("Failed to get the Java compiler"),
        CreateJavaFileManager("Failed to create a Java file manager"),
        CloseJavaFileManager("Failed to close the Java file manager");
        
        private final String message;
        
        private Failure(String message) {
            this.message = message;
        }
        
        public String message() {
            return message;
        }
    }
    
    private final Failure failure;
    
    /**
     * Constructs a new JCompilerException with the specified failure.
     * 
     * @param failure the failure
     */
    public JCompilerException(Failure failure) {
        this(failure, null);
    }
    
    /**
     * Constructs a new JCompilerException with the specified failure.
     * 
     * @param failure  the failure
     * @param cause    the cause exception
     */
    public JCompilerException(Failure failure, Exception cause) {
        super(failure.toString(), cause);
        this.failure = failure;
    }
    
    /**
     * Constructs a new JCompilerException with the specified failure.
     * 
     * @param failure the failure
     * @param message the message
     * @param cause   the cause
     */
    public JCompilerException(Failure failure, String message, Exception cause) {
        super(failure.message() + ": " + message, cause);
        this.failure = failure;
    }
    
    /**
     * Returns the failure.
     * 
     * @return the failure
     */
    public Failure getFailure() {
        return failure;
    }
    
    /**
     * Returns the message.
     * 
     * @return the message
     */
    public String message() {
        return this.getMessage();
    }
    
    /**
     * Returns the cause.
     * 
     * @return the cause
     */
    public Exception cause() {
        return (Exception)getCause();
    }
    
}
