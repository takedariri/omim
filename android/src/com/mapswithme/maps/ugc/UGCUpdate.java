package com.mapswithme.maps.ugc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class UGCUpdate
{
  @Nullable
  private final UGC.Rating[] mRatings;
  @Nullable
  private String mText;
  private long mTimeMillis;
  @NonNull
  private final String mLocale;

  UGCUpdate(@Nullable UGC.Rating[] ratings, @Nullable String text, long timeMillis,
            @NonNull String locale)
  {
    mRatings = ratings;
    mText = text;
    mTimeMillis = timeMillis;
    mLocale = locale;
  }

  public void setText(@Nullable String text)
  {
    mText = text;
  }

  @Nullable
  public String getText()
  {
    return mText;
  }

  long getTimeMillis()
  {
    return mTimeMillis;
  }

  @NonNull
  List<UGC.Rating> getRatings()
  {
    if (mRatings == null)
      return new ArrayList<>();
    return Arrays.asList(mRatings);
  }
}
