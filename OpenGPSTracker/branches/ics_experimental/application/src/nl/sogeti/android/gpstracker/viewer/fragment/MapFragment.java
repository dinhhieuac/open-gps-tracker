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
package nl.sogeti.android.gpstracker.viewer.fragment;

import java.util.List;
import java.util.concurrent.Semaphore;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import nl.sogeti.android.gpstracker.viewer.SegmentOverlay;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import nl.sogeti.android.gpstracker.viewer.proxy.MapViewProxy;
import nl.sogeti.android.gpstracker.viewer.proxy.MyLocationOverlayProxy;

import org.osmdroid.tileprovider.util.CloudmadeUtil;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Gallery;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

/**
 * Main activity showing a track and allowing logging control
 * 
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class MapFragment extends Fragment
{
   private static final String TAG = "OGT.MapFragment";
   public static final String OSM_PROVIDER = "OSM";
   public static final String GOOGLE_PROVIDER = "GOOGLE";

   private static final String INSTANCE_E6LONG = "e6long";
   private static final String INSTANCE_E6LAT = "e6lat";
   private static final String INSTANCE_ZOOM = "zoom";
   private static final String INSTANCE_SPEED = "averagespeed";
   private static final String INSTANCE_TRACK = "track";
   private static final int ZOOM_LEVEL = 16;

   private TextView[] mSpeedtexts = new TextView[0];
   private TextView mLastGPSSpeedView = null;
   private TextView mLastGPSAltitudeView = null;
   private TextView mDistanceView = null;

   private double mAverageSpeed = 33.33d / 3d;
   private long mTrackId = -1;
   private long mLastSegment = -1;
   private UnitsI18n mUnits;
   private WakeLock mWakeLock = null;
   private SharedPreferences mSharedPreferences;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private SegmentOverlay mLastSegmentOverlay;

   private MapViewProxy mMapView = null;
   private MyLocationOverlayProxy mMylocation;
   private Handler mHandler;

   private ContentObserver mTrackSegmentsObserver;
   private ContentObserver mSegmentWaypointsObserver;
   private ContentObserver mTrackMediasObserver;
   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
   private UnitsI18n.UnitsChangeListener mUnitsChangeListener;


   private Runnable speedCalculator;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle load)
   {
      super.onCreate(load);
      final Semaphore calulatorSemaphore = new Semaphore(0);
      Thread calulator = new Thread("OverlayCalculator")
      {
         @Override
         public void run()
         {
            Looper.prepare();
            mHandler = new Handler();
            calulatorSemaphore.release();
            Looper.loop();
         }
      };
      calulator.start();
      try
      {
         calulatorSemaphore.acquire();
      }
      catch (InterruptedException e)
      {
         Log.e(TAG, "Failed waiting for a semaphore", e);
      }
      
      mMapView = new MapViewProxy();

      createListeners();
      onRestoreInstanceState(load);
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
   {

      View view = inflater.inflate(R.layout.map, container, false);

      view.findViewById(R.id.mapScreen).setDrawingCacheEnabled(true);

      mSpeedtexts = new TextView[] { (TextView) view.findViewById(R.id.speedview05), (TextView) view.findViewById(R.id.speedview04),
            (TextView) view.findViewById(R.id.speedview03), (TextView) view.findViewById(R.id.speedview02), (TextView) view.findViewById(R.id.speedview01),
            (TextView) view.findViewById(R.id.speedview00) };
      mLastGPSSpeedView = (TextView) view.findViewById(R.id.currentSpeed);
      mLastGPSAltitudeView = (TextView) view.findViewById(R.id.currentAltitude);
      mDistanceView = (TextView) view.findViewById(R.id.currentDistance);

      updateMapProvider();
      mMylocation = new MyLocationOverlayProxy(this.getActivity(), mMapView);
      mMapView.setBuiltInZoomControls(true);
      mMapView.setClickable(true);
      return view;
   }

   @Override
   public void onActivityCreated(Bundle savedInstanceState)
   {
      super.onActivityCreated(savedInstanceState);

      mUnits = new UnitsI18n(this.getActivity());
      mLoggerServiceManager = new GPSLoggerServiceManager(this.getActivity());
      mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
   }

   @Override
   public void onResume()
   {
      super.onResume();
      mLoggerServiceManager.startup(this, mServiceConnected);

      mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
      mUnits.setUnitsChangeListener(mUnitsChangeListener);
      updateMapProvider();

      if (mTrackId >= 0)
      {
         ContentResolver resolver = getActivity().getContentResolver();
         Uri trackUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/segments");
         Uri lastSegmentUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/segments/" + mLastSegment + "/waypoints");
         Uri mediaUri = ContentUris.withAppendedId(Media.CONTENT_URI, mTrackId);

         resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
         resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
         resolver.unregisterContentObserver(this.mTrackMediasObserver);
         resolver.registerContentObserver(trackUri, false, this.mTrackSegmentsObserver);
         resolver.registerContentObserver(lastSegmentUri, true, this.mSegmentWaypointsObserver);
         resolver.registerContentObserver(mediaUri, true, this.mTrackMediasObserver);
      }
      updateDataOverlays();

      updateSpeedColoring();
      updateSpeedDisplayVisibility();
      updateAltitudeDisplayVisibility();
      updateDistanceDisplayVisibility();
      updateCompassDisplayVisibility();
      updateLocationDisplayVisibility();

      mMapView.executePostponedActions();
   }

   @Override
   public void onPause()
   {
      if (this.mWakeLock != null && this.mWakeLock.isHeld())
      {
         this.mWakeLock.release();
         Log.w(TAG, "onPause(): Released lock to keep screen on!");
      }
      ContentResolver resolver = getActivity().getContentResolver();
      resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.unregisterContentObserver(this.mTrackMediasObserver);
      mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
      mUnits.setUnitsChangeListener(null);
      mMylocation.disableMyLocation();
      mMylocation.disableCompass();

      this.mLoggerServiceManager.shutdown(this.getActivity());

      super.onPause();
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   public void onDestroy()
   {
      super.onDestroy();

      mLastSegmentOverlay = null;
      mMapView.clearOverlays();
      mHandler.post(new Runnable()
      {
         public void run()
         {
            Looper.myLooper().quit();
         }
      });

      if (mWakeLock != null && mWakeLock.isHeld())
      {
         mWakeLock.release();
         Log.w(TAG, "onDestroy(): Released lock to keep screen on!");
      }
      if (mLoggerServiceManager.getLoggingState() == Constants.STOPPED)
      {
         stopService(new Intent(Constants.SERVICENAME));
      }
      mUnits = null;
   }

   @Override
   protected void onRestoreInstanceState(Bundle load)
   {
      if (load != null)
      {
         super.onRestoreInstanceState(load);
      }

      Uri data = this.getIntent().getData();
      if (load != null && load.containsKey(INSTANCE_TRACK)) // 1st method: track from a previous instance of this activity
      {
         long loadTrackId = load.getLong(INSTANCE_TRACK);
         if (load.containsKey(INSTANCE_SPEED))
         {
            mAverageSpeed = load.getDouble(INSTANCE_SPEED);
         }
         moveToTrack(loadTrackId, false);
      }
      else if (data != null) // 2nd method: track ordered to make
      {
         long loadTrackId = Long.parseLong(data.getLastPathSegment());
         mAverageSpeed = 0.0;
         moveToTrack(loadTrackId, true);
      }
      else
      // 3rd method: just try the last track
      {
         moveToLastTrack();
      }

      if (load != null && load.containsKey(INSTANCE_ZOOM))
      {
         mMapView.getController().setZoom(load.getInt(INSTANCE_ZOOM));
      }
      else
      {
         mMapView.getController().setZoom(MapFragment.ZOOM_LEVEL);
      }

      if (load != null && load.containsKey(INSTANCE_E6LAT) && load.containsKey(INSTANCE_E6LONG))
      {
         GeoPoint storedPoint = new GeoPoint(load.getInt(INSTANCE_E6LAT), load.getInt(INSTANCE_E6LONG));
         this.mMapView.getController().animateTo(storedPoint);
      }
      else
      {
         GeoPoint lastPoint = getLastTrackPoint();
         this.mMapView.getController().animateTo(lastPoint);
      }
   }

   @Override
   public void onSaveInstanceState(Bundle save)
   {
      super.onSaveInstanceState(save);
      save.putLong(INSTANCE_TRACK, this.mTrackId);
      save.putDouble(INSTANCE_SPEED, mAverageSpeed);
      save.putInt(INSTANCE_ZOOM, this.mMapView.getZoomLevel());
      GeoPoint point = this.mMapView.getMapCenter();
      save.putInt(INSTANCE_E6LAT, point.getLatitudeE6());
      save.putInt(INSTANCE_E6LONG, point.getLongitudeE6());
   }

   private void createListeners()
   {
      /*******************************************************
       * 8 Runnable listener actions
       */
      speedCalculator = new Runnable()
      {
         public void run()
         {
            double avgspeed = 0.0;
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor waypointsCursor = null;
            try
            {
               waypointsCursor = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, MapFragment.this.mTrackId + "/waypoints"), new String[] {
                     "avg(" + Waypoints.SPEED + ")", "max(" + Waypoints.SPEED + ")" }, null, null, null);

               if (waypointsCursor != null && waypointsCursor.moveToLast())
               {
                  double average = waypointsCursor.getDouble(0);
                  double maxBasedAverage = waypointsCursor.getDouble(1) / 2;
                  avgspeed = Math.min(average, maxBasedAverage);
               }
               if (avgspeed < 2)
               {
                  avgspeed = 5.55d / 2;
               }
            }
            finally
            {
               if (waypointsCursor != null)
               {
                  waypointsCursor.close();
               }
            }
            mAverageSpeed = avgspeed;
            getActivity().runOnUiThread(new Runnable()
            {
               public void run()
               {
                  updateSpeedColoring();
               }
            });
         }
      };
      /**
       * Listeners to events outside this mapview
       */
      mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
      {
         public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
         {
            if (key.equals(Constants.TRACKCOLORING))
            {
               mAverageSpeed = 0.0;
               updateSpeedColoring();
            }
            else if (key.equals(Constants.SPEED))
            {
               updateSpeedDisplayVisibility();
            }
            else if (key.equals(Constants.ALTITUDE))
            {
               updateAltitudeDisplayVisibility();
            }
            else if (key.equals(Constants.DISTANCE))
            {
               updateDistanceDisplayVisibility();
            }
            else if (key.equals(Constants.COMPASS))
            {
               updateCompassDisplayVisibility();
            }
            else if (key.equals(Constants.TRAFFIC))
            {
               updateGoogleOverlays();
            }
            else if (key.equals(Constants.SATELLITE))
            {
               updateGoogleOverlays();
            }
            else if (key.equals(Constants.LOCATION))
            {
               updateLocationDisplayVisibility();
            }
            else if (key.equals(Constants.MAPPROVIDER))
            {
               updateMapProvider();
            }
            else if (key.equals(Constants.OSMBASEOVERLAY))
            {
               updateOsmBaseOverlay();
            }
         }
      };
      mTrackMediasObserver = new ContentObserver(new Handler())
      {
         @Override
         public void onChange(boolean selfUpdate)
         {
            if (!selfUpdate)
            {
               if (mLastSegmentOverlay != null)
               {
                  mLastSegmentOverlay.calculateMedia();
                  mMapView.postInvalidate();
               }
            }
            else
            {
               Log.w(TAG, "mTrackMediasObserver skipping change on " + mLastSegment);
            }
         }
      };
      mTrackSegmentsObserver = new ContentObserver(new Handler())
      {
         @Override
         public void onChange(boolean selfUpdate)
         {
            if (!selfUpdate)
            {
               MapFragment.this.updateDataOverlays();
            }
            else
            {
               Log.w(TAG, "mTrackSegmentsObserver skipping change on " + mLastSegment);
            }
         }
      };
      mSegmentWaypointsObserver = new ContentObserver(new Handler())
      {
         @Override
         public void onChange(boolean selfUpdate)
         {
            if (!selfUpdate)
            {
               MapFragment.this.updateTrackNumbers();
               if (mLastSegmentOverlay != null)
               {
                  moveActiveViewWindow();
                  MapFragment.this.updateMapProviderAdministration();
               }
               else
               {
                  Log.e(TAG, "Error the last segment changed but it is not on screen! " + mLastSegment);
               }
            }
            else
            {
               Log.w(TAG, "mSegmentWaypointsObserver skipping change on " + mLastSegment);
            }
         }
      };
      mUnitsChangeListener = new UnitsI18n.UnitsChangeListener()
      {
         public void onUnitsChange()
         {
            mAverageSpeed = 0.0;
            updateTrackNumbers();
            updateSpeedColoring();
         }
      };
   }

//   /**
//    * (non-Javadoc)
//    * 
//    * @see com.google.android.maps.MapActivity#isRouteDisplayed()
//    */
//   @Override
//   protected boolean isRouteDisplayed()
//   {
//      return true;
//   }
//
//   /**
//    * (non-Javadoc)
//    * 
//    * @see com.google.android.maps.MapActivity#isLocationDisplayed()
//    */
//   @Override
//   protected boolean isLocationDisplayed()
//   {
//      return mSharedPreferences.getBoolean(Constants.LOCATION, false) || mLoggerServiceManager.getLoggingState() == Constants.LOGGING;
//   }



   private void updateMapProvider()
   {
      int provider = new Integer(mSharedPreferences.getString(Constants.MAPPROVIDER, "" + Constants.GOOGLE)).intValue();
      switch (provider)
      {
         case Constants.GOOGLE:
            getView().findViewById(R.id.myOsmMapView).setVisibility(View.GONE);
            getView().findViewById(R.id.myMapView).setVisibility(View.VISIBLE);
            mMapView.setMap(getView().findViewById(R.id.myMapView));
            updateGoogleOverlays();
            break;
         case Constants.OSM:
            CloudmadeUtil.retrieveCloudmadeKey(this.getActivity());
            getView().findViewById(R.id.myMapView).setVisibility(View.GONE);
            getView().findViewById(R.id.myOsmMapView).setVisibility(View.VISIBLE);
            mMapView.setMap(getView().findViewById(R.id.myOsmMapView));
            updateOsmBaseOverlay();
            break;
         default:
            Log.e(TAG, "Fault in value " + provider + " as MapProvider.");
            break;
      }
   }

   private void updateGoogleOverlays()
   {
      MapFragment.this.mMapView.setSatellite(mSharedPreferences.getBoolean(Constants.SATELLITE, false));
      MapFragment.this.mMapView.setTraffic(mSharedPreferences.getBoolean(Constants.TRAFFIC, false));
   }

   private void updateOsmBaseOverlay()
   {
      int baselayer = mSharedPreferences.getInt(Constants.OSMBASEOVERLAY, 0);
      mMapView.setOSMType(baselayer);
   }

   protected void updateMapProviderAdministration()
   {
      if (getView().findViewById(R.id.myMapView).getVisibility() == View.VISIBLE)
      {
         mLoggerServiceManager.storeDerivedDataSource(GOOGLE_PROVIDER);
      }
      if (getView().findViewById(R.id.myOsmMapView).getVisibility() == View.VISIBLE)
      {
         mLoggerServiceManager.storeDerivedDataSource(OSM_PROVIDER);

      }
   }

   private void updateSpeedColoring()
   {
      int trackColoringMethod = new Integer(mSharedPreferences.getString(Constants.TRACKCOLORING, "3")).intValue();
      View speedbar = getView().findViewById(R.id.speedbar);

      if (trackColoringMethod == SegmentOverlay.DRAW_MEASURED || trackColoringMethod == SegmentOverlay.DRAW_CALCULATED)
      {
         // mAverageSpeed is set to 0 if unknown or to trigger an recalculation here
         if (mAverageSpeed == 0.0)
         {
            mHandler.removeCallbacks(speedCalculator);
            mHandler.post(speedCalculator);
         }
         else
         {
            drawSpeedTexts(mAverageSpeed);

            speedbar.setVisibility(View.VISIBLE);
            for (int i = 0; i < mSpeedtexts.length; i++)
            {
               mSpeedtexts[i].setVisibility(View.VISIBLE);
            }
         }
      }
      else
      {
         speedbar.setVisibility(View.INVISIBLE);
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility(View.INVISIBLE);
         }
      }
      List< ? > overlays = mMapView.getOverlays();
      for (Object overlay : overlays)
      {
         if (overlay instanceof SegmentOverlay)
         {
            ((SegmentOverlay) overlay).setTrackColoringMethod(trackColoringMethod, mAverageSpeed);
         }
      }
   }

   private void updateSpeedDisplayVisibility()
   {
      boolean showspeed = mSharedPreferences.getBoolean(Constants.SPEED, false);
      if (showspeed)
      {
         mLastGPSSpeedView.setVisibility(View.VISIBLE);
      }
      else
      {
         mLastGPSSpeedView.setVisibility(View.GONE);
      }
   }

   private void updateAltitudeDisplayVisibility()
   {
      boolean showaltitude = mSharedPreferences.getBoolean(Constants.ALTITUDE, false);
      if (showaltitude)
      {
         mLastGPSAltitudeView.setVisibility(View.VISIBLE);
      }
      else
      {
         mLastGPSAltitudeView.setVisibility(View.GONE);
      }
   }

   private void updateDistanceDisplayVisibility()
   {
      boolean showdistance = mSharedPreferences.getBoolean(Constants.DISTANCE, false);
      if (showdistance)
      {
         mDistanceView.setVisibility(View.VISIBLE);
      }
      else
      {
         mDistanceView.setVisibility(View.GONE);
      }
   }

   private void updateCompassDisplayVisibility()
   {
      boolean compass = mSharedPreferences.getBoolean(Constants.COMPASS, false);
      if (compass)
      {
         mMylocation.enableCompass();
      }
      else
      {
         mMylocation.disableCompass();
      }
   }

   private void updateLocationDisplayVisibility()
   {
      boolean location = mSharedPreferences.getBoolean(Constants.LOCATION, false);
      if (location)
      {
         mMylocation.enableMyLocation();
      }
      else
      {
         mMylocation.disableMyLocation();
      }
   }

   /**
    * Retrieves the numbers of the measured speed and altitude from the most
    * recent waypoint and updates UI components with this latest bit of
    * information.
    */
   private void updateTrackNumbers()
   {
      Location lastWaypoint = mLoggerServiceManager.getLastWaypoint();
      UnitsI18n units = mUnits;
      if (lastWaypoint != null && units != null)
      {
         // Speed number
         double speed = lastWaypoint.getSpeed();
         speed = units.conversionFromMetersPerSecond(speed);
         String speedText = units.formatSpeed(speed, false);
         mLastGPSSpeedView.setText(speedText);

         // Speed color bar and refrence numbers
         if (speed > 2 * mAverageSpeed)
         {
            mAverageSpeed = 0.0;
            updateSpeedColoring();
            mMapView.postInvalidate();
         }

         //Altitude number
         double altitude = lastWaypoint.getAltitude();
         altitude = units.conversionFromMeterToHeight(altitude);
         String altitudeText = String.format("%.0f %s", altitude, units.getHeightUnit());
         mLastGPSAltitudeView.setText(altitudeText);

         //Distance number
         double distance = units.conversionFromMeter(mLoggerServiceManager.getTrackedDistance());
         String distanceText = String.format("%.2f %s", distance, units.getDistanceUnit());
         mDistanceView.setText(distanceText);
      }
   }

   /**
    * For the current track identifier the route of that track is drawn by
    * adding a OverLay for each segments in the track
    * 
    * @param trackId
    * @see SegmentOverlay
    */
   private void createDataOverlays()
   {
      mLastSegmentOverlay = null;
      mMapView.clearOverlays();
      mMapView.addOverlay(mMylocation);

      ContentResolver resolver = getActivity().getContentResolver();
      Cursor segments = null;
      int trackColoringMethod = new Integer(mSharedPreferences.getString(Constants.TRACKCOLORING, "2")).intValue();

      try
      {
         Uri segmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments");
         segments = resolver.query(segmentsUri, new String[] { Segments._ID }, null, null, null);
         if (segments != null && segments.moveToFirst())
         {
            do
            {
               long segmentsId = segments.getLong(0);
               Uri segmentUri = ContentUris.withAppendedId(segmentsUri, segmentsId);
               SegmentOverlay segmentOverlay = new SegmentOverlay(this, segmentUri, trackColoringMethod, mAverageSpeed, this.mMapView, mHandler);
               mMapView.addOverlay(segmentOverlay);
               mLastSegmentOverlay = segmentOverlay;
               if (segments.isFirst())
               {
                  segmentOverlay.addPlacement(SegmentOverlay.FIRST_SEGMENT);
               }
               if (segments.isLast())
               {
                  segmentOverlay.addPlacement(SegmentOverlay.LAST_SEGMENT);
               }
               mLastSegment = segmentsId;
            }
            while (segments.moveToNext());
         }
      }
      finally
      {
         if (segments != null)
         {
            segments.close();
         }
      }

      Uri lastSegmentUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/segments/" + mLastSegment + "/waypoints");
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.registerContentObserver(lastSegmentUri, false, this.mSegmentWaypointsObserver);
   }

   private void updateDataOverlays()
   {
      ContentResolver resolver = getActivity().getContentResolver();
      Uri segmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments");
      Cursor segmentsCursor = null;
      List< ? > overlays = this.mMapView.getOverlays();
      int segmentOverlaysCount = 0;

      for (Object overlay : overlays)
      {
         if (overlay instanceof SegmentOverlay)
         {
            segmentOverlaysCount++;
         }
      }
      try
      {
         segmentsCursor = resolver.query(segmentsUri, new String[] { Segments._ID }, null, null, null);
         if (segmentsCursor != null && segmentsCursor.getCount() == segmentOverlaysCount)
         {
            //            Log.d( TAG, "Alignment of segments" );
         }
         else
         {
            createDataOverlays();
         }
      }
      finally
      {
         if (segmentsCursor != null)
         {
            segmentsCursor.close();
         }
      }
   }

   /**
    * Call when an overlay has recalulated and has new information to be redrawn
    */
   public void onDateOverlayChanged()
   {
      this.mMapView.postInvalidate();
   }

   private void moveActiveViewWindow()
   {
      GeoPoint lastPoint = getLastTrackPoint();
      if (lastPoint != null && mLoggerServiceManager.getLoggingState() == Constants.LOGGING)
      {
         Point out = new Point();
         this.mMapView.getProjection().toPixels(lastPoint, out);
         int height = this.mMapView.getHeight();
         int width = this.mMapView.getWidth();
         if (out.x < 0 || out.y < 0 || out.y > height || out.x > width)
         {

            this.mMapView.clearAnimation();
            this.mMapView.getController().setCenter(lastPoint);
            //            Log.d( TAG, "mMapView.setCenter()" );
         }
         else if (out.x < width / 4 || out.y < height / 4 || out.x > (width / 4) * 3 || out.y > (height / 4) * 3)
         {
            this.mMapView.clearAnimation();
            this.mMapView.getController().animateTo(lastPoint);
            //            Log.d( TAG, "mMapView.animateTo()" );
         }
      }
   }

   /**
    * @param avgSpeed avgSpeed in m/s
    */
   private void drawSpeedTexts(double avgSpeed)
   {
      UnitsI18n units = mUnits;
      if (units != null)
      {
         avgSpeed = units.conversionFromMetersPerSecond(avgSpeed);
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility(View.VISIBLE);
            double speed;
            if (mUnits.isUnitFlipped())
            {
               speed = ((avgSpeed * 2d) / 5d) * (mSpeedtexts.length - i - 1);
            }
            else
            {
               speed = ((avgSpeed * 2d) / 5d) * i;
            }
            String speedText = units.formatSpeed(speed, false);
            mSpeedtexts[i].setText(speedText);
         }
      }
   }

   /**
    * Alter this to set a new track as current.
    * 
    * @param trackId
    * @param center center on the end of the track
    */
   private void moveToTrack(long trackId, boolean center)
   {
      Cursor track = null;
      try
      {
         ContentResolver resolver = getActivity().getContentResolver();
         Uri trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, trackId);
         track = resolver.query(trackUri, new String[] { Tracks.NAME }, null, null, null);
         if (track != null && track.moveToFirst())
         {
            this.mTrackId = trackId;
            mLastSegment = -1;
            resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
            resolver.unregisterContentObserver(this.mTrackMediasObserver);
            Uri tracksegmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments");

            resolver.registerContentObserver(tracksegmentsUri, false, this.mTrackSegmentsObserver);
            resolver.registerContentObserver(Media.CONTENT_URI, true, this.mTrackMediasObserver);

            this.mMapView.clearOverlays();

            updateTitleBar();
            updateDataOverlays();
            updateSpeedColoring();
            if (center)
            {
               GeoPoint lastPoint = getLastTrackPoint();
               this.mMapView.getController().animateTo(lastPoint);
            }
         }
      }
      finally
      {
         if (track != null)
         {
            track.close();
         }
      }
   }

   /**
    * Get the last know position from the GPS provider and return that
    * information wrapped in a GeoPoint to which the Map can navigate.
    * 
    * @see GeoPoint
    * @return
    */
   private GeoPoint getLastKnowGeopointLocation()
   {
      int microLatitude = 0;
      int microLongitude = 0;
      LocationManager locationManager = (LocationManager) getActivity().getApplication().getSystemService(Context.LOCATION_SERVICE);
      Location locationFine = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (locationFine != null)
      {
         microLatitude = (int) (locationFine.getLatitude() * 1E6d);
         microLongitude = (int) (locationFine.getLongitude() * 1E6d);
      }
      if (locationFine == null || microLatitude == 0 || microLongitude == 0)
      {
         Location locationCoarse = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
         if (locationCoarse != null)
         {
            microLatitude = (int) (locationCoarse.getLatitude() * 1E6d);
            microLongitude = (int) (locationCoarse.getLongitude() * 1E6d);
         }
         if (locationCoarse == null || microLatitude == 0 || microLongitude == 0)
         {
            microLatitude = 51985105;
            microLongitude = 5106132;
         }
      }
      GeoPoint geoPoint = new GeoPoint(microLatitude, microLongitude);
      return geoPoint;
   }

   /**
    * Retrieve the last point of the current track
    * 
    * @param context
    */
   private GeoPoint getLastTrackPoint()
   {
      Cursor waypoint = null;
      GeoPoint lastPoint = null;
      // First try the service which might have a cached version
      Location lastLoc = mLoggerServiceManager.getLastWaypoint();
      if (lastLoc != null)
      {
         int microLatitude = (int) (lastLoc.getLatitude() * 1E6d);
         int microLongitude = (int) (lastLoc.getLongitude() * 1E6d);
         lastPoint = new GeoPoint(microLatitude, microLongitude);
      }

      // If nothing yet, try the content resolver and query the track
      if (lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0)
      {
         try
         {
            ContentResolver resolver = getActivity().getContentResolver();
            waypoint = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/waypoints"), new String[] { Waypoints.LATITUDE,
                  Waypoints.LONGITUDE, "max(" + Waypoints.TABLE + "." + Waypoints._ID + ")" }, null, null, null);
            if (waypoint != null && waypoint.moveToLast())
            {
               int microLatitude = (int) (waypoint.getDouble(0) * 1E6d);
               int microLongitude = (int) (waypoint.getDouble(1) * 1E6d);
               lastPoint = new GeoPoint(microLatitude, microLongitude);
            }
         }
         finally
         {
            if (waypoint != null)
            {
               waypoint.close();
            }
         }
      }

      // If nothing yet, try the last generally known location
      if (lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0)
      {
         lastPoint = getLastKnowGeopointLocation();
      }
      return lastPoint;
   }

   private void moveToLastTrack()
   {
      int trackId = -1;
      Cursor track = null;
      try
      {
         ContentResolver resolver = getActivity().getContentResolver();
         track = resolver.query(Tracks.CONTENT_URI, new String[] { "max(" + Tracks._ID + ")", Tracks.NAME, }, null, null, null);
         if (track != null && track.moveToLast())
         {
            trackId = track.getInt(0);
            mAverageSpeed = 0.0;
            moveToTrack(trackId, false);
         }
      }
      finally
      {
         if (track != null)
         {
            track.close();
         }
      }
   }

   public void showDialog(BaseAdapter adapter)
   {
      Activity activity = getActivity();
      if( activity instanceof LoggerMap )
      {
         ((LoggerMap)activity).showDialog(adapter);
      }
   }
}