package org.systemexception.adtrap.logtailer.test;

import org.junit.Test;
import org.systemexception.adtrap.logtailer.services.JsonMapper;

import java.text.ParseException;

import static junit.framework.TestCase.assertTrue;

/**
 * @author leo
 * @date 02/11/2016 19:22
 */
public class JsonMapperTest {

	private final JsonMapper sut = new JsonMapper();

	@Test
	public void should_convert_to_json() throws ParseException {
		String jsonFromLogLine = sut.jsonFromLogLine(LogParserTest.LOG_LINE);

		assertTrue(jsonFromLogLine.contains("\"queryType\":\"forwarded\"," +
				"\"queryDomain\":\"e4478.a.akamaiedge.net\",\"queryTarget\":\"8.8.4.4\"}"));
	}

	@Test
	public void should_convert_dhcp_to_json() throws ParseException {
		String jsonFromLogLine = sut.jsonFromLogLine(LogParserTest.DHCP_LOG_LINE);

		assertTrue(jsonFromLogLine.contains("\"queryType\":\"DHCPREQUEST(eth0)\"," +
				"\"queryDomain\":\"e8:b2:ac:1a:79:af\",\"queryTarget\":\"192.168.0.168\"}"));
	}

	@Test
	public void should_convert_dhcpack_to_json() throws ParseException {
		String jsonFromLogLine = sut.jsonFromLogLine(LogParserTest.DHCPACK_LOG_LINE);

		assertTrue(jsonFromLogLine.contains("\"queryType\":\"DHCPACK(eth0)\"," +
				"\"queryDomain\":\"b0:e8:92:6f:a2:df\",\"queryTarget\":\"192.168.0.208\"}"));
	}

}