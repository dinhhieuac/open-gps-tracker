package nl.sogeti.android.gpstracker.actions.utils;

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GraphCanvas extends View
{
   private static final String TAG = "GraphCanvas";
   private static final int TIMESPEEDGRAPH = 0;
   private static final int DISTANCESPEEDGRAPH = 1;
   private static final int TIMEALTITUDEGRAPH = 2;
   private static final int DISTANCEALTITUDEGRAPH = 3;
   private Uri mUri;
   private Bitmap mRenderBuffer;
   private Canvas mRenderCanvas;
   private Context mContext;
   private UnitsI18n mUnits;
   private int graphType = TIMESPEEDGRAPH;
   private long mEndTime;
   private long mStartTime;
   
   public GraphCanvas(Context context)
   {
      super(context);
      mContext = context;
   }   
   public GraphCanvas( Context context, AttributeSet attrs )
   {
      super(context, attrs);
      mContext = context;
   }
   public GraphCanvas( Context context, AttributeSet attrs, int defStyle )
   {
      super(context, attrs, defStyle);
      mContext = context;
   }
   
   @Override
   protected void onSizeChanged( int w, int h, int oldw, int oldh )
   {
      super.onSizeChanged( w, h, oldw, oldh );
      
      if( mRenderBuffer == null || mRenderBuffer.getWidth() != w || mRenderBuffer.getHeight() != h )
      {
         mRenderBuffer = Bitmap.createBitmap( w, h, Config.ARGB_8888 );
         mRenderCanvas = new Canvas( mRenderBuffer );
      }
      else
      {
         mRenderBuffer.eraseColor( Color.TRANSPARENT );
      }
   }

   @Override
   protected void onDraw( Canvas canvas )
   {
      super.onDraw(canvas);
      switch( graphType )
      {
         case( TIMESPEEDGRAPH ):
            //drawSpeedGraphOnCanvas( mRenderCanvas, new String[] { Waypoints.TIME, Waypoints.LATITUDE } );
            drawSpeedGraphOnCanvas( mRenderCanvas, new String[] { Waypoints.TIME, Waypoints.SPEED } );
            break;
         case( DISTANCESPEEDGRAPH ):
            break;
         case( TIMEALTITUDEGRAPH ):
            break;
         case( DISTANCEALTITUDEGRAPH ):
            break;
         default:
            drawSpeedGraphOnCanvas( mRenderCanvas, new String[] { Waypoints.TIME, Waypoints.SPEED } );
            break;
      }
      canvas.drawBitmap( mRenderBuffer, 0, 0, null );
   }
   
   public void setData( Uri uri, long startTime, long endTime, UnitsI18n units )
   {
      mUri = uri;
      mStartTime = startTime;
      mEndTime = endTime;
      mUnits = units;
   }
   
   private void drawSpeedGraphOnCanvas( Canvas canvas, String[] params )
   {
      ContentResolver resolver = mContext.getApplicationContext().getContentResolver();
      Uri segmentsUri = Uri.withAppendedPath( mUri, "/segments" );
      Uri waypointsUri = null;
      Cursor segments = null;
      Cursor waypoints = null;
      int width = canvas.getWidth()-5;
      int height = canvas.getHeight()-10;
      long duration = mEndTime - mStartTime;
      double[][] values ;
      int[][] valueDepth;
      double maxValue = 1;
      double minValue = 0;
      try 
      {
         segments = resolver.query( 
               segmentsUri, 
               new String[]{ Segments._ID }, 
               null, null, null );
         values = new double[segments.getCount()][width];
         valueDepth = new int[segments.getCount()][width];
         if( segments.moveToFirst() )
         {
            do
            {
               int p = segments.getPosition();
               long segmentId = segments.getLong( 0 );
               waypointsUri = Uri.withAppendedPath( segmentsUri, segmentId+"/waypoints" );
               try
               {
                  waypoints = resolver.query( 
                     waypointsUri, 
                     params, 
                     null, null, null );
                  if( waypoints.moveToFirst() )
                  {
                     do 
                     {
                        long time = waypoints.getLong( 0 );
                        double value = waypoints.getDouble( 1 );
                        if( value > 1 )
                        {
                           int i = (int) ((time-mStartTime)*(width-1) / duration);
                           valueDepth[p][i]++;
                           values[p][i] = values[p][i]+((value-values[p][i])/valueDepth[p][i]);
                        }
                     }
                     while( waypoints.moveToNext() );
                  }
               }
               finally
               {
                  if( waypoints != null )
                  {
                     waypoints.close();
                  }
               }
            }
            while( segments.moveToNext() );
         }
      }
      finally
      {
         if( segments != null )
         {
            segments.close();
         }
      }
      for( int p=0;p<values.length;p++)
      {
         for( int x=0;x<values[p].length;x++)
         {
            if( valueDepth[p][x] > 0 )
            {
               if( values[p][x] > maxValue )
               {
                  maxValue = values[p][x];
               }            
            }
         }
      }
      minValue = mUnits.conversionFromMetersPerSecond( minValue );
      maxValue = mUnits.conversionFromMetersPerSecond( maxValue );
      int minAxis = 4 * (int)(minValue / 4);
      int maxAxis = 4 + 4 * (int)(maxValue / 4);
//      Log.d( TAG, String.format( "Found a scope of (%.2f,%.2f) and will axis at (%d,%d)", minValue, maxValue, minAxis, maxAxis ) );
      
      for( int p=0;p<values.length;p++)
      {
         for( int x=0;x<values[p].length;x++)
         {
            if( valueDepth[p][x] > 0 )
            {
               values[p][x] = mUnits.conversionFromMetersPerSecond( values[p][x] );
            }
         }
      }
      
      Paint grey = new Paint();
      grey.setColor( Color.LTGRAY );
      grey.setStrokeWidth( 1 );
      // Horizontals
      grey.setPathEffect( new DashPathEffect( new float[]{2,4}, 0 ) );
      canvas.drawLine( 5, 5           , 5+width, 5           , grey );
      canvas.drawLine( 5, 5+height/4  , 5+width, 5+height/4  , grey );
      canvas.drawLine( 5, 5+height/2  , 5+width, 5+height/2  , grey );
      canvas.drawLine( 5, 5+height/4*3, 5+width, 5+height/4*3, grey );
      // Verticals
      canvas.drawLine( 5+width/4  , 5, 5+width/4  , 5+height, grey );
      canvas.drawLine( 5+width/2  , 5, 5+width/2  , 5+height, grey );
      canvas.drawLine( 5+width/4*3, 5, 5+width/4*3, 5+height, grey );
      canvas.drawLine( 5+width-1   , 5, 5+width-1 , 5+height, grey );
      
      Paint routePaint = new Paint();
      routePaint.setPathEffect( new CornerPathEffect( 8 ) );
      routePaint.setStyle( Paint.Style.STROKE );
      routePaint.setStrokeWidth( 4 );
      routePaint.setAntiAlias( true );
      routePaint.setColor(Color.GREEN);
      Path mPath;
      mPath = new Path();
      for( int p=0;p<values.length;p++)
      {
         int start = 0;
         while( valueDepth[p][start] == 0 && start < values[p].length-1 )
         {
            start++;
         }
         mPath.moveTo( (float)start+5, 5f+ (float) ( height - ( ( values[p][start]-minAxis )*height ) / ( maxAxis-minAxis ) ) );
         for( int x=start;x<values[p].length;x++)
         {
            double y =   height - ( ( values[p][x]-minAxis )*height ) / ( maxAxis-minAxis ) ;
            
            if( valueDepth[p][x] > 0 )
            {
   //            Log.d( TAG, String.format( "Point (%d,%.1f) will be (%d,%.1f) on cavas height %d",x,values[x],x,y,height) );
               mPath.lineTo( (float)x+5, (float) y+5 );
            }
         }
      }
      canvas.drawPath( mPath, routePaint );
      
      grey = new Paint();
      grey.setColor( Color.DKGRAY );
      grey.setStrokeWidth( 2 );
      canvas.drawLine( 5, 5       , 5      , 5+height, grey );
      canvas.drawLine( 5, 5+height, 5+width, 5+height, grey );
      
      
      Paint white = new Paint();
      white.setColor( Color.WHITE );
      white.setAntiAlias( true );
      canvas.drawText( String.format( "%d %s", minAxis, mUnits.getSpeedUnit() )  , 8,  height, white );
      canvas.drawText( String.format( "%d %s", (maxAxis+minAxis)/2, mUnits.getSpeedUnit() ) , 8,  5+height/2, white );
      canvas.drawText( String.format( "%d %s", maxAxis, mUnits.getSpeedUnit() ), 8,  15, white );
   }

}
