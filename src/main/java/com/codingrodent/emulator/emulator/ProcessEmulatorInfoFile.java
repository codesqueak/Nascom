/*
 * MIT License
 *
 * Copyright (c) 2016
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.codingrodent.emulator.emulator;

import com.sun.org.apache.xpath.internal.XPathAPI;
import com.sun.org.apache.xpath.internal.objects.XObject;
import org.w3c.dom.*;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;

/**
 * This class holds an internal representation of the machineInfo.xml document.
 */
public class ProcessEmulatorInfoFile implements ErrorHandler {

    private static final String EMULATOR_INFO_FILE = "emulatorInfo.xml";
    private final static String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private final static String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    private final SystemContext context;
    private DocumentBuilderFactory dbf;
    private ArrayList<CardData> cardInfo;

    /**
     * Standard constructor - Loads the machineInfo.xml document and resets
     * system ready for use. Will wait and poll until the document becomes
     * available.
     *
     * @param context Context required to be passed in as it is used in the
     *                constructor of system context
     * @throws ProcessEmulatorInfoFileException Thrown if unable to recover the document
     */
    ProcessEmulatorInfoFile(SystemContext context) throws ProcessEmulatorInfoFileException {
        this.context = context;
        reset();
        try {
            processMachineInfoDocument(new InputSource(new InputStreamReader(new FileInputStream(EMULATOR_INFO_FILE), "UTF-8")));
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            throw new ProcessEmulatorInfoFileException("Invalid configuration file.");
        }
    }

    /**
     * Debug code to test processing
     *
     * @param args No parameters
     */
    public static void main(String[] args) {
        System.out.println("-- start --");
        try {
            SystemContext context = SystemContext.createInstance();
            new ProcessEmulatorInfoFile(context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("-- stop --");
    }

    /**
     * Reset internal data structures
     */
    private void reset() {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        dbf.setNamespaceAware(true);
        dbf.setValidating(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        //
        cardInfo = new ArrayList<>(16);
    }

    /**
     * Create the internal representation of the machineInfo.xml document
     *
     * @param inputSource Document to be processed
     * @throws ProcessEmulatorInfoFileException Thrown on any error while processing the file
     */
    private void processMachineInfoDocument(InputSource inputSource) throws ProcessEmulatorInfoFileException {
        try {
            // Create parser
            DocumentBuilder db = dbf.newDocumentBuilder();
            // Process machineInfo.xml
            {
                db.setErrorHandler(this);
                Document document = db.parse(inputSource);
                processDocument(document);
                context.logInfoEvent("Loading emulatorInfo.XML - Complete");
            }
        } catch (Exception e) {
            // e.printStackTrace();
            throw new ProcessEmulatorInfoFileException(e);
        }
    }

    /**
     * Execute a basic xpath expression on a document from its root node down
     *
     * @param localDocument The document to be procesed
     * @param expression    The xpath expression
     * @return An iteration object for the returned node set
     */
    private NodeIterator executeXPathExpression(Node localDocument, String expression) {
        // if (debug) System.err.println("Xpath");
        try {
            XObject obj = XPathAPI.eval(localDocument, expression);
            return obj.nodeset();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract the required information from the emulatorInfo.xml document
     *
     * @param localDocument A parsed emulatorInfo.xml ready for data extraction
     * @throws ProcessEmulatorInfoFileException Thrown if any defect in the file detected
     */
    private void processDocument(Document localDocument) throws ProcessEmulatorInfoFileException {
        NodeIterator iter;
        NodeIterator propertyIterator;
        Node node;
        Node propertyNode;
        NamedNodeMap attributes;
        NamedNodeMap propertyAttributes;
        //
        try {
            //
            iter = executeXPathExpression(localDocument, "child::EmulatorInformation/child::Cards/child::Card");
            while ((iter != null) && ((node = iter.nextNode()) != null)) {
                CardData cardData = new CardData();
                cardInfo.add(cardData);
                attributes = node.getAttributes();
                cardData.setName(attributes.getNamedItem("Name").getNodeValue());
                cardData.setClassName(attributes.getNamedItem("Class").getNodeValue());
                //
                propertyIterator = executeXPathExpression(node, "child::Properties/child::Property");
                while ((propertyIterator != null) && ((propertyNode = propertyIterator.nextNode()) != null)) {
                    propertyAttributes = propertyNode.getAttributes();
                    cardData.setProperty(propertyAttributes.getNamedItem("Name").getNodeValue(), propertyAttributes.getNamedItem("Value").getNodeValue());
                }
            }
        } catch (Exception ex) {
            throw new ProcessEmulatorInfoFileException(ex.getMessage());
        }
    }

    /**
     * Get the card set as derived from the emulatorInfo.xml
     *
     * @return An array holding all cards identified in the emulatorInfo.xml
     */
    ArrayList<CardData> getAllCards() {
        return cardInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        context.logWarnEvent("Problem with the configuration file (" + exception.getMessage() + ")");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        context.logErrorEvent("Problem with the configuration file (" + exception.getMessage() + ")");
        throw new SAXException("Problem with the configuration file");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        context.logFatalEvent("Problem with the configuration file (" + exception.getMessage() + ")");
        throw new SAXException("Problem with the configuration file");
    }
}