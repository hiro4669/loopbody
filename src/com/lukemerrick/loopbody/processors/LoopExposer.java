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
	private final String LOOP_BODY_METHOD_COUNTER_NAME = "$loopIterator$" + TAG_FOR_NAMING;
	private final String ENV_OBJECT_NAME = "$envObject$" + TAG_FOR_NAMING;
	private enum LoopType {WHILE, DO, FOR, FOR_EACH, OTHER}

	// Step 1: pick a loop
	public void process(CtLoop element) {
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
		debugHeader("local variables to cache"); varsToCache.stream().forEach(a -> debug(a.toString())); debugNewline();

		// Step 4: create internal class 
		// make class declaration
		CtClass envClass = createEnvClass(element);
		HashMap<CtLocalVariableReference, CtFieldReference> varMappings = new HashMap<CtLocalVariableReference, CtFieldReference>();
		// add fields
		List<CtFieldReference>  cacheFields = varsToCache.stream()
														.map(var -> generateFieldInEnvClass(var, envClass, varMappings))
														.collect(toList());
		if (loopHasReturnStatement) //set up a local value to store the return value if there is one
			getFactory().Field().create(envClass, JUST_PUBLIC_MODIFIER_SET, retType, LOOP_BODY_RET_VAL_NAME);
		
		debugHeader("fields of envClass"); envClass.getFields().stream().forEach(field -> debug(field.toString())); debugNewline();
		
		// make the constructor
		CtConstructor envConstructor = makeEnvConstructor(cacheFields);
		envClass.addConstructor(envConstructor);

		// create the loopbody method
		CtBlock loopBodyMethodBody = makeLoopBodyMethodBlock(element, varMappings, loopHasReturnStatement, retType);
		CtMethod loopBodyMethod =
			getFactory().Method().create(envClass, JUST_PUBLIC_MODIFIER_SET,getFactory().Type().BOOLEAN,
											LOOP_BODY_METHOD_NAME,makeLoopBodyMethodParams(element),
											exceptionTypes, loopBodyMethodBody);

		// Step 5: create environement-object initialization statment
		element.insertBefore(initializeEnvironment(element, varsToCache, envClass, envConstructor));

		// TODO Step 6: Generate new loop body, replace
		// List<CtExpression> loopBodyActualParams = new ArrayList<>();
		// if (loopType(element) == LoopType.FOR)

		// CtExpression runLoopBody = getFactory().Code().createInvocation(envClass, loopBodyMethod.getReference(), );
		// CtBlock newLoopBody = getFactory().Core().createBlock();
			/* 
			* -> appropriate type (for, while, do-while)
			* -> call loop body function
			* -> if return type true, check for return value
			* -> before returning or breaking, uncache local variables
			* 	--> if return value, return it, otherwise break
			*/
		// TODO Step 7: After loop, uncache variables 
		// 				through the injection of assignment statements
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

	private List<CtParameter<?>> makeLoopBodyMethodParams (CtLoop loop) {
		// add a counter for better exposure
		CtParameter<Integer> counter = getFactory().Core().createParameter();
		counter.setSimpleName(LOOP_BODY_METHOD_COUNTER_NAME);
		counter.setType(getFactory().Type().INTEGER);
		List<CtParameter<?>> loopBodyMethodParams = new ArrayList<>(Arrays.asList(counter));
		if (getType(loop) == LoopType.FOR) { //also pass along the value from for loop
			CtLocalVariable iteratorDeclaration = (CtLocalVariable)((CtFor)loop).getForInit().get(0);
			CtParameter iteratorParam = getFactory().Core().createParameter();
			//debug("HERE: " + forInitStatements.get(0).getClass().getName());
			//TODO: finish this
		}
		return loopBodyMethodParams;
	}
	CtLocalVariable initializeEnvironment(CtLoop loop, Iterable<CtLocalVariableReference> varsToCache, CtClass envClass, CtConstructor envConstructor) {
		List<CtExpression<?>> constructorArgs = new ArrayList<>();
		for(CtVariableReference var: varsToCache)
			constructorArgs.add(getFactory().Code().createVariableRead(var, false));
		CtTypeReference classType = ((CtType)envClass).getReference();
		CtConstructorCall initialization = getFactory().Core().createConstructorCall();
		initialization.setType(classType);
		initialization.setExecutable(envConstructor.getReference());
		initialization.setArguments(constructorArgs);
		return getFactory().Code().createLocalVariable(
			classType,
			ENV_OBJECT_NAME,
			initialization
		);

	}
	
	// (first we copy the raw body of the loop)
	private CtBlock makeLoopBodyMethodBlock(CtLoop loop, Map<CtLocalVariableReference, CtFieldReference> varMappings, boolean loopHasReturnStatement, CtTypeReference retType) {
		CtStatement loopBody = loop.getBody().clone();
		// replace variable accesses with accesses to the cached vars
		for (CtVariableAccess varAccess : loopBody.getElements(new TypeFilter<CtVariableAccess>(CtVariableAccess.class))) {
			CtVariableReference var = varAccess.getVariable();
			if(varMappings.containsKey(var)) {
				CtFieldReference cached = varMappings.get(var);
				debug("TODO: replace access \"" + varAccess.toString() + "\" with a var access to the field: " + "\"" + cached.toString() +  "\"");
			}
		}
		CtLiteral trueLiteral = getFactory().Core().createLiteral();
		trueLiteral.setValue(true);
		CtLiteral falseLiteral = getFactory().Core().createLiteral();
		falseLiteral.setValue(false);
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
				CtAssignment substituteReturnStatmenet = setRetVar(r, retType);
				r.replace(substituteReturnStatmenet);
				substituteReturnStatmenet.insertAfter(retTrue);
			}
		}
		CtBlock loopBodyMethodBody = getFactory().Core().createBlock();
		loopBodyMethodBody.insertBegin(loopBody);
		return loopBodyMethodBody;
	}

	/**
	* Creates an assignment statement of the retVar variable to duplicate the functionality
	* of the given return statement
	*/
	private CtAssignment setRetVar(CtReturn r, CtTypeReference retType) {
		CtVariableReference retValRef = getFactory().Code().createLocalVariableReference(retType, LOOP_BODY_RET_VAL_NAME);
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
		CtClass declaringClass = loop.getParent(CtClass.class);
		return getFactory().Class().create(declaringClass, nextEnvName());
	}

	/**
	* Generates a unique name for the internal loop environment class
	*/
	private String nextEnvName() {
		loopNumber++; //keeps the names unique
		return BASE_ENV_CLASS_NAME + TAG_FOR_NAMING + Integer.toString(loopNumber);
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
		String cacheFieldSimpleName = varToCache.getSimpleName() + TAG_FOR_NAMING;
		CtTypeReference varTypeReference = varToCache.getType();
		CtField cacheField = getFactory().Field().create(envClass, JUST_PUBLIC_MODIFIER_SET, varTypeReference, cacheFieldSimpleName);
		varToCacheMap.put(varToCache, cacheField.getReference());
		return cacheField.getReference();
	}

	/**
	* Creates a parameter used to take an initial value for the field "fieldToSet"
	* Parameter has the same simple name as the field to set.
	*/
	private CtParameter generateParam(CtFieldReference fieldToSet) {
		CtParameter param = getFactory().Core().createParameter();
		param.setSimpleName(fieldToSet.getSimpleName());
		param.setType(fieldToSet.getType());
		return param;
	}


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
