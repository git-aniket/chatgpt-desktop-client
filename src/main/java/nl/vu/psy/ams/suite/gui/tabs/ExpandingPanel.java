package nl.vu.psy.ams.suite.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

public class ExpandingPanel extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//private JPanel expanding;

    public ExpandingPanel(JPanel expanding, boolean addButton, String title)
    {
        setLayout( new BorderLayout() );

        JButton button = new JButton("collapse " + title);
		button.setPreferredSize(new Dimension(1, 15));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));
        if (addButton) {
        	add(button, BorderLayout.NORTH);

        //expanding = new JPanel();
        //expanding.setBackground( color );
        expanding.setVisible( true );
        }
        add(expanding, BorderLayout.CENTER);

        button.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                expanding.setVisible( !expanding.isVisible() );

                Container parent = ExpandingPanel.this.getParent();
                LayoutManager2 layout = (LayoutManager2)parent.getLayout();

                if (expanding.isVisible()) {
                    layout.addLayoutComponent(ExpandingPanel.this, 1F);
                    button.setText("collapse " + title);
                } else {
                    layout.addLayoutComponent(ExpandingPanel.this, null);
                    button.setText("expand " + title);
                }
                revalidate();
                repaint();
            }
        });
    }

    public void toggle() {
        //setVisible( !isVisible() );

        Container parent = ExpandingPanel.this.getParent();
        LayoutManager2 layout = (LayoutManager2)parent.getLayout();

        //if (isVisible())
            layout.addLayoutComponent(ExpandingPanel.this, 1F);
        //else
        //    layout.addLayoutComponent(ExpandingPanel.this, null);

        revalidate();
        repaint();
   	
    }
}
