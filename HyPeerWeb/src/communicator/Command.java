package communicator;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Implements the Command pattern in such a way that the command can be sent over a socket.
 * @author Scott Woodfield
 */
public class Command implements Serializable{	
	//The class in which the method is defined.
	private final String className;
	//The name of the method to be invoked.
	private final String methodName;
	//Fully qualified parameter class names (or raw primitive name: "int", "boolean", etc)
	private final String[] paramTypes;
	//The actual parameters to be used when the method is invoked.
	private final Object[] paramVals;
	//Indicates whether a result is expected.
	protected boolean sync;
	//localObjectId of the object of target object
	protected int UID;
	
	/**
	 * Create a command object
	 * @param cname the class in which the method is defined.
	 * @param mname the name of the method to be invoked.
	 * @param paramTypes the names of parameter types in the method specification.  Used to uniquely distinguish
	 * two methods with the same same.  The names should be fully qualified.
	 * @param paramVals the actual parameters to be used when invoking the method.
	 */
	public Command(String cname, String mname, String[] paramTypes, Object[] paramVals){
		this.className = cname;
		this.methodName = mname;
		this.paramTypes = paramTypes == null ? new String[0] : paramTypes;
		this.paramVals = paramVals == null ? new Object[0] : paramVals;
	}
	
	/**
	 * Executes this method on the indicated object.
	 * @return the return value of the target method
	 */
	public Object execute(){
		try{
			Class<?> targetClass = getClass(className);
			Class<?>[] parameterTypes = new Class<?>[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++)
				parameterTypes[i] = getClass(paramTypes[i]);
			Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
			Object target = Communicator.resolveId(targetClass, UID);
			return method.invoke(target, paramVals);
		} catch (Exception e){
			return e;
		}
	}
	
	/**
	 * isSynchronous getter
	 * @return true, if this command is synchronous
	 */
	public boolean isSynchronous(){
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
	private Class<?> getClass(String className) throws Exception{
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