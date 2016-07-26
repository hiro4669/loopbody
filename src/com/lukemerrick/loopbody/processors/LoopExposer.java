package com.lukemerrick.loopbody.processors;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.visitor.filter.*;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.reference.CtLocalVariableReference;
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
	//------------------------- ^SIMPLE DEGUGGING SYSTEM^ -------------------

	/**
	* Transforms the body of a loop so it can be written into a method of a local class
	*/
	private CtStatement processLoopBodyWithoutReturn(CtStatement originalLoopBody) {
		//TODO: Implement
		return originalLoopBody;
	}

	private boolean checkForReturnStatement(CtLoop loop) {
		TypeFilter<CtReturn> returnFilter = new TypeFilter<CtReturn>(CtReturn.class);
		List<CtReturn> retStatementList = loop.getBody().getElements(returnFilter);
		return !retStatementList.isEmpty();
	}

	// Step 1: pick a loop
	public void process(CtLoop element) {
		debugHeader("Processing Loop");
		debug("Contents:\n\"" + element + "\"");
		// Step 2: identify if a return statment exists
		boolean hasReturnStatement = checkForReturnStatement(element);
		debug("return statment exists: " + hasReturnStatement);


		// // Step 3: identify all local variables accessed within the loop
		// // TODO: make sure this works with objects and arrays
		// TypeFilter<CtVariableAccess> localVarAccessFilter = new TypeFilter<CtVariableAccess>(CtVariableAccess.class);
		// ArrayList<CtVariableAccess> localVarAccesses = new ArrayList(element.getElements(localVarAccessFilter));
		// HashSet<CtVariableReference> referencedVars = new HashSet<CtVariableReference>();
		// for (CtVariableAccess a : localVarAccesses) {
		// 	referencedVars.add(a.getVariable());
		// }



		// // TODO Step 4: Create internal class
		// CtClass envClass = getFactory().Core().createClass();
		// HashMap<CtLocalVariableReference, CtLocalVariableReference> variableMappings = new HashMap<CtLocalVariableReference, CtLocalVariableReference>();
		// for (CtVariableReference var : referencedVars) {
		// 	// create "cache variable" and add it to this new class
		// 	// map the caching inside of "variableMappings"
		// }

		
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
	

}
