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
package nl.sogeti.android.gpstracker.viewer;

import java.util.regex.Pattern;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.streaming.VoiceOver;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Controller for the settings dialog
 * 
 * @version $Id: ApplicationPreferenceActivity.java 1146 2011-11-05 11:36:51Z
 *          rcgroot $
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class ApplicationPreferenceActivity extends PreferenceActivity
{

   private EditTextPreference time;
   private EditTextPreference distance;
   private EditTextPreference implentWidth;

   private EditTextPreference streambroadcast_distance;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.layout.settings);

      ListPreference precision = (ListPreference) findPreference("precision");
      time = (EditTextPreference) findPreference("customprecisiontime");
      distance = (EditTextPreference) findPreference("customprecisiondistance");
      implentWidth = (EditTextPreference) findPreference("units_implement_width");
      streambroadcast_distance = (EditTextPreference) findPreference("streambroadcast_distance");

      setEnabledCustomValues(precision.getValue());
      precision.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            setEnabledCustomValues(newValue);
            return true;
         }
      });
      implentWidth.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            String fpExpr = "\\d+([,\\.]\\d+)?";
            return Pattern.matches(fpExpr, newValue.toString());
         }
      });
      streambroadcast_distance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            String fpExpr = "\\d+";
            boolean matches = Pattern.matches(fpExpr, newValue.toString());
            if (matches)
            {
               Editor editor = getPreferenceManager().getSharedPreferences().edit();
               double value = new UnitsI18n(ApplicationPreferenceActivity.this).conversionFromLocalToMeters(Integer.parseInt(newValue.toString()));
               editor.putFloat("streambroadcast_distance_meter", (float) value);
               editor.commit();
            }
            return matches;
         }
      });
   }

   private void setEnabledCustomValues(Object newValue)
   {
      boolean customPresicion = Integer.toString(Constants.LOGGING_CUSTOM).equals(newValue);
      time.setEnabled(customPresicion);
      distance.setEnabled(customPresicion);
   }
}
