package x.mvmn.jscrcap.gui.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import x.mvmn.jscrcap.model.CapturedImage;
import x.mvmn.jscrcap.model.CapturesTableModel;
import x.mvmn.jscrcap.util.SequenceCaptureThread;
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
	private final JButton btnCaptureSequence = new JButton("Start sequence capturing");
	private final JButton btnSaveOne = new JButton("Save image");
	private final JSlider sliderOpacity = new JSlider(JSlider.HORIZONTAL, 0, 100, 55);
	private final JSlider sliderDelay = new JSlider(JSlider.HORIZONTAL, 1, 600, 5);
	private final JTextField fldDelay = new JTextField("5");

	private volatile CapturedImage currentlyPreviewed = null;
	private volatile SequenceCaptureThread captureThread = null;
	private final Object CAPTURE_THREAD_LOCK_OBJECT = new Object();

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
				ControlWindow.this.doCapture();
			}
		});

		sliderOpacity.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				ControlWindow.this.captureRectFrame.setOpacity(((float) ControlWindow.this.sliderOpacity.getValue()) / 100);
			}
		});

		sliderDelay.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				fldDelay.setText(String.valueOf(ControlWindow.this.sliderDelay.getValue()));
			}
		});
		fldDelay.addKeyListener(new KeyListener() {
			private final Pattern NUMERIC = Pattern.compile("\\d+");

			@Override
			public void keyTyped(KeyEvent e) {
				String val = fldDelay.getText().trim();
				if (val.length() > 0) {
					if (NUMERIC.matcher(val).matches()) {
						try {
							int intVal = Integer.parseInt(val);
							int validIntVal = intVal;
							if (validIntVal < 0) {
								validIntVal = -validIntVal;
							}
							if (validIntVal > 600) {
								validIntVal = 600;
							}
							sliderDelay.setValue(validIntVal);
							if (intVal != validIntVal) {
								fldDelay.setText(String.valueOf(validIntVal));
							}
						} catch (Exception pe) {
						}
					} else {
						fldDelay.setText(val.replaceAll("[^\\d]", ""));
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});

		btnCaptureSequence.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				synchronized (CAPTURE_THREAD_LOCK_OBJECT) {
					SequenceCaptureThread thread = captureThread;
					if (thread == null) {
						thread = new SequenceCaptureThread(ControlWindow.this, sliderDelay.getValue());
						thread.start();
						captureThread = thread;
						btnCaptureSequence.setText("Stop sequence capturing");
					} else {
						thread.requestStop();
						while (!thread.isStopped()) {
							// TODO: reconsider
							Thread.yield();
						}
						captureThread = null;
						btnCaptureSequence.setText("Start sequence capturing");
					}
				}
			}
		});

		JPanel controlsForDelayPanel = new JPanel(new BorderLayout());
		controlsForDelayPanel.add(sliderDelay, BorderLayout.CENTER);
		controlsForDelayPanel.add(new JLabel("Delay (seconds)"), BorderLayout.WEST);
		controlsForDelayPanel.add(fldDelay, BorderLayout.EAST);
		fldDelay.setPreferredSize(new Dimension(fldDelay.getFont().getSize() * 6, fldDelay.getPreferredSize().height));

		JPanel buttonsForCapturingPanel = new JPanel(new BorderLayout());
		buttonsForCapturingPanel.add(btnCaptureSequence, BorderLayout.WEST);
		buttonsForCapturingPanel.add(btnCaptureOne, BorderLayout.EAST);
		buttonsForCapturingPanel.add(controlsForDelayPanel, BorderLayout.CENTER);

		// TODO: stop overusing BorderLayout - use more appropriate layout here
		JPanel controlsForCaptureRectControlPanel = new JPanel(new BorderLayout());
		controlsForCaptureRectControlPanel.add(btnToggleViewCaptureRect, BorderLayout.WEST);
		JPanel sliderOpacityPanel = new JPanel(new BorderLayout());
		sliderOpacityPanel.add(new JLabel("Opacity:"), BorderLayout.WEST);
		sliderOpacityPanel.add(sliderOpacity, BorderLayout.CENTER);
		controlsForCaptureRectControlPanel.add(sliderOpacityPanel, BorderLayout.CENTER);
		controlsForCaptureRectControlPanel.add(btnResetCaptureRect, BorderLayout.EAST);

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(buttonsForCapturingPanel, BorderLayout.NORTH);
		contentPane.add(controlsForCaptureRectControlPanel, BorderLayout.SOUTH);

		JPanel previewPanel = new JPanel(new BorderLayout());
		previewPanel.add(btnSaveOne, BorderLayout.NORTH);
		previewPanel.add(new JScrollPane(preview), BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tblResults), previewPanel);
		split.setResizeWeight(0.5);
		split.setDividerLocation(0.5);
		contentPane.add(split, BorderLayout.CENTER);

		tblResults.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (tblResults.getSelectedRow() >= 0) {
					CapturedImage toPreview = capturesTableModel.getValueAt(tblResults.getSelectedRow(), 0);
					if (toPreview != currentlyPreviewed) {
						currentlyPreviewed = toPreview;
						preview.setIcon(new ImageIcon(toPreview.getImage()));
					}
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

	public void doCapture() {
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
		captureRectFrame.setSize(0, 0);
		captureRectFrame.setVisible(false);
		try {
			// TODO: Dirty workaround for OS X - investigate alternatives
			Thread.sleep(15);
		} catch (InterruptedException e) {
		}
		BufferedImage screenshot = SwingHelper.getRobot().createScreenCapture(captureRect);
		capturesTableModel.addImage(new CapturedImage(screenshot));
		captureRectFrame.setSize(captureRect.width, captureRect.height);
		captureRectFrame.setVisible(captureRectVisible);
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
