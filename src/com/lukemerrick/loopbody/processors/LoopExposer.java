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

	// Step 1: pick a loop
	public void process(CtLoop element) {
		debugHeader("Processing Loop");
		debug("Contents:\n\"" + element + "\"");
		// Step 2a: identify if a return statment exists
		boolean loopHasReturnStatement = checkForReturnStatement(element);

		// Step 2b: identify exception types 
		Set<CtTypeReference<? extends Throwable>> exceptionTypes = getExceptionTypes(element);

		debugNewline();
		debug("return statment exists: " + loopHasReturnStatement);
		debugHeader("thrown types:");
		exceptionTypes.stream().forEach(x -> debug(x.toString()));

		// debugHeader("elements of loop:");
		// element.getElements(new TypeFilter(CtElement.class)).stream().forEach(x -> debug(x.getClass().getName()));

		// Step 3: identify all local variables accessed within the loop
		Set<CtLocalVariableReference> varsToCache = referencedLocalVars(element); 
		debugHeader("local variables to cache");
		varsToCache.stream().forEach(a -> debug(a.toString()));
		debugNewline();

		// Step 4: create internal class 
		// generating the class and cache fields
		debug("creating internal envClass");
		CtClass envClass = createEnvClass(element);
		HashMap<CtLocalVariableReference, CtFieldReference> varMappings = new HashMap<CtLocalVariableReference, CtFieldReference>();
		debug("caching varsToCache inside envClass");
		List<CtFieldReference>  cacheFields = 
			varsToCache.stream()
						.map(var -> generateFieldInEnvClass(var, envClass, varMappings))
						.collect(toList());

		if (loopHasReturnStatement) { //set up a local value to store the return value if there is one
			CtTypeReference retType = element.getParent(new TypeFilter<CtMethod>(CtMethod.class)).getType();
			getFactory().Field().create(envClass, JUST_PUBLIC_MODIFIER_SET, retType, LOOP_BODY_RET_VAL_NAME);
		}

		// debug readout
		debugHeader("fields of envClass");
		envClass.getFields().stream().forEach(field -> debug(field.toString()));

		// making the constructor
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
		envClass.addConstructor(envClassConstructor);

		// create the loopbody method		
		CtMethod loopBodyMethod = getFactory().Core().createMethod();
		loopBodyMethod.setType(getFactory().Type().BOOLEAN);
		
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


	private CtExecutable execFromReference(CtExecutableReference ref) {
		CtExecutable executable = ref.getDeclaration();
		// if getDeclaration returns null, then we know that we're looking at something external and need to use getExecutableDeclaration
		if (executable == null)
			executable = ref.getExecutableDeclaration();
		return executable;
	}
	
	/**
	* Returns a set of types of all exceptions that can be thrown in the given loop
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
