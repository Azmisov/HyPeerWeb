package hypeerweb;

/**
 * Interface for the visitor pattern
 * @author briands
 */
public interface VisitorInterface {
    /**
     * This method will be called by the accept method in if statements
     * @param o
     * @return 
     */
    public Object visit(Object o);
}
