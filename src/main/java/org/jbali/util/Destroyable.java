package org.jbali.util;

import java.util.concurrent.Future;

public interface Destroyable {

	/**
	 * Destroy this component, rendering it unusable. This should not throw any exceptions when used correctly.
	 * @throws IllegalStateException if the component has already been - or is being - destroyed. Detecting and throwing this is optional.
	 */
	public void destroy();
	
	public static Destroyable future(Future<?> f) {
		return () -> f.cancel(true);
	}
}
