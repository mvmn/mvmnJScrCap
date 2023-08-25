package x.mvmn.jscrcap.util;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import x.mvmn.jscrcap.model.CapturedImage;
import x.mvmn.jscrcap.model.CapturesTableModel;
import x.mvmn.jscrcap.util.swing.SwingUtil;

public class SequenceCaptureThread extends Thread {

    private volatile boolean stopRequested = false;
    private volatile boolean stopped = false;
    private final int captureInterval;
    private final Rectangle captureArea;
    private final CapturesTableModel capturesTableModel;

    public SequenceCaptureThread(final CapturesTableModel capturesTableModel, final Rectangle captureArea,
            final int captureInterval) {
        this.captureInterval = captureInterval * 100;
        this.capturesTableModel = capturesTableModel;
        this.captureArea = captureArea;
    }

    public void requestStop() {
        this.stopRequested = true;
    }

    public void run() {
        boolean stopMe = false;
        while (!stopMe) {
            try {
                BufferedImage screenshot = SwingUtil.getRobot().createScreenCapture(captureArea);
                capturesTableModel.addImage(new CapturedImage(screenshot));
                if (captureInterval != 0) {
                    Thread.sleep(captureInterval);
                }
            } catch (InterruptedException e) {
                // Ignore interrupted exception
            } catch (Exception e) {
                e.printStackTrace();
            }
            stopMe = this.stopRequested;
        }
        stopped = true;
    }

    public boolean isStopped() {
        boolean result = this.stopped;
        if (!result) {
            result = this.stopped;
        }
        return result;
    }
}
