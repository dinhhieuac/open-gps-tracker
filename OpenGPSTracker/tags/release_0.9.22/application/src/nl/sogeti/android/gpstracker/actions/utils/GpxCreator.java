/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Jan 21, 2010 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.actions.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.ShareTrack.ProgressMonitor;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.Constants;

import org.xmlpull.v1.XmlSerializer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

/**
 * Create a GPX version of a stored track
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class GpxCreator extends XmlCreator
{
   public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   public static final String NS_GPX_11 = "http://www.topografix.com/GPX/1/1";
   public static final String NS_GPX_10 = "http://www.topografix.com/GPX/1/0";
   public static final SimpleDateFormat ZULU_DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
   static
   {
      TimeZone utc = TimeZone.getTimeZone( "UTC" );
      ZULU_DATE_FORMAT.setTimeZone( utc ); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
   }

   private String TAG = "OGT.GpxCreator";
   private boolean includeAttachments;

   public GpxCreator(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments, ProgressMonitor listener)
   {
      super( context, trackUri, chosenBaseFileName, listener );
      includeAttachments = attachments;
   }

   public void run()
   {
      Looper.prepare();

      String xmlFilePath;
      if( fileName.endsWith( ".gpx" ) || fileName.endsWith( ".xml" ) )
      {
         setExportDirectoryPath( Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR + fileName.substring( 0, fileName.length() - 4 ) );

         xmlFilePath = getExportDirectoryPath() + "/" + fileName;
      }
      else
      {
         setExportDirectoryPath( Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR + fileName );
         xmlFilePath = getExportDirectoryPath() + "/" + fileName + ".gpx";
      }
      
      new File( getExportDirectoryPath() ).mkdirs();

      if( mProgressListener != null )
      {
         determineProgressGoal();
         mProgressListener.startNotification();
      }

      String resultFilename = null;
      FileOutputStream fos = null;
      BufferedOutputStream buf = null;
      try
      {
         verifySdCardAvailibility();
         
         XmlSerializer serializer = Xml.newSerializer();
         File xmlFile = new File( xmlFilePath );
         fos = new FileOutputStream( xmlFile );
         buf = new BufferedOutputStream( fos, 8192 );
         serializer.setOutput( buf, "UTF-8" );

         serializeTrack( mTrackUri, serializer );
         buf.close();
         buf = null;
         fos.close();
         fos = null;

         if( needsBundling() )
         {
            resultFilename = bundlingMediaAndXml( xmlFile.getParentFile().getName(), ".zip" );
         }
         else
         {
            File finalFile = new File( Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR + "/" + xmlFile.getName() );
            xmlFile.renameTo( finalFile );
            resultFilename = finalFile.getAbsolutePath();

            XmlCreator.deleteRecursive( xmlFile.getParentFile() );
         }

         fileName = new File( resultFilename ).getName();

         CharSequence text = mContext.getString( R.string.ticker_stored ) + " \"" + fileName + "\" ";
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch( FileNotFoundException e )
      {
         Log.e( TAG, "Unable to save ", e );
         CharSequence text = mContext.getString( R.string.ticker_failed ) + " \"" + xmlFilePath + "\" " + mContext.getString( R.string.error_filenotfound );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch( IllegalArgumentException e )
      {
         Log.e( TAG, "Unable to save ", e );
         CharSequence text = mContext.getString( R.string.ticker_failed ) + " \"" + xmlFilePath + "\" " + mContext.getString( R.string.error_filename );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch( IllegalStateException e )
      {
         Log.e( TAG, "Unable to save ", e );
         CharSequence text = mContext.getString( R.string.ticker_failed ) + " \"" + xmlFilePath + "\" " + mContext.getString( R.string.error_buildxml );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      catch( IOException e )
      {
         Log.e( TAG, "Unable to save ", e );
         CharSequence text = mContext.getString( R.string.ticker_failed ) + " \"" + xmlFilePath + "\" " + mContext.getString( R.string.error_writesdcard );
         Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
         toast.show();
      }
      finally
      {
         if( buf != null )
         {
            try
            {
               buf.close();
            }
            catch( IOException e )
            {
               Log.e( TAG, "Failed to close buf after completion, ignoring." , e );
            }
         }
         if( fos != null )
         {
            try
            {
               fos.close();
            }
            catch( IOException e )
            {
               Log.e( TAG, "Failed to close fos after completion, ignoring." , e );
            }
         }
         if( mProgressListener != null )
         {
            mProgressListener.endNotification( resultFilename, getContentType() );
         }
         Looper.loop();
      }
   }

   private void serializeTrack( Uri trackUri, XmlSerializer serializer ) throws IllegalArgumentException, IllegalStateException, IOException
   {
      serializer.startDocument( "UTF-8", true );
      serializer.setPrefix( "xsi", NS_SCHEMA );
      serializer.setPrefix( "gpx10", NS_GPX_10 );
      serializer.text( "\n" );
      serializer.startTag( "", "gpx" );
      serializer.attribute( null, "version", "1.1" );
      serializer.attribute( null, "creator", "nl.sogeti.android.gpstracker" );
      serializer.attribute( NS_SCHEMA, "schemaLocation", NS_GPX_11 + " http://www.topografix.com/gpx/1/1/gpx.xsd" );
      serializer.attribute( null, "xmlns", NS_GPX_11 );

      // Big header of the track
      String name = serializeTrackHeader( mContext, serializer, trackUri );

      serializer.text( "\n" );
      serializer.startTag( "", "trk" );
      serializer.text( "\n" );
      serializer.startTag( "", "name" );
      serializer.text( name );
      serializer.endTag( "", "name" );

      // The list of segments in the track
      serializeSegments( serializer, Uri.withAppendedPath( trackUri, "segments" ) );

      serializer.text( "\n" );
      serializer.endTag( "", "trk" );
      serializer.text( "\n" );
      serializer.endTag( "", "gpx" );
      serializer.endDocument();
   }

   private String serializeTrackHeader( Context context, XmlSerializer serializer, Uri trackUri ) throws IOException
   {
      ContentResolver resolver = context.getContentResolver();
      Cursor trackCursor = null;
      String name = null;

      try
      {
         trackCursor = resolver.query( trackUri, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, null );
         if( trackCursor.moveToFirst() )
         {
            name = trackCursor.getString( 1 );
            serializer.text( "\n" );
            serializer.startTag( "", "metadata" );
            serializer.text( "\n" );
            serializer.startTag( "", "time" );
            Date time = new Date( trackCursor.getLong( 2 ) );
            serializer.text( ZULU_DATE_FORMAT.format( time ) );
            serializer.endTag( "", "time" );
            serializer.text( "\n" );
            serializer.endTag( "", "metadata" );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
      return name;
   }

   private void serializeSegments( XmlSerializer serializer, Uri segments ) throws IOException
   {
      Cursor segmentCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         segmentCursor = resolver.query( segments, new String[] { Segments._ID }, null, null, null );
         if( segmentCursor.moveToFirst() )
         {
            do
            {
               Uri waypoints = Uri.withAppendedPath( segments, segmentCursor.getLong( 0 ) + "/waypoints" );
               serializer.text( "\n" );
               serializer.startTag( "", "trkseg" );
               serializeWaypoints( serializer, waypoints );
               serializer.text( "\n" );
               serializer.endTag( "", "trkseg" );
            }
            while( segmentCursor.moveToNext() );

         }
      }
      finally
      {
         if( segmentCursor != null )
         {
            segmentCursor.close();
         }
      }
   }

   private void serializeWaypoints( XmlSerializer serializer, Uri waypoints ) throws IOException
   {
      Cursor waypointsCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query( waypoints, new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.TIME, Waypoints.ALTITUDE, Waypoints._ID, Waypoints.SPEED }, null, null, null );
         if( waypointsCursor.moveToFirst() )
         {
            do
            {
               if( mProgressListener != null )
               {
                  mProgressListener.increaseProgress( 1 );
               }

               serializer.text( "\n" );
               serializer.startTag( "", "trkpt" );
               serializer.attribute( null, "lat", Double.toString( waypointsCursor.getDouble( 1 ) ) );
               serializer.attribute( null, "lon", Double.toString( waypointsCursor.getDouble( 0 ) ) );
               serializer.text( "\n" );
               serializer.startTag( "", "ele" );
               serializer.text( Double.toString( waypointsCursor.getDouble( 3 ) ) );
               serializer.endTag( "", "ele" );
               serializer.text( "\n" );
               serializer.startTag( "", "time" );
               Date time = new Date( waypointsCursor.getLong( 2 ) );
               serializer.text( ZULU_DATE_FORMAT.format( time ) );
               serializer.endTag( "", "time" );
               if( includeAttachments )
               {
                  serializeWaypointDescription( mContext, serializer, Uri.withAppendedPath( waypoints, waypointsCursor.getLong( 4 ) + "/media" ) );
               }
               serializer.text( "\n" );
               serializer.startTag( "", "extensions" );
               quickTag( serializer, NS_GPX_10, "speed", Double.toString( waypointsCursor.getDouble( 5 ) ) );

               serializer.endTag( "", "extensions" );
               serializer.text( "\n" );
               serializer.endTag( "", "trkpt" );
            }
            while( waypointsCursor.moveToNext() );
         }
      }
      finally
      {
         if( waypointsCursor != null )
         {
            waypointsCursor.close();
         }
      }

   }

   private void serializeWaypointDescription( Context context, XmlSerializer serializer, Uri media ) throws IOException
   {
      String mediaPathPrefix = Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.EXTERNAL_DIR;
      Cursor mediaCursor = null;
      ContentResolver resolver = context.getContentResolver();
      try
      {
         mediaCursor = resolver.query( media, new String[] { Media.URI }, null, null, null );
         if( mediaCursor.moveToFirst() )
         {
            do
            {
               Uri mediaUri = Uri.parse( mediaCursor.getString( 0 ) );
               if( mediaUri.getScheme().equals( "file" ) )
               {
                  if( mediaUri.getLastPathSegment().endsWith( "3gp" ) )
                  {
                     serializer.text( "\n" );
                     serializer.startTag( "", "link" );
                     serializer.attribute( null, "href", includeMediaFile( mediaUri.getLastPathSegment() ) );
                     serializer.startTag( "", "text" );
                     serializer.text( mediaUri.getLastPathSegment() );
                     serializer.endTag( "", "text" );
                     serializer.endTag( "", "link" );
                  }
                  else if( mediaUri.getLastPathSegment().endsWith( "jpg" ) )
                  {
                     serializer.text( "\n" );
                     serializer.startTag( "", "link" );
                     serializer.attribute( null, "href", includeMediaFile( mediaPathPrefix + mediaUri.getLastPathSegment() ) );
                     serializer.startTag( "", "text" );
                     serializer.text( mediaUri.getLastPathSegment() );
                     serializer.endTag( "", "text" );
                     serializer.endTag( "", "link" );
                  }
                  else if( mediaUri.getLastPathSegment().endsWith( "txt" ) )
                  {
                     serializer.text( "\n" );
                     serializer.startTag( "", "desc" );
                     BufferedReader buf = new BufferedReader( new FileReader( mediaUri.getEncodedPath() ) );
                     String line;
                     while( ( line = buf.readLine() ) != null )
                     {
                        serializer.text( line );
                        serializer.text( "\n" );
                     }
                     serializer.endTag( "", "desc" );
                  }
               }
               else if( mediaUri.getScheme().equals( "content" ) )
               {
                  if( mediaUri.getAuthority().equals( GPStracking.AUTHORITY + ".string" ) )
                  {
                     serializer.text( "\n" );
                     serializer.startTag( "", "name" );
                     serializer.text( mediaUri.getLastPathSegment() );
                     serializer.endTag( "", "name" );
                  }
                  else if( mediaUri.getAuthority().equals( "media" ) )
                  {

                     Cursor mediaItemCursor = null;
                     try
                     {
                        mediaItemCursor = resolver.query( mediaUri, new String[] { MediaColumns.DATA, MediaColumns.DISPLAY_NAME }, null, null, null );
                        if( mediaItemCursor.moveToFirst() )
                        {
                           serializer.text( "\n" );
                           serializer.startTag( "", "link" );
                           serializer.attribute( null, "href", includeMediaFile( mediaItemCursor.getString( 0 ) ) );
                           serializer.startTag( "", "text" );
                           serializer.text( mediaItemCursor.getString( 1 ) );
                           serializer.endTag( "", "text" );
                           serializer.endTag( "", "link" );
                        }
                     }
                     finally
                     {
                        if( mediaItemCursor != null )
                        {
                           mediaItemCursor.close();
                        }
                     }
                  }
               }
            }
            while( mediaCursor.moveToNext() );
            //TODO: Multiple media items per waypoint might break XML validity
         }
      }
      finally
      {
         if( mediaCursor != null )
         {
            mediaCursor.close();
         }
      }
   }

   private String getContentType()
   {
      return needsBundling() ? "application/zip" : "text/xml";
   }
}