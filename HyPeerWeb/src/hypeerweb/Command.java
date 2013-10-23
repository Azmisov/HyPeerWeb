/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

/**
 *
 * @author condiej
 */
public abstract class Command {
    
    private Parameters parameters;
    
    public Command(Parameters parameters) {
	this.parameters = parameters;
    }
    
    public abstract void execute(Node node);
}
