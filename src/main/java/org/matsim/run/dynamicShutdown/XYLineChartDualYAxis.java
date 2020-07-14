/* *********************************************************************** *
 * project: org.matsim.*
 * BarChart.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run.dynamicShutdown;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.utils.charts.ChartUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Creates a new XYLineChart with dual Y axes
 *
 * @author jakobrehmann
 * adapted from mrieser
 */
public class XYLineChartDualYAxis extends ChartUtil {

	private final XYSeriesCollection dataset1;
	private final XYSeriesCollection dataset2;


	public XYLineChartDualYAxis(final String title, final String xAxisLabel,
								final String yAxisLabel, final String yAxisLabel2) {
		super(title, xAxisLabel, yAxisLabel);
		this.dataset1 = new XYSeriesCollection();
		this.dataset2 = new XYSeriesCollection();

		this.chart = createChart(title, xAxisLabel, yAxisLabel, this.dataset1, yAxisLabel2, dataset2);
		addDefaultFormatting();
	}

	@Override
	public JFreeChart getChart() {
		return this.chart;
	}

	private JFreeChart createChart(final String title, final String categoryAxisLabel, final String yAxisLabel, final XYSeriesCollection dataset1,
								   final String yAxisLabel2, final XYSeriesCollection dataset2) {
		XYPlot plot = new XYPlot();
		plot.setDataset(1, dataset1);
		plot.setDataset(0, dataset2);
		XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(true, false);
		renderer1.setSeriesFillPaint(0,Color.YELLOW);
		plot.setRenderer(1, renderer1);//use default fill paint for first series


		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
		renderer.setSeriesFillPaint(0, Color.GREEN);
		plot.setRenderer(0, renderer);

		plot.setRangeAxis(1, new NumberAxis(yAxisLabel));
		plot.setRangeAxis(0, new NumberAxis(yAxisLabel2));
		plot.setDomainAxis(new NumberAxis(categoryAxisLabel));

		
		//Map the data to the appropriate axis
		plot.mapDatasetToRangeAxis(0, 0);
		plot.mapDatasetToRangeAxis(1, 1);

		JFreeChart chart = new JFreeChart(title, null, plot, true);
		chart.setBackgroundPaint(Color.WHITE);

		return chart;

	}

	public void addVerticalRange(double botLimit, double topLimit) {

		XYPlot plot = this.chart.getXYPlot();
		IntervalMarker marker = new IntervalMarker(botLimit, topLimit);
		marker.setPaint(Color.PINK);
		marker.setLabel("CovergenceZone");
		marker.setLabelAnchor(RectangleAnchor.TOP);

		plot.addRangeMarker(marker);

	}

	/**
	 * Adds a new data series to the chart with the specified title.
	 * <code>xs<code> and <code>ys</code> should have the same length. If not, only as many items
	 * are shown as the shorter array contains.
	 *
	 * @param title
	 * @param xs The x values.
	 * @param ys The y values.
	 */
	public final void addSeries(final String title, final double[] xs, final double[] ys) {
		XYSeries series = new XYSeries(title, false, true);
		for (int i = 0, n = Math.min(xs.length, ys.length); i < n; i++) {
			series.add(xs[i], ys[i]);
		}
		this.dataset1.addSeries(series);
	}

	public final void addSeries(String title, Map<Integer, Double> map) {
		XYSeries series = new XYSeries(title, false, true);
		for ( Entry<Integer,Double> entry : map.entrySet() ) {
			series.add(entry.getKey(), entry.getValue() );
		}
		this.dataset1.addSeries(series);
	}

	public final void addSeries2(String title, Map<Integer, Double> map) {
		XYSeries series = new XYSeries(title, false, true);
		for ( Entry<Integer,Double> entry : map.entrySet() ) {
			series.add(entry.getKey(), entry.getValue() );
		}
		this.dataset2.addSeries(series);
	}
	
}
