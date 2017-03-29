package org.skyve.impl.metadata.repository.view.actions;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.skyve.impl.metadata.view.ActionImpl;
import org.skyve.impl.util.XMLMetaData;

@XmlType(namespace = XMLMetaData.VIEW_NAMESPACE)
public abstract class PositionableAction extends ActionMetaData {
	private Boolean inActionPanel;

	public Boolean isInActionPanel() {
		return inActionPanel;
	}
	
	@XmlAttribute(required = false)
	public void setInActionPanel(Boolean inActionPanel) {
		this.inActionPanel = inActionPanel;
	}

	@Override
	public ActionImpl toMetaDataAction() {
		ActionImpl result = super.toMetaDataAction();
		result.setInActionPanel(inActionPanel);
		return result;
	}
}
