package communicator;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Implements the Command pattern in such a way that the command can be sent over a socket.
 * 
 * <pre>
 * <b>Domain:</b>
 *     localObjectId      : LocalObjectId -- the id of the object on which this command is to be executed.
 *                                           If this is null it means the method is a static method.
 *     className          : String        -- the fully qualified name of the class containing the method to be executed.
 *     methodName         : String        -- the name of the method to be executed.
 *     parameterTypeNames : String[]      -- a list of names of the parameter types of the method to be executed.
 *                                        -- Used to identify the proper method since two methods with the same name are
 *                                        -- uniquely identified by the sequence of parameter names in the method's specification
 *     actualParameters   : Object[]      -- the actual parameters used to invoke the indicated method.
 *     isSynchronous      : boolean       -- indicates whether the invoking object is waiting for a result.
 *     
 *     <b>Invariant</b>
 *     className &ne; null AND methodName &ne; null AND parameterTypeNames &ne; null AND actualParameters &ne; null AND
 *     |actualParameters| = |parameterTypeNames| AND there exists a method in the given class with the specified name and
 *     parameters of the given parameterTypeNames and in the given sequence AND
 *     &forall; 0 &lt; |actualParameters| (actualParameters[i] &isin; Class.forName(parameterTypeNames[i]))
 * </pre>
 * 
 * @author Scott Woodfield
 */
public class Command
    implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * The localObjectId of the object on which the command is to be invoked.  The value null indicates the method
	 * is a static method of the class.
	 */
	private LocalObjectId localObjectId;
	
	/**
	 * The class in which the method is defined.
	 */
	private String className;
	
	/**
	 * The name of the method to be invoked.
	 */
	private String methodName;
	
	/**
	 * The list of parameter types of the method.  The must appear in the same order as they appeared in the definition
	 * of the method.  They should be fully qualified.
	 */
	private String[] parameterTypeNames;
	
	/**
	 * The actual parameters to be used when the method is invoked.
	 */
	private Object[] actualParameters;
	
	/**
	 * Indicates whether a result is expected.
	 */
	private boolean isSynchronous;
	
	/**
	 * The NULL_COMMAND.
	 */
	public static final Command NULL_COMMAND = new Command(null, null, null, null, null, false);
	
	/**
	 * The constructor.
	 * @param localObjectId the localObjectId of the object on which the command is to be invoked.  The value null
	 * indicates the method is a static method of the class.
	 * @param className the class in which the method is defined.
	 * @param methodName the name of the method to be invoked.
	 * @param parameterTypeNames the names of parameter types in the method specification.  Used to uniquely distinguish
	 * two methods with the same same.  The names should be fully qualified.
	 * @param actualParameters the actual parameters to be used when invoking the method.
	 * @param isSynchronous indicates whether a result is to be returned.
	 * 
	 * @pre className &ne; null AND methodName &ne; null AND parameterTypeNames &ne; null AND actualParameters &ne; null AND
	 * |parameterTypeNames| = | actualParameters| AND
	 * there is a class with the indicated class name that has a method ,m, with the indicated methodName such that the
	 * names of the parameter types of m match corresponding names in  parameterTypeNames AND each of the actualParameters
	 * is an instance of the corresponding parameterTypes in m.
	 * 
	 * @post
	 * A command that can be sent over a socket has been created.
	 */
	public Command(LocalObjectId localObjectId,
			       String className, 
			       String methodName,
			       String[] parameterTypeNames,
			       Object[] actualParameters,
			       boolean isSynchronous)
	{
		this.localObjectId = localObjectId;
		this.className = className;
		this.methodName = methodName;
		this.parameterTypeNames = parameterTypeNames;
		this.actualParameters = actualParameters;
		this.isSynchronous = isSynchronous;
	}
	
	/**
	 * The toString method for a command.
	 * 
	 * @pre <i>None</i>
	 * @post result = "Command:: localObjectId = " + localObjectId + ", className = " + className + ", methodName = " + methodName
	 */
	public String toString(){
		String result =
			"Command:: localObjectId = " + localObjectId + ", className = " + className + ", methodName = " + methodName;
		return result;
	}
	
	/**
	 * Executes this method on the indicated object.
	 * 
	 * @pre <i>None</i> as long as the invariants hold.
	 * @post the command is executed on the indicated object with the actual parameters.  If the command is synchronous the
	 * commands result is returned, otherwise null is returned.  If there was an exception, it is returned.
	 */
	public Object execute(){
		Object result = null;
		try{
			Class<?> targetClass = getClass(className);
			Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
			for(int i = 0; i < parameterTypeNames.length; i++){
				parameterTypes[i] = getClass(parameterTypeNames[i]);
			}
			Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
			Object target = null;
			if(localObjectId != null){
			    target = ObjectDB.getSingleton().getValue(localObjectId);
			}
			if(isSynchronous){
				result = method.invoke(target, actualParameters);
			} else {
				method.invoke(target, actualParameters);
			}
		}catch(Throwable t){
			result = t;
		}
		return result;
	}
	
	/**
	 * isSynchronous getter.
	 * 
	 * @pre <i>None</i>
	 * @post result = isSynchronous
	 */
	public boolean isSynchronous(){
		return isSynchronous;
	}
	
	/**
	 * Returns a class for the indicated className.  Works for built in and non-built in classes.  Usually you would
	 * use the Class.forName(String) method but this doesn't work for built in types such as <i>int</i>.  This method
	 * checks to see if the name is the name of a built in type and, if so, returns the appropriate class.  Otherwise
	 * it returns Class.forName(className).
	 * 
	 * @param className the name of the class we want to return.
	 * @throws Exception  if a class with the indicated name is not found.
	 * 
	 * @pre the className must be the name of a built in class or a valid user-defined class.
	 * @post result = the class with the given name.
	 */
    private Class<?> getClass(String className)
        throws Exception
    {
	    Class<?> result = null;
	    if(className.equals("boolean")){
	    	result = boolean.class;
	    } else if(className.equals("byte")){
	    	result = byte.class;
	    } else if(className.equals("char")){
	    	result = char.class;
	    } else if(className.equals("short")){
	    	result = short.class;
	    } else if(className.equals("int")){
	    	result = int.class;
	    } else if(className.equals("long")){
	    	result = long.class;
	    } else if(className.equals("float")){
	    	result = float.class;
	    } else if(className.equals("double")){
	    	result = double.class;
	    } else {
	    	result = Class.forName(className);
	    }
	    return result;
    }

}