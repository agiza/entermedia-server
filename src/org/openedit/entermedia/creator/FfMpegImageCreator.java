/*
 * Created on Sep 20, 2005
 */
package org.openedit.entermedia.creator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.page.Page;

public class FfMpegImageCreator extends BaseImageCreator
{
	private static final Log log = LogFactory.getLog(FfMpegImageCreator.class);
	protected String fieldCommandName = "ffmpeg"; // ffmpeg -itsoffset 10

	// -deinterlace -i $TRACK -y
	// -vframes 1 -f mjpeg
	// $OUTPUT


	public String getCommandName()
	{
		return fieldCommandName;
	}

	public void setCommandName(String inCommandName)
	{
		fieldCommandName = inCommandName;
	}

	public boolean canReadIn(MediaArchive inArchive, String inFileFormatInput)
	{
		//TODO: Add a bunch of video formats
		if( inFileFormatInput == null)
		{
			return false;
		}
		String lcfileformat = inFileFormatInput.toLowerCase();
		return "flv".equals(lcfileformat) || "avi".equals(lcfileformat) || (lcfileformat.startsWith("m") && !lcfileformat.equals("mp3"));
	}

	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOutFile, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		result.setOk(false);

		// We are going to take frames from the converted flv video
		ConvertInstructions ci = new ConvertInstructions();
		ci.setAssetSourcePath(inAsset.getSourcePath());
		ci.setOutputExtension("flv");
		inArchive.getCreatorManager().getMediaCreatorByOutputFormat("flv").populateOutputPath(inArchive, ci);
		Page input = getPageManager().getPage(ci.getOutputPath());
		
		// Or the original file, if the flv does not exist
		if( !input.exists() )
		{
			input = inArchive.findOriginalMediaByType("video",inAsset);
		}
		if( input == null || !input.exists())
		{
			//no such original
			return result;
		}

		String offset = inStructions.getProperty("timeoffset");
		if( offset == null)
		{
			offset = "2";
		}
		try
		{
			offset = String.valueOf(Integer.parseInt(offset));
		}
		catch( Exception e )
		{
			offset = "0";
		}
		
		List<String> com = new ArrayList<String>();

		// These two MUST be the first two arguments. See below.
		com.add("-ss");
		com.add(offset);
		com.add("-deinterlace");
		com.add("-i");
		com.add(input.getContentItem().getAbsolutePath()); // TODO: Might need [0] to pick the
		// first image only
		com.add("-y");
		com.add("-vframes");
		com.add("1");
		com.add("-f");
		com.add("mjpeg");

		// -s 640x480
		// com.add("-s");
		// com.add( (int)inStructions.getMaxScaledSize().getWidth() + "x" +
		// (int)inStructions.getMaxScaledSize().getHeight() + ">" );
		
		String outputpath = inOutFile.getContentItem().getAbsolutePath();
		new File(outputpath).getParentFile().mkdirs();
		com.add(outputpath);
		result.setOutputPath(null);
		long start = System.currentTimeMillis();
		if (runExec(getCommandName(), com))
		{
			log.info("Resize complete in:" + (System.currentTimeMillis() - start) + " " + inOutFile.getName());
			result.setOk(true);
			result.setOutputPath(inOutFile.getContentItem().getAbsolutePath());
		}
		
		if(!inOutFile.exists() || inOutFile.length() == 0)
		{
			log.info("Thumnail creation failed " + result.getOutputPath());
		}

		return result;
	}
	
	public String createConvertPath(ConvertInstructions inStructions)
	{
		String frame = inStructions.getProperty("frame");
		if( frame == null )
		{
			frame="0";
		}
		String path = inStructions.getAssetSourcePath() + "frame" + frame + ".jpg";
		return path;
	}

	
}
