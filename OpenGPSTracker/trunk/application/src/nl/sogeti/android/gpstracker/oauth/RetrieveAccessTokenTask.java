package nl.sogeti.android.gpstracker.oauth;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class RetrieveAccessTokenTask extends AsyncTask<Uri, Void, Void>
{
   private static final String TAG = "OGT.RetrieveAccessTokenTask";
   private OAuthProvider provider;
   private OAuthConsumer consumer;
   private SharedPreferences prefs;
   private String mTokenKey;
   private String mSecretKey;

   public RetrieveAccessTokenTask(Context context, OAuthConsumer consumer, OAuthProvider provider, SharedPreferences prefs, String tokenKey, String secretKey)
   {
      this.consumer = consumer;
      this.provider = provider;
      this.prefs = prefs;
      mTokenKey = tokenKey;
      mSecretKey = secretKey;
   }

   /**
    * Retrieve the oauth_verifier, and store the oauth and oauth_token_secret
    * for future API calls.
    */
   @Override
   protected Void doInBackground(Uri... params)
   {
      final Uri uri = params[0];
      final String oauth_verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

      try
      {
         provider.retrieveAccessToken(consumer, oauth_verifier);

         final Editor edit = prefs.edit();
         edit.putString(mTokenKey, consumer.getToken());
         edit.putString(mSecretKey, consumer.getTokenSecret());
         edit.commit();

         Log.i(TAG, "OAuth - Access Token Retrieved");

      }
      catch (Exception e)
      {
         Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
      }

      return null;
   }
}