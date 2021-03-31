package org.skyve.util;

import java.util.List;

import org.owasp.encoder.Encode;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.skyve.domain.Bean;
import org.skyve.impl.bind.BindUtil;
import org.skyve.metadata.module.query.MetaDataQueryColumn;
import org.skyve.metadata.module.query.MetaDataQueryProjectedColumn;
import org.skyve.metadata.view.TextOutput.Sanitisation;

/**
 * Utility methods inspired by the OWASP project.
 * @author mike
 */
public class OWASP {
	private static final PolicyFactory TEXT_SANITIZER = new HtmlPolicyBuilder().toFactory();
	private static final PolicyFactory BASIC_SANITIZER = Sanitizers.FORMATTING;
	private static final PolicyFactory SIMPLE_SANITIZER = BASIC_SANITIZER.and(Sanitizers.BLOCKS);
	private static final PolicyFactory RELAXED_SANITIZER = SIMPLE_SANITIZER.and(Sanitizers.TABLES).and(Sanitizers.IMAGES).and(Sanitizers.LINKS).and(Sanitizers.STYLES);

	private OWASP() {
		// nothing to see here
	}
	
	public static String sanitise(Sanitisation sanitise, String html) {
		String result = html;

		if (sanitise != null) {
			switch (sanitise) {
			case text:
				result = TEXT_SANITIZER.sanitize(html);
				break;
			case basic:
				result = BASIC_SANITIZER.sanitize(html);
				break;
			case simple:
				result = SIMPLE_SANITIZER.sanitize(html);
				break;
			case relaxed:
				result = RELAXED_SANITIZER.sanitize(html);
				break;
			default:
			}
		}

		return result;
	}

	public static String escapeHtml(String html) {
		return Encode.forHtml(html);
	}

	public static String sanitiseAndEscapeHtml(Sanitisation sanitise, String html) {
		String result = sanitise(sanitise, html);
		return escapeHtml(result);
	}

	public static void sanitiseAndEscapeListModelRows(List<Bean> rows, List<MetaDataQueryColumn> columns) {
		for (Bean row : rows) {
			for (MetaDataQueryColumn column : columns) {
				// Don't sanitise columns that are not projected
				if ((column instanceof MetaDataQueryProjectedColumn) && (! ((MetaDataQueryProjectedColumn) column).isProjected()))  {
					continue;
				}
				
				String key = column.getBinding();
				if (key == null) {
					key = column.getName();
				}

				// escape and sanitise string values if needed
				boolean escape = column.isEscape();
				Sanitisation sanitise = column.getSanitise();
				if (escape || ((sanitise != null) && (! Sanitisation.none.equals(sanitise)))) {
					Object value = BindUtil.get(row, key);
					if (value instanceof String) {
						String string = (String) value;
						string = OWASP.sanitise(sanitise, string);
						if (escape) {
							string = OWASP.escapeHtml(string);
						}
						BindUtil.set(row, key, string);
					}
				}
			}
		}
	}
}
