/**
 * Copyright (C) 2011,2012 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 * 
 * This file is part of EvoSuite.
 * 
 * EvoSuite is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * EvoSuite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Public License for more details.
 * 
 * You should have received a copy of the GNU Public License along with
 * EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.coverage.branch;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.CoverageGoal;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.ExecutionResult;
import org.evosuite.testcase.TestChromosome;

/**
 * A single branch coverage goal Either true/false evaluation of a jump
 * condition, or a method entry
 * 
 * @author Gordon Fraser, Andre Mis
 */
public class BranchCoverageGoal extends CoverageGoal implements Serializable,
        Comparable<BranchCoverageGoal> {

	private static final long serialVersionUID = 2962922303111452419L;
	transient Branch branch;
	boolean value;

	String className;
	String methodName;

	private int lineNumber;

	/**
	 * Can be used to create an arbitrary BranchCoverageGoal trying to cover the
	 * given Branch
	 * 
	 * If the given branch is null, this goal will try to cover the root branch
	 * of the method identified by the given name - meaning it will just try to
	 * call the method at hand
	 * 
	 * Otherwise this goal will try to reach the given branch and if value is
	 * true, make the branchInstruction jump and visa versa
	 * 
	 * @param branch
	 *            a {@link org.evosuite.coverage.branch.Branch} object.
	 * @param value
	 *            a boolean.
	 * @param className
	 *            a {@link java.lang.String} object.
	 * @param methodName
	 *            a {@link java.lang.String} object.
	 */
	public BranchCoverageGoal(Branch branch, boolean value, String className,
	        String methodName) {
		if (className == null || methodName == null)
			throw new IllegalArgumentException("null given");
		if (branch == null && !value)
			throw new IllegalArgumentException(
			        "expect goals for a root branch to always have value set to true");

		this.branch = branch;
		this.value = value;

		this.className = className;
		this.methodName = methodName;

		if (branch != null) {
			lineNumber = branch.getInstruction().getLineNumber();
			if (!branch.getMethodName().equals(methodName)
			        || !branch.getClassName().equals(className))
				throw new IllegalArgumentException(
				        "expect explicitly given information about a branch to coincide with the information given by that branch");
		} else {
			//			lineNumber = BranchPool.getBranchlessMethodLineNumber(className, methodName);
			lineNumber = BytecodeInstructionPool.getInstance(TestGenerationContext.getClassLoader()).getFirstLineNumberOfMethod(className,
			                                                                                                                    methodName);
		}
	}

	/**
	 * <p>
	 * Constructor for BranchCoverageGoal.
	 * </p>
	 * 
	 * @param cd
	 *            a {@link org.evosuite.graphs.cfg.ControlDependency} object.
	 * @param className
	 *            a {@link java.lang.String} object.
	 * @param methodName
	 *            a {@link java.lang.String} object.
	 */
	public BranchCoverageGoal(ControlDependency cd, String className, String methodName) {
		this(cd.getBranch(), cd.getBranchExpressionValue(), className, methodName);
	}

	/**
	 * Methods that have no branches don't need a cfg, so we just set the cfg to
	 * null
	 * 
	 * @param className
	 *            a {@link java.lang.String} object.
	 * @param methodName
	 *            a {@link java.lang.String} object.
	 */
	public BranchCoverageGoal(String className, String methodName) {
		this.branch = null;
		this.value = true;

		this.className = className;
		this.methodName = methodName;
		//		lineNumber = BranchPool.getBranchlessMethodLineNumber(className, methodName);
		lineNumber = BytecodeInstructionPool.getInstance(TestGenerationContext.getClassLoader()).getFirstLineNumberOfMethod(className,
		                                                                                                                    methodName);
	}

	/**
	 * Determines whether this goals is connected to the given goal
	 * 
	 * This is the case when this goals target branch is control dependent on
	 * the target branch of the given goal or visa versa
	 * 
	 * This is used in the ChromosomeRecycler to determine if tests produced to
	 * cover one goal should be used initially when trying to cover the other
	 * goal
	 * 
	 * @param goal
	 *            a {@link org.evosuite.coverage.branch.BranchCoverageGoal}
	 *            object.
	 * @return a boolean.
	 */
	public boolean isConnectedTo(BranchCoverageGoal goal) {
		if (branch == null || goal.branch == null) {
			// one of the goals targets a root branch
			return goal.methodName.equals(methodName) && goal.className.equals(className);
		}

		// TODO map this to new CDG !

		return branch.getInstruction().isDirectlyControlDependentOn(goal.branch)
		        || goal.branch.getInstruction().isDirectlyControlDependentOn(branch);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Determine if there is an existing test case covering this goal
	 */
	@Override
	public boolean isCovered(TestChromosome test) {
		ExecutionResult result = runTest(test);
		ControlFlowDistance d = getDistance(result);
		if (d.getApproachLevel() == 0 && d.getBranchDistance() == 0.0)
			return true;
		else
			return false;
	}

	/**
	 * <p>
	 * getDistance
	 * </p>
	 * 
	 * @param result
	 *            a {@link org.evosuite.testcase.ExecutionResult} object.
	 * @return a {@link org.evosuite.coverage.ControlFlowDistance} object.
	 */
	public ControlFlowDistance getDistance(ExecutionResult result) {

		ControlFlowDistance r = ControlFlowDistanceCalculator.getDistance(result, branch,
		                                                                  value,
		                                                                  className,
		                                                                  methodName);

		return r;
	}

	// inherited from Object

	/**
	 * {@inheritDoc}
	 * 
	 * Readable representation
	 */
	@Override
	public String toString() {
		String name = className + "." + methodName + ":";
		if (branch != null) {
			name += " " + branch.toString();
			if (value)
				name += " - true";
			else
				name += " - false";
		} else
			name += " root-Branch";

		return name;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (branch == null ? 0 : branch.getActualBranchId());
		result = prime * result
		        + (branch == null ? 0 : branch.getInstruction().getInstructionId());
		// TODO sure you want to call hashCode() on the cfg? doesn't that take
		// long?
		// Seems redundant -- GF
		/*
		result = prime
		        * result
		        + ((branch == null) ? 0
		                : branch.getInstruction().getActualCFG().hashCode());
		                */
		result = prime * result + className.hashCode();
		result = prime * result + methodName.hashCode();
		result = prime * result + (value ? 1231 : 1237);
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		BranchCoverageGoal other = (BranchCoverageGoal) obj;
		// are we both root goals?
		if (this.branch == null) {
			if (other.branch != null)
				return false;
			else
				// i don't have to check for value at this point, because if
				// branch is null we are talking about the root branch here
				return this.methodName.equals(other.methodName)
				        && this.className.equals(other.className);
		}
		// well i am not, if you are we are different
		if (other.branch == null)
			return false;

		// so we both have a branch to cover, let's look at that branch and the
		// way we want it to be evaluated
		if (!this.branch.equals(other.branch))
			return false;
		else {
			return this.value == other.value;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(BranchCoverageGoal o) {
		return lineNumber - o.lineNumber;
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		// Write/save additional fields
		if (branch != null)
			oos.writeInt(branch.getActualBranchId());
		else
			oos.writeInt(-1);
	}

	// assumes "static java.util.Date aDate;" declared
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException,
	        IOException {
		ois.defaultReadObject();

		int branchId = ois.readInt();
		if (branchId >= 0)
			this.branch = BranchPool.getBranch(branchId);
		else
			this.branch = null;
	}

	/* (non-Javadoc)
	 * @see org.evosuite.coverage.CoverageGoal#getTargetClass()
	 */
	@Override
	public String getTargetClass() {
		return className;
	}

	/* (non-Javadoc)
	 * @see org.evosuite.coverage.CoverageGoal#getTargetMethods()
	 */
	@Override
	public Set<String> getTargetMethods() {
		Set<String> targetMethods = new HashSet<String>();
		targetMethods.add(methodName);
		return targetMethods;
	}

}
