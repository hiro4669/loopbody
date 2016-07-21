package com.lukemerrick.loopbody.processors;

// TODO: use correct import statements
// import spoon.processing.AbstractProcessor;
// import spoon.reflect.code.CtCodeSnippetStatement;
// import spoon.reflect.code.CtLoop;
// import spoon.reflect.code.CtStatement;
// import spoon.reflect.declaration.CtExecutable;
// import spoon.reflect.declaration.CtParameter;

/**
 * Transforms regular loops into "exposed" loops by injecting a local class
 * that caches and exposes the local environment. Also involves tweaks
 * that allow for the traditional functionality of "continue" "break" and "return"
 * statements.
 * 
 * @author Luke Merrick
 *
 */
public class BasicWrapper extends AbstractProcessor<CtLoop> {

	// Step 1: pick a loop
	public void process(CtLoop element) {
		// TODO Step 2: identify if a return statment exists
		// TODO Step 3: identify all local variables accessed within the loop
		// TODO Step 4: Create internal class
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
	}
	

}
