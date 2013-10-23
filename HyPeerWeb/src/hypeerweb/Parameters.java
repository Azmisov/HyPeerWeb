/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the commands mapped to specific visitors
 * @author briands
 */
public class Parameters {
    private HashMap<String, Object> commandMap;
            
            public Parameters(){
                commandMap = new HashMap();
            }
            
            public Map getCommandMap(){
                return commandMap;
            }
            
            public Object get(String name){
                return commandMap.get(name);
            }
            
            public void set(String name, Object value){
                commandMap.put(name, value);
            }
}
