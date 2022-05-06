package org.skyve.domain;

import java.util.Map;

public class DynamicPersistentChildBean extends DynamicPersistentBean implements ChildBean<Bean> {
	private static final long serialVersionUID = -1007733210670332962L;

	public DynamicPersistentChildBean(String bizModule, String bizDocument, Map<String, Object> properties) {
		super(bizModule, bizDocument, properties);
	}

	@Override
	public Bean getParent() {
		return (Bean) get(ChildBean.PARENT_NAME);
	}

	@Override
	public void setParent(Bean parent) {
		set(ChildBean.PARENT_NAME, parent);
	}

	@Override
	public Integer getBizOrdinal() {
		return (Integer) get(Bean.ORDINAL_NAME);
	}

	@Override
	public void setBizOrdinal(Integer bizOrdinal) {
		set(Bean.ORDINAL_NAME, bizOrdinal);
	}
}
