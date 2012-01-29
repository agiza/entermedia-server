package publishing;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*
import org.openedit.event.*

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page


public void init() {

	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos

	Searcher queuesearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "publishqueue");


	SearchQuery query = queuesearcher.createSearchQuery();
	WebEvent webevent = context.getPageValue("webevent");
	Asset asset = null;
	if( webevent != null)
	{
		String sourcepath = webevent.getSourcePath();
		asset = mediaArchive.getAssetBySourcePath(sourcepath);
		if( asset != null)
		{
			query.addExact("assetid",asset.getId());
		}
	}
	String assetid = context.getRequestParameter("assetid");
	if(assetid != null){
		query.addExact("assetid", assetid);
	}
	query.addOrsGroup("status","new transcoding pending retry");
	HitTracker tracker = queuesearcher.search(query);
	log.info("publishing " + tracker.size() + " assets");
	if( tracker.size() > 0)
	{
		for( Data result:tracker)
		{
			Data publishrequest = queuesearcher.searchById(result.getId());
			if( publishrequest == null)
			{
				log.error("Publish queue index out of date");
				continue;
			}
			String presetid = publishrequest.get("presetid");

			if(asset == null)
			{
				assetid = result.get("assetid");
				asset = mediaArchive.getAsset(assetid);
			}

			String publishdestination = publishrequest.get("publishdestination");
			Data preset = mediaArchive.getSearcherManager().getData(mediaArchive.getCatalogId(), "convertpreset", presetid);
			Data destination = mediaArchive.getSearcherManager().getData(mediaArchive.getCatalogId(), "publishdestination",publishdestination);

			try
			{

				Page inputpage = null;
				if( preset.get("type") != "original")
				{
					String input= "/WEB-INF/data/${mediaArchive.catalogId}/generated/${asset.sourcepath}/${preset.outputfile}";
					inputpage= mediaArchive.getPageManager().getPage(input);
				}
				else
				{
					inputpage = mediaArchive.getOriginalDocument(asset);
				}

				if(!inputpage.exists() || inputpage.length() == 0)
				{
					log.info("Input file ${inputpage.getName()} did not exist. Skipping publishing.");
					if( !"transcoding".equals( publishrequest.get("status") ) )
					{
						publishrequest.setProperty('status', 'transcoding');
						queuesearcher.saveData(publishrequest, context.getUser());
						//TODO: fire log event for transcoding
					}
					continue;
				}

				
				String existingstatus = publishrequest.get("status");				
				if ( existingstatus == null || "new".equals( existingstatus ) || "transcoding".equals( existingstatus ) )
				{
					firePublishEvent(publishrequest.getId(), "publishing/publishstart");
				}
				
				Publisher publisher = getPublisher(mediaArchive, destination.get("publishtype"));
				PublishResult presult = null
				boolean isPublishError = false
				try{
					presult = publisher.publish(mediaArchive,asset,publishrequest, destination,preset);
				}catch(Exception e){
					isPublishError = true
				}
				if( isPublishError || presult.isError() )
				{
					publishrequest.setProperty('status', 'error');
					publishrequest.setProperty("errordetails", presult?.getErrorMessage());
					queuesearcher.saveData(publishrequest, context.getUser());
					firePublishEvent(publishrequest.getId(), "publishing/publisherror");
					continue;
				}
				else if( presult.isComplete() )
				{
					log.info("Published " +  asset + " to " + destination);
					publishrequest.setProperty('status', 'complete');
					publishrequest.setProperty("errordetails", " ");
					queuesearcher.saveData(publishrequest, context.getUser());
					firePublishEvent(publishrequest.getId(), "publishing/publishcomplete");
					
				}
				else if( presult.isPending() ) //smartjog is working on it
				{
					publishrequest.setProperty('status', 'pending');
					publishrequest.setProperty("errordetails", " ");
					queuesearcher.saveData(publishrequest, context.getUser());
				}
				else  //smartjog was not ready to publish yet
				{
					publishrequest.setProperty('status', 'retry');
					publishrequest.setProperty("errordetails", " ");
					queuesearcher.saveData(publishrequest, context.getUser());
				}
			}
			catch( Throwable ex)
			{
				log.error("Problem publishing ${asset} to ${publishdestination}", ex);
				publishrequest.setProperty('status', 'exportrequested');
//				publishrequest.setProperty('status', 'error');
				if(ex.getCause() != null)
				{
					ex = ex.getCause();
				}
				publishrequest.setProperty("errordetails", "${destination} publish failed ${ex}");
				queuesearcher.saveData(publishrequest, context.getUser());
			}
		}

	}
}
protected firePublishEvent(String inOrderItemId, String operation)
{
	WebEvent event = new WebEvent();
	event.setSearchType("publisheventLog");
	event.setProperty("publishqueueid", inOrderItemId);
	event.setOperation( operation );
	event.setUser(context.getUser());
	event.setCatalogId(mediaarchive.getCatalogId());
	mediaarchive.getMediaEventHandler().eventFired(event);
}



protected Publisher getPublisher(MediaArchive inArchive, String inType)
{
	GroovyClassLoader loader = engine.getGroovyClassLoader();
	Class groovyClass = loader.loadClass("publishing.publishers.${inType}publisher");
	Publisher publisher = (Publisher) groovyClass.newInstance();
	return publisher;
}

init();