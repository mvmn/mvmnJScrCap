package x.mvmn.jscrcap.util;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import x.mvmn.jscrcap.model.CapturedImage;
import x.mvmn.jscrcap.model.CapturesTableModel;
import x.mvmn.jscrcap.util.swing.SwingHelper;

public class SequenceCaptureThread extends Thread {

	private volatile boolean stopRequested = false;
	private volatile boolean stopped = false;
	private final Object THREAD_LOCK_OBJECT = new Object();
	private final int captureInterval;
	private final Rectangle captureArea;
	private final CapturesTableModel capturesTableModel;

	public SequenceCaptureThread(final CapturesTableModel capturesTableModel, final Rectangle captureArea, final int captureInterval) {
		this.captureInterval = captureInterval * 1000;
		this.capturesTableModel = capturesTableModel;
		this.captureArea = captureArea;
	}

	public void requestStop() {
		synchronized (THREAD_LOCK_OBJECT) {
			this.stopRequested = true;
		}
	}

	public void run() {
		boolean stopMe = false;
		while (!stopMe) {
			try {
				BufferedImage screenshot = SwingHelper.getRobot().createScreenCapture(captureArea);
				capturesTableModel.addImage(new CapturedImage(screenshot));
				Thread.sleep(captureInterval);
			} catch (InterruptedException e) {
				// Ignore interrupted exception
			} catch (Exception e) {
				e.printStackTrace();
			}
			synchronized (THREAD_LOCK_OBJECT) {
				stopMe = this.stopRequested;
			}
		}
		synchronized (THREAD_LOCK_OBJECT) {
			stopped = true;
		}
	}

	public boolean isStopped() {
		boolean result = this.stopped;
		if (!result) {
			synchronized (THREAD_LOCK_OBJECT) {
				result = this.stopped;
			}
		}
		return result;
	}
}
