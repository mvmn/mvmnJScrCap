package x.mvmn.jscrcap.util;

import java.awt.Component;
import java.io.File;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import x.mvmn.jscrcap.gui.swing.ExportProgressDialog;
import x.mvmn.jscrcap.model.CapturedImage;

public class GifExportThread extends Thread {

    private final CapturedImage[] captures;
    private final Component parentComponent;
    private final int delayBetweenFramesInTenthOfSeconds;
    private final File outputFile;
    private final boolean loopContinuously;
    private final ExportProgressDialog progressDialog;
    private volatile boolean stopRequested = false;

    public GifExportThread(final Component parentComponent, final ExportProgressDialog progressDialog,
            final CapturedImage[] captures, final int delayBetweenFramesInTenthOfSeconds, final File outputFile,
            final boolean loopContinuously) {
        this.progressDialog = progressDialog;
        this.captures = captures;
        this.parentComponent = parentComponent;
        this.delayBetweenFramesInTenthOfSeconds = delayBetweenFramesInTenthOfSeconds;
        this.outputFile = outputFile;
        this.loopContinuously = loopContinuously;
        progressDialog.setExportThread(this);
    }

    public void requestStop() {
        this.stopRequested = true;
    }

    public void run() {
        String resultMessage = "Nothing to export.";
        if (captures.length > 0) {
            FileImageOutputStream outputStream = null;
            ImageWriter writer = null;
            try {
                outputStream = new FileImageOutputStream(outputFile);
                Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
                if (iter.hasNext()) {
                    writer = iter.next();
                } else {
                    iter = ImageIO.getImageWritersBySuffix("GIF");
                    if (iter.hasNext()) {
                        writer = iter.next();
                    }
                }
                if (writer == null) {
                    throw new RuntimeException("The JVM doesn't have GIF image writers registered.");
                }
                // Code by example of GifSequenceWriter by Elliot Kroo
                ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();
                ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier
                        .createFromBufferedImageType(captures[0].getImage().getType());
                IIOMetadata imageMetaData = writer.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);
                String metaFormatName = imageMetaData.getNativeMetadataFormatName();

                IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

                IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");

                graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
                graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
                graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
                graphicsControlExtensionNode.setAttribute("delayTime",
                        Integer.toString(delayBetweenFramesInTenthOfSeconds * 10));
                graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

                IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
                commentsNode.setAttribute("CommentExtension", "Created with MVMn Java Screen Capture");
                IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");

                IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

                child.setAttribute("applicationID", "NETSCAPE");
                child.setAttribute("authenticationCode", "2.0");

                int loop = loopContinuously ? 0 : 1;

                child.setUserObject(new byte[] { 0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF) });
                appEntensionsNode.appendChild(child);

                imageMetaData.setFromTree(metaFormatName, root);

                writer.setOutput(outputStream);

                writer.prepareWriteSequence(null);

                int i = 0;
                for (CapturedImage capture : captures) {
                    if (stopRequested) {
                        throw new InterruptedException();
                    }
                    writer.writeToSequence(new IIOImage(capture.getImage(), null, imageMetaData), imageWriteParam);
                    progressDialog.setProgress(++i);
                    Thread.yield();
                }

                resultMessage = "Animated GIF successfully saved to " + outputFile.getAbsolutePath();
            } catch (InterruptedException iex) {
                resultMessage = "Export interrupted.";
            } catch (Exception e) {
                resultMessage = "Error occurred while exporting animated GIF: " + e.getClass().getSimpleName() + " - "
                        + e.getMessage();
            } finally {
                try {
                    if (writer != null) {
                        writer.endWriteSequence();
                    }
                } catch (Exception e) {
                }
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (Exception e) {
                }
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            private String message;

            @Override
            public void run() {
                progressDialog.setVisible(false);
                JOptionPane.showMessageDialog(parentComponent, message);
            }

            public Runnable init(String message) {
                this.message = message;
                return this;
            }
        }.init(resultMessage));
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
                return ((IIOMetadataNode) rootNode.item(i));
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return (node);
    }
}
