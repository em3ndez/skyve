package org.skyve.metadata.view.fluent;

import org.skyve.impl.metadata.view.widget.bound.input.ContentSignature;

public class FluentContentSignature extends FluentWidget {
	private ContentSignature signature = null;
	
	public FluentContentSignature() {
		signature = new ContentSignature();
	}
	
	public FluentContentSignature(ContentSignature signature) {
		this.signature = signature;
	}

	public FluentContentSignature from(@SuppressWarnings("hiding") ContentSignature signature) {
		return this;
	}

	@Override
	public ContentSignature get() {
		return signature;
	}
}