package org.skyve.impl.web.service.smartclient;

import org.skyve.impl.generate.ViewGenerator;
import org.skyve.impl.metadata.model.document.field.Field;
import org.skyve.impl.metadata.view.HorizontalAlignment;
import org.skyve.impl.metadata.view.widget.bound.input.HTML;
import org.skyve.impl.metadata.view.widget.bound.input.InputWidget;
import org.skyve.impl.metadata.view.widget.bound.input.LookupDescription;
import org.skyve.impl.metadata.view.widget.bound.input.RichText;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.Attribute;
import org.skyve.metadata.model.Attribute.AttributeType;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.model.document.Relation;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.user.User;
import org.skyve.util.BeanValidator;
import org.skyve.util.OWASP;
import org.skyve.util.Util;

public class SmartClientDataGridFieldDefinition extends SmartClientAttributeDefinition {
    protected HorizontalAlignment align;
    protected boolean editable;
    protected Integer pixelWidth;
    protected String defaultValueJavascriptExpression;

    SmartClientDataGridFieldDefinition(User user,
    									Customer customer, 
                                        Module module, 
                                        Document document, 
                                        InputWidget widget,
                                        String dataGridBindingOverride,
                                        boolean runtime) {
		super(user,
				customer,
				module,
				document,
				(dataGridBindingOverride == null) ? widget.getBinding() : dataGridBindingOverride,
				null,
				runtime,
				false);
        // for datagrids, ensure that enum types are text so that valueMaps don't have to be set all the time.
		if ("enum".equals(type)) {
			type = "text";
		}
        Attribute attribute = target.getAttribute();

        if (attribute instanceof Field) {
            // determine the defaultValue expression for the list grid
        	defaultValueJavascriptExpression = ((Field) attribute).getDefaultValue();
        	if (defaultValueJavascriptExpression != null) {
        		AttributeType attributeType = attribute.getAttributeType();
				if (AttributeType.date.equals(attributeType) || 
						AttributeType.dateTime.equals(attributeType) || 
						AttributeType.time.equals(attributeType) || 
						AttributeType.timestamp.equals(attributeType)) {
					defaultValueJavascriptExpression = new StringBuilder(128).append("isc.DateUtil.parseSchemaDate('").append(defaultValueJavascriptExpression).append("')").toString();
				}
				else if (! (AttributeType.bool.equals(attributeType) || 
								AttributeType.integer.equals(attributeType) ||
								AttributeType.longInteger.equals(attributeType))) {
					defaultValueJavascriptExpression = new StringBuilder(128).append('\'').append(defaultValueJavascriptExpression).append('\'').toString();
				}
        	}
        }
        
        if ((attribute instanceof Relation) && (widget instanceof LookupDescription)) { // widget could be a combo for instance
        	editorType = "comboBox";
        	lookup = new SmartClientLookupDefinition(dataGridBindingOverride != null,
        												user,
        												customer,
        												module,
        												document,
        												(Relation) attribute,
        												(LookupDescription) widget,
        												runtime);
        }

        // By default a SmartClientDataGridDefinition sets memo fields to a text area.
		if ((attribute != null) && AttributeType.memo.equals(attribute.getAttributeType())) {
			if ((widget instanceof RichText) ||
					(widget instanceof HTML)) {
				editorType = null;
			}
		}

		// set the default alignment
		if (attribute != null) {
			AttributeType attributeType = attribute.getAttributeType();
			align = ViewGenerator.determineDefaultColumnAlignment(attributeType);
			pixelWidth = ViewGenerator.determineDefaultColumnWidth(attributeType);
		}
    }

    public HorizontalAlignment getAlign() {
		return align;
	}

	public void setAlign(HorizontalAlignment align) {
		this.align = align;
	}

	public boolean getEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public Integer getPixelWidth() {
		return pixelWidth;
	}

	public void setPixelWidth(Integer pixelWidth) {
		this.pixelWidth = pixelWidth;
	}

	public String toJavascript() {
        StringBuilder result = new StringBuilder(128);

        result.append("name:'");
        result.append(name);
        result.append("',title:'");
        result.append(OWASP.escapeJsString(title));
        result.append("',type:'");
        result.append(type).append('\'');
        if (defaultValueJavascriptExpression != null) {
			result.append(",defaultValue:").append(defaultValueJavascriptExpression);
        }
        if (editorType != null) {
            result.append(",editorType:'").append(editorType).append('\'');
        }
        appendEditorProperties(result, true, pixelWidth, null, null);
        if (required) {
        	result.append(",bizRequired:true,requiredMessage:'");
        	result.append(OWASP.escapeJsString(Util.i18n(BeanValidator.VALIDATION_REQUIRED_KEY, title))).append('\'');
        }
        if (valueMap != null) {
            result.append(",valueMap:").append(valueMap);
        }
        if (align != null) {
        	result.append(",align:'").append(align.toAlignmentString()).append('\'');
        }
        if (length != null) {
            result.append(",length:").append(length);
        }
        if (! editable) {
        	result.append(",canEdit:false");
        }
        if (pixelWidth != null) {
        	result.append(",width:").append(pixelWidth);
        }
        if (escape) {
        	result.append(",escapeHTML:true");
        }
//TODO fix this
//result.append(",changed:'alert(item.grid.getSelectedRecord().bizId)'");
        
        return result.toString();
    }
}
