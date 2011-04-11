/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package com.globalros.tftp.common;

public class TFTPFile {
	/**
	 * Constructor TFTPFile.
	 * @param filename
	 * @param data
	 */
	public TFTPFile(String filename, Object data) {
	}

	private String name = "";
	private byte[] content = null;

	public String getName() {
		return name;
	}

	synchronized public void setName(String name) {
		this.name = name;
	}

	public Object getContent() {
		return content;
	}
	
	synchronized public void setContent(Object content) {
		if (content instanceof byte[]) {
		  this.content = (byte[]) content;
		}
	}
}
