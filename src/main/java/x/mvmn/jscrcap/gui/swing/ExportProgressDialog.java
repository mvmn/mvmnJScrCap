package x.mvmn.jscrcap.gui.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import x.mvmn.jscrcap.util.GifExportThread;

public class ExportProgressDialog extends JDialog {

	private static final long serialVersionUID = 4028626350781591141L;

	private final JProgressBar progressBar;
	private final JLabel progressLabel = new JLabel("Exporting...");
	private final JLabel fileNameLabel;
	private final JButton btnCancel = new JButton("Cancel export");
	private final int progressMax;

	private volatile GifExportThread exportThread = null;
	private final Object exportThreadLockObject = new Object();

	public ExportProgressDialog(final Frame parentFrame, final int progressMax, final String fileName) {
		super(parentFrame, "GIF export progress", false);
		this.progressMax = progressMax;

		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				synchronized (exportThreadLockObject) {
					exportThread.requestStop();
					Thread.yield();
					exportThread.interrupt();
				}
			}
		});

		progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, progressMax);
		fileNameLabel = new JLabel("Saving file " + fileName);
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(fileNameLabel, BorderLayout.NORTH);
		contentPane.add(progressLabel, BorderLayout.WEST);
		contentPane.add(progressBar, BorderLayout.CENTER);
		contentPane.add(btnCancel, BorderLayout.SOUTH);
		progressLabel.setText("Exported frames 0 of " + progressMax);
	}

	public void setExportThread(final GifExportThread exportThread) {
		synchronized (exportThreadLockObject) {
			this.exportThread = exportThread;
		}
	}

	public void setProgress(final int progress) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progressBar.setValue(progress);
				progressLabel.setText("Exported frames " + progress + " of " + progressMax);
			}
		});
	}
}
