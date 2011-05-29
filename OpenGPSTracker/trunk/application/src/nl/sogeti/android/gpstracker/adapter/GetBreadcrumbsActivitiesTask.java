/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.adapter;

import java.io.IOException;
import java.io.InputStream;

import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.os.AsyncTask;
import android.util.Log;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request
 * token. (OAuthGetRequestToken) After receiving the request token from Twitter,
 * pop a browser to the user to authorize the Request Token.
 * (OAuthAuthorizeToken)
 */
public class GetBreadcrumbsActivitiesTask extends BreadcrumbsTask
{

   final String TAG = "OGT.GetBreadcrumbsActivitiesTask";
   private BreadcrumbsAdapter mAdapter;
   private OAuthConsumer mConsumer;
   private DefaultHttpClient mHttpClient;
   
   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the
    *           browser.
    * @param httpclient 
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsActivitiesTask(BreadcrumbsAdapter adapter, ProgressListener listener, DefaultHttpClient httpclient, OAuthConsumer consumer)
   {
      super(listener, adapter);
      mAdapter = adapter;
      mHttpClient = httpclient;
      mConsumer = consumer;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected BreadcrumbsTracks doInBackground(Void... params)
   {
      BreadcrumbsTracks tracks = mAdapter.getBreadcrumbsTracks();
      try
      {
         HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/activities.xml");         
         mConsumer.sign(request);
         if( isCancelled() )
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         HttpResponse response = mHttpClient.execute(request);
         HttpEntity entity = response.getEntity();
         InputStream instream = entity.getContent();

         
         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         factory.setNamespaceAware(true);
         XmlPullParser xpp = factory.newPullParser();
         xpp.setInput(instream, "UTF-8");

         String tagName = null;
         int eventType = xpp.getEventType();
         
         String activityName = null;
         Integer activityId = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if( "activity".equals(xpp.getName()) && activityId != null && activityName != null )
               {
                  tracks.addActivity( activityId, activityName );
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if( "id".equals(tagName) )
               {
                  activityId = Integer.parseInt(xpp.getText() );
               }
               else if( "name".equals(tagName) )
               {
                  activityName = xpp.getText();
               }
            }
            eventType = xpp.next();
         }
      }
      catch (OAuthMessageSignerException e)
      {
         handleError(e, "TODO");
      }
      catch (OAuthExpectationFailedException e)
      {
         handleError(e, "TODO");
      }
      catch (OAuthCommunicationException e)
      {
         handleError(e, "TODO");
      }
      catch (IOException e)
      {
         handleError(e, "TODO");
      }
      catch (XmlPullParserException e)
      {
         handleError(e, "TODO");
      }
      return tracks;
   }
   

}