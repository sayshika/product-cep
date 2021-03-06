/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.integration.test.outputflow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.event.simulator.stub.types.EventDto;
import org.wso2.carbon.integration.test.client.WireMonitorServer;
import org.wso2.cep.integration.common.utils.CEPIntegrationTest;

public class HTTPTestCase extends CEPIntegrationTest {

    private static final Log log = LogFactory.getLog(SOAPTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);

        String loggedInSessionCookie = getSessionCookie();

        eventStreamManagerAdminServiceClient = configurationUtil.getEventStreamManagerAdminServiceClient(
                backendURL, loggedInSessionCookie);
        eventPublisherAdminServiceClient = configurationUtil.getEventPublisherAdminServiceClient(
                backendURL, loggedInSessionCookie);
        eventSimulatorAdminServiceClient = configurationUtil.getEventSimulatorAdminServiceClient(
                backendURL, loggedInSessionCookie);
        Thread.sleep(45000);

    }

    @Test(groups = {"wso2.cep"}, description = "Testing HTTP publisher with JSON formatted event with default mapping")
    public void httpJSONTestWithDefaultMappingScenario() throws Exception {

        int startESCount = eventStreamManagerAdminServiceClient.getEventStreamCount();
        int startEPCount = eventPublisherAdminServiceClient.getActiveEventPublisherCount();

        EventDto eventDto = new EventDto();
        eventDto.setEventStreamId("org.wso2.event.sensor.stream:1.0.0");
        eventDto.setAttributeValues(new String[]{"199008131245", "false", "100", "temperature", "23.45656", "7.12324",
                "100.34", "23.4545"});

        //Add StreamDefinition
        String streamDefinitionAsString = getJSONArtifactConfiguration("outputflows/sample0062",
                "org.wso2.event.sensor.stream_1.0.0.json");
        eventStreamManagerAdminServiceClient.addEventStreamAsString(streamDefinitionAsString);
        Assert.assertEquals(eventStreamManagerAdminServiceClient.getEventStreamCount(), startESCount + 1);

        //Add ActiveMQ JMS EventPublisher
        String eventPublisherConfig = getXMLArtifactConfiguration("outputflows/sample0062", "httpJson.xml");
        eventPublisherAdminServiceClient.addEventPublisherConfiguration(eventPublisherConfig);
        Assert.assertEquals(eventPublisherAdminServiceClient.getActiveEventPublisherCount(), startEPCount + 1);

        Thread.sleep(10000);
        WireMonitorServer wireMonitorServer = new WireMonitorServer(9445);
        Thread wireMonitorServerThread = new Thread(wireMonitorServer);
        wireMonitorServerThread.start();

        Thread.sleep(3000);

        eventSimulatorAdminServiceClient.sendEvent(eventDto);
        //wait while all stats are published
        Thread.sleep(30000);

        wireMonitorServer.shutdown();

        String receivedEvent = wireMonitorServer.getCapturedMessage().replaceAll("\\s+","");
        log.info(receivedEvent);

        String sentEvent = "{\"event\":{\"metaData\":{\"timestamp\":\"199008131245\",\"isPowerSaverEnabled\":\"false\"," +
                "\"sensorId\":\"100\",\"sensorName\":\"temperature\"},\"correlationData\":{\"longitude\":\"23.45656\"," +
                "\"latitude\":\"7.12324\"},\"payloadData\":{\"humidity\":\"100.34\",\"sensorValue\":\"23.4545\"}}}";

        eventStreamManagerAdminServiceClient.removeEventStream("org.wso2.event.sensor.stream", "1.0.0");
        eventPublisherAdminServiceClient.removeInactiveEventPublisherConfiguration("httpJson.xml");

        try {
            Assert.assertTrue(receivedEvent.contains(sentEvent), "Incorrect mapping has occurred!");
        } catch (Throwable e) {
            log.error("Exception thrown: " + e.getMessage(), e);
            Assert.fail("Exception: " + e.getMessage());
        }

    }

    @Test(groups = {"wso2.cep"}, description = "Testing HTTP publisher with Text formatted event with custom mapping",
            dependsOnMethods = {"httpJSONTestWithDefaultMappingScenario"})
    public void httpTextTestWithDefaultMappingScenario() throws Exception {

        int startESCount = eventStreamManagerAdminServiceClient.getEventStreamCount();
        int startEPCount = eventPublisherAdminServiceClient.getActiveEventPublisherCount();

        EventDto eventDto = new EventDto();
        eventDto.setEventStreamId("org.wso2.event.message.stream:1.0.0");
        eventDto.setAttributeValues(new String[]{"199008131245", "Lasantha Fernando", "2321.56", "BATA", "199008031245"});

        //Add StreamDefinition
        String streamDefinitionAsString = getJSONArtifactConfiguration("outputflows/sample0062",
                "org.wso2.event.message.stream_1.0.0.json");
        eventStreamManagerAdminServiceClient.addEventStreamAsString(streamDefinitionAsString);
        Assert.assertEquals(eventStreamManagerAdminServiceClient.getEventStreamCount(), startESCount + 1);

        //Add ActiveMQ JMS EventPublisher
        String eventPublisherConfig = getXMLArtifactConfiguration("outputflows/sample0062", "httpText.xml");
        eventPublisherAdminServiceClient.addEventPublisherConfiguration(eventPublisherConfig);
        Assert.assertEquals(eventPublisherAdminServiceClient.getActiveEventPublisherCount(), startEPCount + 1);

        Thread.sleep(10000);
        WireMonitorServer wireMonitorServer = new WireMonitorServer(9445);
        Thread wireMonitorServerThread = new Thread(wireMonitorServer);
        wireMonitorServerThread.start();

        Thread.sleep(3000);

        eventSimulatorAdminServiceClient.sendEvent(eventDto);
        //wait while all stats are published
        Thread.sleep(30000);

        wireMonitorServer.shutdown();

        String receivedEvent = wireMonitorServer.getCapturedMessage();
        log.info(receivedEvent);

        String sentEvent = "Hello Lasantha Fernando, " +
                "You have done transaction with your credit card for an amount Rs. 2321.56 with vendor: BATA.";

        eventStreamManagerAdminServiceClient.removeEventStream("org.wso2.event.message.stream", "1.0.0");
        eventPublisherAdminServiceClient.removeInactiveEventPublisherConfiguration("httpText.xml");

        try {
            Assert.assertTrue(receivedEvent.contains(sentEvent), "Incorrect mapping has occurred!");
        } catch (Throwable e) {
            log.error("Exception thrown: " + e.getMessage(), e);
            Assert.fail("Exception: " + e.getMessage());
        }

    }

    @Test(groups = {"wso2.cep"}, description = "Testing HTTP publisher with XML formatted event with default mapping",
            dependsOnMethods = {"httpTextTestWithDefaultMappingScenario"})
    public void httpXMLTestWithDefaultMappingScenario() throws Exception {

        int startESCount = eventStreamManagerAdminServiceClient.getEventStreamCount();
        int startEPCount = eventPublisherAdminServiceClient.getActiveEventPublisherCount();

        EventDto eventDto = new EventDto();
        eventDto.setEventStreamId("org.wso2.event.sensor.stream:1.0.0");
        eventDto.setAttributeValues(new String[]{"199008131245", "false", "100", "temperature", "23.45656", "7.12324",
                "100.34", "23.4545"});

        //Add StreamDefinition
        String streamDefinitionAsString = getJSONArtifactConfiguration("outputflows/sample0062",
                "org.wso2.event.sensor.stream_1.0.0.json");
        eventStreamManagerAdminServiceClient.addEventStreamAsString(streamDefinitionAsString);
        Assert.assertEquals(eventStreamManagerAdminServiceClient.getEventStreamCount(), startESCount + 1);

        //Add ActiveMQ JMS EventPublisher
        String eventPublisherConfig = getXMLArtifactConfiguration("outputflows/sample0062", "httpXml.xml");
        eventPublisherAdminServiceClient.addEventPublisherConfiguration(eventPublisherConfig);
        Assert.assertEquals(eventPublisherAdminServiceClient.getActiveEventPublisherCount(), startEPCount + 1);

        Thread.sleep(10000);
        WireMonitorServer wireMonitorServer = new WireMonitorServer(9445);
        Thread wireMonitorServerThread = new Thread(wireMonitorServer);
        wireMonitorServerThread.start();

        Thread.sleep(3000);

        eventSimulatorAdminServiceClient.sendEvent(eventDto);
        //wait while all stats are published
        Thread.sleep(30000);

        wireMonitorServer.shutdown();

        String receivedEvent = wireMonitorServer.getCapturedMessage().replaceAll("\\s+", "");
        log.info(receivedEvent);

        String sentEvent = "<events><event><metaData><timestamp>199008131245</timestamp>" +
                "<isPowerSaverEnabled>false</isPowerSaverEnabled><sensorId>100</sensorId>" +
                "<sensorName>temperature</sensorName></metaData><correlationData><longitude>23.45656</longitude>" +
                "<latitude>7.12324</latitude></correlationData><payloadData><humidity>100.34</humidity>" +
                "<sensorValue>23.4545</sensorValue></payloadData></event></events>";

        eventStreamManagerAdminServiceClient.removeEventStream("org.wso2.event.sensor.stream", "1.0.0");
        eventPublisherAdminServiceClient.removeInactiveEventPublisherConfiguration("httpXml.xml");

        try {
            Assert.assertTrue(receivedEvent.contains(sentEvent), "Incorrect mapping has occurred!");
        } catch (Throwable e) {
            log.error("Exception thrown: " + e.getMessage(), e);
            Assert.fail("Exception: " + e.getMessage());
        }

    }
}
