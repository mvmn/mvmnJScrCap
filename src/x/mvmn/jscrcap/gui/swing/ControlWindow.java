package x.mvmn.jscrcap.gui.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import x.mvmn.jscrcap.model.CapturedImage;
import x.mvmn.jscrcap.model.CapturesTableModel;
import x.mvmn.jscrcap.util.swing.SwingHelper;

public class ControlWindow extends JFrame implements WindowListener {

	private static final long serialVersionUID = -2200911929118097957L;

	private final JButton btnToggleViewCaptureRect = new JButton("Show capture rect");
	private final JButton btnResetCaptureRect = new JButton("Reset capture rect");
	private final CaptureRectFrame captureRectFrame = new CaptureRectFrame();
	private final CapturesTableModel capturesTableModel = new CapturesTableModel();
	private final JTable tblResults;
	private final JLabel preview = new JLabel();
	private final JButton btnCaptureOne = new JButton("Capture image");
	private final JButton btnSaveOne = new JButton("Save image");

	private volatile CapturedImage currentlyPreviewed = null;

	public ControlWindow() {
		super("MVMn Java Screen Capture tool");

		tblResults = new JTable(capturesTableModel);

		btnToggleViewCaptureRect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (captureRectFrame.isVisible()) {
					captureRectFrame.setVisible(false);
					btnToggleViewCaptureRect.setText("Show capture rect");
				} else {
					captureRectFrame.setVisible(true);
					btnToggleViewCaptureRect.setText("Hide capture rect");
					captureRectFrame.updateInspector();
				}
			}
		});

		btnResetCaptureRect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				captureRectFrame.setSize(400, 300);
				SwingHelper.moveToScreenCenter(captureRectFrame);
			}
		});

		btnCaptureOne.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean captureRectVisible = captureRectFrame.isVisible();
				if (!captureRectVisible) {
					captureRectFrame.setVisible(true);
					{
						// Workaround. Don't even ask...
						captureRectFrame.invalidate();
						captureRectFrame.validate();
						captureRectFrame.repaint();
						int i = 0;
						while (!captureRectFrame.isVisible() && i++ < 100) {
							try {
								Thread.sleep(1);
							} catch (InterruptedException e1) {
							}
						}
					}
				}
				Rectangle captureRect = SwingHelper.getComponentRect(captureRectFrame);
				captureRectFrame.setVisible(false);
				{
					// Same workaround again.
					captureRectFrame.invalidate();
					captureRectFrame.validate();
					captureRectFrame.repaint();
					int i = 0;
					while (captureRectFrame.isVisible() && i++ < 100) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e1) {
						}
					}
				}
				BufferedImage screenshot = SwingHelper.getRobot().createScreenCapture(captureRect);
				capturesTableModel.addImage(new CapturedImage(screenshot));
				captureRectFrame.setVisible(captureRectVisible);
			}
		});

		JPanel buttonsForCapturingPanel = new JPanel(new BorderLayout());
		buttonsForCapturingPanel.add(btnCaptureOne, BorderLayout.EAST);

		JPanel buttonsForCaptureRectControlPanel = new JPanel(new BorderLayout());
		buttonsForCaptureRectControlPanel.add(btnToggleViewCaptureRect, BorderLayout.CENTER);
		buttonsForCaptureRectControlPanel.add(btnResetCaptureRect, BorderLayout.EAST);

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(buttonsForCapturingPanel, BorderLayout.NORTH);
		contentPane.add(buttonsForCaptureRectControlPanel, BorderLayout.SOUTH);

		JPanel previewPanel = new JPanel(new BorderLayout());
		previewPanel.add(btnSaveOne, BorderLayout.NORTH);
		previewPanel.add(new JScrollPane(preview), BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tblResults), previewPanel);
		split.setResizeWeight(0.5);
		split.setDividerLocation(0.5);
		contentPane.add(split, BorderLayout.CENTER);

		tblResults.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (tblResults.getSelectedRow() >= 0) {
					currentlyPreviewed = capturesTableModel.getValueAt(tblResults.getSelectedRow(), 0);
					preview.setIcon(new ImageIcon(currentlyPreviewed.getImage()));
				}
			}
		});

		btnSaveOne.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentlyPreviewed != null) {
					CapturedImage capture = currentlyPreviewed;
					Object formatName = JOptionPane.showInputDialog(ControlWindow.this, "Choose format", "Save image", JOptionPane.INFORMATION_MESSAGE, null,
							ImageIO.getWriterFormatNames(), ImageIO.getWriterFormatNames()[0]);
					if (formatName != null) {
						JFileChooser chooser = new JFileChooser();
						chooser.setMultiSelectionEnabled(false);
						// chooser.setName("image." + formatName.toString());
						if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(ControlWindow.this)) {
							try {
								ImageIO.write(capture.getImage(), formatName.toString(), chooser.getSelectedFile());
							} catch (IOException e1) {
								e1.printStackTrace();
								JOptionPane.showMessageDialog(ControlWindow.this,
										"Error occurred while saving: " + e1.getClass().getSimpleName() + " - " + e1.getMessage());
							}
						}
					}
				}
			}
		});

		this.addWindowListener(this);
		this.pack();
		split.setDividerLocation(0.5);
		SwingHelper.moveToScreenCenter(this);
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		captureRectFrame.setVisible(false);
		captureRectFrame.dispose();
		this.dispose();
	}

	@Override
	public void windowClosed(WindowEvent e) {

	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {

	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
