package x.mvmn.jscrcap.util;

import java.awt.Component;

import javax.swing.JOptionPane;

import x.mvmn.jscrcap.model.CapturedImage;

public class GifExportThread extends Thread {

	private final CapturedImage[] captures;
	private final Component parentComponent;

	public GifExportThread(final Component parentComponent, final CapturedImage[] captures) {
		this.captures = captures;
		this.parentComponent = parentComponent;
	}

	public void run() {
		if (captures.length > 0) {
			
		}
	}
}
