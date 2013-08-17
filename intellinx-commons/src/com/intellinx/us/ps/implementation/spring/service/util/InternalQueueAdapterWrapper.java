package com.intellinx.us.ps.implementation.spring.service.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.intellinx.integration.internalqueue.AbstractInternalQueueAdapterBean;
import com.intellinx.queue.GXIQueueMessage;
import com.intellinx.util.JobsLogger;

/**
 * 
 * @author Renato Mendes
 *
 */
public class InternalQueueAdapterWrapper extends
		AbstractInternalQueueAdapterBean {

	private static com.intellinx.util.Logger logger = JobsLogger
			.getLogger(InternalQueueAdapterWrapper.class);

	private static String DEBUG_PARSE_PRP = "debug.in.queue.parse";

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	/**
	 * 
	 */
	protected Object getObjectFromMessage(GXIQueueMessage message) {

		StopWatch stopWatch = new Slf4JStopWatch("InternalQueueAdapterWrapper",
				"getObjectFromMessage", LOGGER_PERFORMANCE);

		try {
			ByteArrayInputStream bin = new ByteArrayInputStream(
					message.getBody());
			Element root = deserialize(bin);

			stopWatch.stop("InternalQueueAdapterWrapper",
					"getObjectFromMessage-done");

			return createValuesMap(root);
		} catch (Exception e) {
			if ((e instanceof SAXException)
					&& ("true".equalsIgnoreCase(System
							.getProperty(DEBUG_PARSE_PRP))))
				try {
					String xmlStr = new String(message.getBody(), "UTF8");
					logger.error("error_parse_int_queue_message",
							new Object[] { xmlStr });
				} catch (UnsupportedEncodingException e1) {
				}

			stopWatch.stop("InternalQueueAdapterWrapper",
					"getObjectFromMessage-done (exception)");

			throw new RuntimeException("Failed reading input queue message", e);
		}
	}

	/**
	 * 
	 * @param root
	 * @return
	 */
	private Map<String, String> createValuesMap(Element root) {

		StopWatch stopWatch = new Slf4JStopWatch("InternalQueueAdapterWrapper",
				"createValuesMap", LOGGER_PERFORMANCE);

		Map<String, String> values = new HashMap<String, String>();

		NamedNodeMap attrs = root.getAttributes();
		for (int i = 0; i < attrs.getLength(); ++i) {
			Node attr = attrs.item(i);
			values.put("event_" + attr.getNodeName(), attr.getNodeValue());
		}

		NodeList fields = root.getElementsByTagName("field");
		for (int i = 0; i < fields.getLength(); ++i) {
			Element field = (Element) fields.item(i);
			String name = StringUtils.replace(field.getAttribute("name"), " ",
					"_");
			String value = field.getTextContent();
			values.put(name, value);
		}

		stopWatch.stop("InternalQueueAdapterWrapper", "createValuesMap-done");

		return values;
	}

	/**
	 * 
	 * @param in
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private Element deserialize(InputStream in)
			throws ParserConfigurationException, SAXException, IOException {

		StopWatch stopWatch = new Slf4JStopWatch("InternalQueueAdapterWrapper",
				"deserialize", LOGGER_PERFORMANCE);

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
		Document doc = documentBuilder.parse(in);
		Element root = doc.getDocumentElement();

		stopWatch.stop("InternalQueueAdapterWrapper", "deserialize-done");

		return root;
	}

	/**
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Message get() {
		StopWatch stopWatch = new Slf4JStopWatch("InternalQueueAdapterWrapper",
				"get", LOGGER_PERFORMANCE);
		Message message = super.get();
		stopWatch.stop("InternalQueueAdapterWrapper", "get-done");
		return message;
	}

}
