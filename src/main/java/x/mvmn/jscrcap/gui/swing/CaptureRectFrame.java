package x.mvmn.jscrcap.gui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import x.mvmn.jscrcap.util.swing.SwingUtil;

public class CaptureRectFrame extends JFrame {
    private static final long serialVersionUID = 5152573028950399411L;
    private Point mouseDownCompCoords;
    private Dimension initialSize;
    private JPanel moveMePanel = new JPanel();
    private JPanel resizeMePanel = new JPanel();
    private JLabel inspectorLabel = new JLabel();
    private volatile Boolean opacityIsSupported = null;
    private final Point locationOnScreen;
    private final Rectangle componentRect = new Rectangle();

    public CaptureRectFrame() {
        super("Capturing rectangle");
        this.setUndecorated(true);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());
        moveMePanel.add(new JLabel("Drag here to move"));
        this.getContentPane().add(moveMePanel, BorderLayout.CENTER);
        resizeMePanel.add(new JLabel("Drag here to resize"));
        this.getContentPane().add(resizeMePanel, BorderLayout.SOUTH);
        this.getContentPane().add(inspectorLabel, BorderLayout.NORTH);

        moveMePanel.addMouseListener(new MouseListener() {

            public void mouseReleased(MouseEvent e) {
                mouseDownCompCoords = null;
            }

            public void mousePressed(MouseEvent e) {
                mouseDownCompCoords = e.getPoint();
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
            }
        });

        moveMePanel.addMouseMotionListener(new MouseMotionListener() {
            public void mouseMoved(MouseEvent e) {
            }

            public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                int newX = currCoords.x - mouseDownCompCoords.x;
                int newY = currCoords.y - mouseDownCompCoords.y;
                CaptureRectFrame.this.setLocation(newX, newY);
                locationOnScreen.setLocation(newX, newY);
                CaptureRectFrame.this.updateInspector();
            }
        });

        resizeMePanel.addMouseListener(new MouseListener() {

            public void mouseReleased(MouseEvent e) {
                mouseDownCompCoords = null;
                initialSize = null;
            }

            public void mousePressed(MouseEvent e) {
                mouseDownCompCoords = e.getLocationOnScreen();
                initialSize = CaptureRectFrame.this.getSize();
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
            }
        });

        resizeMePanel.addMouseMotionListener(new MouseMotionListener() {
            public void mouseMoved(MouseEvent e) {
            }

            public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                int newWidth = initialSize.width - mouseDownCompCoords.x + currCoords.x;
                if (newWidth < 8) {
                    newWidth = 8;
                }
                int newHeight = initialSize.height - mouseDownCompCoords.y + currCoords.y;
                if (newHeight < 8) {
                    newHeight = 8;
                }
                CaptureRectFrame.this.setSize(newWidth, newHeight);
                CaptureRectFrame.this.updateInspector();
            }
        });

        this.setSize(400, 300);
        locationOnScreen = SwingUtil.moveToScreenCenter(this);
        updateInspector();
        setOpacity(0.55f);
    }

    public void setOpacity(float value) {
        if (!(opacityIsSupported != null && !opacityIsSupported.booleanValue())) {
            opacityIsSupported = false;
            try {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();
                if (gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
                    super.setOpacity(value);
                    opacityIsSupported = true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void updateInspector() {
        SwingUtil.getComponentRect(locationOnScreen, this, componentRect);
        inspectorLabel.setText(componentRect.toString());
    }

    public Rectangle getRectSnapshot() {
        return new Rectangle(componentRect);
    }
}
