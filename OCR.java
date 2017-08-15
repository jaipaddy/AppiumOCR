import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.sikuli.basics.Debug;
import org.sikuli.basics.Settings;
import org.sikuli.script.Finder;
import org.sikuli.script.Location;
import org.sikuli.script.Match;
import org.sikuli.script.Pattern;

import com.google.common.base.Function;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;

public class OCR {

	private static final double DEFAULT_MIN_SIMILARITY = 0.7;
	private static Logger Log = Logger.getLogger(OCR.class);
	private AppiumDriver<?> driver;
	private Point2D coords;

	public OCR(AppiumDriver<?> driver) {
		this.driver = driver;
		Settings.MinSimilarity = DEFAULT_MIN_SIMILARITY;
		coords = new Point2D.Double(-1, -1);
	}

	/**
	 * @param targetImgPath
	 * @param timeoutDuration
	 */
	public void clickImage(String targetImgPath, long timeoutDuration) {
		clickImage(targetImgPath, timeoutDuration, Settings.MinSimilarity, null);
	}

	/**
	 * @param targetImgPath
	 * @param timeoutDuration
	 * @param minSimilarityValue
	 * @param targetOffset
	 */
	public void clickImage(String targetImgPath, long timeoutDuration, double minSimilarityValue,
			Location targetOffset) {
		// Wait for image detection
		waitUntilImageExists(targetImgPath, timeoutDuration, minSimilarityValue, targetOffset);
		// Click on the coordinates found
		if ((coords.getX() > -1) && (coords.getY() > -1)) {
			Log.info(String.format("Tap on [%4.0f,%4.0f]", coords.getX(), coords.getY()));
			TouchAction action = new TouchAction(driver);
			action.tap((int) coords.getX(), (int) coords.getY()).perform();
		} else {
			throw new ElementNotVisibleException("Element not found - " + targetImgPath);
		}
	}

	/**
	 * Return Point2D X,Y coordinates for the target image
	 *
	 * @param baseImg
	 * @param targetImgPath
	 * @param minSimilarityValue
	 * @param targetOffset
	 * @return X,Y coordinates
	 */
	public Point2D getCoords(BufferedImage baseImg, String targetImgPath, double minSimilarityValue,
			Location targetOffset) {
		// set new minimum similarity
		Settings.MinSimilarity = minSimilarityValue;
		Match m;
		Finder f = null;
		try {
			f = new Finder(baseImg);
			assert f != null;
			String found = null;
			if (targetOffset == null) {
				found = f.find(new Pattern(targetImgPath).similar((float) minSimilarityValue));
			} else if (targetOffset.getX() >= 0 || targetOffset.getY() >= 0) {
				found = f.find(
						new Pattern(targetImgPath).similar((float) minSimilarityValue).targetOffset(targetOffset));
			}
			Log.debug("Found image match for " + found);
			if (f.hasNext()) {
				m = f.next();
				coords.setLocation(m.getTarget().getX() / 2, m.getTarget().getY() / 2);
			}
			Log.debug(
					String.format("Coordinates on phone screen - [X,Y] = [%4.0f,%4.0f]", coords.getX(), coords.getY()));
		} catch (NullPointerException e) {
			Log.error("Is targetImage valid?", e);
		} finally {
			if (f != null)
				f.destroy();
			// revert to default similarity
			Settings.MinSimilarity = DEFAULT_MIN_SIMILARITY;
		}

		return coords;
	}

	public Point2D getCoords() {
		return coords;
	}

	/**
	 * Convenience method that takes a screenshot of the device and returns a
	 * BufferedImage for further processing.
	 *
	 * @return screenshot from the device as BufferedImage
	 */
	private BufferedImage takeScreenshot() {
		Debug.on(3);
		File scrFile = driver.getScreenshotAs(OutputType.FILE);
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(scrFile);
		} catch (IOException e) {
			Log.error("Screenshot could not be taken!", e);
		}
		return bufferedImage;
	}

	/**
	 * Convenience method that takes a screenshot of the device and returns the path
	 * for further processing.
	 *
	 * @param filename
	 * @return File path
	 */
	private String takeScreenshot(String filename) {
		Debug.on(3);
		File scrFile = driver.getScreenshotAs(OutputType.FILE);

		File classpathRoot = new File(System.getProperty("user.dir"));
		File imgDir = new File(classpathRoot, "src/test/resources/img");

		String path = imgDir + "/" + filename + ".png";
		try {
			FileUtils.copyFile(scrFile, new File(path));
		} catch (IOException e) {
			Log.error("Could not copy file", e);
		}
		Log.info("*** Screenshot File path ***" + path);
		return path;
	}

	/**
	 * Convenience method that returns true if the element is visible on the screen.
	 * Used as an expected condition in waitUntilImageExists
	 *
	 * @throws IOException
	 */
	private Boolean elementExists(String targetImgPath, double similarity, final Location targetOffset)
			throws IOException {
		coords = new Point2D.Double(-1, -1);
		coords = getCoords(takeScreenshot(), targetImgPath, similarity, targetOffset);
		return (coords.getX() > -1) && (coords.getY() > -1);
	}

	/**
	 * @param targetImgPath
	 * @param timeoutDuration
	 *            in seconds
	 */
	public void waitUntilImageExists(final String targetImgPath, long timeoutDuration) {
		waitUntilImageExists(targetImgPath, timeoutDuration, Settings.MinSimilarity, null);
	}

	/**
	 * Custom explicit wait method that waits for @timeoutDuration until element is
	 * visible.
	 *
	 * @param targetImgPath
	 * @param timeoutDuration
	 * @param similarity
	 * @param targetOffset
	 */
	private void waitUntilImageExists(final String targetImgPath, long timeoutDuration, final double similarity,
			final Location targetOffset) {
		int count = 0;
		// Scroll down 5 times and then try match image
		while (count < 5) {
			try {
				new WebDriverWait(driver, timeoutDuration).until(new Function<WebDriver, Boolean>() {
					public Boolean apply(WebDriver arg0) {
						try {
							return elementExists(targetImgPath, similarity, targetOffset);
						} catch (IOException e) {
							Log.error(e);
						}
						return false;
					}
				});
			} catch (org.openqa.selenium.TimeoutException e) {
				Log.error("Image not detected, scrolling down to detect image match...");
				scrollDown();
			}
			count++;
		}
	}

	private void scrollDown() {
		Dimension size = driver.manage().window().getSize();
		int starty = (int) (size.height * 0.80);
		int endy = (int) (size.height * 0.40);
		int startx = size.width / 2;
		TouchAction action = new TouchAction(driver);
		action.press(startx, starty).waitAction(Duration.ofMillis(0)).moveTo(startx, endy - starty).release().perform();
	}
}
