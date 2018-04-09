package com.spider.soapclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Utils {
	
	public static String readTextFromFile(String absolutePath) {
		BufferedReader br = null;
		try {
			File file = new File(absolutePath);
			br = new BufferedReader(new FileReader(file));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while(line != null) {
				sb.append(line);
				sb.append(" \n");
				line = br.readLine();
			}
			return sb.toString();
		} catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				br.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
