package communicator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Used to create remote proxies.  The result may need some slight modification when finished.  Usually the import statements.
 * @author Scott Woodfield
 */
public class ProxyConstructor {
	public static void create(String className){
		try{
		    Class<?> javaClass = Class.forName(className);
		    String simpleClassName = javaClass.getSimpleName();
		    
			String result = "";
		    result += getPackageDeclaration(javaClass);
		    result += getImportStatements();
		    result += getClassDeclaration(Modifier.toString(javaClass.getModifiers()),  simpleClassName);
		    result += getAttributes() + "\n";
		    result += getConstructor(simpleClassName) + "\n";
		    result += getPublicMethods(javaClass);
		    result += "}";
		    System.out.println(result);
		}catch(Error error){
			System.err.println(error.getMessage());
		}catch(ClassNotFoundException exception){
			System.err.println(exception.toString());
		}
	}

	
	private static String getPackageDeclaration(Class<?> javaClass){
		String result = null;
		Package packageOfClass = javaClass.getPackage();
		if(packageOfClass != null){
			result = packageOfClass.toString() + ";\n\n";
		}
		return result;
	}
	
	private static String getImportStatements(){
		String result = "import GlobalObjectId;\n";
		return result;
	}
	
	private static String getClassDeclaration(String classModifiers, String simpleClassName) {
	    String result = classModifiers + " class " +  simpleClassName + "Proxy\n" +
                         "    extends " + simpleClassName + "\n{\n";
	    return result;
	}

	private static String getAttributes(){
		String result  = "    private GlobalObjectId globalObjectId;\n";
		return result;
	}
	
	private static String getConstructor(String className){
		String result  = "    public " + className + "Proxy(GlobalObjectId globalObjectId){\n";
		       result += "        this.globalObjectId = globalObjectId;\n";
		       result += "    }\n";
		return result;
	}
	
	private static String getPublicMethods(Class<?> javaClass){
		Method[] methods = javaClass.getMethods();
		String result = "";
		for(Method method : methods){
			result += getMethodDeclaration(method);
		}
		return result;
	}
	
	private static String getMethodDeclaration(Method method){
		String result = "";
		int modifiers = method.getModifiers();
		if(!Modifier.isFinal(modifiers)){
			String className = removeLeadingWordClassIfAny(method.getDeclaringClass().toString());
			String modifierString = Modifier.toString(modifiers);
			if(Modifier.isNative(modifiers)){
				if(modifierString.equals("native")){
					modifierString = "";
				} else if(modifierString.startsWith("native ")){
					modifierString = modifierString.replace("native ", "");
				} else {
					modifierString = modifierString.replace(" native", "");
				}
			}
			result  = "    " + modifierString + " ";
			String returnType = removeLeadingWordClassIfAny(method.getReturnType().toString());
			result += convertParameterTypeName(returnType);
			String methodName = method.getName();
			result += " " + methodName + "(";
			String[] parameterTypeNames = getParameterTypeNames(method);
			result += getParameterTypes(parameterTypeNames);
			result += "){\n";
			String returnTypeName = method.getReturnType().getName();
			result += createBody(className, methodName, parameterTypeNames, returnTypeName);
			result += "    }\n";
			result += "\n";
		}
		return result;
	}
	
	private static String createBody(String className,
			                         String methodName, 
			                         String[] parameterTypeNames,
			                         String returnTypeName
			                        )
	{
		String result = "";
		result += "        String[] parameterTypeNames = new String[" + parameterTypeNames.length + "];\n";
		for(int i = 0; i < parameterTypeNames.length; i++){
			result += "        parameterTypeNames[" + i + "] = \"" + parameterTypeNames[i] + "\";\n";
		}
		result += "        Object[] actualParameters = new Object[" + parameterTypeNames.length + "];\n";
		for(int i = 0; i < parameterTypeNames.length; i++){
		    result += "        actualParameters[" + i + "] = p" + i + ";\n";
		}
		if(returnTypeName.equals("void")){
		    result += "        Command command = new Command(globalObjectId.getLocalObjectId(), \"" + className + 
		                           "\", \"" + methodName + "\", parameterTypeNames, actualParameters, false);\n";
		} else {
			result += "        Command command = new Command(globalObjectId.getLocalObjectId(), \"" + className +
			                       "\", \"" + methodName + "\", parameterTypeNames, actualParameters, true);\n";
		}
		result += createInvocation(returnTypeName);
		result += createReturnStatement(returnTypeName);
		return result;
	}
	
	private static String createInvocation(String returnTypeName){
		String result = "";
		if(returnTypeName.equals("void")){
			result = "        PeerCommunicator.getSingleton().sendASynchronous(globalObjectId, command);\n";
		} else {
			result = "        Object result = PeerCommunicator.getSingleton().sendSynchronous(globalObjectId, command);\n";
		}
		return result;
	}
	
	private static String createReturnStatement(String returnTypeName){
		String result = "";
		if(!returnTypeName.equals("void")){
			String convertedReturnTypeName = convertReturnTypeName(returnTypeName);
			result = "        return (" + convertedReturnTypeName + ")result;\n";
		}
		return result;
	}
	
	private static String removeLeadingWordClassIfAny(String str){
		String result = str;
		if(str.startsWith("class ")){
			result = str.substring(6);
		} 
		return result;
	}
	
	private static String[] getParameterTypeNames(Method method){
		Class<?>[] parameterTypes = method.getParameterTypes();
		String result[] = new String[parameterTypes.length];
		int i = 0;
		for(Class<?> parameterType : parameterTypes){
			result[i] = removeLeadingWordClassIfAny(parameterType.toString());
			i++;
		}
		return result;
	}
	
	private static String getParameterTypes(String[] parameterTypeNames){
        String result = "";
        if(parameterTypeNames.length > 0){
        	result = convertParameterTypeName(parameterTypeNames[0]) + " p0";
        	for(int i = 1; i < parameterTypeNames.length; i++){
        		result += ", " + convertParameterTypeName(parameterTypeNames[i]) + " p" + i;
        	}
        }
		return result;
	}
	
	private static String convertReturnTypeName(String className){
		String result = convertParameterTypeName(className);
		result = convertPrimitiveToClass(result);
		return result;
	}
	
    private static String convertPrimitiveToClass(String className) {
    	String result = null;	

    	if(className.equals("boolean")){
    		result = "Boolean";
    	} else if(className.equals("byte")){
    		result = "Byte";
    	} else if(className.equals("char")){
    		result = "Character";
    	} else if(className.equals("short")){
    		result = "Short";
    	} else if(className.equals("int")){
    		result = "Integer";
    	} else if(className.equals("long")){
    		result = "Long";
    	} else if(className.equals("float")){
    		result = "Float";
    	} else if(className.equals("double")){
    		result = "Double";
    	} else {
    		result = className;
    	}
    	return result;
    }
    
    private static String convertParameterTypeName(String parameterName){
    	assert parameterName != null && parameterName.length() > 0;
    	String result = parameterName;
    	if(parameterName.charAt(0) == '['){
    		result = convertToArrayType(parameterName);
    	}
    	return result;
    }
    
    private static String convertToArrayType(String parameterName){
    	String result = "";
    	int dimensions = getDimensions(parameterName);
    	if(parameterName.charAt(dimensions) == 'L'){
    		result = getObjectElementType(parameterName, dimensions+1);
    	} else {
    		result = getPrimitiveElementType(parameterName, dimensions);
    	}
    	for(int i = 0; i < dimensions; i++){
    		result += "[]";
    	}
    	return result;
    }
    
    private static int getDimensions(String parameterName){
    	int result = 0;
    	while(result < parameterName.length() && parameterName.charAt(result) == '['){
    		result++;
    	}
    	return result;
    }
    
    private static String getObjectElementType(String parameterName, int firstChar){
    	String result = "";
    	int i = firstChar;
    	while(parameterName.charAt(i) != ';'){
    		result += parameterName.charAt(i);
    		i++;
    	}
    	return result;
    }
    
    private static String getPrimitiveElementType(String parameterName, int dimensions){
    	String result = "";
    	char letter = parameterName.charAt(dimensions);
    	switch(letter){
    	    case 'Z':
    	    	result = "boolean";
    	    	break;
    	    case 'B':
    	    	result = "byte";
    	    	break;
    	    case 'C':
    	    	result = "char";
    	    	break;
    	    case 'S':
    	    	result = "short";
    	    	break;
    	    case 'I':
    	    	result = "int";
    	    	break;
    	    case 'J':
    	    	result = "long";
    	    	break;
    	    case 'F':
    	    	result = "float";
    	    	break;
    	    case 'D':
    	    	result = "double";
    	    	break;
    	    default:
    	}
    	return result;
    }
	
	public static void main(String[] args){
		create("TestClass");
	}
}