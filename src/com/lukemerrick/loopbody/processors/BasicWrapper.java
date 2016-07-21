package com.lukemerrick.loopbody.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;

/**
 * Transforms all loop bodies into wrapped local classes for better join-point
 * exposure.
 * 
 * @author Luke Merrick
 *
 */
public class BasicWrapper extends AbstractProcessor<CtLoop> {

//	@Override
//	public boolean isToBeProcessed(CtParameter<?> element) {
//		return true; // default implementation, actually
//	}

	public void process(CtLoop element) {
		CtStatement a = element.getBody().clone();
		System.out.println(a.toString());
	}
	

}
