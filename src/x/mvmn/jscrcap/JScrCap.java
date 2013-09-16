package x.mvmn.jscrcap;

import javax.swing.SwingUtilities;

import x.mvmn.jscrcap.gui.swing.ControlWindow;

public class JScrCap {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new ControlWindow().setVisible(true);
			}
		});
	}
}
