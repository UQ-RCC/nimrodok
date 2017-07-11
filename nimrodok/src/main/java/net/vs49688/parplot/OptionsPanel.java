package net.vs49688.parplot;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Hashtable;
import javax.swing.*;

public class OptionsPanel extends JPanel {

	private ActionListener m_Listener;

	public OptionsPanel(ActionListener changeListener) {
		this();
		m_Listener = changeListener;
		m_VarSlider.setEnabled(false);
		m_VarSlider.setMinorTickSpacing(1);
		m_VarSlider.setSnapToTicks(true);
	}

	/* Leave this for the editor. */
	private OptionsPanel() {
		initComponents();
		m_Listener = null;
	}

	public void setNames(String[] names) {
		m_VarSlider.setEnabled(true);
		m_VarSlider.setModel(new DefaultBoundedRangeModel(0, 0, 0, names.length - 1));
		m_VarSlider.setValue(0);

		Hashtable<Integer, JComponent> labelTable = new Hashtable<>();
		for(int i = 0; i < names.length; ++i) {
			labelTable.put(i, new JLabel(names[i]));
		}
		m_VarSlider.setLabelTable(labelTable);
	}

	private void changeColour(JComponent component) {
		Color col = JColorChooser.showDialog(this, "Select Colour", component.getBackground());
		component.setBackground(col);
	}

	public Color getBackgroundColour() {
		return m_BackgroundPreview.getBackground();
	}

	public Color getAxisColour() {
		return m_AxisPreview.getBackground();
	}

	public Color getMaxColour() {
		return m_MaxPreview.getBackground();
	}
	
	public Color getMinColour() {
		return m_MinPreview.getBackground();
	}

	public float getScale() {
		return ((Number)m_ScaleField.getValue()).floatValue();
	}

	public int getVarValue() {
		return m_VarSlider.getValue();
	}

	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        m_VarSlider = new javax.swing.JSlider();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JLabel scaleLabel = new javax.swing.JLabel();
        m_ScaleField = new javax.swing.JFormattedTextField();
        javax.swing.JSeparator jSeparator1 = new javax.swing.JSeparator();
        m_BackgroundPreview = new javax.swing.JPanel();
        javax.swing.JLabel backgroundLabel = new javax.swing.JLabel();
        javax.swing.JSeparator jSeparator3 = new javax.swing.JSeparator();
        m_AxisPreview = new javax.swing.JPanel();
        javax.swing.JLabel axisLabel = new javax.swing.JLabel();
        javax.swing.JSeparator jSeparator4 = new javax.swing.JSeparator();
        m_MaxPreview = new javax.swing.JPanel();
        javax.swing.JLabel maxLabel = new javax.swing.JLabel();
        javax.swing.JSeparator jSeparator5 = new javax.swing.JSeparator();
        m_MinPreview = new javax.swing.JPanel();
        javax.swing.JLabel maxLabel1 = new javax.swing.JLabel();
        m_ApplyBtn = new javax.swing.JButton();

        m_VarSlider.setPaintLabels(true);
        m_VarSlider.setSnapToTicks(true);
        m_VarSlider.setToolTipText("");
        m_VarSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                onVarChange(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(m_VarSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(m_VarSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        scaleLabel.setText("Scale:");
        jPanel2.add(scaleLabel);

        m_ScaleField.setColumns(3);
        m_ScaleField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));
        m_ScaleField.setText("1");
        m_ScaleField.setToolTipText("");
        jPanel2.add(m_ScaleField);

        jSeparator1.setPreferredSize(new java.awt.Dimension(5, 24));
        jPanel2.add(jSeparator1);

        m_BackgroundPreview.setBackground(new java.awt.Color(255, 255, 255));
        m_BackgroundPreview.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        m_BackgroundPreview.setPreferredSize(new java.awt.Dimension(16, 16));
        m_BackgroundPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onBackgroundChange(evt);
            }
        });

        javax.swing.GroupLayout m_BackgroundPreviewLayout = new javax.swing.GroupLayout(m_BackgroundPreview);
        m_BackgroundPreview.setLayout(m_BackgroundPreviewLayout);
        m_BackgroundPreviewLayout.setHorizontalGroup(
            m_BackgroundPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 14, Short.MAX_VALUE)
        );
        m_BackgroundPreviewLayout.setVerticalGroup(
            m_BackgroundPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 14, Short.MAX_VALUE)
        );

        jPanel2.add(m_BackgroundPreview);

        backgroundLabel.setText("Background");
        jPanel2.add(backgroundLabel);

        jSeparator3.setPreferredSize(new java.awt.Dimension(5, 24));
        jPanel2.add(jSeparator3);

        m_AxisPreview.setBackground(new java.awt.Color(0, 0, 0));
        m_AxisPreview.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        m_AxisPreview.setPreferredSize(new java.awt.Dimension(16, 16));
        m_AxisPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onAxisChanged(evt);
            }
        });

        javax.swing.GroupLayout m_AxisPreviewLayout = new javax.swing.GroupLayout(m_AxisPreview);
        m_AxisPreview.setLayout(m_AxisPreviewLayout);
        m_AxisPreviewLayout.setHorizontalGroup(
            m_AxisPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 14, Short.MAX_VALUE)
        );
        m_AxisPreviewLayout.setVerticalGroup(
            m_AxisPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 14, Short.MAX_VALUE)
        );

        jPanel2.add(m_AxisPreview);

        axisLabel.setText("Axis");
        jPanel2.add(axisLabel);

        jSeparator4.setMinimumSize(new java.awt.Dimension(5, 24));
        jSeparator4.setPreferredSize(new java.awt.Dimension(5, 24));
        jPanel2.add(jSeparator4);

        m_MaxPreview.setBackground(new java.awt.Color(0, 0, 255));
        m_MaxPreview.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        m_MaxPreview.setPreferredSize(new java.awt.Dimension(16, 16));
        m_MaxPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onMaxChanged(evt);
            }
        });

        javax.swing.GroupLayout m_MaxPreviewLayout = new javax.swing.GroupLayout(m_MaxPreview);
        m_MaxPreview.setLayout(m_MaxPreviewLayout);
        m_MaxPreviewLayout.setHorizontalGroup(
            m_MaxPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 14, Short.MAX_VALUE)
        );
        m_MaxPreviewLayout.setVerticalGroup(
            m_MaxPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 14, Short.MAX_VALUE)
        );

        jPanel2.add(m_MaxPreview);

        maxLabel.setText("Max");
        jPanel2.add(maxLabel);

        jSeparator5.setMinimumSize(new java.awt.Dimension(5, 24));
        jSeparator5.setPreferredSize(new java.awt.Dimension(5, 24));
        jPanel2.add(jSeparator5);

        m_MinPreview.setBackground(new java.awt.Color(0, 255, 0));
        m_MinPreview.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        m_MinPreview.setPreferredSize(new java.awt.Dimension(16, 16));
        m_MinPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onMinChanged(evt);
            }
        });

        javax.swing.GroupLayout m_MinPreviewLayout = new javax.swing.GroupLayout(m_MinPreview);
        m_MinPreview.setLayout(m_MinPreviewLayout);
        m_MinPreviewLayout.setHorizontalGroup(
            m_MinPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 14, Short.MAX_VALUE)
        );
        m_MinPreviewLayout.setVerticalGroup(
            m_MinPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 14, Short.MAX_VALUE)
        );

        jPanel2.add(m_MinPreview);

        maxLabel1.setText("Min");
        jPanel2.add(maxLabel1);

        m_ApplyBtn.setText("Apply");
        m_ApplyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onApply(evt);
            }
        });
        jPanel2.add(m_ApplyBtn);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 572, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void onBackgroundChange(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_onBackgroundChange
		changeColour(m_BackgroundPreview);
    }//GEN-LAST:event_onBackgroundChange

    private void onMaxChanged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_onMaxChanged
		changeColour(m_MaxPreview);
    }//GEN-LAST:event_onMaxChanged

    private void onApply(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onApply
		m_Listener.actionPerformed(new ActionEvent(this, 0, "apply"));
    }//GEN-LAST:event_onApply

    private void onVarChange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_onVarChange
		m_Listener.actionPerformed(new ActionEvent(this, 1, "varSlide"));
    }//GEN-LAST:event_onVarChange

    private void onAxisChanged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_onAxisChanged
		changeColour(m_AxisPreview);
    }//GEN-LAST:event_onAxisChanged

    private void onMinChanged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_onMinChanged
		changeColour(m_MinPreview);
    }//GEN-LAST:event_onMinChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton m_ApplyBtn;
    private javax.swing.JPanel m_AxisPreview;
    private javax.swing.JPanel m_BackgroundPreview;
    private javax.swing.JPanel m_MaxPreview;
    private javax.swing.JPanel m_MinPreview;
    private javax.swing.JFormattedTextField m_ScaleField;
    private javax.swing.JSlider m_VarSlider;
    // End of variables declaration//GEN-END:variables
}
