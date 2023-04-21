package org.matsim.synthetic.opt;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;

import java.util.*;

/**
 * Select a move that is guaranteed to improve the solution.
 */
public class LargeShuffleMoveSelector implements MoveIteratorFactory<PlanAssignmentProblem, LargeChangeMove> {

	private static final int SIZE = 30;

	@Override
	public long getSize(ScoreDirector<PlanAssignmentProblem> scoreDirector) {
		return scoreDirector.getWorkingSolution().getPersons().size() / 8;
	}

	@Override
	public Iterator<LargeChangeMove> createOriginalMoveIterator(ScoreDirector<PlanAssignmentProblem> scoreDirector) {
		return createRandomMoveIterator(scoreDirector, new Random(0));
	}

	@Override
	public Iterator<LargeChangeMove> createRandomMoveIterator(ScoreDirector<PlanAssignmentProblem> scoreDirector, Random workingRandom) {
		List<PlanPerson> persons = scoreDirector.getWorkingSolution().getPersons();

		return new It(scoreDirector.getWorkingSolution().getMaxK(), persons.subList(0, persons.size() / 8), workingRandom);
	}

	private static final class It implements Iterator<LargeChangeMove> {

		private final int maxK;
		private final List<PlanPerson> list;
		private final Random random;
		private int done = 0;

		public It(int maxK, List<PlanPerson> list, Random random) {
			this.maxK = maxK;
			this.list = list;
			this.random = random;
		}

		@Override
		public boolean hasNext() {
			return done < list.size();
		}

		@Override
		public LargeChangeMove next() {

			done++;

			List<PlanPerson> subset = new ArrayList<>();
			for (int i = 0; i < SIZE; i++) {
				subset.add(list.get(random.nextInt(list.size())));
			}

			int[] ks = new int[subset.size()];
			for (int i = 0; i < ks.length; i++) {
				ks[i] = random.nextInt(maxK);
			}

			return new LargeChangeMove(subset, ks);
		}
	}

}
