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
package nl.sogeti.android.gpstracker.actions.utils;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Work around based on input from the comment section of
 * <a href="http://code.google.com/p/android/issues/detail?can=2&q=6191&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars&id=6191">Issue 6191</a>
 * 
 * @version $Id$
 * @author rene (c) May 8, 2010, Sogeti B.V.
 */
public class ViewFlipper extends android.widget.ViewFlipper
{
   private static final String TAG = "OGT.ViewFlipper";

   public ViewFlipper(Context context)
   {
      super( context );
   }

   public ViewFlipper(Context context, AttributeSet attrs)
   {
      super( context, attrs );
   }
}
