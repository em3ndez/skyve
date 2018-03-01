package org.skyve.metadata.sail.language.step.interaction.actions;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.skyve.impl.util.XMLMetaData;
import org.skyve.metadata.sail.execution.Executor;
import org.skyve.metadata.sail.language.Step;

/**
 * Cancel implicit action
 * @author mike
 */
@XmlType(namespace = XMLMetaData.SAIL_NAMESPACE)
@XmlRootElement(namespace = XMLMetaData.SAIL_NAMESPACE)
public class Cancel implements Step {
	@Override
	public void execute(Executor executor) {
		executor.execute(this);
	}
}
