package org.skyve.util;

import java.io.Serializable;

import org.skyve.content.MimeType;

/**
 * MailAttachment
 * 
 * @author RB
 * 
 * attachmentFileName: the name of the attachment as it will appear
 * attachment: the byte array 
 * attachmentMimeType: the mimetype for the attachment
 */
public class MailAttachment implements Serializable {
	private static final long serialVersionUID = 8103370634731869625L;

	private String attachmentFileName;
	private byte[] attachment;
	private MimeType attachmentMimeType;

	/**
	 * Default constructor
	 */
	public MailAttachment() {
		// nothing to see here
	}

	/**
	 * Simple constructor
	 * 
	 * @param attachmentFileName
	 * @param attachment
	 * @param attachmentMimeType
	 */
	public MailAttachment(String attachmentFileName, byte[] attachment, MimeType attachmentMimeType) {
		this.attachmentFileName = attachmentFileName;
		this.attachment = attachment;
		this.attachmentMimeType = attachmentMimeType;
	}
	
	public String getAttachmentFileName() {
		return attachmentFileName;
	}

	public void setAttachmentFileName(String attachmentFileName) {
		this.attachmentFileName = attachmentFileName;
	}

	public byte[] getAttachment() {
		return attachment;
	}

	public void setAttachment(byte[] attachment) {
		this.attachment = attachment;
	}

	public MimeType getAttachmentMimeType() {
		return attachmentMimeType;
	}

	public void setAttachmentMimeType(MimeType attachmentMimeType) {
		this.attachmentMimeType = attachmentMimeType;
	}
}
