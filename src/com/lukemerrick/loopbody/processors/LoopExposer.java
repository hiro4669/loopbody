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
	private final String LOOP_BODY_METHOD_PARAMETER_NAME = "$loopIterator$" + TAG_FOR_NAMING;
	private final String ENV_OBJECT_NAME = "$envObject$" + TAG_FOR_NAMING;
	private enum LoopType {WHILE, DO, FOR, FOR_EACH, OTHER}

	// Step 1: pick a loop
	public void process(CtLoop element) {
		debugHeader("Processing Loop");
		debug("Contents:\n\"" + element + "\"");

		// Step 2: determine loop info
		boolean loopHasReturnStatement = checkForReturnStatement(element);
		CtTypeReference retType = loopHasReturnStatement ? parentMethod(element).getType() : null;
		Set<CtTypeReference<? extends Throwable>> exceptionTypes = parentMethod(element).getThrownTypes();
		debugNewline();
		debug("loop type: " + getType(element));
		debug("return statment exists: " + loopHasReturnStatement);
		if (loopHasReturnStatement) debug("return type: " + retType.toString());
		debugHeader("thrown types:");
		exceptionTypes.stream().forEach(x -> debug(x.toString()));

		// Step 3: identify all local variables accessed within the loop
		Set<CtLocalVariableReference> varsToCache = referencedLocalVars(element);
		List<CtLocalVariableReference> orderedVarsToCache = new ArrayList<CtLocalVariableReference>();
		orderedVarsToCache.addAll(varsToCache);
		debugHeader("local variables to cache");
		orderedVarsToCache.stream().forEach(a -> debug(a.toString()));
		debugNewline();

		// Step 4: create internal class 
		// make class declaration
		debug("creating internal envClass");
		CtClass envClass = createEnvClass(element);
		HashMap<CtLocalVariableReference, CtFieldReference> varMappings = new HashMap<CtLocalVariableReference, CtFieldReference>();
		debug("caching orderedVarsToCache inside envClass");
		// add fields
		List<CtFieldReference>  cacheFields = 
			orderedVarsToCache.stream()
						.map(var -> generateFieldInEnvClass(var, envClass, varMappings))
						.collect(toList());
		if (loopHasReturnStatement) //set up a local value to store the return value if there is one
			getFactory().Field().create(envClass, JUST_PUBLIC_MODIFIER_SET, retType, LOOP_BODY_RET_VAL_NAME);
		debugHeader("fields of envClass");
		envClass.getFields().stream().forEach(field -> debug(field.toString()));
		debugNewline();
		// make the constructor
		CtConstructor envConstructor = makeEnvConstructor(cacheFields);
		envClass.addConstructor(envConstructor);

		// create the loopbody method
		CtBlock loopBodyMethodBody = makeLoopBodyMethodBlock(element, varMappings);
		CtMethod loopBodyMethod = getFactory().Method().create(
			envClass,
			JUST_PUBLIC_MODIFIER_SET,
			getFactory().Type().BOOLEAN,
			LOOP_BODY_METHOD_NAME,
			makeLoopBodyMethodParams(element),
			exceptionTypes,
			loopBodyMethodBody
		);
		// TODO Step 5: create environement-object initialization statment
		List<CtExpression<?>> constructorArgs = new ArrayList<>();
		orderedVarsToCache.stream()
							.forEach(var -> constructorArgs.add(getFactory().Code().createVariableRead(var, false)));
		CtTypeReference classType = ((CtType)envClass).getReference();
		CtConstructorCall initialization = getFactory().Core().createConstructorCall();
		initialization.setType(classType);
		initialization.setExecutable(envConstructor.getReference());
		initialization.setArguments(constructorArgs);
		CtLocalVariable envClassInstance = getFactory().Code().createLocalVariable(
			classType,
			ENV_OBJECT_NAME,
			initialization
		);
		element.insertBefore(envClassInstance);

		// TODO Step 6: Generate loop
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
		// create iteration index parameter for the method
		CtParameter<Integer> iteratorParam = getFactory().Core().createParameter();
		iteratorParam.setSimpleName(LOOP_BODY_METHOD_PARAMETER_NAME);
		iteratorParam.setType(getFactory().Type().INTEGER);
		List<CtParameter<?>> loopBodyMethodParams = Arrays.asList(iteratorParam);

		// TODO: also handle for loops that don't use a simple integer to iterate
		// replace for loop iteration index with the new parameter of this method
		if (getType(loop) == LoopType.FOR) {
			CtFor forLoop = (CtFor) loop;
			List<CtStatement> forInitStatements = forLoop.getForInit();
			forInitStatements.get(0);
			//TODO: finish this
		}
		return loopBodyMethodParams;
	}
	
	// (first we copy the raw body of the loop)
	private CtBlock makeLoopBodyMethodBlock(CtLoop loop, Map<CtLocalVariableReference, CtFieldReference> varMappings) {
		CtStatement loopBody = loop.getBody().clone();
		// replace variable accesses with accesses to the cached vars
		for (CtVariableAccess varAccess : loopBody.getElements(new TypeFilter<CtVariableAccess>(CtVariableAccess.class))) {
			CtVariableReference var = varAccess.getVariable();
			if(varMappings.containsKey(var)) {
				CtFieldReference cached = varMappings.get(var);
				debug("TODO: replace access \"" + varAccess.toString() + "\" with a var access to the field: " + "\"" + cached.toString() +  "\"");
			}
		}

		// TODO: replace continue statements with "return false"

		// TODO: replace break statements with "return true"

		// TODO: replace return statements with setting "retVal" and "return true"

		CtBlock loopBodyMethodBody = getFactory().Core().createBlock();
		loopBodyMethodBody.insertBegin(loopBody);
		return loopBodyMethodBody;
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
