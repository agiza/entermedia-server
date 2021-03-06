package org.entermedia.email;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.repository.ContentItem;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.PathProcessor;

public class EmailSearcher extends BaseLuceneSearcher implements Searcher {

	protected XmlEmailArchive fieldEmailArchive; 
	protected PageManager fieldPageManager;
	
	public PageManager getPageManager() {
		return fieldPageManager;
	}
	public void setPageManager(PageManager pageManager) {
		fieldPageManager = pageManager;
	}
	public XmlEmailArchive getEmailArchive() {
		
		return fieldEmailArchive;
	}
	public void setEmailArchive(XmlEmailArchive inEmailArchive) {
		fieldEmailArchive = inEmailArchive;
		inEmailArchive.setCatalogId(getCatalogId());
		
	}
	
	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
		getEmailArchive().setCatalogId(getCatalogId());
		
	}
	
	private static final Log log = LogFactory.getLog(EmailSearcher.class);
	
	public void reIndexAll(final IndexWriter writer, final TaxonomyWriter inTaxonomyWriter)
	{
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html?page=2
		// writer.mergeFactor = 10;
		
		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				
				String sourcepath = inContent.getPath();
				String rootstring = getEmailArchive().getEmailPath();
				sourcepath = sourcepath.substring(rootstring.length(),
						sourcepath.length());
				sourcepath = sourcepath.replace(".xml", "");
				
				TemplateWebEmail email = getEmailArchive().loadEmail(sourcepath);
				populateEmail(writer, inTaxonomyWriter, email, true);
					
				
			}
		};
		processor.setRecursive(true);
		processor.setRootPath(getEmailArchive().getEmailPath());
		processor.setPageManager(getEmailArchive().getPageManager());
		processor.setIncludeExtensions("xml");
		processor.process();
		
		
		
	
		// HitCollector
		log.info("Reindex done");

	}
	private void populateEmail(IndexWriter inWriter, TaxonomyWriter inTaxonomyWriter, TemplateWebEmail inEmail, boolean add)  {
		
		super.updateIndex(inWriter,  inTaxonomyWriter,inEmail);
		
//		try {
//			if (add) {
//				inWriter.addDocument(doc, getAnalyzer());
//			} else {
//				Term term = new Term("id", inEmail.getId().toLowerCase());
//				inWriter.updateDocument(term, doc, getAnalyzer());
//			}
//			log.debug("Indexed " + inEmail.getId());
//		} catch (IOException ex) {
//			throw new OpenEditRuntimeException(ex);
//		}
		
	}
	
	
	public  void saveData(Data inData, User inUser){
		if(inData instanceof TemplateWebEmail){
			getEmailArchive().saveEmail((TemplateWebEmail) inData, inUser);
		}
		updateIndex(inData);
	}
	
	
	public HitTracker getAllHits(WebPageRequest inReq) {
		return fieldSearch("id", "*");
	
	}
	
	
	public Object searchById(String inId) {
		Data hit =(Data) super.searchById(inId);
		String sourcepath = hit.getSourcePath();
		return getEmailArchive().loadEmail(sourcepath);
	}
	
	
}
