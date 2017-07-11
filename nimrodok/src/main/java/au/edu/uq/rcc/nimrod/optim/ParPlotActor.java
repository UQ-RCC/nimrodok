/*
 * Copyright (C) 2017 Zane van Iperen
 * All rights reserved.
 * 
 * NOTICE: This code may not be used unless explicit permission
 * is obtained from Zane van Iperen.
 * 
 * CONTACT: zane@zanevaniperen.com
 */
package au.edu.uq.rcc.nimrod.optim;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import net.vs49688.parplot.ColourLerpingParallelPanel;
import net.vs49688.parplot.OptionsPanel;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class ParPlotActor extends TypedAtomicActor {

	private final TypedIOPort m_InputPort;
	private final Parameter m_InputNames;

	private Frame m_Frame;
	private String[] m_VarNames;

	public ParPlotActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);

		m_InputPort = new TypedIOPort(this, "input", true, false);
		m_InputPort.setMultiport(true);
		m_InputPort.setTypeEquals(BaseType.DOUBLE);

		m_InputNames = new Parameter(this, "inputNames");
		m_InputNames.setDisplayName("Input Names");
		m_InputNames.setTypeEquals(new ArrayType(BaseType.STRING));
	}

	@Override
	public void initialize() throws IllegalActionException {
		Token[] toks = ((ArrayToken)m_InputNames.getToken()).arrayValue();
		m_VarNames = new String[toks.length];
		for(int i = 0; i < m_VarNames.length; ++i) {
			m_VarNames[i] = ((StringToken)toks[i]).stringValue();
		}

		if(m_InputPort.numberOfSources() != m_VarNames.length) {
			throw new IllegalActionException("Var Count != input count");
		}

		if(m_Frame == null) {
			m_Frame = new Frame();
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_Frame.getOptionsPanel().setNames(m_VarNames);
				m_Frame.getGraph().setAxes(m_VarNames);
				m_Frame.pack();
				m_Frame.setVisible(true);
			}
		});
	}

	@Override
	public void wrapup() throws IllegalActionException {
		super.wrapup();
	}

	@Override
	public boolean prefire() throws IllegalActionException {
		if(m_InputPort.numberOfSources() > 0) {
			for(int i = 0; i < m_InputPort.getWidth(); ++i) {
				if(!m_InputPort.hasToken(i)) {
					return false;
				}
			}
		}

		return super.prefire();
	}

	@Override
	public void fire() throws IllegalActionException {
		double[] value = new double[m_VarNames.length];
		for(int i = 0; i < m_VarNames.length; ++i) {
			value[i] = ((DoubleToken)m_InputPort.get(i)).doubleValue();
		}
		addPoint(value);
	}

	private void addPoint(final double[] point) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_Frame.getGraph().addPoint(point);
			}
		});
	}

	private class Frame extends JFrame {

		private final ColourLerpingParallelPanel m_GraphPanel;
		private final OptionsPanel m_OptionsPanel;

		public Frame() {
			super(ParPlotActor.this.getName());
			this.setPreferredSize(new Dimension(640, 480));

			this.setLayout(new BorderLayout());

			m_OptionsPanel = new OptionsPanel(m_OptionListener);
			this.add(m_OptionsPanel, BorderLayout.NORTH);

			m_GraphPanel = new ColourLerpingParallelPanel();
			this.add(m_GraphPanel, BorderLayout.CENTER);
		}

		private ActionListener m_OptionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				String cmd = e.getActionCommand();
				switch(cmd) {
					case "varSlide":
						m_GraphPanel.setIndex(m_OptionsPanel.getVarValue());
						m_GraphPanel.redraw();
					default:
						//m_GraphPanel.setLineColour(m_OptionsPanel.getLineColour());
						m_GraphPanel.setBackground(m_OptionsPanel.getBackgroundColour());
						for(int i = 0; i < m_GraphPanel.getAxisCount(); ++i) {
							m_GraphPanel.setAxisColour(i, m_OptionsPanel.getAxisColour());
						}

						m_GraphPanel.setScale(m_OptionsPanel.getScale());
						m_GraphPanel.setMaxColour(m_OptionsPanel.getMaxColour());
						m_GraphPanel.setMinColour(m_OptionsPanel.getMinColour());
						m_GraphPanel.redraw();
						break;
				}
			}
		};

		public ColourLerpingParallelPanel getGraph() {
			return m_GraphPanel;
		}

		public OptionsPanel getOptionsPanel() {
			return m_OptionsPanel;
		}

	}
}
