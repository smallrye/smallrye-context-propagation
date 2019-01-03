package io.smallrye.concurrency.impl;

import org.eclipse.microprofile.concurrent.ThreadContext;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ThreadContextBuilderImpl implements ThreadContext.Builder {

	private String[] propagated;
	private String[] unchanged;
	private String[] cleared;
	private SmallRyeConcurrencyManager manager;

	public ThreadContextBuilderImpl(SmallRyeConcurrencyManager manager) {
		this.manager = manager;
		this.propagated = SmallRyeConcurrencyManager.ALL_REMAINING_ARRAY;
		this.unchanged = SmallRyeConcurrencyManager.NO_STRING;
		this.cleared = SmallRyeConcurrencyManager.TRANSACTION_ARRAY;
	}

	@Override
	public ThreadContext build() {
		// check overlap of configs
		Set<String> clearedSet = new HashSet<>(Arrays.asList(cleared));
		Set<String> unchangedSet = new HashSet<>(Arrays.asList(unchanged));
		Set<String> propagatedSet = new HashSet<>(Arrays.asList(propagated));
		Set<String> overlap = new HashSet<>(clearedSet);
		overlap.retainAll(unchangedSet);
		throwExceptionOnOverlap(overlap, "cleared", "unchanged");
		overlap = new HashSet<>(clearedSet);
		overlap.retainAll(propagatedSet);
		throwExceptionOnOverlap(overlap, "cleared", "propagated");
		overlap = new HashSet<>(unchangedSet);
		overlap.retainAll(propagatedSet);
		throwExceptionOnOverlap(overlap, "unchanged", "propagated");
		return new ThreadContextImpl(manager, propagated, unchanged, cleared);
	}

	private void throwExceptionOnOverlap(Set<String> overlap, String firstSetName, String secondSetName) {
		if (!overlap.isEmpty()) {
			throw new IllegalStateException("Cannot build instance of ThreadContext, " +
					"following items were found in both, " + firstSetName + " and " + secondSetName +
					" sets - " + overlap);
		}
	}

	@Override
	public ThreadContext.Builder propagated(String... types) {
		propagated = types;
		return this;
	}

	@Override
	public ThreadContext.Builder unchanged(String... types) {
		unchanged = types;
		return this;
	}

	@Override
	public ThreadContext.Builder cleared(String... types) {
		cleared = types;
		return this;
	}

}
