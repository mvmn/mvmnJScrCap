package x.mvmn.jscrcap.gui.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import x.mvmn.jscrcap.model.CapturedImage;
import x.mvmn.jscrcap.model.CapturesTableModel;
import x.mvmn.jscrcap.util.GifExportThread;
import x.mvmn.jscrcap.util.SequenceCaptureThread;
import x.mvmn.jscrcap.util.swing.SwingHelper;

public class ControlWindow extends JFrame implements WindowListener {

	private static final long serialVersionUID = -2200911929118097957L;

	private static final int MAX_DELAY_VALUE = 6000;
	private static final int DEFAULT_DELAY_VALUE = 10;

	private final JButton btnToggleViewCaptureRect = new JButton("Show capture rect");
	private final JButton btnResetCaptureRect = new JButton("Reset capture rect");
	private final CaptureRectFrame captureRectFrame = new CaptureRectFrame();
	private final CapturesTableModel capturesTableModel = new CapturesTableModel();
	private final JTable tblResults;
	private final JLabel preview = new JLabel();
	private final JButton btnCaptureOne = new JButton("Capture image");
	private final JButton btnCaptureSequence = new JButton("Start sequence capturing");
	private final JButton btnSaveOne = new JButton("Save image");
	private final JButton btnExport = new JButton("Export animated GIF");
	private final JButton btnLoad = new JButton("Load images");
	private final JSlider sliderOpacity = new JSlider(JSlider.HORIZONTAL, 0, 100, 55);
	private final JSlider sliderDelay = new JSlider(JSlider.HORIZONTAL, 1, MAX_DELAY_VALUE, DEFAULT_DELAY_VALUE);
	private final JTextField fldDelay = new JTextField(String.valueOf(DEFAULT_DELAY_VALUE));
	private final JCheckBox cbLoopGif = new JCheckBox("Loop GIF");
	private final JComboBox<String> cbxImageFormat;
	private final String[] writerFormatNames;

	private volatile CapturedImage currentlyPreviewed = null;
	private volatile SequenceCaptureThread captureThread = null;
	private final Object CAPTURE_THREAD_LOCK_OBJECT = new Object();

	public ControlWindow() {
		super("MVMn Java Screen Capture tool");

		Set<String> uniqueFormatNames = new HashSet<String>();
		{
			String[] availableFormatNames = ImageIO.getWriterFormatNames();
			Set<String> allFormatNames = new HashSet<String>();
			for (String formatName : availableFormatNames) {
				allFormatNames.add(formatName);
			}
			for (String formatName : availableFormatNames) {
				String lowercaseName = formatName.toLowerCase();
				if (!lowercaseName.equals(formatName)) {
					if (!allFormatNames.contains(formatName.toLowerCase())) {
						uniqueFormatNames.add(formatName);
					}
				} else {
					uniqueFormatNames.add(formatName);
				}
			}
			writerFormatNames = uniqueFormatNames.toArray(new String[uniqueFormatNames.size()]);
		}
		cbxImageFormat = new JComboBox<String>(writerFormatNames);
		if (writerFormatNames.length > 0) {
			if (uniqueFormatNames.contains("jpg")) {
				cbxImageFormat.setSelectedItem("jpg");
			} else if (uniqueFormatNames.contains("jpeg")) {
				cbxImageFormat.setSelectedItem("jpeg");
			} else {
				cbxImageFormat.setSelectedIndex(0);
			}
		} else {
			cbxImageFormat.setEnabled(false);
			btnSaveOne.setEnabled(false);
		}

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
			public void actionPerformed(ActionEvent actEvent) {
				boolean captureRectWasVisible = captureRectFrame.isVisible();
				Rectangle captureRect = captureRectFrame.getRectSnapshot();
				captureRectFrame.setSize(0, 0);
				if (captureRectWasVisible) {
					captureRectFrame.setVisible(false);
					try {
						// TODO: Dirty workaround for OS X - investigate
						// alternatives
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
				BufferedImage screenshot = SwingHelper.getRobot().createScreenCapture(captureRect);
				capturesTableModel.addImage(new CapturedImage(screenshot));
				captureRectFrame.setSize(captureRect.width, captureRect.height);
				if (captureRectWasVisible) {
					try {
						// TODO: Dirty workaround for OS X - investigate
						// alternatives
						Thread.sleep(15);
					} catch (InterruptedException e) {
					}
					captureRectFrame.setVisible(true);
				}
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
							if (validIntVal > MAX_DELAY_VALUE) {
								validIntVal = MAX_DELAY_VALUE;
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
						thread = new SequenceCaptureThread(ControlWindow.this.capturesTableModel, ControlWindow.this.captureRectFrame.getRectSnapshot(),
								sliderDelay.getValue());
						thread.start();
						captureThread = thread;
						btnCaptureSequence.setText("Stop sequence capturing");
					} else {
						thread.requestStop();
						try {
							thread.interrupt();
						} catch (Exception ex) {
						}
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

		btnExport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actEvent) {
				CapturedImage[] images = ControlWindow.this.capturesTableModel.getDataSnapshot();
				if (images.length > 0) {
					boolean sizesOkToExport = true;
					int width = images[0].getImage().getWidth();
					int height = images[0].getImage().getHeight();
					for (int i = 1; i < images.length; i++) {
						BufferedImage image = images[i].getImage();
						if (image.getWidth() != width || image.getHeight() != height) {
							sizesOkToExport = false;
							break;
						}
					}
					if (!sizesOkToExport) {
						sizesOkToExport = (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(ControlWindow.this,
								"Frames have different sizes - resulting GIF will be corrupt. Continue anyway?", "Frames sizes mismatch",
								JOptionPane.OK_CANCEL_OPTION));
					}
					if (sizesOkToExport) {
						final JFileChooser fileChooser = new JFileChooser();
						if (fileChooser.showSaveDialog(ControlWindow.this) == JFileChooser.APPROVE_OPTION) {
							final ExportProgressDialog progressDialog = new ExportProgressDialog(ControlWindow.this, images.length, fileChooser
									.getSelectedFile().getAbsolutePath());
							progressDialog.pack();
							SwingHelper.moveToScreenCenter(progressDialog);
							progressDialog.setVisible(true);
							GifExportThread exportThread = new GifExportThread(ControlWindow.this, progressDialog, images, sliderDelay.getValue(), fileChooser
									.getSelectedFile(), cbLoopGif.isSelected());
							exportThread.start();
						}
					}
				} else {
					JOptionPane.showMessageDialog(ControlWindow.this, "Nothing to export.");
				}
			}
		});

		btnLoad.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent actEvent) {
				final JFileChooser fileChooser = new JFileChooser();
				fileChooser.setMultiSelectionEnabled(true);
				if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(ControlWindow.this)) {
					final File files[] = fileChooser.getSelectedFiles();
					Arrays.sort(files, new Comparator<File>() {
						@Override
						public int compare(final File o1, final File o2) {
							return o1.getName().compareTo(o2.getName());
						}
					});
					new Thread() {
						public void run() {
							for (final File file : files) {
								try {
									final BufferedImage image = ImageIO.read(file);
									capturesTableModel.addImage(new CapturedImage(image));
								} catch (final Exception e1) {
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											JOptionPane.showMessageDialog(ControlWindow.this, "Error occurred while loading: " + e1.getClass().getSimpleName()
													+ " - " + e1.getMessage());
										}
									});
								}
							}
						}
					}.start();
				}
			}
		});

		final JPanel controlsForDelayPanel = new JPanel(new BorderLayout());
		controlsForDelayPanel.add(sliderDelay, BorderLayout.CENTER);
		controlsForDelayPanel.add(new JLabel("Delay (1/10 of second)"), BorderLayout.WEST);
		controlsForDelayPanel.add(fldDelay, BorderLayout.EAST);
		fldDelay.setPreferredSize(new Dimension(fldDelay.getFont().getSize() * 6, fldDelay.getPreferredSize().height));

		final JPanel buttonsForCapturingPanel = new JPanel(new BorderLayout());
		buttonsForCapturingPanel.add(btnCaptureSequence, BorderLayout.WEST);
		buttonsForCapturingPanel.add(btnCaptureOne, BorderLayout.EAST);
		buttonsForCapturingPanel.add(controlsForDelayPanel, BorderLayout.CENTER);

		// TODO: stop overusing BorderLayout - use more appropriate layout here
		final JPanel controlsForCaptureRectControlPanel = new JPanel(new BorderLayout());
		controlsForCaptureRectControlPanel.add(btnToggleViewCaptureRect, BorderLayout.WEST);
		final JPanel sliderOpacityPanel = new JPanel(new BorderLayout());
		sliderOpacityPanel.add(new JLabel("Opacity:"), BorderLayout.WEST);
		sliderOpacityPanel.add(sliderOpacity, BorderLayout.CENTER);
		controlsForCaptureRectControlPanel.add(sliderOpacityPanel, BorderLayout.CENTER);
		controlsForCaptureRectControlPanel.add(btnResetCaptureRect, BorderLayout.EAST);

		final Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(buttonsForCapturingPanel, BorderLayout.NORTH);
		contentPane.add(controlsForCaptureRectControlPanel, BorderLayout.SOUTH);

		final JPanel btnsPanel = new JPanel(new GridLayout(3, 2));
		btnsPanel.add(btnExport);
		btnsPanel.add(cbLoopGif);
		btnsPanel.add(btnSaveOne);
		btnsPanel.add(cbxImageFormat);
		btnsPanel.add(btnLoad);

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tblResults), new JScrollPane(preview));
		split.setResizeWeight(0.5);
		split.setDividerLocation(0.5);

		final JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(split, BorderLayout.CENTER);
		mainPanel.add(btnsPanel, BorderLayout.SOUTH);

		contentPane.add(mainPanel, BorderLayout.CENTER);

		tblResults.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE) {
					int[] selectedRows = tblResults.getSelectedRows();
					if (selectedRows != null && selectedRows.length > 0) {
						Arrays.sort(selectedRows);
						for (int i = selectedRows.length - 1; i >= 0; i--) {
							capturesTableModel.delete(selectedRows[i]);
						}
						// Dear JVM,
						// It is quite possible that several relatively heavy
						// objects are ready to be removed from the heap, so
						// please kindly accept my suggestion to execute garbage
						// collection at this moment.
						// Yours truly.
						System.gc();
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});

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

					JFileChooser chooser = new JFileChooser();
					chooser.setMultiSelectionEnabled(false);
					// chooser.setName("image." + formatName.toString());
					if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(ControlWindow.this)) {
						try {
							ImageIO.write(capture.getImage(), cbxImageFormat.getSelectedItem().toString(), chooser.getSelectedFile());
						} catch (IOException e1) {
							e1.printStackTrace();
							JOptionPane.showMessageDialog(ControlWindow.this,
									"Error occurred while saving: " + e1.getClass().getSimpleName() + " - " + e1.getMessage());
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
