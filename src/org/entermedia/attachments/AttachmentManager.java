package org.entermedia.attachments;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.data.XmlFileSearcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.scanner.MetaDataReader;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;

public class AttachmentManager
{
	private static final Log	log = LogFactory.getLog(AttachmentManager.class);
	protected SearcherManager fieldSearcherManager;
	protected PageManager fieldPageManager;
	protected MetaDataReader fieldMetaDataReader;

	public MetaDataReader getMetaDataReader()
	{
		return fieldMetaDataReader;
	}

	public void setMetaDataReader(MetaDataReader inMetaDataReader)
	{
		fieldMetaDataReader = inMetaDataReader;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public void processAttachments(MediaArchive inArchive, Asset inAsset, boolean reprocess)
	{
		// HitTracker hittracker = new ListHitTracker();
		String inCatalogId = inArchive.getCatalogId();
		Searcher attachmentSearcher = getAttachmentSearcher(inCatalogId);

		String root = "/WEB-INF/data/" + inCatalogId + "/originals/";

		syncFolder(attachmentSearcher, inArchive, inAsset.getId(), root, inAsset.getSourcePath(), inAsset.getSourcePath(), reprocess);
	}

	public HitTracker listChildren(WebPageRequest inReq, MediaArchive inArchive, String inFolderSourcePath)
	{
		String root = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/";
		Searcher attachmentSearcher = getAttachmentSearcher(inArchive.getCatalogId());
		SearchQuery query = attachmentSearcher.createSearchQuery();
		query.addExact("parentsourcepath", inFolderSourcePath);
		HitTracker hits = attachmentSearcher.cachedSearch(inReq, query);
		return hits;
	}

	protected void syncFolder(Searcher attachmentSearcher, MediaArchive inArchive, String inAssetId, String inRootFolder, String inFolderSourcePath, String inAssetSourcePath, boolean inReprocess)
	{
		SearchQuery query = attachmentSearcher.createSearchQuery();
		query.addExact("assetid", inAssetId);
		query.addExact("parentsourcepath", inFolderSourcePath);

		//List known data children for this one folder
		HitTracker hits = attachmentSearcher.search(query);

		if (inReprocess || hits.size() == 0) //Could not find any existing children, add a child
		{
			Map alreadyhave = new HashMap();
			if (inReprocess)
			{
				for (Iterator iterator2 = hits.iterator(); iterator2.hasNext();)
				{
					Data data = (Data) iterator2.next();
					alreadyhave.put(data.get("name"), data);
				}
			}
			//see what files are already there
			List files = getPageManager().getChildrenPaths(inRootFolder + inFolderSourcePath);
			for (Iterator iterator = files.iterator(); iterator.hasNext();)
			{
				String file = (String) iterator.next();
				Page page = getPageManager().getPage(file);
				if( page.getName().startsWith(".") )
				{
					continue;
				}

				
				if (!alreadyhave.containsKey(page.getName()))
				{
					// need a new entry
					Data attachment = attachmentSearcher.createNewData();
					attachment.setSourcePath(inAssetSourcePath);
					attachment.setProperty("name", page.getName());
					attachment.setProperty("assetid", inAssetId);
					attachment.setProperty("folder", String.valueOf(page.isFolder()));
					attachment.setProperty("parentsourcepath", inFolderSourcePath);

					Asset asset = new Asset();
					getMetaDataReader().populateAsset(inArchive, page.getContentItem(), asset);
					for (Iterator iterator2 = asset.getProperties().keySet().iterator(); iterator2.hasNext();)
					{
						String key = (String) iterator2.next();
						String value = asset.get(key);
						attachment.setProperty(key, value);
					}
					attachmentSearcher.saveData(attachment, null);
				}
				else
				{
					alreadyhave.remove(page.getName());
				}
				if (page.isFolder())
				{
					syncFolder(attachmentSearcher, inArchive, inAssetId, inRootFolder, inFolderSourcePath + "/" + page.getName(), inAssetSourcePath, inReprocess);
				}
			}
			//delete old records
			for (Iterator iterextra = alreadyhave.values().iterator(); iterextra.hasNext();)
			{
				Data data = (Data) iterextra.next();
				attachmentSearcher.delete(data, null);				
			}
		}
	}

	private void deleteEntries(Asset inAsset, Searcher inAttachmentSearcher)
	{
		HitTracker hits = inAttachmentSearcher.fieldSearch("assetid", inAsset.getId());
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			inAttachmentSearcher.delete(hit, null);
		}
	}

	public Searcher getAttachmentSearcher(String inCatalogId)
	{
		Searcher attachmentSearcher = getSearcherManager().getSearcher(inCatalogId, "attachment");
		return attachmentSearcher;
	}

	public void syncAttachments(WebPageRequest inReq, MediaArchive inArchive, Asset inAsset, boolean inReload)
	{
		if (inReload)
		{
			processAttachments(inArchive, inAsset, inReload);
		}
		else
		{
			HitTracker hits = listChildren(inReq, inArchive, inAsset.getSourcePath());
			if (hits.size() == 0)
			{
				processAttachments(inArchive, inAsset, inReload);
			}
		}
		//log.info("Found " + summary.getCount() + " attachments for asset " + inAsset.getId() );
	}

	public void createFolder(WebPageRequest inReq, MediaArchive inArchive, Asset inAsset, String inParentid, String inName)
	{
		Searcher attachmentSearcher = getAttachmentSearcher(inArchive.getCatalogId());
		String sourcepath = null;
		if (inParentid != null)
		{
			Data parent = (Data) attachmentSearcher.searchById(inParentid);
			if( parent == null)
			{
				sourcepath = inAsset.getSourcePath();
			}
			else
			{
				sourcepath = parent.get("parentsourcepath") + "/" + parent.getName();
			}
		}
		else
		{
			sourcepath = inAsset.getSourcePath();
		}
		// parentsourcepath
		String root = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/";
		String parentsourcepath = root + sourcepath + "/" + inName;
		Page folder = getPageManager().getPage( parentsourcepath + "/");
		getPageManager().putPage(folder);

		//syncFolder(attachmentSearcher, inArchive, inAsset.getId(), root, inAsset.getSourcePath(), inAsset.getSourcePath(), true);
		processAttachments(inArchive,inAsset,true);
	}

	public void delete(WebPageRequest inReq, MediaArchive inArchive, Asset inAsset, String inFileid)
	{
		Searcher attachmentSearcher = getAttachmentSearcher(inArchive.getCatalogId());
		Data file = (Data) attachmentSearcher.searchById(inFileid);

		String root = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/";
		String sourcepath = file.get("parentsourcepath") + "/" + file.getName();

		Page page = getPageManager().getPage(root + sourcepath);
		getPageManager().removePage(page);

		attachmentSearcher.delete(file, inReq.getUser());
	}

	public void renameFilder(WebPageRequest inReq, MediaArchive inArchive, Asset inAsset, String inFolderId, String inNewName)
	{
		Searcher attachmentSearcher = getAttachmentSearcher(inArchive.getCatalogId());
		String sourcepath = null;
		if (inFolderId == null)
		{
			return;
		}
		Data parent = (Data) attachmentSearcher.searchById(inFolderId);
		sourcepath = parent.get("parentsourcepath") + "/" + parent.getName();

		String root = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/";
		Page oldfolder = getPageManager().getPage(root + sourcepath + "/");
		if (oldfolder.exists())
		{
			String parentpath = oldfolder.getParentPath();
			parentpath = parentpath.substring(0, parentpath.lastIndexOf("/"));
			Page newfolder = getPageManager().getPage(parentpath + "/" + inNewName);

			getPageManager().movePage(oldfolder, newfolder);
		}
	}

	public void clearAttachmentData(WebPageRequest inReq, MediaArchive inArchive, Asset inAsset, boolean inB)
	{
		Searcher attachmentSearcher = (Searcher) getAttachmentSearcher(inArchive.getCatalogId());

		SearchQuery query = attachmentSearcher.createSearchQuery();
		query.addMatches("assetid", inAsset.getId());
		//query.addMatches("parentsourcepath", inAsset.getSourcePath());
		HitTracker hits = attachmentSearcher.search(query);
		if (hits.size() > 0)
		{
			for (Iterator iterator2 = hits.iterator(); iterator2.hasNext();)
			{
				Data data = (Data) iterator2.next();
				attachmentSearcher.delete(data, inReq.getUser());
			}
		}
	}
	public String parseFileSubPath(String inAssetSourcePath, String inParentSourcePath, String inFilename)
	{
		if( inParentSourcePath == null)
		{
			return null;
		}
		String folder = inParentSourcePath.substring(inAssetSourcePath.length());
		if( folder.length() > 0)
		{
			folder = folder + "/";
		}
		return folder + inFilename;
	}

}
