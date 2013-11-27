package communicator;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Implements the Command pattern in such a way that the command can be sent over a socket.
 * @author Scott Woodfield
 */
public class Command implements Serializable{	
	//The class in which the method is defined.
	protected final String className;
	//The name of the method to be invoked.
	protected final String methodName;
	//Fully qualified parameter class names (or raw primitive name: "int", "boolean", etc)
	protected String[] paramTypes;
	//The actual parameters to be used when the method is invoked.
	protected Object[] paramVals;
	//Indicates whether a result is expected.
	protected boolean sync;
	//localObjectId of the object of target object
	protected int UID;
	
	/**
	 * Create a command object with no parameters
	 * @param cname the class in which the method is defined
	 * @param mname the name of the method to be invoked
	 */
	public Command(String cname, String mname){
		this(cname, mname, null, null);
	}
	/**
	 * Create a command object
	 * @param cname the class in which the method is defined.
	 * @param mname the name of the method to be invoked.
	 * @param ptypes the names of parameter types in the method specification.  Used to uniquely distinguish
	 * two methods with the same same.  The names should be fully qualified.
	 * @param pvals the actual parameters to be used when invoking the method.
	 */
	public Command(String cname, String mname, String[] ptypes, Object[] pvals){
		className = cname;
		methodName = mname;
		paramTypes = ptypes == null ? new String[0] : ptypes;
		paramVals = pvals == null ? new Object[0] : pvals;
	}
	
	/**
	 * Adds a parameter to the beginning of the list
	 * This is an O(n) operation
	 * @param paramType the parameter class name
	 * @param paramVal the parameter's value
	 */
	public void addParameter(String paramType){
		paramTypes = unshift(paramTypes, new String[paramTypes.length+1]);
		paramTypes[0] = paramType;
		paramVals = unshift(paramVals, new Object[paramVals.length+1]);
	}
	private static <T> T[] unshift(T[] original, T[] shifted){
		for (int i=1, l=original.length; i<=l; i++)
			shifted[i] = original[i-1];
		return shifted;
	}
	
	/**
	 * Sets the value of a parameter
	 * @param index the index of the parameter
	 * @param o the value of the parameter
	 */
	public void setParameter(int index, Object o){
		paramVals[index] = o;
	}
	
	/**
	 * Executes this method on the indicated object.
	 * @return the return value of the target method
	 */
	public Object execute(){
		try{
			Class<?> targetClass = resolveClassName(className);
			Class<?>[] parameterTypes = new Class<?>[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++)
				parameterTypes[i] = resolveClassName(paramTypes[i]);
			Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
			//Override protected modifier
			if (!method.isAccessible())
				method.setAccessible(true);
			Object target = null;
			//If the target isn't a static method, resolve the UID
			if (!Modifier.isStatic(method.getModifiers()))
				target = Communicator.resolveId(targetClass, UID);
			return method.invoke(target, paramVals);
		} catch (Exception e){
			System.err.println("Command: Failed to execute "+className+"."+methodName);
			//e.printStackTrace();
			e.getCause().printStackTrace();
			return e;
		}
	}
	
	/**
	 * isSynchronous getter
	 * @return true, if this command is synchronous
	 */
	protected boolean isSynchronous(){
		return sync;
	}
	
	/**
-	 * Returns a class for the indicated className.  Works for built in and non-built in classes.  Usually you would
-	 * use the Class.forName(String) method but this doesn't work for built in types such as <i>int</i>.  This method
-	 * checks to see if the name is the name of a built in type and, if so, returns the appropriate class.  Otherwise
-	 * it returns Class.forName(className).
-	 * 
-	 * @param className the name of the class we want to return.
-	 * @throws Exception  if a class with the indicated name is not found.
-	 */
	private static Class<?> resolveClassName(String className) throws Exception{
		switch (className) {
			case "boolean":	return boolean.class;
			case "byte":	return byte.class;
			case "char":	return char.class;
			case "short":	return short.class;
			case "int":		return int.class;
			case "long":	return long.class;
			case "float":	return float.class;
			case "double":	return double.class;
			default:		return Class.forName(className);
		}
	}
}