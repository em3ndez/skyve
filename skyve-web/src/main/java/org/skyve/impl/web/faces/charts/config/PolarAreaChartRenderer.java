package org.skyve.impl.web.faces.charts.config;

import java.io.IOException;

import org.primefaces.model.charts.ChartModel;
import org.skyve.impl.sail.mock.MockFacesContext;

import jakarta.faces.context.FacesContext;

public class PolarAreaChartRenderer extends org.primefaces.component.polarareachart.PolarAreaChartRenderer {
	public String encodeConfig(ChartModel model) throws IOException {
		FacesContext fc = new MockFacesContext();
		try (ResponseWriter writer = new ResponseWriter()) {
			fc.setResponseWriter(writer);
			encodeConfig(fc, model);
			return ChartConfigRenderer.spruce(writer);
		}
	}
}
