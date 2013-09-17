package x.mvmn.jscrcap.util;

import x.mvmn.jscrcap.gui.swing.ControlWindow;

public class SequenceCaptureThread extends Thread {

	private volatile boolean stopRequested = false;
	private volatile boolean stopped = false;
	private final Object THREAD_LOCK_OBJECT = new Object();
	private final int captureInterval;
	final ControlWindow capturer;

	public SequenceCaptureThread(final ControlWindow capturer, final int captureInterval) {
		// -15 is for dirty workadound in ControlWindow.doCapture() that uses 15
		// millisec delay between hiding rect. selection window and making a
		// screenshot
		this.captureInterval = captureInterval * 1000 - 15;
		this.capturer = capturer;
	}

	public void requestStop() {
		synchronized (THREAD_LOCK_OBJECT) {
			this.stopRequested = true;
		}
	}

	public void run() {
		boolean stopMe = false;
		while (!stopMe) {
			synchronized (THREAD_LOCK_OBJECT) {
				stopMe = this.stopRequested;
			}
			try {
				capturer.doCapture();
				Thread.sleep(captureInterval);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		stopped = true;
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
