/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Jan 7, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.viewer;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.ControlTracking;
import nl.sogeti.android.gpstracker.actions.InsertNote;
import nl.sogeti.android.gpstracker.actions.ShareTrack;
import nl.sogeti.android.gpstracker.actions.Statistics;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.viewer.fragment.MapFragment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Gallery;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) Jan 7, 2012, Sogeti B.V.
 */
public class LoggerMap extends Activity
{
   private static final String TAG = "OGT.LoggerMap";
   // MENU'S
   private static final int MENU_SETTINGS = 1;
   private static final int MENU_TRACKING = 2;
   private static final int MENU_TRACKLIST = 3;
   private static final int MENU_STATS = 4;
   private static final int MENU_ABOUT = 5;
   private static final int MENU_LAYERS = 6;
   private static final int MENU_NOTE = 7;
   private static final int MENU_SHARE = 13;
   private static final int MENU_CONTRIB = 14;
   private static final int DIALOG_NOTRACK = 24;
   private static final int DIALOG_INSTALL_ABOUT = 29;
   private static final int DIALOG_LAYERS = 31;
   private static final int DIALOG_URIS = 34;
   private static final int DIALOG_CONTRIB = 35;
   // UI's
   private CheckBox mTraffic;
   private CheckBox mSpeed;
   private CheckBox mAltitude;
   private CheckBox mDistance;
   private CheckBox mCompass;
   private CheckBox mLocation;
   private BaseAdapter mMediaAdapter;
   private Gallery mGallery;
   // Listeners
   private SharedPreferences mSharedPreferences;
   private DialogInterface.OnClickListener mNoTrackDialogListener;
   private DialogInterface.OnClickListener mOiAboutDialogListener;
   private OnClickListener mNoteSelectDialogListener;
   private OnCheckedChangeListener mCheckedChangeListener;
   private android.widget.RadioGroup.OnCheckedChangeListener mGroupCheckedChangeListener;
   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
   
   /**
    * Run after the ServiceManager completes the binding to the remote service
    */
   private Runnable mServiceConnected;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       if (savedInstanceState == null) {
           // During initial setup, plug in the details fragment.
           MapFragment mapFragment = new MapFragment();
           mapFragment.setArguments(getIntent().getExtras());
           getFragmentManager().beginTransaction().add(android.R.id.content, mapFragment).commit();
       }

       mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
       createListeners();
       onRestoreInstanceState(savedInstanceState);
   }
   
   @Override
   protected void onResume()
   {
      super.onResume();
      mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
      updateTitleBar();
      updateBlankingBehavior();
   }
   
   @Override
   protected void onPause()
   {
      mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
      super.onPause();
   }
   
   /*
    * (non-Javadoc)
    * @see
    * com.google.android.maps.MapActivity#onNewIntent(android.content.Intent)
    */
   @Override
   public void onNewIntent(Intent newIntent)
   {
      Uri data = newIntent.getData();
      if (data != null)
      {
         moveToTrack(Long.parseLong(data.getLastPathSegment()), true);
      }
   }

   private void createListeners()
   {
      mServiceConnected = new Runnable()
      {
         public void run()
         {
            updateBlankingBehavior();
         }
      };
      /*******************************************************
       * 8 Various dialog listeners
       */
      mNoteSelectDialogListener = new DialogInterface.OnClickListener()
      {

         public void onClick(DialogInterface dialog, int which)
         {
            Uri selected = (Uri) mGallery.getSelectedItem();
            SegmentOverlay.handleMedia(getActivity(), selected);
         }
      };
      mGroupCheckedChangeListener = new android.widget.RadioGroup.OnCheckedChangeListener()
      {
         public void onCheckedChanged(RadioGroup group, int checkedId)
         {
            switch (checkedId)
            {
               case R.id.layer_google_satellite:
                  setSatelliteOverlay(true);
                  break;
               case R.id.layer_google_regular:
                  setSatelliteOverlay(false);
                  break;
               case R.id.layer_osm_cloudmade:
                  setOsmBaseOverlay(Constants.OSM_CLOUDMADE);
                  break;
               case R.id.layer_osm_maknik:
                  setOsmBaseOverlay(Constants.OSM_MAKNIK);
                  break;
               case R.id.layer_osm_bicycle:
                  setOsmBaseOverlay(Constants.OSM_CYCLE);
                  break;
               default:
                  break;
            }
         }
      };
      mCheckedChangeListener = new OnCheckedChangeListener()
      {
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
         {
            int checkedId;
            checkedId = buttonView.getId();
            switch (checkedId)
            {
               case R.id.layer_traffic:
                  setTrafficOverlay(isChecked);
                  break;
               case R.id.layer_speed:
                  setSpeedOverlay(isChecked);
                  break;
               case R.id.layer_altitude:
                  setAltitudeOverlay(isChecked);
                  break;
               case R.id.layer_distance:
                  setDistanceOverlay(isChecked);
                  break;
               case R.id.layer_compass:
                  setCompassOverlay(isChecked);
                  break;
               case R.id.layer_location:
                  setLocationOverlay(isChecked);
                  break;
               default:
                  break;
            }
         }
      };
      mNoTrackDialogListener = new DialogInterface.OnClickListener()
      {
         public void onClick(DialogInterface dialog, int which)
         {
            //            Log.d( TAG, "mNoTrackDialogListener" + which);
            Intent tracklistIntent = new Intent(MapFragment.this, TrackList.class);
            tracklistIntent.putExtra(Tracks._ID, MapFragment.this.mTrackId);
            startActivityForResult(tracklistIntent, MENU_TRACKLIST);
         }
      };
      mOiAboutDialogListener = new DialogInterface.OnClickListener()
      {
         public void onClick(DialogInterface dialog, int which)
         {
            Uri oiDownload = Uri.parse("market://details?id=org.openintents.about");
            Intent oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
            try
            {
               startActivity(oiAboutIntent);
            }
            catch (ActivityNotFoundException e)
            {
               oiDownload = Uri.parse("http://openintents.googlecode.com/files/AboutApp-1.0.0.apk");
               oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
               startActivity(oiAboutIntent);
            }
         }
      };
      /**
       * Listeners to events outside this mapview
       */
      mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
      {
         public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
         {
            if (key.equals(Constants.DISABLEBLANKING) || key.equals(Constants.DISABLEDIMMING))
            {
               updateBlankingBehavior();
            }
         }
      };
   }

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event)
   {
      boolean propagate = true;
      switch (keyCode)
      {
         case KeyEvent.KEYCODE_T:
            propagate = this.mMapView.getController().zoomIn();
            break;
         case KeyEvent.KEYCODE_G:
            propagate = this.mMapView.getController().zoomOut();
            break;
         case KeyEvent.KEYCODE_S:
            setSatelliteOverlay(!this.mMapView.isSatellite());
            propagate = false;
            break;
         case KeyEvent.KEYCODE_A:
            setTrafficOverlay(!this.mMapView.isTraffic());
            propagate = false;
            break;
         case KeyEvent.KEYCODE_F:
            mAverageSpeed = 0.0;
            moveToTrack(this.mTrackId - 1, true);
            propagate = false;
            break;
         case KeyEvent.KEYCODE_H:
            mAverageSpeed = 0.0;
            moveToTrack(this.mTrackId + 1, true);
            propagate = false;
            break;
         default:
            propagate = super.onKeyDown(keyCode, event);
            break;
      }
      return propagate;
   }
   
   private void setTrafficOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.TRAFFIC, b);
      editor.commit();
   }

   private void setSatelliteOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.SATELLITE, b);
      editor.commit();
   }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      boolean result = super.onCreateOptionsMenu(menu);

      menu.add(ContextMenu.NONE, MENU_TRACKING, ContextMenu.NONE, R.string.menu_tracking).setIcon(R.drawable.ic_menu_movie).setAlphabeticShortcut('T');
      menu.add(ContextMenu.NONE, MENU_LAYERS, ContextMenu.NONE, R.string.menu_showLayers).setIcon(R.drawable.ic_menu_mapmode).setAlphabeticShortcut('L');
      menu.add(ContextMenu.NONE, MENU_NOTE, ContextMenu.NONE, R.string.menu_insertnote).setIcon(R.drawable.ic_menu_myplaces);

      menu.add(ContextMenu.NONE, MENU_STATS, ContextMenu.NONE, R.string.menu_statistics).setIcon(R.drawable.ic_menu_picture).setAlphabeticShortcut('S');
      menu.add(ContextMenu.NONE, MENU_SHARE, ContextMenu.NONE, R.string.menu_shareTrack).setIcon(R.drawable.ic_menu_share).setAlphabeticShortcut('I');
      // More

      menu.add(ContextMenu.NONE, MENU_TRACKLIST, ContextMenu.NONE, R.string.menu_tracklist).setIcon(R.drawable.ic_menu_show_list).setAlphabeticShortcut('P');
      menu.add(ContextMenu.NONE, MENU_SETTINGS, ContextMenu.NONE, R.string.menu_settings).setIcon(R.drawable.ic_menu_preferences).setAlphabeticShortcut('C');
      menu.add(ContextMenu.NONE, MENU_ABOUT, ContextMenu.NONE, R.string.menu_about).setIcon(R.drawable.ic_menu_info_details).setAlphabeticShortcut('A');
      menu.add(ContextMenu.NONE, MENU_CONTRIB, ContextMenu.NONE, R.string.menu_contrib).setIcon(R.drawable.ic_menu_allfriends);

      return result;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
    */
   @Override
   public boolean onPrepareOptionsMenu(Menu menu)
   {
      MenuItem noteMenu = menu.findItem(MENU_NOTE);
      noteMenu.setEnabled(mLoggerServiceManager.isMediaPrepared());

      MenuItem shareMenu = menu.findItem(MENU_SHARE);
      shareMenu.setEnabled(mTrackId >= 0);

      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = false;

      Uri trackUri;
      Intent intent;
      switch (item.getItemId())
      {
         case MENU_TRACKING:
            intent = new Intent(this, ControlTracking.class);
            startActivityForResult(intent, MENU_TRACKING);
            handled = true;
            break;
         case MENU_LAYERS:
            showDialog(DIALOG_LAYERS);
            handled = true;
            break;
         case MENU_NOTE:
            intent = new Intent(this, InsertNote.class);
            startActivityForResult(intent, MENU_NOTE);
            handled = true;
            break;
         case MENU_SETTINGS:
            intent = new Intent(this, ApplicationPreferenceActivity.class);
            startActivity(intent);
            handled = true;
            break;
         case MENU_TRACKLIST:
            intent = new Intent(this, TrackList.class);
            intent.putExtra(Tracks._ID, this.mTrackId);
            startActivityForResult(intent, MENU_TRACKLIST);
            break;
         case MENU_STATS:
            if (this.mTrackId >= 0)
            {
               intent = new Intent(this, Statistics.class);
               trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId);
               intent.setData(trackUri);
               startActivity(intent);
               handled = true;
               break;
            }
            else
            {
               showDialog(DIALOG_NOTRACK);
            }
            handled = true;
            break;
         case MENU_ABOUT:
            intent = new Intent("org.openintents.action.SHOW_ABOUT_DIALOG");
            try
            {
               startActivityForResult(intent, MENU_ABOUT);
            }
            catch (ActivityNotFoundException e)
            {
               showDialog(DIALOG_INSTALL_ABOUT);
            }
            break;
         case MENU_SHARE:
            intent = new Intent(Intent.ACTION_RUN);
            trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId);
            intent.setDataAndType(trackUri, Tracks.CONTENT_ITEM_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Bitmap bm = findViewById(R.id.mapScreen).getDrawingCache();
            Uri screenStreamUri = ShareTrack.storeScreenBitmap(bm);
            intent.putExtra(Intent.EXTRA_STREAM, screenStreamUri);
            startActivityForResult(Intent.createChooser( intent, getString( R.string.share_track ) ), MENU_SHARE);
            handled = true;
            break;
         case MENU_CONTRIB:
            showDialog(DIALOG_CONTRIB);
         default:
            handled = super.onOptionsItemSelected(item);
            break;
      }
      return handled;
   }
   
   /**
    * Enables a SegmentOverlay to call back to the MapActivity to show a dialog
    * with choices of media
    * 
    * @param mediaAdapter
    */
   public void showDialog(BaseAdapter mediaAdapter)
   {
      mMediaAdapter = mediaAdapter;
      showDialog(MapFragment.DIALOG_URIS);
   }
   
   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_LAYERS:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.layerdialog, null);

            mTraffic = (CheckBox) view.findViewById(R.id.layer_traffic);
            mSpeed = (CheckBox) view.findViewById(R.id.layer_speed);
            mAltitude = (CheckBox) view.findViewById(R.id.layer_altitude);
            mDistance = (CheckBox) view.findViewById(R.id.layer_distance);
            mCompass = (CheckBox) view.findViewById(R.id.layer_compass);
            mLocation = (CheckBox) view.findViewById(R.id.layer_location);

            ((RadioGroup) view.findViewById(R.id.google_backgrounds)).setOnCheckedChangeListener(mGroupCheckedChangeListener);
            ((RadioGroup) view.findViewById(R.id.osm_backgrounds)).setOnCheckedChangeListener(mGroupCheckedChangeListener);

            mTraffic.setOnCheckedChangeListener(mCheckedChangeListener);
            mSpeed.setOnCheckedChangeListener(mCheckedChangeListener);
            mAltitude.setOnCheckedChangeListener(mCheckedChangeListener);
            mDistance.setOnCheckedChangeListener(mCheckedChangeListener);
            mCompass.setOnCheckedChangeListener(mCheckedChangeListener);
            mLocation.setOnCheckedChangeListener(mCheckedChangeListener);

            builder.setTitle(R.string.dialog_layer_title).setIcon(android.R.drawable.ic_dialog_map).setPositiveButton(R.string.btn_okay, null).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_NOTRACK:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_notrack_title).setMessage(R.string.dialog_notrack_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_selecttrack, mNoTrackDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_INSTALL_ABOUT:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_nooiabout).setMessage(R.string.dialog_nooiabout_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_install, mOiAboutDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_URIS:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.mediachooser, null);
            mGallery = (Gallery) view.findViewById(R.id.gallery);
            builder.setTitle(R.string.dialog_select_media_title).setMessage(R.string.dialog_select_media_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(R.string.btn_cancel, null).setPositiveButton(R.string.btn_okay, mNoteSelectDialogListener).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_CONTRIB:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.contrib, null);
            TextView contribView = (TextView) view.findViewById(R.id.contrib_view);
            contribView.setText(R.string.dialog_contrib_message);
            builder.setTitle(R.string.dialog_contrib_title).setView(view).setIcon(android.R.drawable.ic_dialog_email)
                  .setPositiveButton(R.string.btn_okay, null);
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      RadioButton satellite;
      RadioButton regular;
      RadioButton cloudmade;
      RadioButton mapnik;
      RadioButton cycle;
      switch (id)
      {
         case DIALOG_LAYERS:
            satellite = (RadioButton) dialog.findViewById(R.id.layer_google_satellite);
            regular = (RadioButton) dialog.findViewById(R.id.layer_google_regular);
            satellite.setChecked(mSharedPreferences.getBoolean(Constants.SATELLITE, false));
            regular.setChecked(!mSharedPreferences.getBoolean(Constants.SATELLITE, false));

            int osmbase = mSharedPreferences.getInt(Constants.OSMBASEOVERLAY, 0);
            cloudmade = (RadioButton) dialog.findViewById(R.id.layer_osm_cloudmade);
            mapnik = (RadioButton) dialog.findViewById(R.id.layer_osm_maknik);
            cycle = (RadioButton) dialog.findViewById(R.id.layer_osm_bicycle);
            cloudmade.setChecked(osmbase == Constants.OSM_CLOUDMADE);
            mapnik.setChecked(osmbase == Constants.OSM_MAKNIK);
            cycle.setChecked(osmbase == Constants.OSM_CYCLE);

            mTraffic.setChecked(mSharedPreferences.getBoolean(Constants.TRAFFIC, false));
            mSpeed.setChecked(mSharedPreferences.getBoolean(Constants.SPEED, false));
            mAltitude.setChecked(mSharedPreferences.getBoolean(Constants.ALTITUDE, false));
            mDistance.setChecked(mSharedPreferences.getBoolean(Constants.DISTANCE, false));
            mCompass.setChecked(mSharedPreferences.getBoolean(Constants.COMPASS, false));
            mLocation.setChecked(mSharedPreferences.getBoolean(Constants.LOCATION, false));
            int provider = new Integer(mSharedPreferences.getString(Constants.MAPPROVIDER, "" + Constants.GOOGLE)).intValue();
            switch (provider)
            {
               case Constants.GOOGLE:
                  dialog.findViewById(R.id.google_backgrounds).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.osm_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.shared_layers).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_overlays).setVisibility(View.VISIBLE);
                  break;
               case Constants.OSM:
                  dialog.findViewById(R.id.osm_backgrounds).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.shared_layers).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_overlays).setVisibility(View.GONE);
                  break;
               default:
                  Log.e(TAG, "Fault in value " + provider + " as MapProvider.");
                  break;
            }
            break;
         case DIALOG_URIS:
            mGallery.setAdapter(mMediaAdapter);
         default:
            break;
      }
      super.onPrepareDialog(id, dialog);
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int,
    * android.content.Intent)
    */
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);
      Uri trackUri;
      long trackId;
      switch (requestCode)
      {
         case MENU_TRACKLIST:
            if (resultCode == RESULT_OK)
            {
               trackUri = intent.getData();
               trackId = Long.parseLong(trackUri.getLastPathSegment());
               mAverageSpeed = 0.0;
               moveToTrack(trackId, true);
            }
            break;
         case MENU_ABOUT:
            break;
         case MENU_TRACKING:
            if (resultCode == RESULT_OK)
            {
               trackUri = intent.getData();
               if (trackUri != null)
               {
                  trackId = Long.parseLong(trackUri.getLastPathSegment());
                  mAverageSpeed = 0.0;
                  moveToTrack(trackId, true);
               }
            }
            break;
         case MENU_SHARE:
            ShareTrack.clearScreenBitmap();
            break;
         default:
            Log.e(TAG, "Returned form unknow activity: " + requestCode);
            break;
      }
   }
   
   private void setSpeedOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.SPEED, b);
      editor.commit();
   }

   private void setAltitudeOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.ALTITUDE, b);
      editor.commit();
   }

   private void setDistanceOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.DISTANCE, b);
      editor.commit();
   }

   private void setCompassOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.COMPASS, b);
      editor.commit();
   }

   private void setLocationOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.LOCATION, b);
      editor.commit();
   }

   private void setOsmBaseOverlay(int b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putInt(Constants.OSMBASEOVERLAY, b);
      editor.commit();
   }
   
   private void updateTitleBar()
   {
      ContentResolver resolver = this.getContentResolver();
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query(ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId), new String[] { Tracks.NAME }, null, null, null);
         if (trackCursor != null && trackCursor.moveToLast())
         {
            String trackName = trackCursor.getString(0);
            this.setTitle(this.getString(R.string.app_name) + ": " + trackName);
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
   }
   
   private void updateBlankingBehavior()
   {
      boolean disableblanking = mSharedPreferences.getBoolean(Constants.DISABLEBLANKING, false);
      boolean disabledimming = mSharedPreferences.getBoolean(Constants.DISABLEDIMMING, false);
      if (disableblanking)
      {
         if (mWakeLock == null)
         {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (disabledimming)
            {
               mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            }
            else
            {
               mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
            }
         }
         if (mLoggerServiceManager.getLoggingState() == Constants.LOGGING && !mWakeLock.isHeld())
         {
            mWakeLock.acquire();
            Log.w(TAG, "Acquired lock to keep screen on!");
         }
      }
   }
}
