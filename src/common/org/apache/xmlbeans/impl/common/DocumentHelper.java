/*   Copyright 2004-2018 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.xmlbeans.impl.common;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.events.Namespace;

import org.apache.xmlbeans.XmlOptionsBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class DocumentHelper {
    private static XBLogger logger = XBLogFactory.getLogger(DocumentHelper.class);
    private static long lastLog;

    private DocumentHelper() {}

    private static class DocHelperErrorHandler implements ErrorHandler {

        public void warning(SAXParseException exception) throws SAXException {
            printError(XBLogger.WARN, exception);
        }

        public void error(SAXParseException exception) throws SAXException {
            printError(XBLogger.ERROR, exception);
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            printError(XBLogger.FATAL, exception);
            throw exception;
        }

        /** Prints the error message. */
        private void printError(int type, SAXParseException ex) {
            StringBuilder sb = new StringBuilder();
            
            String systemId = ex.getSystemId();
            if (systemId != null) {
                int index = systemId.lastIndexOf('/');
                if (index != -1)
                    systemId = systemId.substring(index + 1);
                sb.append(systemId);
            }
            sb.append(':');
            sb.append(ex.getLineNumber());
            sb.append(':');
            sb.append(ex.getColumnNumber());
            sb.append(": ");
            sb.append(ex.getMessage());

            logger.log(type, sb.toString(), ex);
        }
    }
    
    /**
     * Creates a new document builder, with sensible defaults
     *
     * @param xmlOptions
     * @throws IllegalStateException If creating the DocumentBuilder fails, e.g.
     *  due to {@link ParserConfigurationException}.
     */
    public static DocumentBuilder newDocumentBuilder(XmlOptionsBean xmlOptions) {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory(xmlOptions).newDocumentBuilder();
            documentBuilder.setEntityResolver(SAXHelper.IGNORING_ENTITY_RESOLVER);
            documentBuilder.setErrorHandler(new DocHelperErrorHandler());
            return documentBuilder;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("cannot create a DocumentBuilder", e);
        }
    }

    private static final DocumentBuilderFactory documentBuilderFactory(XmlOptionsBean options) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setValidating(false);
        trySetFeature(documentBuilderFactory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetFeature(documentBuilderFactory, XMLBeansConstants.FEATURE_LOAD_DTD_GRAMMAR, options.isLoadDTDGrammar());
        trySetFeature(documentBuilderFactory, XMLBeansConstants.FEATURE_LOAD_EXTERNAL_DTD, options.isLoadExternalDTD());
        trySetXercesSecurityManager(documentBuilderFactory, options);
        return documentBuilderFactory;
    }

    private static void trySetFeature(DocumentBuilderFactory dbf, String feature, boolean enabled) {
        try {
            dbf.setFeature(feature, enabled);
        } catch (Exception e) {
            logger.log(XBLogger.WARN, "SAX Feature unsupported", feature, e);
        } catch (AbstractMethodError ame) {
            logger.log(XBLogger.WARN, "Cannot set SAX feature because outdated XML parser in classpath", feature, ame);
        }
    }
    
    private static void trySetXercesSecurityManager(DocumentBuilderFactory dbf, XmlOptionsBean options) {
        // Try built-in JVM one first, standalone if not
        for (String securityManagerClassName : new String[]{
                //"com.sun.org.apache.xerces.internal.util.SecurityManager",
                "org.apache.xerces.util.SecurityManager"
        }) {
            try {
                Object mgr = Class.forName(securityManagerClassName).newInstance();
                Method setLimit = mgr.getClass().getMethod("setEntityExpansionLimit", Integer.TYPE);
                setLimit.invoke(mgr, options.getEntityExpansionLimit());
                dbf.setAttribute(XMLBeansConstants.SECURITY_MANAGER, mgr);
                // Stop once one can be setup without error
                return;
            } catch (ClassNotFoundException e) {
                // continue without log, this is expected in some setups
            } catch (Throwable e) {     // NOSONAR - also catch things like NoClassDefError here
                if(System.currentTimeMillis() > lastLog + TimeUnit.MINUTES.toMillis(5)) {
                    logger.log(XBLogger.WARN, "DocumentBuilderFactory Security Manager could not be setup [log suppressed for 5 minutes]", e);
                    lastLog = System.currentTimeMillis();
                }
            }
        }

        // separate old version of Xerces not found => use the builtin way of setting the property
        try {
            dbf.setAttribute(XMLBeansConstants.ENTITY_EXPANSION_LIMIT, options.getEntityExpansionLimit());
        } catch (Throwable e) {
            if(System.currentTimeMillis() > lastLog + TimeUnit.MINUTES.toMillis(5)) {
                logger.log(XBLogger.WARN, "DocumentBuilderFactory Entity Expansion Limit could not be setup [log suppressed for 5 minutes]", e);
                lastLog = System.currentTimeMillis();
            }
        }
    }

    /**
     * Parses the given stream via the default (sensible)
     * DocumentBuilder
     * @param inp Stream to read the XML data from
     * @return the parsed Document 
     */
    public static Document readDocument(XmlOptionsBean xmlOptions, InputStream inp) throws IOException, SAXException {
        return newDocumentBuilder(xmlOptions).parse(inp);
    }

    /**
     * Parses the given stream via the default (sensible)
     * DocumentBuilder
     * @param inp sax source to read the XML data from
     * @return the parsed Document 
     */
    public static Document readDocument(XmlOptionsBean xmlOptions, InputSource inp) throws IOException, SAXException {
        return newDocumentBuilder(xmlOptions).parse(inp);
    }

    // must only be used to create empty documents, do not use it for parsing!
    private static final DocumentBuilder documentBuilderSingleton = newDocumentBuilder(new XmlOptionsBean());

    /**
     * Creates a new DOM Document
     */
    public static Document createDocument() {
        return documentBuilderSingleton.newDocument();
    }
}
