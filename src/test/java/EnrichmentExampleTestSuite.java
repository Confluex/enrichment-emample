import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.modules.interceptor.processors.MuleMessageTransformer;
import org.mule.munit.common.mocking.Attribute;
import org.mule.munit.common.mocking.MessageProcessorMocker;
import org.mule.munit.runner.functional.FunctionalMunitSuite;

public class EnrichmentExampleTestSuite extends FunctionalMunitSuite {

	@Override
	protected String getConfigResources() {
		return "enrichment-example.xml,usps-lookup.xml";
	}

	@Test
	public void uspsCityStateLookupTest() throws Exception {
		setupMock();
		MuleEvent testEvent = testEvent(getSingleRecordMessage("02861"));
		MuleEvent resultMuleEvent = runFlow("usps-city-state-lookup", testEvent);
		verifySingleXmlRecordMessage(resultMuleEvent);
	}

	@Test
	public void singleAddressEnricherTest() throws Exception {
		setupMock();
		MuleEvent testEvent = testEvent(getSingleRecordMessage("02861"));
		MuleEvent resultMuleEvent = runFlow("address-enricher-flow", testEvent);
		verifySingleJsonRecordMessage(resultMuleEvent);
	}

	@Test
	public void multipleAddressEnricherTest() throws Exception {
		setupMock();
		MuleEvent testEvent = testEvent(getMultipleRecordMessage());
		MuleEvent resultMuleEvent = runFlow("multiple-address-enricher-flow", testEvent);
		verifyMultipleJsonRecordMessage(resultMuleEvent);
	}

	@Test
	public void dataweaveAddressEnricherTest() throws Exception {
		setupMock();
		MuleEvent testEvent = testEvent(getMultipleRecordMessage());
		MuleEvent resultMuleEvent = runFlow("dataweave-multiple-address-enricher-flow", testEvent);
		verifyMultipleJsonRecordMessage(resultMuleEvent);
	}

	private void verifySingleXmlRecordMessage(MuleEvent resultMuleEvent) {
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<CityStateLookupResponse><ZipCode ID=\"0\"><Zip5>02861</Zip5><City>PAWTUCKET</City><State>RI</State></ZipCode></CityStateLookupResponse>";
		Assert.assertEquals(expected, resultMuleEvent.getMessage().getPayload().toString());
	}

	private void verifySingleJsonRecordMessage(MuleEvent resultMuleEvent) {
		Assert.assertEquals("Ralph", getPayloadField(resultMuleEvent.getMessage().getPayload(), "name"));
		Assert.assertEquals("PAWTUCKET", getPayloadField(resultMuleEvent.getMessage().getPayload(), "city"));
		Assert.assertEquals("RI", getPayloadField(resultMuleEvent.getMessage().getPayload(), "state"));
		Assert.assertEquals("02861", getPayloadField(resultMuleEvent.getMessage().getPayload(), "zip"));
	}

	private void verifyMultipleJsonRecordMessage(MuleEvent resultMuleEvent) {
		Object payload = getPayloadMap(resultMuleEvent.getMessage(), 0);
		Assert.assertEquals("Ralph", getPayloadField(payload, "name"));
		Assert.assertEquals("PAWTUCKET", getPayloadField(payload, "city"));
		Assert.assertEquals("RI", getPayloadField(payload, "state"));
		Assert.assertEquals("02861", getPayloadField(payload, "zip"));
		payload = getPayloadMap(resultMuleEvent.getMessage(), 1);
		Assert.assertEquals("George", getPayloadField(payload, "name"));
		Assert.assertEquals("FORT CAMPBELL", getPayloadField(payload, "city"));
		Assert.assertEquals("KY", getPayloadField(payload, "state"));
		Assert.assertEquals("42223", getPayloadField(payload, "zip"));
	}

	private HashMap<String, String> getSingleRecordMessage(String zip) {
		HashMap<String, String> result = new HashMap<String, String>();

		if ("02861".equals(zip)) {
			result.put("name", "Ralph");
			result.put("zip", "02861");
		} else {
			result.put("name", "George");
			result.put("zip", "42223");
		}

		return result;
	}

	private HashMap<String, List<HashMap<String, String>>> getMultipleRecordMessage() {
		HashMap<String, List<HashMap<String, String>>> result = new HashMap<String, List<HashMap<String, String>>>();
		List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

		list.add(getSingleRecordMessage("02861"));
		list.add(getSingleRecordMessage("42223"));
		result.put("addresses", list);

		return result;
	}

	@SuppressWarnings("unchecked")
	private String getPayloadField(Object payload, String key) {
		// Extracted this common code to avoid littering the rest of the code
		// with SuppressWarnings.
		return ((HashMap<String, String>) payload).get(key);
	}

	@SuppressWarnings("unchecked")
	private HashMap<String, String> getPayloadMap(MuleMessage original, int index) {
		// Extracted this common code to avoid littering the rest of the code
		// with SuppressWarnings.
		List<HashMap<String, String>> list = ((HashMap<String, List<HashMap<String, String>>>) original.getPayload())
				.get("addresses");
		return list.get(index);
	}

	private void setupMock() {
		MessageProcessorMocker mock = whenMessageProcessor("flow").ofNamespace("mule")
				.withAttributes(Attribute.attribute("name").withValue("usps-city-state-lookup"));
		mock.thenApply(new MockTransformer());
	}

	class MockTransformer implements MuleMessageTransformer {
		private String zip02861 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<CityStateLookupResponse><ZipCode ID=\"0\"><Zip5>02861</Zip5><City>PAWTUCKET</City><State>RI</State></ZipCode></CityStateLookupResponse>";
		private String zip42223 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<CityStateLookupResponse><ZipCode ID=\"0\"><Zip5>42223</Zip5><City>FORT CAMPBELL</City><State>KY</State></ZipCode></CityStateLookupResponse>";

		@Override
		public MuleMessage transform(MuleMessage original) {
			String key = getPayloadField(original.getPayload(), "zip");
			String payload = key == "02861" ? zip02861 : key == "42223" ? zip42223 : "Error: unknown zip code";
			MuleMessage result = muleMessageWithPayload(payload);
			result.setProperty("content-length", payload.length(), PropertyScope.INBOUND);
			result.setProperty("content-type", "text/xml", PropertyScope.INBOUND);

			original = result;
			return result;
		}
	}
}
