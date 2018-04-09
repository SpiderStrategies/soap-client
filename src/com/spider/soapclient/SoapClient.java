package com.spider.soapclient;

public class SoapClient {

	public static void main(String[] args) throws Exception {
		if(args.length < 7) {
			System.err.println("Usage: java SoapClient url soapBodyFileLoc soapHeaderFileLoc soapVersion(1.1 or 1.2) soapAction username password");
			System.exit(0);
		}
		String url = args[0];
		String soapBody = Utils.readTextFromFile(args[1]);
		String soapHeader = Utils.readTextFromFile(args[2]);
		String soapVersion = "SOAP " + args[3] + " Protocol";
		String soapAction = args[4];
		String username = args[5];
		String password = args[6];
		String output = SOAPSender.send(url, soapBody, soapHeader, soapAction, soapVersion, username, password);
		System.out.println(output);
	}
	
}
