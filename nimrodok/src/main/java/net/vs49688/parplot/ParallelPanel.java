package net.vs49688.parplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;

public class ParallelPanel extends JPanel {

	/**
	 * Percentage of the panel width that's used to separate the graph from the edges.
	 */
	private static final double OUTER_GAP = 0.05;

	/**
	 * Distance applied to the min/max ranges for each axis.
	 */
	private static final double RANGE_PAD = 0.5;
	
	protected final List<double[]> m_Points;
	
	private Axis[] m_Axes;
	private Point[] m_DrawBuffer;
	private double m_Scale;
	private Point2D.Double m_Range;
	private boolean m_RecalcRange;
	
	private Color m_LineColour;
	
	public ParallelPanel() {
		super();
		m_Points = new ArrayList<>();
		m_Axes = null;
		m_DrawBuffer = null;
		m_Scale = 1.0;
		m_Range = new Point2D.Double(0.0, 1.0);
		m_RecalcRange = true;
		
		this.setBackground(Color.white);
		m_LineColour = Color.red;
		
	}
	
	public void clear() {
		m_Points.clear();
		redraw();
	}
	
	public void setAxisColour(int index, Color col) {
		if(m_Axes == null || index > m_Axes.length) {
			throw new IllegalArgumentException();
		}
		m_Axes[index].setLineColour(col);
	}
	
	public void setLineColour(Color col) {
		m_LineColour = col == null ? Color.black : col;
	}

	public int getAxisCount() {
		return m_Axes == null ? 0 : m_Axes.length;
	}
	
	public double getScale() {
		return m_Scale;
	}
	
	public void setScale(float scale) {
		m_Scale = scale;
	}
	
	public void redraw() {
		validate();
		repaint();
	}
	
	public void setRange(double min, double max) {
		m_Range.x = min;
		m_Range.y = max;
	}
	
	public void setRecalculateRangeOnAdd(boolean recalc) {
		m_RecalcRange = recalc;
	}
	
	public void setAxes(String[] axes) {
		if(axes == null) {
			throw new IllegalArgumentException("axes");
		}
		
		clear();
		
		m_Axes = new Axis[axes.length];
		for(int i = 0; i < axes.length; ++i) {
			m_Axes[i] = new Axis(axes[i]);
		}
		
		m_DrawBuffer = new Point[m_Axes.length];
	}
	
	public void addPoint(Double[] point) {
		if(point == null) {
			return;
		}
		
		double[] ourPoint = new double[point.length];
		for(int i = 0; i < point.length; ++i) {
			ourPoint[i] = point[i];
		}
		
		addPoint(ourPoint);
	}
	
	public void addPoint(double[] point) {
		if(point == null) {
			return;
		}
		
		if(m_Axes == null) {
			throw new IllegalStateException("No axes specified");
		}
		
		if(point.length != m_Axes.length) {
			throw new IllegalStateException("Invalid component count");
		}
		
		double[] ourPoint = Arrays.copyOf(point, point.length);
		m_Points.add(ourPoint);
		
		if(m_RecalcRange) {
			recalculateAxisRanges();
		}
		
		redraw();
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;

		/* Handle DPI */
		final AffineTransform trans = g2d.getDeviceConfiguration().getNormalizingTransform();
		trans.scale(m_Scale, m_Scale);
		g2d.transform(trans);
		
		Rectangle bounds = g.getClipBounds();

		/* Clear */
		g2d.setColor(this.getBackground());
		g2d.fill(bounds);
		
		int width = bounds.width;
		int height = bounds.height;
		
		if(m_Axes == null) {
			return;
		}
		
		g2d.setStroke(new BasicStroke(2.0f));
		int axisStartX = (int)(width * OUTER_GAP);
		int axisGapX = (width - 2 * axisStartX) / (m_Axes.length - 1);
		
		for(int i = 0; i < m_Axes.length; ++i) {
			Axis axis = m_Axes[i];
			
			int axisX = axisStartX + axisGapX * i;
			axis.setTopCoordinate(axisX, 0);
			axis.setBottomCoordinate(axisX, height - axisStartX);
			axis.setRange(m_Range.x - RANGE_PAD, m_Range.y + RANGE_PAD);
			axis.draw(g2d);
		}
		
		drawLines(g2d);
	}
	
	protected void drawLines(Graphics2D g2d) {
		g2d.setStroke(new BasicStroke(1.0f));
		int idx = 0;
		for(double[] point : m_Points) {
			g2d.setColor(getLineColour(point, idx));
			
			for(int i = 0; i < point.length; ++i) {
				m_DrawBuffer[i] = m_Axes[i].getScreenPosForCoordinate(point[i]);
			}
			
			for(int i = 1; i < m_DrawBuffer.length; ++i) {
				/* Out of range, ignore. */
				if(m_DrawBuffer[i] == null) {
					continue;
				}
				g2d.drawLine(m_DrawBuffer[i - 1].x, m_DrawBuffer[i - 1].y, m_DrawBuffer[i].x, m_DrawBuffer[i].y);
			}
			
			++idx;
		}
	}

	protected Color getLineColour(double[] point, int index) {
		return m_LineColour;
	}

	/**
	 * Get the min-max range capable of fitting all registered points.
	 *
	 * @param axis The axis index.
	 * @return
	 */
	private Point2D.Double getRangeForAxis(int axis) {
		Double min = null, max = null;
		
		for(int i = 0; i < m_Points.size(); ++i) {
			double[] point = m_Points.get(i);
			if(max == null) {
				max = point[axis];
			}
			
			if(min == null) {
				min = point[axis];
			}
			
			if(point[axis] < min) {
				min = point[axis];
			}
			
			if(point[axis] > max) {
				max = point[axis];
			}
		}
		
		return new Point2D.Double(min, max);
	}
	
	private void recalculateAxisRanges() {
		Double min = null, max = null;
		
		for(int i = 0; i < m_Axes.length; ++i) {
			Point2D.Double range = getRangeForAxis(i);
			
			if(min == null) {
				min = range.x;
			}
			
			if(max == null) {
				max = range.y;
			}
			
			if(range.x < min) {
				min = range.x;
			}
			
			if(range.y > max) {
				max = range.y;
			}
		}
		
		m_Range.x = min;
		m_Range.y = max;
	}
}
