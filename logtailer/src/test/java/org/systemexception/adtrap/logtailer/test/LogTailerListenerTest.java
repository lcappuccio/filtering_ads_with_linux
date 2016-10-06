package org.systemexception.adtrap.logtailer.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.systemexception.adtrap.logtailer.services.HttpConnector;
import org.systemexception.adtrap.logtailer.services.LogTailer;
import org.systemexception.adtrap.logtailer.services.LogTailerListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystemException;
import java.util.concurrent.LinkedBlockingQueue;

import static junit.framework.TestCase.assertTrue;

public class LogTailerListenerTest {

	private static File testLogFile, testLogFileRotate;
	private LogTailerListener logTailerListener;
	private LogTailer logTailer;
	private final LinkedBlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>();
	private final static File INFO_LOG_FILE = new File("target/info.log");
	private final static String TEST_LOG_FILE = "empty.log";
	private final static int SLEEP_TIMER = 100, THREAD_SLEEP = 500;

	@BeforeClass
	public static void setLogTailerListenerTest() throws URISyntaxException, FileSystemException {
		URL testLogFileUrl = ClassLoader.getSystemResource(TEST_LOG_FILE);
		testLogFile = new File(testLogFileUrl.toURI());
		if (testLogFile.exists()) {
			boolean delete = testLogFile.delete();
			if (!delete) {
				throw new FileSystemException("Could not delete");
			}
		}
		testLogFileRotate = new File(testLogFile.getAbsolutePath() + ".1");
		if (testLogFileRotate.exists()) {
			boolean delete = testLogFileRotate.delete();
			if (!delete) {
				throw new FileSystemException("Could not delete");
			}
		}
	}

	@Before
	public void setUp() throws URISyntaxException {
		logTailerListener = new LogTailerListener(blockingQueue);
		logTailer = new LogTailer(testLogFile, logTailerListener, SLEEP_TIMER);

		Thread threadLogTailer = new Thread(logTailer);
		threadLogTailer.start();

		HttpConnector httpConnector = new HttpConnector(blockingQueue);
		Thread threadHttpConnector = new Thread(httpConnector);
		threadHttpConnector.start();
	}

	@After
	public void tearDown() {
		logTailer.stop();
	}

	@Test
	public void should_listen_new_lines() throws InterruptedException, IOException {
		String outString = "TEST" + System.currentTimeMillis();
		write(testLogFile, outString);
		Thread.sleep(THREAD_SLEEP);
		String logFileToString = FileUtils.readFileToString(INFO_LOG_FILE, Charset.defaultCharset());

		assertTrue("Not logged " + outString, logFileToString.contains(outString));
	}

	@Test
	public void should_log_file_not_found() throws InterruptedException, IOException {
		String nonExistingLogFile = "idontexist.log";
		File testLogFile = new File(nonExistingLogFile);
		logTailerListener = new LogTailerListener(blockingQueue);
		logTailer = new LogTailer(testLogFile, logTailerListener, SLEEP_TIMER);
		Thread thread = new Thread(logTailer);
		thread.start();
		Thread.sleep(THREAD_SLEEP);
		String logFileToString = FileUtils.readFileToString(new File("target/error.log"), Charset.defaultCharset());

		assertTrue("FileNotFoundException not logged", logFileToString.contains(FileNotFoundException.class.getName()));
	}

	@Test
	public void should_log_file_rotate() throws InterruptedException, IOException {
		String outString = "TEST" + System.currentTimeMillis();
		write(testLogFile, outString);
		Thread.sleep(THREAD_SLEEP);
		FileUtils.moveFile(testLogFile, testLogFileRotate);
		outString = "TEST" + System.currentTimeMillis();
		write(testLogFile, outString);
		Thread.sleep(THREAD_SLEEP);
		String logFileToString = FileUtils.readFileToString(INFO_LOG_FILE, Charset.defaultCharset());

		assertTrue("File rotation not logged", logFileToString.contains("File rotated"));
	}

	/**
	 * Append some lines to a file
	 */
	private void write(File file, String... lines) throws IOException {
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
			for (String line : lines) {
				writer.write(line + System.getProperty("line.separator"));
			}
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

}