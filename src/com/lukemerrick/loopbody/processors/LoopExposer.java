package com.lukemerrick.loopbody.processors;

import static java.util.stream.Collectors.*;
import java.util.function.Function;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.visitor.filter.*;
import spoon.reflect.reference.*;
import spoon.reflect.declaration.*;

/**
 * Transforms regular loops into "exposed" loops by injecting a local class
 * that caches and exposes the local environment. Also involves tweaks
 * that allow for the traditional functionality of "continue" "break" and "return"
 * statements.
 * 
 * @author Luke Merrick
 *
 */
public class LoopExposer extends AbstractProcessor<CtLoop> {
	private final boolean DEBUG_MODE = true;
	private final String TAG_FOR_NAMING = "$EXPOSED_JAVA$";
	private final String BASE_ENV_CLASS_NAME = "LoopBodyEnvironment";
	private final Set<ModifierKind> JUST_PUBLIC_MODIFIER_SET = new HashSet<ModifierKind>(Arrays.asList(ModifierKind.PUBLIC));
	private static int loopNumber = 0;
	private final String LOOP_BODY_RET_VAL_NAME = "retValue" + TAG_FOR_NAMING;
	private final String LOOP_BODY_METHOD_NAME = "loopBody" + TAG_FOR_NAMING;
	private final String ENV_OBJECT_NAME = "$envObject$" + TAG_FOR_NAMING;
	private enum LoopType {WHILE, DO, FOR, FOR_EACH, OTHER}

	// Step 1: pick a loop
	public void process(CtLoop element) {
		if (getType(element) == LoopType.FOR_EACH) return; // currently FOR_EACH are not supported
		if (getType(element) == LoopType.WHILE) return; // currently WHILE are not supported
		if (getType(element) == LoopType.DO) return; // currently DO are not supported
		debugHeader("Processing Loop"); debug("Contents:\n\"" + element + "\"");

		// Step 2: determine loop info
		boolean loopHasReturnStatement = checkForReturnStatement(element);
		CtTypeReference retType = loopHasReturnStatement ? parentMethod(element).getType() : null;
		Set<CtTypeReference<? extends Throwable>> exceptionTypes = parentMethod(element).getThrownTypes();

		debugNewline(); debug("loop type: " + getType(element)); debug("return statment exists: " + loopHasReturnStatement);
		if (loopHasReturnStatement) debug("return type: " + retType.toString());
		debugHeader("thrown types:"); exceptionTypes.stream().forEach(x -> debug(x.toString()));

		// Step 3: identify all local variables accessed within the loop
		List<CtLocalVariableReference> varsToCache = new ArrayList<CtLocalVariableReference>(referencedLocalVars(element));
		if (getType(element) == LoopType.FOR) // we don't cache this, instead we pass it as a parameter
			varsToCache.remove(getIteratorVariable((CtFor)element));
		debugHeader("local variables to cache"); varsToCache.stream().forEach(a -> debug(a.toString())); debugNewline();

		// Step 4: create internal class 
		// make class declaration
		CtClass envClass = createEnvClass(element);
		element.insertBefore(envClass);

		// for naming reasons, we create the loop count variable here
		CtLocalVariable counterVar = getFactory().Code().createLocalVariable(
														getFactory().Type().INTEGER, 
														"$counter_of$" + envClass.getSimpleName(), 
														getFactory().Code().createLiteral(0));
		element.insertBefore(counterVar);

		HashMap<CtLocalVariableReference, CtFieldReference> varMappings = new HashMap<CtLocalVariableReference, CtFieldReference>();
		// add fields
		List<CtFieldReference>  cacheFields = varsToCache.stream()
														.map(var -> generateFieldInEnvClass(var, envClass, varMappings))
														.collect(toList());
		CtVariableReference retValRef = null;
		if (loopHasReturnStatement) //set up a local value to store the return value if there is one
			retValRef = getFactory().Field().create(envClass, JUST_PUBLIC_MODIFIER_SET, retType, LOOP_BODY_RET_VAL_NAME).getReference();
		
		debugHeader("fields of envClass"); envClass.getFields().stream().forEach(field -> debug(field.toString())); debugNewline();

		// make the constructor
		CtConstructor envConstructor = makeEnvConstructor(cacheFields);
		envClass.addConstructor(envConstructor);

		// create the loopbody method
		CtBlock loopBodyMethodBody = makeLoopBodyMethodBlock(element, varMappings, loopHasReturnStatement, retValRef);
		CtMethod loopBodyMethod = getFactory().Method().create(
											envClass, JUST_PUBLIC_MODIFIER_SET,getFactory().Type().BOOLEAN,
											LOOP_BODY_METHOD_NAME,makeLoopBodyMethodParams(element, counterVar.getReference()),
											exceptionTypes, loopBodyMethodBody);

		// Step 5: create environment-object initialization statment
		List<CtExpression<?>> constructorArgs = new ArrayList<CtExpression<?>>();
		for (CtVariableReference var : varsToCache)
			constructorArgs.add(getFactory().Code().createVariableRead(var, false));
		CtLocalVariable envInit = initializeEnvironment(envClass, envConstructor, constructorArgs);
		element.insertBefore(envInit);

		// // TODO: clean up!
		// for(CtVariableReference var : varsToCache) {
		// 	// CtCodeSnippetExpression rhs = getFactory().Code().createCodeSnippetExpression(var.getSimpleName());
		// 	// CtCodeSnippetExpression lhs = getFactory().Code().createCodeSnippetExpression(envInit.getSimpleName() + "." + var.getSimpleName());
		// 	// CtAssignment assignment = getFactory().Code().createVariableAssignment(varMappings.get(var), false, rhs);
		// 	CtCodeSnippetStatement a = getFactory().Code().createCodeSnippetStatement(envInit.getSimpleName() + "." + var.getSimpleName() + " = " + var.getSimpleName());
		// 	element.insertBefore(a);
		// }

		// Step 6: Generate new loop body, replace
		CtBlock newLoopBody = getFactory().Core().createBlock();

		// increment the counter
		CtExpression<?> counterAccessExp = getFactory().Code().createVariableRead(counterVar.getReference(), false);
		CtUnaryOperator incrementCounter = getFactory().Core().createUnaryOperator();
		incrementCounter.setOperand(counterAccessExp);
		incrementCounter.setKind(UnaryOperatorKind.POSTINC);
		newLoopBody.insertBegin(incrementCounter);

		// run the loopbody method and act upon the results
		List<CtExpression<?>> loopBodyActualParams = new ArrayList<CtExpression<?>>(Arrays.asList(counterAccessExp));
		if (getType(element) == LoopType.FOR)
			loopBodyActualParams.add(getFactory().Code().createVariableRead(getIteratorVariable((CtFor)element), false));
		CtExpression<Boolean> runLoopBody = getFactory().Code().createInvocation(
												getFactory().Code().createVariableRead(envInit.getReference(), false),
												loopBodyMethod.getReference(),
												loopBodyActualParams);
		
		CtStatement breakProcedure = getFactory().Core().createBreak();
		CtIf mainIf = getFactory().Core().createIf();
		mainIf.setCondition(runLoopBody);

		if (loopHasReturnStatement) {
			CtVariableAccess retValAccess = getFactory().Code().createVariableRead(retValRef, false);
			CtBinaryOperator retValNull = getFactory().Core().createBinaryOperator();
			retValNull.setKind(BinaryOperatorKind.EQ);
			retValNull.setLeftHandOperand(retValAccess);
			retValNull.setRightHandOperand(getFactory().Code().createLiteral(null));
			CtBlock returnProcedure = getFactory().Core().createBlock();
			CtReturn returnStatement = getFactory().Core().createReturn();
			returnStatement.setReturnedExpression(retValAccess);
			returnProcedure.insertEnd(returnStatement);
			CtIf ifBreak = getFactory().Core().createIf();
			ifBreak.setCondition(retValNull);
			ifBreak.setThenStatement(breakProcedure);
			ifBreak.setElseStatement(returnProcedure);
			mainIf.setThenStatement(ifBreak);
		}
		else
			mainIf.setThenStatement(breakProcedure);

		newLoopBody.insertEnd(mainIf);
		element.setBody(newLoopBody);
		
		
		// TODO Step 7: After loop, uncache variables 
		// 				through the injection of assignment statements
		for (CtLocalVariableReference var : varsToCache) {
			CtExpression rhs = getFactory().Code().createCodeSnippetExpression(envInit.getSimpleName() + "." + var.getSimpleName());
			CtAssignment assignment = getFactory().Code().createVariableAssignment(var, false, rhs);
			element.insertAfter(assignment);
		}

		System.out.println("");
	}

	
	//========================================================================
	//========================= *HELPER METHODS BELOW* =======================
	//========================================================================

	private LoopType getType(CtLoop loop) {
		if (loop instanceof CtWhile)
			return LoopType.WHILE;
		if (loop instanceof CtDo)
			return LoopType.DO;
		if (loop instanceof CtFor)
			return LoopType.FOR;
		if (loop instanceof CtForEach)
			return LoopType.FOR_EACH;
		return LoopType.OTHER;
	}

	/**
	* Returns a reference to the variable declaration inside the given for statement.
	* for example, given loop = "for(int i = 0; i < 10; i++)...", this method returns a reference to "i"
	*/
	private CtLocalVariableReference getIteratorVariable (CtFor loop) {
		return ((CtLocalVariable)loop.getForInit().get(0)).getReference();
	}

	/**
	* Creates the parameters for the loop body method:
	* 0) integer counter that tracks how many iterations have passed
	* 1) [FOR LOOP ONLY] the variable initialized in the for statment
	*/
	private List<CtParameter<?>> makeLoopBodyMethodParams (CtLoop loop, CtVariableReference counterRef) {
		// add a counter for better exposure
		CtParameter<Integer> counter = generateParam(counterRef);
		List<CtParameter<?>> loopBodyMethodParams = new ArrayList<>(Arrays.asList(counter));
		if (getType(loop) == LoopType.FOR) { //also pass along the value from for loop iterator
			CtLocalVariableReference iteratorVariable = getIteratorVariable((CtFor)loop);
			loopBodyMethodParams.add(generateParam(iteratorVariable));
		}
		return loopBodyMethodParams;
	}

	/**
	* Returns the local variable declaration to initialize an object of class envClass
	*/
	CtLocalVariable initializeEnvironment(CtClass envClass, CtConstructor constructor, List<CtExpression<?>> args) {
		CtTypeReference classType = correctedTypeReference(envClass);
		//the following code is generated as a snippet because spoon mistakenly appends a "Main." to the constructor call when generated using the commented-out code
		String constructorCallSnippet = "new " + classType.toString() + "(" + String.join(", ", args.stream().map(a -> a.toString()).collect(toList())) + ")";
		CtExpression initialization = getFactory().Code().createCodeSnippetExpression(constructorCallSnippet);
		// CtConstructorCall initialization = getFactory().Core().createConstructorCall();
		// initialization.setType(classType);
		// initialization.setExecutable(constructorExecutable);
		// initialization.setArguments(args);
		return getFactory().Code().createLocalVariable(
			classType,
			"$initialized$" + envClass.getSimpleName(),
			initialization
		);

	}


	private CtTypeReference correctedTypeReference(CtClass internalClass) {
		CtTypeReference classType = ((CtType)internalClass).getReference();
		classType.setDeclaringType(null);
		return classType;
	}
	
	// (first we copy the raw body of the loop)
	private CtBlock makeLoopBodyMethodBlock(CtLoop loop, Map<CtLocalVariableReference, CtFieldReference> varMappings, boolean loopHasReturnStatement, CtVariableReference retValRef) {
		CtStatement loopBody = loop.getBody().clone();
		// commented-out code below unnecessary because we have taken a variable hiding approach
		// // replace variable accesses with accesses to the cached vars
		// for (CtVariableAccess varAccess : loopBody.getElements(new TypeFilter<CtVariableAccess>(CtVariableAccess.class))) {
		// 	CtVariableReference var = varAccess.getVariable();
		// 	if(varMappings.containsKey(var)) {
		// 		CtFieldReference cached = varMappings.get(var);
		// 		debug("TODO: replace access \"" + varAccess.toString() + "\" with a var access to the field: " + "\"" + cached.toString() +  "\"");
		// 	}
		// }
		CtLiteral trueLiteral = getFactory().Code().createLiteral(true);
		CtLiteral falseLiteral = getFactory().Code().createLiteral(false);
		CtReturn retTrue = getFactory().Core().createReturn();
		retTrue.setReturnedExpression(trueLiteral);
		CtReturn retFalse = getFactory().Core().createReturn();
		retFalse.setReturnedExpression(falseLiteral);
		// replace continue statements with "return false"
		for (CtContinue c : loopBody.getElements(new TypeFilter<CtContinue>(CtContinue.class)))
			c.replace(retFalse);
		// replace break statements with "return true"
		for (CtBreak b : loopBody.getElements(new TypeFilter<CtBreak>(CtBreak.class)))
			b.replace(retTrue);
		if (loopHasReturnStatement) {
			// replace return statements with setting "retVal" and "return true"
			for (CtReturn r : loopBody.getElements(new TypeFilter<CtReturn>(CtReturn.class))) {
				CtAssignment substituteReturnStatmenet = setRetVar(r, retValRef);
				r.replace(substituteReturnStatmenet);
				substituteReturnStatmenet.insertAfter(retTrue);
			}
		}
		CtBlock loopBodyMethodBody = getFactory().Core().createBlock();
		loopBodyMethodBody.insertBegin(loopBody);
		loopBodyMethodBody.insertEnd(retFalse);
		return loopBodyMethodBody;
	}

	/**
	* Creates an assignment statement of the retVar variable to duplicate the functionality
	* of the given return statement
	*/
	private CtAssignment setRetVar(CtReturn r, CtVariableReference retValRef) {
		CtVariableAccess retVal = getFactory().Code().createVariableRead(retValRef, false);
		CtAssignment a = getFactory().Core().createAssignment();
		a.setAssigned(retVal);
		a.setAssignment(r.getReturnedExpression());
		return a;
	}


	private CtConstructor makeEnvConstructor(List<CtFieldReference> cacheFields) {
		List<CtParameter> constuctorParams = cacheFields.stream()
														.map(this::generateParam)
														.collect(toList());
		CtBlock envConstructorBody = getFactory().Core().createBlock();
		for (CtFieldReference field : cacheFields) {
			CtCodeSnippetExpression rhs = getFactory().Code().createCodeSnippetExpression(field.getSimpleName());
			CtAssignment assignment = getFactory().Code().createVariableAssignment(field, false, rhs);
			envConstructorBody.insertEnd(assignment);
		}
		CtConstructor envClassConstructor = getFactory().Core().createConstructor();
		envClassConstructor.setBody(envConstructorBody);
		envClassConstructor.setParameters(constuctorParams);
		envClassConstructor.addModifier(ModifierKind.PUBLIC);
		return envClassConstructor;
	}

	/**
	* Returns true if the body of "loop" includes a return statement, false otherwise
	*/
	private boolean checkForReturnStatement(CtLoop loop) {
		TypeFilter<CtReturn> returnFilter = new TypeFilter<CtReturn>(CtReturn.class);
		List<CtReturn> retStatementList = loop.getBody().getElements(returnFilter);
		return !retStatementList.isEmpty();
	}

	/**
	* gets the parent method of the loop
	*/
	private CtMethod parentMethod(CtLoop loop) {
		return (CtMethod)loop.getParent(CtMethod.class);
	}

	/**
	* Returns a Set<CtLocalVariableReference> of all non-final local variables accessed in the body of "loop"
	*/
	private Set<CtLocalVariableReference> referencedLocalVars(CtLoop loop) {
		TypeFilter<CtVariableAccess> variableAccess = new TypeFilter<CtVariableAccess>(CtVariableAccess.class);
		return loop.getElements(variableAccess)
					.stream()
					.map(access -> access.getVariable())
					.filter(var -> var instanceof CtLocalVariableReference)
					.map(var -> (CtLocalVariableReference) var)
					.filter(var -> !var.getModifiers().contains(ModifierKind.FINAL))
					.collect(toSet());
	}

	/**
	* Creates a uniquely-named internal class in the same class as the given loop
	*/
	private CtClass createEnvClass(CtLoop loop) {
		// The old implementation inserted the class outside of the method
		//CtClass declaringClass = loop.getParent(CtClass.class);
		//return getFactory().Class().create(declaringClass, nextEnvName());
		CtClass envClass = getFactory().Core().createClass();
		envClass.setSimpleName(nextEnvName());
		return envClass;
	}

	/**
	* Generates a unique name for the internal loop environment class
	*/
	private String nextEnvName() {
		loopNumber++; //keeps the names unique
		return BASE_ENV_CLASS_NAME + Integer.toString(loopNumber) + TAG_FOR_NAMING;
	}

	/**
	* Creates a CtVariableRead expression of a given variable
	*/
	private CtVariableRead readExpressionOf(CtVariableReference var) {
		CtVariableRead readExpression = getFactory().Core().createVariableRead();
		readExpression.setVariable(var);
		return readExpression;
	}

	/**
	* Creates a new public field in the given envClass to cache the given varToCache. 
	* Adds a mapping from varToCache to the new field in the given map.
	* Returns a reference to the created cache.
	*/
	private CtFieldReference generateFieldInEnvClass(CtLocalVariableReference varToCache,
								CtClass envClass, Map<CtLocalVariableReference, CtFieldReference> varToCacheMap) {
		String cacheFieldSimpleName = varToCache.getSimpleName();
		CtTypeReference varTypeReference = varToCache.getType();
		CtField cacheField = getFactory().Field().create(envClass, JUST_PUBLIC_MODIFIER_SET, varTypeReference, cacheFieldSimpleName);
		varToCacheMap.put(varToCache, cacheField.getReference());
		return cacheField.getReference();
	}

	/**
	* Creates a parameter used to take an initial value for the field "ref"
	* Parameter has the same simple name as the field to set.
	*/
	private CtParameter generateParam(CtVariableReference ref) {
		CtParameter param = getFactory().Core().createParameter();
		param.setSimpleName(ref.getSimpleName());
		param.setType(ref.getType());
		return param;
	}

	/** not used */
	private CtExecutable execFromReference(CtExecutableReference ref) {
		CtExecutable executable = ref.getDeclaration();
		// if getDeclaration returns null, then we know that we're looking at something external and need to use getExecutableDeclaration
		if (executable == null)
			executable = ref.getExecutableDeclaration();
		return executable;
	}
	
	/**
	* Returns a set of types of all exceptions that can be thrown in the given loop
	* NOT USED because it fails to capture the exception types of methods imported from external libraries
	*/
	private Set<CtTypeReference<? extends Throwable>> getExceptionTypes(CtElement elem) {
		//start with an empty set
		Set<CtTypeReference<? extends Throwable>> result = new HashSet<>(Collections.<CtTypeReference<? extends Throwable>>emptySet());
		// if top level element is itself an executable, add all of its thrown types
		if (elem instanceof CtExecutableReference) {
			debug("adding thrown types of " + elem.toString());
			Set<CtTypeReference<? extends Throwable>> thrownTypes = execFromReference((CtExecutableReference)elem).getThrownTypes();
			debug("generated thrownTypes; size: " + Integer.toString(thrownTypes.size()));
			result.addAll(thrownTypes);
		}
		// get sub executables
		List<CtExecutableReference> subExecutables = elem.getElements(new TypeFilter<>(CtExecutableReference.class));
		subExecutables.remove(elem);

		// recursively add thrown types from sub executables
		if (!subExecutables.isEmpty()) {
			Set<CtTypeReference<? extends Throwable>> subThrownTypes = 
				subExecutables
				.stream()
				.map(this::getExceptionTypes)
				.flatMap(Set::stream)
				.collect(toSet());
			if (subThrownTypes != null)
				result.addAll(subThrownTypes);
		}
		return result;
		// return Collections.<CtTypeReference>emptySet(); //PLACEHOLDER EMPTY SET
	}


	//------------------------- *SIMPLE DEGUGGING SYSTEM* -------------------
	private void debug(String message) {
		if (!DEBUG_MODE)
			return;
		System.out.println(">>> " + message);
	}
	private void debugHeader(String header) {
		if (!DEBUG_MODE)
			return;
		debugNewline();
		System.out.println("-------" + header + "-------");
	}
	private void debugNewline() {
		if (!DEBUG_MODE)
			return;
		System.out.println();
	}
	//------------------------- ^SIMPLE DEGUGGING SYSTEM^ -------------------
	

}
