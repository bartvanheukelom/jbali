package org.jbali.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

public class DestroyablePool implements Destroyable {

	private final Object lock = new Object();
	private List<Destroyable> things = new ArrayList<>();
	private boolean doneAdding = false;
	
	public <C extends Destroyable> C addComponent(C d) { // TODO rename to add
		synchronized (lock) {
			Preconditions.checkState(!doneAdding, "Already done adding");
			Preconditions.checkState(things != null, "Already destroyed");
			things.add(d);
		}
		return d;
	}
	
	public void doneAdding() {
		synchronized (lock) {
			doneAdding = true;
		}
	}

	@Override
	public void destroy() {
		List<Destroyable> toDstr = things;
		synchronized (lock) {
			Preconditions.checkState(things != null, "Already destroyed");
			things = null;
		}
		toDstr.forEach(Destroyable::destroy);
	}
	
}
