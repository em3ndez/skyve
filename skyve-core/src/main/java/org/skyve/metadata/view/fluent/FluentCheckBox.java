package org.skyve.metadata.view.fluent;

import org.skyve.impl.metadata.view.widget.bound.input.CheckBox;

public class FluentCheckBox extends FluentChangeableInputWidget<FluentCheckBox> implements FluentAbsoluteSize<FluentCheckBox> {
	private CheckBox check = null;

	public FluentCheckBox() {
		check = new CheckBox();
	}

	public FluentCheckBox(CheckBox check) {
		this.check = check;
	}

	public FluentCheckBox from(@SuppressWarnings("hiding") CheckBox check) {
		Boolean b = check.getTriState();
		if (b != null) {
			triState(b.booleanValue());
		}

		absoluteSize(check, this);

		super.from(check);
		return this;
	}

	@Override
	public FluentCheckBox pixelWidth(int width) {
		check.setPixelWidth(Integer.valueOf(width));
		return this;
	}

	@Override
	public FluentCheckBox pixelHeight(int height) {
		check.setPixelHeight(Integer.valueOf(height));
		return this;
	}

	public FluentCheckBox triState(boolean triState) {
		check.setTriState(triState ? Boolean.TRUE : Boolean.FALSE);
		return this;
	}

	@Override
	public CheckBox get() {
		return check;
	}

}
