package net.vs49688.parplot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Comparator;

public class ColourLerpingParallelPanel extends ParallelPanel {

	private static final Color DEFAULT_MAX = Color.blue;
	private static final Color DEFAULT_MIN = Color.green;

	private final _Comparator m_Comparator;

	private Color[] m_DrawColours;
	private Color m_MaxColour;
	private Color m_MinColour;
	private int m_Index;

	public ColourLerpingParallelPanel() {
		m_Comparator = new _Comparator();
		m_DrawColours = null;
		m_MaxColour = DEFAULT_MAX;
		m_MinColour = DEFAULT_MIN;
		m_Index = 0;
	}

	public void setIndex(int index) {
		if(index < 0 || index >= this.getAxisCount()) {
			throw new IllegalArgumentException();
		}

		m_Index = index;
	}

	public void setMaxColour(Color maxColour) {
		m_MaxColour = maxColour == null ? DEFAULT_MAX : maxColour;
	}

	public void setMinColour(Color minColour) {
		m_MinColour = minColour == null ? DEFAULT_MIN : minColour;
	}

	@Override
	public void drawLines(Graphics2D g2d) {
		/* Yes, this is slow. No, I don't care. */

 /* Sort the points. */
		m_Comparator.setIndex(m_Index);
		m_Points.sort(m_Comparator);

		m_DrawColours = new Color[m_Points.size()];
		for(int i = 0; i < m_DrawColours.length; ++i) {
			m_DrawColours[i] = lerp(m_MinColour, m_MaxColour, i / (float)m_DrawColours.length);
		}

		super.drawLines(g2d);
	}

	@Override
	public Color getLineColour(double[] point, int index) {
		return m_DrawColours[index];
	}

	private Color lerp(Color a, Color b, float t) {
		float red = lerp(a.getRed() / 255.0f, b.getRed() / 255.0f, t);
		float green = lerp(a.getGreen() / 255.0f, b.getGreen() / 255.0f, t);
		float blue = lerp(a.getBlue() / 255.0f, b.getBlue() / 255.0f, t);

		return new Color(
				clamp01(red),
				clamp01(green),
				clamp01(blue)
		);
	}

	private float lerp(float v0, float v1, float t) {
		return (1 - t) * v0 + t * v1;
	}

	private float clamp01(float val) {
		return clamp(val, 0, 1);
	}

	private float clamp(float val, float min, float max) {
		if(val < min)
			return min;
		if(val > max)
			return max;
		return val;
	}

	private class _Comparator implements Comparator<double[]> {

		private int m_Index;

		public _Comparator() {
			m_Index = 0;
		}

		public void setIndex(int index) {
			m_Index = index;
		}

		@Override
		public int compare(double[] o1, double[] o2) {
			return Double.compare(o1[m_Index], o2[m_Index]);
		}

	}
}
