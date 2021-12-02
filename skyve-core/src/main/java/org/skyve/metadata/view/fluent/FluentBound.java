package org.skyve.metadata.view.fluent;

import org.skyve.impl.metadata.view.widget.bound.AbstractBound;

abstract class FluentBound<T extends FluentBound<T>>  extends FluentWidget {
	protected FluentBound() {
		// nothing to see
	}
	
	@SuppressWarnings("unchecked")
	protected T from(AbstractBound bound) {
		binding(bound.getBinding());
		return (T) this;
	}
	
	@SuppressWarnings("unchecked")
	public T binding(String binding) {
		get().setBinding(binding);
		return (T) this;
	}

	@Override
	public abstract AbstractBound get();
}
