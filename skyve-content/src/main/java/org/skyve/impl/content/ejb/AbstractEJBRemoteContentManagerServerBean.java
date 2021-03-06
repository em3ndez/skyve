package org.skyve.impl.content.ejb;

import org.skyve.EXT;
import org.skyve.content.AttachmentContent;
import org.skyve.content.BeanContent;
import org.skyve.content.ContentManager;
import org.skyve.content.SearchResults;

/**
 * Extend this to make a stateless session bean in the skyve server instance.
 * <p/>
 * <code>
 * <pre>
 *	@Stateless
 *	public class EJBRemoteContentManagerServerBean extends org.skyve.impl.content.ejb.AbstractEJBRemoteContentManagerServerBean {
 *		// nothing to do here
 *	}
 * </pre>
 * </code>
 * 
 * @author mike
 */
public abstract class AbstractEJBRemoteContentManagerServerBean implements EJBRemoteContentManagerServer {
	@Override
	public void put(BeanContent content) throws Exception {
		try (ContentManager cm = EXT.newContentManager()) {
			cm.put(content);
		}
	}
	
	@Override
	public String put(AttachmentContent content, boolean index) throws Exception {
		try (ContentManager cm = EXT.newContentManager()) {
			cm.put(content, index);
			return content.getContentId();
		}
	}

	@Override
	public AttachmentContent getAttachment(String contentId) throws Exception {
		try (ContentManager cm = EXT.newContentManager()) {
			return cm.getAttachment(contentId);
		}
	}

	@Override
	public void removeBean(String bizId) throws Exception {
		try (ContentManager cm = EXT.newContentManager()) {
			cm.removeBean(bizId);
		}
	}

	@Override
	public void removeAttachment(String contentId) throws Exception {
		try (ContentManager cm = EXT.newContentManager()) {
			cm.removeAttachment(contentId);
		}
	}
	
	@Override
	public SearchResults google(String search, int maxResults) throws Exception {
		try (ContentManager cm = EXT.newContentManager()) {
			return cm.google(search, maxResults);
		}
	}
}
