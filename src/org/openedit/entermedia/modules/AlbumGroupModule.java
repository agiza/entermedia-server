package org.openedit.entermedia.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.albums.Album;
import org.openedit.entermedia.friends.FriendManager;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.users.User;


public class AlbumGroupModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(AlbumGroupModule.class);
	protected FriendManager getFriendManager(WebPageRequest inReq)
	{
		return getEnterMedia(inReq).getFriendManager();
	}
	public HitTracker getGroups(WebPageRequest inReq)
	{
		inReq.getUser().getGroups();
		
		HitTracker hits = getFriendManager(inReq).getFriends(getOwnerId(inReq));
		inReq.putPageValue("friends", hits);
		return hits;
	}
	
	public Data getFriend(WebPageRequest inReq)
	{
		String targetid = inReq.getRequestParameter("targetid");
		if(targetid == null)
		{
			//use current logged in user
			targetid = inReq.getUserName();
		}
		Data friend = null;
		if(targetid != null)
		{
			friend = getFriendManager(inReq).getFriendByFriendId(targetid);
		}
		return friend;
	}
		
	public boolean isFriend(WebPageRequest inReq)
	{
		String targetid = inReq.getRequestParameter("targetid");
		if(targetid == null)
		{
			//use current logged in user
			targetid = inReq.getUserName();
		}
		String ownerid = getOwnerId(inReq);
		boolean result = getFriendManager(inReq).isFriend(ownerid, targetid);
		inReq.putPageValue("isfriend", result);
		return result;
	}
	
	public void addFriend(WebPageRequest inReq)
	{
//		addFriend(inReq, inReq.getUserName(), false);
		//check for a duplicate
		User user = getFriendTarget(inReq);
		if(user == null)
		{
			return;
		}
		getEnterMedia(inReq).getFriendManager().makeFriends(inReq.getUserName(), user.getUserName(), inReq.getUser());
		inReq.putPageValue("target", user);

	}
	
	public void removeFriend(WebPageRequest inReq)
	{
		//need to get the data object of the subscriber to remove
		String targetid = inReq.getRequestParameter("targetid");
		getFriendManager(inReq).breakFriends(inReq.getUserName(), targetid, inReq.getUser());
		inReq.putPageValue("target", getUserManager().getUser(targetid));
	}
	
	public void sentInvites(WebPageRequest inReq)
	{
		String addresses = inReq.getRequestParameter("email");
		if(addresses == null)
		{
			String targetid = inReq.getRequestParameter("targetid");
			User targetUser = getUserManager().getUser(targetid);
			addresses = targetUser.getEmail();
		}
		try
		{
			getFriendManager(inReq).invite(inReq.getUser(), addresses, inReq);
		}
		catch ( Exception ex)
		{
			inReq.putPageValue("emailerror", ex);
			log.error( ex);
		}
	}
	//This is fired from an event
	public void sendAlbumCommentNotification(WebPageRequest inReq)
	{
		String commentpath = inReq.getRequestParameter("commentpath");
		Page path = getPageManager().getPage(commentpath);
		Album album = getEnterMedia(inReq).getAlbumArchive().loadAlbumInPath(path);
		getFriendManager(inReq).sendAlbumCommentNotification(album,inReq);
	}
	public void sendAssetCommentNotification(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.getRequestParameter("catalogid");
		String sourcepath = inReq.getRequestParameter("sourcepath");
		
		Asset asset = getEnterMedia(inReq).getAssetBySourcePath(catalogid, sourcepath);
		getFriendManager(inReq).sendAssetCommentNotification(asset,inReq);
	}
	
	public void toggleCommentNotification(WebPageRequest inReq)
	{
		String idtotoggle = inReq.getRequestParameter("idtotoggle");
		if(idtotoggle != null)
		{
			getFriendManager(inReq).toggleCommentNotification(idtotoggle, inReq.getUser());
		}
	}

	//	public void sendAssetsAddedToAlbumNotification(WebPageRequest inReq)
//	{
//		//inReq.putPageValue("comment", inReq.getRequestParameter("comment"));
//		getFriendManager(inReq).notifyFriendsOfComment(inReq);
//	}
	
	protected String getOwnerId(WebPageRequest inReq)
	{
		User user = (User)inReq.getPageValue("owner");
		String ownerid = null;
		if(user != null)
		{
			ownerid = user.getId();
		}
		
		if(ownerid == null)
		{
			ownerid = inReq.getRequestParameter("ownerid");
		}
		
		if(ownerid == null)
		{
			ownerid = inReq.getUser().getId();
		}
		return ownerid;
	}
	
	protected User getFriendTarget(WebPageRequest inReq)
	{
		String targetid = inReq.getRequestParameter("targetid");
		User targetuser = null;
		if(targetid != null)
		{
			targetuser = getUserManager().getUser(targetid);
		}
		return targetuser;
	}
	
	public void checkInvitation(WebPageRequest inReq)
	{
		String inviteid = inReq.getRequestParameter("invitationid");
		boolean accepted = false;
		if( inviteid != null)
		{
			String page = getOwnerId(inReq);
			accepted = getFriendManager(inReq).acceptInvitation(page, inviteid, inReq.getUser());
		}
		inReq.putPageValue("autofriend", accepted);
	}

	public void listInvitations(WebPageRequest inReq)
	{
		HitTracker hits = getFriendManager(inReq).getOpenInvitations(inReq.getUserName());
		inReq.putPageValue("invitations", hits);
	}

}