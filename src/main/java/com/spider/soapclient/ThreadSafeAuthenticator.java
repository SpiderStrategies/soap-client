package com.spider.soapclient;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class ThreadSafeAuthenticator extends Authenticator {
	
	private static ThreadLocal<String> username = new ThreadLocal<String>();
	private static ThreadLocal<String> password = new ThreadLocal<String>();

	public void setThreadUsername(String user) {
		username.set(user);
	}
	
	public void setThreadPassword(String pwd) {
		password.set(pwd);
		}

	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication (username.get(), password.get().toCharArray());
	}
	
}
