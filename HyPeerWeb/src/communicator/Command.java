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
	/**
	 * The list of parameter types of the method.  The must appear in the same order as they appeared in the definition
	 * of the method.  They should be fully qualified.
	 */
	private String[] parameterTypeNames;
	//The actual parameters to be used when the method is invoked.
	private Object[] actualParameters;
	//Indicates whether a result is expected.
	protected boolean sync;
	//localObjectId of the object of target object
	protected LocalObjectId localObjectId;
	
	/**
	 * Create a command object
	 * @param lobj the localObjectId of the object on which the command is to be invoked.  The value null
	 * indicates the method is a static method of the class.
	 * @param cname the class in which the method is defined.
	 * @param mname the name of the method to be invoked.
	 * @param parTypes the names of parameter types in the method specification.  Used to uniquely distinguish
	 * two methods with the same same.  The names should be fully qualified.
	 * @param parVals the actual parameters to be used when invoking the method.
	 */
	public Command(String cname, String mname, String[] parTypes, Object[] parVals){
		this.className = cname;
		this.methodName = mname;
		this.parameterTypeNames = parTypes;
		this.actualParameters = parVals;
	}
	
	/**
	 * Executes this method on the indicated object.
	 * @return the return value of the target method
	 */
	public Object execute(){
		try{
			Class<?> targetClass = Class.forName(className);
			Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
			for (int i = 0; i < parameterTypeNames.length; i++)
				parameterTypes[i] = Class.forName(parameterTypeNames[i]);
			Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
			Object target = null;
			if (localObjectId != null)
				target = ObjectDB.getSingleton().getValue(localObjectId);
			return method.invoke(target, actualParameters);
		} catch(Exception e){
			return e;
		}
	}
	
	/**
	 * isSynchronous getter.
	 * 
	 * @pre <i>None</i>
	 * @post result = isSynchronous
	 */
	public boolean isSynchronous(){
		return sync;
	}
}