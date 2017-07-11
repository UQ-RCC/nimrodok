package net.vs49688.parplot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;

class Axis {

	private static final Color DEFAULT_LINE_COLOUR = Color.RED;
	private static final Color DEFAULT_LABEL_COLOUR = Color.BLACK;

	private final String m_Name;

	private final Point m_Top;
	private final Point m_Bottom;
	private final Point2D.Double m_Range;
	private Color m_LineColour;
	private Color m_LabelColour;

	public Axis(String name) {
		this.m_Name = name;
		m_Top = new Point();
		m_Bottom = new Point();
		m_Range = new Point2D.Double(0.0, 1.0);
		m_LineColour = DEFAULT_LINE_COLOUR;
		m_LabelColour = DEFAULT_LABEL_COLOUR;
	}

	public Color getLineColour() {
		return m_LineColour;
	}

	public Color getLabelColour() {
		return m_LabelColour;
	}

	public Point2D.Double getRange() {
		return new Point2D.Double(m_Range.x, m_Range.y);
	}
	
	public void setLineColour(Color colour) {
		if(colour == null) {
			colour = DEFAULT_LINE_COLOUR;
		}
		m_LineColour = colour;
	}

	public void setLabelColour(Color colour) {
		if(colour == null) {
			colour = DEFAULT_LABEL_COLOUR;
		}
		m_LabelColour = colour;
	}

	public void setRange(double min, double max) {
		m_Range.x = min;
		m_Range.y = max;
	}

	public String getName() {
		return m_Name;
	}

	public void setTopCoordinate(int x, int y) {
		m_Top.setLocation(x, y);
	}

	public void setBottomCoordinate(int x, int y) {
		m_Bottom.setLocation(x, y);
	}

	public Point getTopCoordinate() {
		return new Point(m_Top);
	}

	public Point getBottomCoordinate() {
		return new Point(m_Bottom);
	}

	public Point getScreenPosForCoordinate(double value) {
		if(value < m_Range.x || value > m_Range.y) {
			return null;
		}
		
		double percPos = (value - m_Range.x) / (m_Range.y - m_Range.x);
		
		int pixelValue = (int)(percPos * (m_Top.y - m_Bottom.y) + m_Bottom.y);
		return new Point(m_Bottom.x, pixelValue);
	}
	
	public void draw(Graphics2D g2d) {
		g2d.setColor(m_LineColour);
		g2d.drawLine(m_Top.x, m_Top.y, m_Bottom.x, m_Bottom.y);

		FontMetrics metrics = g2d.getFontMetrics();

		int labelWidth = metrics.stringWidth(m_Name);
		int labelHeight = metrics.getAscent();

		int labelX = m_Bottom.x - labelWidth / 2;
		int labelY = m_Bottom.y + labelHeight;
		g2d.setColor(m_LabelColour);
		g2d.drawString(m_Name, labelX, labelY);
	}
}
