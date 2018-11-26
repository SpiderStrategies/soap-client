package com.spider.soapclient;

public class SoapClient {

	public static void main(String[] args) throws Exception {
		if(args.length < 4) {
			System.err.println("Usage: java -cp soap-client.jar url soapBodyFileLoc soapHeaderFileLoc soapVersion(1.1 or 1.2) soapAction username password");
			System.exit(0);
		}
		String url = args[0];
		String soapBody = Utils.readTextFromFile(args[1]);
		String soapHeader = Utils.readTextFromFile(args[2]);
		String soapVersion = "SOAP " + args[3] + " Protocol";
		String soapAction = getOptionalParam(args, 4);
		String username = getOptionalParam(args, 5);
		String password = getOptionalParam(args, 6);
		String output = SOAPSender.send(url, soapBody, soapHeader, soapAction, soapVersion, username, password);
		System.out.println(output);
	}

	private static String getOptionalParam(String[] args, int index) {
		if(args.length > index) {
			return args[index];
		}
		return null;
	}
	
}
