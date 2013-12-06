package communicator;

import hypeerweb.LinksProxy;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Implements the Command pattern in such a way that the command can be sent over a socket.
 * @author Scott Woodfield
 */
public class Command implements Serializable{
	public static final String className = Command.class.getName();
	//The class in which the method is defined.
	protected final String clazz;
	//The name of the method to be invoked.
	public final String methodName;
	//Fully qualified parameter class names (or raw primitive name: "int", "boolean", etc)
	protected transient ArrayList<String> paramTypes_lst = new ArrayList();
	protected String[] paramTypes;
	//The actual parameters to be used when the method is invoked.
	protected transient ArrayList<Object> paramVals_lst = new ArrayList();
	protected Object[] paramVals;
	//Indicates whether a result is expected.
	protected boolean sync;
	//localObjectId of the object of target object
	protected int UID;
	//number of inserted parameters
	protected int addedParamCount = 0;
	
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
		clazz = cname;
		methodName = mname;
		if (ptypes != null)
			paramTypes_lst.addAll(Arrays.asList(ptypes));
		if (pvals != null)
			paramVals_lst.addAll(Arrays.asList(pvals));
	}
	
	//MANIPULATE PARAMETERS
	public void setParameter(int index, Object paramVal){
		paramVals_lst.set(index, paramVal);
	}
	public void insertParameter(int index, String paramType, Object paramVal){
		addedParamCount++;
		paramTypes_lst.add(index, paramType);
		paramVals_lst.add(index, paramVal);
	}
	/**
	 * Retrieve a parameter value
	 * @param index parameter index
	 * @return Object at this parameter
	 */
	public Object getParameter(int index){
		return paramVals_lst.get(index);
	}
	
	/**
	 * Executes this method on the indicated object.
	 * @return the return value of the target method
	 */
	public Object execute(){
		int l = paramTypes_lst.size();
		try{
			Class<?> targetClass = resolveClassName(clazz);
			Class<?>[] parameterTypes = new Class<?>[l];
			for (int i = 0; i < l; i++)
				parameterTypes[i] = resolveClassName(paramTypes_lst.get(i));
			Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
			//Override protected modifier
			if (!method.isAccessible())
				method.setAccessible(true);
			Object target = null;
			//If the target isn't a static method, resolve the UID
			if (!Modifier.isStatic(method.getModifiers())){
				target = Communicator.resolveId(targetClass, UID);
				if (target == null)
					System.err.println("Failed to resolve UID for remote object: "+targetClass+": "+UID);
			}
			return method.invoke(target, paramVals_lst.toArray(new Object[l]));
		} catch (Exception e){
			System.err.println("Command: Failed to execute "+clazz+"."+methodName);
			if (e.getCause() != null)
				e.getCause().printStackTrace();
			else{
				//This is a reflection error
				System.err.println("Reflection on: "+methodName+"("+Arrays.toString(paramTypes_lst.toArray(new String[l]))+")");
				System.err.println("With parameters: "+Arrays.toString(paramVals_lst.toArray(new Object[l])));
				e.printStackTrace();
			}
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
	
	public Object writeReplace() throws ObjectStreamException {
		//Convert dynamic list to regular list
		paramTypes = paramTypes_lst.toArray(new String[paramTypes_lst.size()]);
		paramVals = paramVals_lst.toArray(new Object[paramVals_lst.size()]);
		return this;
	}
	public Object readResolve() throws ObjectStreamException {
		//Convert regular list to dynamic list
		paramTypes_lst = new ArrayList(Arrays.asList(paramTypes));
		paramVals_lst = new ArrayList(Arrays.asList(paramVals));
		return this;
	}
}