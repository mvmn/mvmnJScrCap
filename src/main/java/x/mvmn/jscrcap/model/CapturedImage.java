package x.mvmn.jscrcap.model;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CapturedImage {

	private final BufferedImage image;
	private final long captureTime;
	private final String stringRepresentation;

	public CapturedImage(BufferedImage image) {
		this.image = image;
		this.captureTime = System.currentTimeMillis();
		stringRepresentation = "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(captureTime)) + "; Size: " + image.getWidth() + " x "
				+ image.getHeight();
	}

	public BufferedImage getImage() {
		return image;
	}

	public long getCaptureTime() {
		return captureTime;
	}

	public String toString() {
		return stringRepresentation;
	}

}
