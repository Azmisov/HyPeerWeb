package hypeerweb;

/**
 * A command that can be executed on a node
 * @author Josh
 */
public abstract class Command {
    
    private Parameters parameters;
    
    public Command(Parameters parameters) {
	this.parameters = parameters;
    }
    
    public abstract void execute(Node node);
}
