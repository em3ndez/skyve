package org.skyve.impl.metadata.view.event;

import java.util.List;

public interface Focusable extends EventSource {
	public List<EventAction> getFocusActions();
	public List<EventAction> getBlurActions();
}
