/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.coverage.exception;

import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.testcase.TestFitnessFunction;

/**
 * Created by gordon on 03/04/2016.
 */
public class TryCatchCoverageTestFitness extends BranchCoverageTestFitness {

    public TryCatchCoverageTestFitness(BranchCoverageGoal goal) throws IllegalArgumentException {
        super(goal);
    }

    @Override
    public int compareTo(TestFitnessFunction other) {
        if (other instanceof TryCatchCoverageTestFitness) {
            TryCatchCoverageTestFitness otherBranchFitness = (TryCatchCoverageTestFitness) other;
            return getBranchGoal().compareTo(otherBranchFitness.getBranchGoal());
        }
        return compareClassName(other);
    }
}
