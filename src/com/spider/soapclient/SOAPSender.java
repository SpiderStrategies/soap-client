package com.spider.soapclient;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.List;

import java.net.URL;
import java.net.HttpURLConnection;
import sun.net.www.protocol.http.AuthCacheImpl;
import sun.net.www.protocol.http.AuthCacheValue;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.composite.util.ObjectUtils;

import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SOAPSender {
	
	static ThreadSafeAuthenticator tsa = new ThreadSafeAuthenticator();
	static {
		Authenticator.setDefault((ThreadSafeAuthenticator) tsa);
	}
	
	public static String getFormattedXml(String str) throws Exception{
		Format format = Format.getPrettyFormat();
		XMLOutputter outputter = new XMLOutputter(format);
		org.jdom2.Document doc = new SAXBuilder().build(new StringReader(str));
		StringWriter writer = new StringWriter();
		outputter.output(doc, writer);
		str = writer.toString();
		writer.close();
		return str;
	}

	public static String send(String url, String soapBody, String soapHeader, String soapAction, String soapVersion, String username, String password) throws Exception {	
		MessageFactory messageFactory = MessageFactory.newInstance(soapVersion);
		SOAPMessage message = messageFactory.createMessage();

		//Construct the body
		List<SOAPElement> soapBodyElementList = buildSOAPElementListFromString(soapBody);
		for(SOAPElement soapElement: soapBodyElementList) {
			message.getSOAPBody().addChildElement(soapElement);
		}

		//Construct the header
		if(!ObjectUtils.isEmpty(soapHeader)) {
			List<SOAPElement> soapHeaderElementList = buildSOAPElementListFromString(soapHeader);
			for(SOAPElement soapElement: soapHeaderElementList) {
				message.getSOAPHeader().addChildElement(soapElement);
			}
		}
		
		//Add a SOAPAction
		if(!ObjectUtils.isEmpty(soapAction)) {
			message.getMimeHeaders().setHeader("SOAPAction", soapAction);
		}
		
		//For NTLM auth (i.e. hitting SharePoint for list content)
		if(!ObjectUtils.isEmpty(username)) {
			tsa.setThreadUsername(username);
			tsa.setThreadPassword(password);
		}
		
		//Send the message
		SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection connection = soapConnectionFactory.createConnection();
		URL u = new URL(url);
		SOAPMessage responseMessage = connection.call(message, u);
		connection.close();
		
		String responseStr = buildStringFromChildrenOfNode(responseMessage.getSOAPBody());
		String formattedResponse = getFormattedXml(responseStr);
		
		//Ensure that the connection is closed (required for proper termination of keep-alive from server requiring NTML auth)
		HttpURLConnection huc = (HttpURLConnection) u.openConnection();
		huc.addRequestProperty("Connection", "close");
		huc.connect();
		
		//clear the AuthCache - https://stackoverflow.com/questions/480895/reset-the-authenticator-credentials
		AuthCacheValue.setAuthCache(new AuthCacheImpl());
		
		return formattedResponse;
	}
	
	/**
	 * Creates a List of SOAPElements based on the passed in XML string.
	 * 
	 * @param soapElementStr
	 *	  a String representation of one or more soap elements, usually a soap header
	 * @return
	 * 		a List of SOAPElements representation of the passed in String
	 * @throws
	 * 		an exception of an error happens
	 * 		   
	 */
	private static List<SOAPElement> buildSOAPElementListFromString(String soapElementStr) throws Exception {
		List<SOAPElement> soapElementList = new ArrayList<SOAPElement>();
		NodeList nodeList = buildNodeListFromXmlString(soapElementStr);
		for(int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if(node instanceof org.w3c.dom.Element) {
				//ignore any Text(whitespace) elements because those aren't real elements
				soapElementList.add(SOAPFactory.newInstance().createElement(((org.w3c.dom.Element) nodeList.item(i))));
			}
			
		}
		return soapElementList;
	}
	
	/**
	 * Creates a NodeList representation of the passed in XML string.
	 * 
	 * @param soapElementStr
	 *	  a String representation of a soap element, usually a soap body
	 * @return
	 * 		all children of the element wrapper as a NodeList
	 * @throws
	 * 		an exception of an error happens
	 * 		   
	 */
	private static NodeList buildNodeListFromXmlString(String soapElementStr) throws Exception {
		DOMResult domResult = new DOMResult();
		//if there are multiple root-level elements in a string, then the transformer errors, so wrap the entire XML in another element
		//just to be safe, and then return the child(ren) of that element in a NodeList
		soapElementStr = "<elementWrapper>" + soapElementStr + "</elementWrapper>";
		TransformerFactory.newInstance().newTransformer().transform(new StreamSource(new StringReader(soapElementStr)), domResult);
		return domResult.getNode().getFirstChild().getChildNodes();
	}
	
	/**
	 * 
	 * @param parentNode
	 *	  an an org.w3c.dom.Node whose children we want to make a big string out of
	 * @return
	 * 		the String representation of the Node
	 * @throws
	 * 		an exception of an error happens
	 * 		   
	 */
	private static String buildStringFromChildrenOfNode(Node parentNode) throws Exception {
		StringBuffer sb = new StringBuffer();
		NodeList nodeList =  parentNode.getChildNodes();
		for(int i = 0; i < nodeList.getLength(); i++) {
			Node childNode = nodeList.item(i);
			if(childNode instanceof org.w3c.dom.Element) {
				//we only care about elements, not text nodes or CDATA or whatever
				Writer outWriter = new StringWriter();
				StreamResult responseOutput = new StreamResult(outWriter);
				//output a string representation of the node to a StringWriter
				TransformerFactory.newInstance().newTransformer().transform(new DOMSource(childNode), responseOutput);
				//add this particular child element, formatted as a string, to the StringBuffer 
				//that holds the string representations of ALL child elements of parentNode
				sb.append(outWriter.toString());
			}
		}
		//return a concatenated string of all child elements of parentNode
		return sb.toString();
	}

}
