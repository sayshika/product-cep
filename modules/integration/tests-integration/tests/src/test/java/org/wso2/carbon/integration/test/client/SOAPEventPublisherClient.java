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

package org.wso2.carbon.integration.test.client;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.log4j.Logger;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * SOAPEventPublisherClient client reads a text file with multiple events and post it to the given url.
 */
public class SOAPEventPublisherClient {
    private static Logger log = Logger.getLogger(SOAPEventPublisherClient.class);
    private static List<String> messagesList = new ArrayList<String>();
    private static BufferedReader bufferedReader = null;
    private static StringBuffer message = new StringBuffer("");
    private static final String asterixLine = "*****";

    public static void publish(String url, String testCaseFolderName, String dataFileName) {
        System.out.println("Starting SOAP EventPublisher Client");
        KeyStoreUtil.setTrustStoreParams();
        ServiceClient serviceClient;
        try {
            serviceClient = new ServiceClient();
            Options options = new Options();
            options.setTo(new EndpointReference(url));
            serviceClient.setOptions(options);

            if (serviceClient != null) {
                readMsg(getTestDataFileLocation(testCaseFolderName, dataFileName));
                OMElement omElement1;

                try {
                    System.out.println("Starting sending of events...");
                    omElement1 = AXIOMUtil.stringToOM(message.toString());
                    serviceClient.fireAndForget(omElement1);
                    log.info("Message sent");

                } catch (XMLStreamException e) {
                    log.error("Error occurred when sending message " + message.toString(), e);
                } catch (AxisFault axisFault) {
                    log.error("Error occurred when sending message " + message.toString(), axisFault);
                }
            }
        } catch (Throwable t) {
            log.error("Error occurred when connecting to endpoint " + url, t);
        }
    }

    /**
     * Xml messages will be read from the given filepath and stored in the array list (messagesList)
     *
     * @param filePath Text file to be read
     */
    private static void readMsg(String filePath) {

        try {

            String line;
            bufferedReader = new BufferedReader(new FileReader(filePath));
            while ((line = bufferedReader.readLine()) != null) {
                if ((line.equals(asterixLine.trim()) && !"".equals(message.toString().trim()))) {
                    messagesList.add(message.toString());
                    message = new StringBuffer("");
                } else {
                    message = message.append(String.format("\n%s", line));
                }
            }
            if (!"".equals(message.toString().trim())) {
                messagesList.add(message.toString());
            }

        } catch (FileNotFoundException e) {
            log.error("Error in reading file " + filePath, e);
        } catch (IOException e) {
            log.error("Error in reading file " + filePath, e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                log.error("Error occurred when closing the file : " + e.getMessage(), e);
            }
        }

    }

    /**
     * File path will be created for the file to be read with respect to the artifact folder and file name
     *
     * @param testCaseFolderName Artifact folder name
     * @param dataFileName       Text file to be read
     */
    public static String getTestDataFileLocation(String testCaseFolderName, String dataFileName) throws Exception {
        String relativeFilePath =
                FrameworkPathUtil.getSystemResourceLocation() + "/artifacts/CEP/" + testCaseFolderName + File.separator
                        + dataFileName;
        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", Matcher.quoteReplacement(File.separator));
        return relativeFilePath;
    }


}

