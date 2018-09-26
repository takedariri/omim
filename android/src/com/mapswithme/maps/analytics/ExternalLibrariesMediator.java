package com.mapswithme.maps.analytics;

import android.app.Application;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.text.TextUtils;

import com.appsflyer.AppsFlyerLib;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.mapswithme.maps.*;
import com.mapswithme.maps.ads.Banner;
import com.mapswithme.util.CrashlyticsUtils;
import com.mapswithme.util.Utils;
import com.mapswithme.util.log.Logger;
import com.mapswithme.util.log.LoggerFactory;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import io.fabric.sdk.android.Fabric;

import java.io.IOException;

public class ExternalLibrariesMediator
{
  private boolean mCrashlyticsInitialized;

  private static final String TAG = ExternalLibrariesMediator.class.getSimpleName();
  private static final Logger LOGGER = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC);

  @NonNull
  private final Application mApplication;
  @NonNull
  private volatile EventLogger mEventLogger;

  public ExternalLibrariesMediator(@NonNull Application application)
  {
    mApplication = application;
    mEventLogger = new DefaultEventLogger(application);
  }

  public void initSensitiveDataToleranceLibraries()
  {
    initMoPub();
    initCrashlytics();
    initAppsFlyer();
  }

  public void initSensitiveDataStrictLibrariesAsync()
  {
    GetAdInfoTask getAdInfoTask = new GetAdInfoTask(this);
    getAdInfoTask.execute();
  }

  private void initSensitiveEventLogger()
  {
    if (com.mapswithme.util.concurrency.UiThread.isUiThread())
    {
      mEventLogger = new EventLoggerAggregator(mApplication);
      mEventLogger.initialize();
      return;
    }

    throw new IllegalStateException("Must be call from Ui thread");
  }

  private void initAppsFlyer()
  {
    // There is no necessary to use a conversion data listener for a while.
    // When it's needed keep in mind that the core can't be used from the mentioned listener unless
    // the AppsFlyer sdk initializes after core initialization.
    AppsFlyerLib.getInstance().init(PrivateVariables.appsFlyerKey(),
                                    null /* conversionDataListener */);
    AppsFlyerLib.getInstance().setDebugLog(BuildConfig.DEBUG);
    AppsFlyerLib.getInstance().startTracking(mApplication);
  }

  public void initCrashlytics()
  {
    if (!isCrashlyticsEnabled())
      return;

    if (isCrashlyticsInitialized())
      return;

    Crashlytics core = new Crashlytics
        .Builder()
        .core(new CrashlyticsCore.Builder().disabled(!isFabricEnabled()).build())
        .build();

    Fabric.with(mApplication, core, new CrashlyticsNdk());
    nativeInitCrashlytics();
    mCrashlyticsInitialized = true;
  }

  public boolean isCrashlyticsEnabled()
  {
    return !BuildConfig.FABRIC_API_KEY.startsWith("0000");
  }

  private boolean isFabricEnabled()
  {
    String prefKey = mApplication.getResources().getString(R.string.pref_opt_out_fabric_activated);
    return MwmApplication.prefs(mApplication).getBoolean(prefKey, true);
  }

  @NonNull
  public EventLogger getEventLogger()
  {
    return mEventLogger;
  }

  public boolean isCrashlyticsInitialized()
  {
    return mCrashlyticsInitialized;
  }

  public boolean setInstallationIdToCrashlytics()
  {
    if (!isCrashlyticsEnabled())
      return false;

    final String installationId = Utils.getInstallationId();
    // If installation id is not found this means id was not
    // generated by alohalytics yet and it is a first run.
    if (TextUtils.isEmpty(installationId))
      return false;

    Crashlytics.setString("AlohalyticsInstallationId", installationId);
    return true;
  }

  private void initMoPub()
  {
    SdkConfiguration sdkConfiguration = new SdkConfiguration
        .Builder(Framework.nativeMoPubInitializationBannerId())
        .build();

    MoPub.initializeSdk(mApplication, sdkConfiguration, null);
  }

  private static class GetAdInfoTask extends AsyncTask<Void, Void, Boolean>
  {
    @NonNull
    private final ExternalLibrariesMediator mMediator;

    private GetAdInfoTask(@NonNull ExternalLibrariesMediator mediator)
    {
      mMediator = mediator;
    }

    @Override
    protected Boolean doInBackground(Void... voids)
    {
      try
      {
        return isAdvertisingTrackingEnabled();
      }
      catch (GooglePlayServicesNotAvailableException | IOException | GooglePlayServicesRepairableException e)
      {
        LOGGER.e(TAG, "Failed to obtain advertising id: ", e);
        CrashlyticsUtils.logException(e);
        return false;
      }
    }

    private boolean isAdvertisingTrackingEnabled() throws GooglePlayServicesNotAvailableException,
                                                          IOException,
                                                          GooglePlayServicesRepairableException
    {
      AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(mMediator.mApplication);
      return info.isLimitAdTrackingEnabled();
    }

    @Override
    protected void onPostExecute(Boolean status)
    {
      super.onPostExecute(status);
      if (status != null && status)
        onEnabled();
      else
        onDisabled();
    }

    private void onDisabled()
    {
      Framework.disableAdProvider(Banner.Type.TYPE_RB);
    }

    private void onEnabled()
    {
      mMediator.initSensitiveEventLogger();
    }
  }

  @UiThread
  private static native void nativeInitCrashlytics();
}
