package com.lukemerrick.loopbody.processors;


import java.util.ArrayList;
import java.util.List;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCatch;

/**
 * Reports warnings when empty catch blocks are found.
 */
public class CatchProcessor extends AbstractProcessor<CtCatch> {

	public List<CtCatch> emptyCatchs = new ArrayList<CtCatch>();

	public void process(CtCatch element) {
		if (element.getBody().getStatements().size() == 0) {
			emptyCatchs.add(element);
			System.out.println("yay");
		}
	}

}
