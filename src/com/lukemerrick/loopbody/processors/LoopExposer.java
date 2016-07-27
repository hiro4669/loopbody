package com.lukemerrick.loopbody.processors;

import static java.util.stream.Collectors.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
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
	private static int loopNumber = 0;

	// Step 1: pick a loop
	public void process(CtLoop element) {
		debugHeader("Processing Loop");
		debug("Contents:\n\"" + element + "\"");
		// Step 2: identify if a return statment exists
		boolean hasReturnStatement = checkForReturnStatement(element);
		debug("return statment exists: " + hasReturnStatement);
		debugNewline();

		// Step 3: identify all local variables accessed within the loop
		Set<CtLocalVariableReference> varsToCache = referencedLocalVars(element); 
		debugHeader("local variables to cache");
		varsToCache.stream().forEach(a -> debug(a.toString()));
		debugNewline();

		// Step 4: create internal class 
		debug("creating internal envClass");
		CtClass envClass = createEnvClass(element);
		HashMap<CtLocalVariableReference, CtFieldReference> varMappings = new HashMap<CtLocalVariableReference, CtFieldReference>();
		debug("caching varsToCache inside envClass");
		varsToCache.stream().forEach(var -> insertCacheField(var, envClass, varMappings));
		debugHeader("fields of envClass");
		envClass.getFields().stream().forEach(field -> debug(field.toString()));
		// create the loopbody method

		
		// CtMethod loopBodyMethod = getFactory().Core().createMethod();
		// loopBodyMethod.setType(getFactory().Type().BOOLEAN);
		// if (hasReturnStatement) { //set up a local value to store the return value
		// 	CtExpression retExpression = retStatementList.get(0).getReturnedExpression();
		// 	CtLocalVariable retVar = getFactory().Core().createLocalVariable();
		// 	retVar.setType(retExpression.getType());
		// }
		
			/* ---- Inner class specifications: -----
			* -> class-local version of every local variable used in the loop body
			* -> class-local variable of return type of most abstract-typed return statement
			* -> boolean "loop body" method with same code as loop body
			* -> modify "loop body" method on "continue/break/return" statements
			*/
		// TODO Step 5: create environement-object initialization statment
		// TODO Step 6: create and inject local variable caching statements
		// TODO Step 7: inject statement to initialize "rev_val" variable to "null"
		// TODO Step 8: Generate loop
			/* 
			* -> appropriate type (for, while, do-while)
			* -> call loop body function
			* -> if return type true, check for return value
			* -> before returning or breaking, uncache local variables
			* 	--> if return value, return it, otherwise break
			*/
		// TODO Step 9: After loop, uncache variables 
		// 				through the injection of assignment statements
		System.out.println("");
	}

	
	//========================================================================
	//========================= *HELPER METHODS BELOW* =======================
	//========================================================================

	/**
	* Returns true if the body of "loop" includes a return statement, false otherwise
	*/
	private boolean checkForReturnStatement(CtLoop loop) {
		TypeFilter<CtReturn> returnFilter = new TypeFilter<CtReturn>(CtReturn.class);
		List<CtReturn> retStatementList = loop.getBody().getElements(returnFilter);
		return !retStatementList.isEmpty();
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
	*/
	private void insertCacheField(CtLocalVariableReference varToCache,
								CtClass envClass, Map<CtLocalVariableReference, CtFieldReference> varToCacheMap) {
		String cacheFieldSimpleName = varToCache.getSimpleName() + TAG_FOR_NAMING;
		CtTypeReference varTypeReference = varToCache.getType();
		Set<ModifierKind> justPublicModifierSet = new HashSet<ModifierKind>(Arrays.asList(ModifierKind.PUBLIC));
		CtField cacheField = getFactory().Field().create(envClass, justPublicModifierSet, varTypeReference, cacheFieldSimpleName);
		debug("adding the following cacheField to the envClass: " + cacheField.toString());
		varToCacheMap.put(varToCache, cacheField.getReference());
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
		System.out.println("-------" + header + "-------");
	}
	private void debugNewline() {
		if (!DEBUG_MODE)
			return;
		System.out.println();
	}
	//------------------------- ^SIMPLE DEGUGGING SYSTEM^ -------------------
	

}
