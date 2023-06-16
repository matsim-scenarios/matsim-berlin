package org.matsim.prepare.opt;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;

import java.util.List;

/**
 * Switch multiple plan assignments at once.
 */
public class LargeChangeMove extends AbstractMove<PlanAssignmentProblem> {

	private final List<PlanPerson> persons;
	private final int[] ks;

	public LargeChangeMove(List<PlanPerson> persons, int[] ks) {
		this.persons = persons;
		this.ks = ks;

		if (persons.size() != ks.length)
			throw new IllegalArgumentException("Persons and k must be of equal size");
	}

	@Override
	protected LargeChangeMove createUndoMove(ScoreDirector<PlanAssignmentProblem> scoreDirector) {
		return new LargeChangeMove(persons, persons.stream().mapToInt(PlanPerson::getK).toArray());
	}

	@Override
	protected void doMoveOnGenuineVariables(ScoreDirector<PlanAssignmentProblem> scoreDirector) {

		for (int i = 0; i < ks.length; i++) {
			PlanPerson p = persons.get(i);
			scoreDirector.beforeVariableChanged(p, "k");
			p.setK(ks[i]);
			scoreDirector.afterVariableChanged(p, "k");
		}
	}

	@Override
	public LargeChangeMove rebase(ScoreDirector<PlanAssignmentProblem> destinationScoreDirector) {
		List<PlanPerson> other = persons.stream()
				.map(destinationScoreDirector::lookUpWorkingObject)
				.toList();

		return new LargeChangeMove(other, ks);
	}

	@Override
	public boolean isMoveDoable(ScoreDirector<PlanAssignmentProblem> scoreDirector) {
		return true;
	}
}
