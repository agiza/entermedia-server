package org.openedit.data.lucene;

import java.util.Collection;

import org.apache.lucene.index.IndexWriter;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.XmlFileSearcher;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class LuceneTransientSearcher extends XmlFileSearcher
{
	public Data createNewData()
	{
		return new BaseData();
	}
	
	public void saveAllData(Collection inAll, User inUser)
	{
		updateIndex(inAll);
	}

	protected void reIndexAll(IndexWriter inWriter)
	{

	}
	public void saveData(Data data, User inUser)
	{
		if(data.getId() == null)
		{
			data.setId(nextId());
		}
		if( data.getSourcePath() == null)
		{
			String sourcepath = getSourcePathCreator().createSourcePath(data, data.getId() );
			data.setSourcePath(sourcepath);
		}
		updateIndex(data);
	}

	public Object searchByField(String inId, String inValue)
	{
		SearchQuery query = createSearchQuery();
		query.addExact(inId, inValue);
		HitTracker hits = search(query);
		hits.setHitsPerPage(1);
		Data first = (Data)hits.first();
		if( first == null)
		{
			return null;
		}
		Data baseData = createNewData();
		baseData.setProperties(first.getProperties());
		baseData.setId(first.getId());
		return baseData;
	}

	
}
