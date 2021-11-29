package org.skyve.metadata.module.fluent;

import org.skyve.impl.metadata.repository.module.GrantedTo;
import org.skyve.impl.metadata.repository.module.Item;
import org.skyve.metadata.module.menu.MenuItem;

abstract class FluentMenuItem<T extends FluentMenuItem<T>> extends FluentMenuAction<T> {
	protected FluentMenuItem() {
		// nothing to see
	}
	
	protected FluentMenuItem(MenuItem item) {
		super(item);
		item.getRoleNames().forEach(r -> addRole(r));
	}
	
	@SuppressWarnings("unchecked")
	public T addRole(String roleName) {
		GrantedTo role = new GrantedTo();
		role.setRoleName(roleName);
		get().getRoles().add(role);
		return (T) this;
	}
	
	@Override
	public abstract Item get();
}
