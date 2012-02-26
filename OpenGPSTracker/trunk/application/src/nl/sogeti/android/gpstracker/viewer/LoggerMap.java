/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Feb 26, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.viewer;

import java.util.List;

import com.google.android.maps.GeoPoint;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) Feb 26, 2012, Sogeti B.V.
 */
public interface LoggerMap
{

   void setDrawingCacheEnabled(boolean b);

   Activity getActivity();

   void updateOverlays();

   void onLayerCheckedChanged(int checkedId, boolean b);

   void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key);

   Bitmap getDrawingCache();

   void showMediaDialog(BaseAdapter mediaAdapter);

   void onDateOverlayChanged();

   String getDataSourceId();

   boolean isOutsideScreen(GeoPoint lastPoint);

   boolean isNearScreenEdge(GeoPoint lastPoint);

   void executePostponedActions();

   void disableMyLocation();

   void disableCompass();

   void clearSegmentOverlays();

   void setZoom(int int1);

   void animateTo(GeoPoint storedPoint);

   int getZoomLevel();

   GeoPoint getMapCenter();

   boolean zoomOut();

   boolean zoomIn();

   void postInvalidate();

   List<SegmentOverlay> getSegmentOverlays();

   void enableCompass();

   void enableMyLocation();

   void addSegmentOverlay(SegmentOverlay segmentOverlay);

   void clearAnimation();

   void setCenter(GeoPoint lastPoint);

   int getMaxZoomLevel();

   GeoPoint fromPixels(int x, int y);

   boolean hasProjection();

   float metersToEquatorPixels(float float1);

   void toPixels(GeoPoint geopoint, Point mMediaScreenPoint);

   TextView[] getSpeedTextViews();

   TextView getAltitideTextView();

   TextView getSpeedTextView();

   TextView getDistanceTextView();
}
