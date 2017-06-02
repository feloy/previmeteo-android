/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.elol.meteo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class WeatherWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        Paint mBackgroundPaint, mWatchPaint, mWatchPaint2, mHourNowPaint;
        Paint mHandPaintHr, mHandPaintMin, mHandPaintSec;
        Paint mMinPaint, m5MinsPaint;
        Paint mCityPaint;
        Paint mTempesPaint;
        Paint mPictosPaint;
        Paint mInfosPaint;

        Bitmap[] mPictoD;
        Bitmap[] mPictoN;

        boolean mAmbient;
        Time mTime;

        boolean mIsRound = false;
        int mChinSize = 0;

        Point[] rectPositions;

        GoogleApiClient wearClient;
        Boolean needUpdateSent = false;

        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();
            mChinSize = insets.getSystemWindowInsetBottom();
            Log.d("Meteo Wear", "onApplyWindowInsets chinsize="+mChinSize);
        }

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setHotwordIndicatorGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL)
                    .setStatusBarGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL)
                    .build());

            Resources resources = WeatherWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

            mWatchPaint = new Paint();
            mWatchPaint.setColor(resources.getColor(R.color.analog_watch_bg));
            mWatchPaint.setAntiAlias(true);

            mWatchPaint2 = new Paint();
            mWatchPaint2.setColor(resources.getColor(R.color.analog_watch_bg));
            mWatchPaint2.setAntiAlias(true);
            mWatchPaint2.setStyle(Paint.Style.STROKE);

            mHourNowPaint = new Paint();
            mHourNowPaint.setColor(resources.getColor(R.color.hour_now));
            mHourNowPaint.setStrokeWidth(resources.getDimension(R.dimen.hour_now_stroke));
            mHourNowPaint.setAntiAlias(true);
            mHourNowPaint.setStrokeCap(Paint.Cap.ROUND);

            mHandPaintHr = new Paint();
            mHandPaintHr.setColor(resources.getColor(R.color.analog_hand_hour));
            mHandPaintHr.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_hour_stroke));
            mHandPaintHr.setAntiAlias(true);
            mHandPaintHr.setStrokeCap(Paint.Cap.ROUND);

            mHandPaintMin = new Paint();
            mHandPaintMin.setColor(resources.getColor(R.color.analog_hand_minute));
            mHandPaintMin.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_minute_stroke));
            mHandPaintMin.setAntiAlias(true);
            mHandPaintMin.setStrokeCap(Paint.Cap.ROUND);

            mHandPaintSec = new Paint();
            mHandPaintSec.setColor(resources.getColor(R.color.analog_hand_second));
            mHandPaintSec.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_second_stroke));
            mHandPaintSec.setAntiAlias(true);
            mHandPaintSec.setStrokeCap(Paint.Cap.ROUND);

            mMinPaint = new Paint();
            mMinPaint.setColor(resources.getColor(R.color.min_paint));
            mMinPaint.setStrokeWidth(resources.getDimension(R.dimen.min_stroke));
            mMinPaint.setAntiAlias(true);
            mMinPaint.setStrokeCap(Paint.Cap.ROUND);

            m5MinsPaint = new Paint();
            m5MinsPaint.setColor(resources.getColor(R.color.mins5_paint));
            m5MinsPaint.setStrokeWidth(resources.getDimension(R.dimen.mins5_stroke));
            m5MinsPaint.setAntiAlias(true);
            m5MinsPaint.setStrokeCap(Paint.Cap.ROUND);

            mCityPaint = new Paint ();
            mCityPaint.setColor(resources.getColor(R.color.city));
            mCityPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.cityTS));
            mCityPaint.setTextAlign(Paint.Align.CENTER);
            mCityPaint.setAntiAlias(true);

            mInfosPaint = new Paint ();
            mInfosPaint.setColor(resources.getColor(R.color.infos));
            mInfosPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.infosTS));
            mInfosPaint.setTextAlign(Paint.Align.CENTER);
            mInfosPaint.setAntiAlias(true);
            mInfosPaint.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/weathericons.ttf"));

            mTempesPaint = new Paint ();
            mTempesPaint.setColor(resources.getColor(R.color.tempes));
            mTempesPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.tempesTS));
            mTempesPaint.setTextAlign(Paint.Align.CENTER);
            mTempesPaint.setAntiAlias(true);
            mTempesPaint.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/weathericons.ttf"));

            mPictosPaint = new Paint();

            mPictoD = new Bitmap[13];
            mPictoN = new Bitmap[13];
            for (int i=0; i<13; i++) {
                String strD = "icon"+i+"_24";
                String strN = "icon"+i+"n_24";
                int res = getResources().getIdentifier(strD, "drawable", getPackageName());
                mPictoD[i] = BitmapFactory.decodeResource(getResources(), res);
                res = getResources().getIdentifier(strN, "drawable", getPackageName());
                mPictoN[i] = BitmapFactory.decodeResource(getResources(), res);
            }
            mTime = new Time();

            rectPositions = new Point[] {
                    new Point(2, 0),
                    new Point(3, 0),
                    new Point(3, 1),
                    new Point(3, 2),
                    new Point(3, 3),
                    new Point(2, 3),
                    new Point(1, 3),
                    new Point(0, 3),
                    new Point(0, 2),
                    new Point(0, 1),
                    new Point(0, 0),
                    new Point(1, 0)
            };

            wearClient = new GoogleApiClient.Builder(WeatherWatchFace.this)
                    .addApi(Wearable.API)
                    .build();
            if (wearClient != null)
                wearClient.connect();
            needUpdateSent = false;
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (wearClient != null)
                wearClient.disconnect();
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaintHr.setAntiAlias(!inAmbientMode);
                    mHandPaintMin.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            if (mIsRound)
                onDrawRound(canvas, bounds);
            else
                onDrawRect(canvas, bounds);
        }

        private void onDrawRound (Canvas canvas, Rect bounds) {
            WeatherData wd = WeatherData.load(WeatherWatchFace.this);

            int width = bounds.width();
            int height = bounds.height();

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            // Draw the background.
            if (!mAmbient) {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
                canvas.drawCircle(centerX, centerY, canvas.getWidth() / 4, mWatchPaint);
                canvas.drawCircle(centerX, centerY, canvas.getWidth() / 4 + 3, mWatchPaint2);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mWatchPaint);
            }

            // Draw quadrant
            for (int i = 0; i < 60; i++) {
                Boolean is5mins = (i % 5 == 0);
                int len = is5mins ? 10 : 5;
                float angle = i / 30f * (float) Math.PI;
                float x1 = (float) Math.sin(angle) * width / 4;
                float y1 = (float) -Math.cos(angle) * height / 4;
                float x2 = (float) Math.sin(angle) * (width / 4 - len);
                float y2 = (float) -Math.cos(angle) * (height / 4 - len);
                canvas.drawLine(centerX + x1, centerY + y1, centerX + x2, centerY + y2,
                        is5mins ? m5MinsPaint : mMinPaint);
            }

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength1 = centerX - width/4 - 5;
            float secLength2 = centerX - width/4 - 10;
            float minLength = centerX - width/4 - 20;
            float hrLength = centerX - width/4 - 40;

            // Hour / minutes hands
            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mHandPaintMin);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHandPaintHr);

            if (!mAmbient) {
                // Seconds hand
                float secX1 = (float) Math.sin(secRot) * secLength1;
                float secY1 = (float) -Math.cos(secRot) * secLength1;
                float secX2 = (float) Math.sin(secRot) * secLength2;
                float secY2 = (float) -Math.cos(secRot) * secLength2;
                canvas.drawLine(centerX + secX1, centerY + secY1, centerX + secX2, centerY + secY2, mHandPaintSec);
            }

            if (!mAmbient && wd.mCityShortname.length() > 0) {
                // City name
                canvas.drawText(wd.mCityShortname, centerX, centerY + 20, mCityPaint);

                // Nebu
                int yNebu = 0, yPrecip = 0;
                if (wd.mNebu > 0 && wd.mPrecip > 0) {
                    yNebu = 40;
                    yPrecip = 60;
                } else if (wd.mNebu > 0) {
                    yNebu = 50;
                } else if (wd.mPrecip > 0) {
                    yPrecip = 50;
                }

                if (wd.mNebu > 0) {
                    canvas.drawText(WeatherIcon.WI_CLOUD + " " + wd.mNebu + " %",
                            centerX,
                            centerY + yNebu, mInfosPaint);
                }

                // Precip
                if (wd.mPrecip > 0) {
                    int precip = (int) Math.ceil((float) wd.mPrecip / 10);
                    canvas.drawText(WeatherIcon.WI_UMBRELLA + " " + precip + " mm",
                            centerX,
                            centerY + yPrecip, mInfosPaint);
                }

                // daylight
                Log.d("Météo Wear", wd.mSunrise + " " + wd.mSunset);
                Date dSunrise, dSunset, dSunrise1;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                Calendar cRise = Calendar.getInstance();
                Calendar cSet = Calendar.getInstance();
                Calendar cRise1 = Calendar.getInstance();
                try {
                    dSunrise = sdf.parse(wd.mSunrise);
                    dSunset = sdf.parse(wd.mSunset);
                    dSunrise1 = sdf.parse(wd.mSunrise1);
                } catch (Exception e) {
                    dSunrise = new Date();
                    dSunset = new Date();
                    dSunrise1 = new Date();
                }
                cRise.setTime(dSunrise);
                cSet.setTime(dSunset);
                cRise1.setTime(dSunrise1);
                int riseHour = cRise.get(Calendar.HOUR_OF_DAY);
                int setHour = cSet.get(Calendar.HOUR_OF_DAY);
                int rise1Hour = cRise1.get(Calendar.HOUR_OF_DAY);

                int descent = (int)mTempesPaint.descent();

                // 12 hours tempes / pictos
                Boolean started = false;
                Boolean complete = false;
                for (int i = 0; i < 12; i++) {

                    int hour = wd.mFirstHour + i;
                    Boolean tomorrow = (hour > 23);
                    hour = hour % 24;
                    int hour12 = hour % 12;

                    if (!started && hour >= mTime.hour) {
                        started = true;
                        if (i == 0) {
                            complete = true;
                        }
                    }
                    if (!started)
                        continue;

                    Boolean isDay;
                    if (tomorrow) {
                        isDay = hour > rise1Hour;
                    } else if (i == 0 && (hour == riseHour || hour == setHour)) {
                        if (hour == riseHour) {
                            isDay = mTime.minute >= cRise.get(Calendar.MINUTE);
                        } else { // hour == setHour
                            isDay = mTime.minute < cSet.get(Calendar.MINUTE);
                        }
                    } else {
                        isDay = hour > riseHour && hour <= setHour;
                    }
                    if (i == 0) {
                        Log.d("Météo Wear", riseHour + " " + hour + " " + setHour);
                    }
                    float angle = ((wd.mFirstHour + i) % 12) / 6f * (float) Math.PI + 1 / 12f * (float) Math.PI;

                    float x = centerX + (float) Math.sin(angle) * (width / 2 - 60);
                    float y = centerY + descent + (float) -Math.cos(angle) * (height / 2 - 60);
                    if (mChinSize > 0) {
                        if (hour12 == 5) {
                            x += 15;
                            y -= 5;
                        } else if (hour12 == 6) {
                            x -= 15;
                            y -= 5;
                        }
                    }
                    mTempesPaint.setAlpha(255 - 10 * i);
                    canvas.drawText(wd.mTempes.get(i) + WeatherIcon.WI_CELSIUS, x, y, mTempesPaint);

                    float xPicto = centerX + (float) Math.sin(angle) * (width / 2 - 25);
                    float yPicto = centerY + (float) -Math.cos(angle) * (height / 2 - 25);

                    if (mChinSize > 0 && (hour12 == 5 || hour12 == 6)) {
                        yPicto -= 20;
                        if (hour12 == 5)
                            xPicto -= 15;
                        else
                            xPicto += 15;
                    }

                    final Bitmap picto;
                    if (isDay) {
                        picto = mPictoD[wd.mPictos.get(i)];
                    } else {
                        picto = mPictoN[wd.mPictos.get(i)];
                    }
                    canvas.drawBitmap(picto, xPicto - picto.getWidth() / 2, yPicto - picto.getHeight() / 2, mPictosPaint);
                }

                if (complete) {
                    // Draw Separator
                    float hrRot1 = (mTime.hour / 6f) * (float) Math.PI;
                    float x1 = (float) Math.sin(hrRot1) * (width / 2 - 10);
                    float y1 = (float) -Math.cos(hrRot1) * (height / 2 - 10);
                    float x2 = (float) Math.sin(hrRot1) * (width / 4 + 15);
                    float y2 = (float) -Math.cos(hrRot1) * (height / 4 + 15);
                    canvas.drawLine(centerX + x1, centerY + y1, centerX + x2, centerY + y2,
                            mHourNowPaint);
                }

                if (!complete) {
                    sendNeedUpdate(wd);
                } else {
                    needUpdateSent = false;
                }

            } else if (!mAmbient) {
                canvas.drawText("Sélectionnez un lieu", centerX, centerY + 20, mCityPaint);
            }
            Log.d("Meteo Wear", "Drawn");
        }

        private void onDrawRect (Canvas canvas, Rect bounds) {
            WeatherData wd = WeatherData.load(WeatherWatchFace.this);

            int width = bounds.width();
            int height = bounds.height();

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            // Draw the background.
            int borderWidth = 70;
            if (!mAmbient) {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
                canvas.drawRect(borderWidth, borderWidth,
                        canvas.getWidth()-borderWidth, canvas.getHeight()-borderWidth, mWatchPaint);
                canvas.drawRect(borderWidth-3, borderWidth-3,
                        canvas.getWidth()-borderWidth+3, canvas.getHeight()-borderWidth+3, mWatchPaint2);

            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mWatchPaint);
            }

            // Draw quadrant
            for (int i = 0; i < 60; i++) {
                Boolean is5mins = (i % 5 == 0);
                int len = is5mins ? 10 : 5;
                float angle = i / 30f * (float) Math.PI;
                int r1 = width/2 - len - borderWidth;
                Point p1 = angleInRect (r1, angle);
                int r2 = width/2 - borderWidth;
                Point p2 = angleInRect (r2, angle);
                canvas.drawLine(centerX + p1.x, centerY + p1.y, centerX + p2.x, centerY + p2.y,
                        is5mins ? m5MinsPaint : mMinPaint);
            }

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength1 = width/2 - borderWidth - 5;
            float secLength2 = secLength1 - 5;
            float minLength = secLength2 - 15;
            float hrLength = width/4 - borderWidth/2 - 10;

            // Hour / minutes hands
            Point min = angleInRect((int)minLength, minRot);
            canvas.drawLine(centerX, centerY, centerX + min.x, centerY + min.y, mHandPaintMin);

            Point hr = angleInRect((int)hrLength, hrRot);
            canvas.drawLine(centerX, centerY, centerX + hr.x, centerY + hr.y, mHandPaintHr);

            if (!mAmbient) {
                // Seconds hand
                Point sec1 = angleInRect((int)secLength1, secRot);
                Point sec2 = angleInRect((int)secLength2, secRot);
                canvas.drawLine(centerX + sec1.x, centerY + sec1.y, centerX + sec2.x, centerY + sec2.y, mHandPaintSec);
            }

            if (!mAmbient && wd.mCityShortname.length() > 0) {
                // City name
                canvas.drawText(wd.mCityShortname, centerX, height - borderWidth - 20, mCityPaint);

                int xNebu = 0, xPrecip = 0;
                if (wd.mNebu > 0 && wd.mPrecip > 0) {
                    xNebu = (int)centerX - width/4 + borderWidth/2;
                    xPrecip = (int)centerX + width/4 - borderWidth/2;
                } else if (wd.mNebu > 0) {
                    xNebu = (int)centerX;
                } else if (wd.mPrecip > 0) {
                    xPrecip = (int)centerX;
                }
                // Nebu
                if (wd.mNebu > 0) {
                    canvas.drawText(WeatherIcon.WI_CLOUD + " " + wd.mNebu + " %",
                            xNebu,
                            centerY + 20, mInfosPaint);
                }

                // Precip
                if (wd.mPrecip > 0) {
                    int precip = (int) Math.ceil((float) wd.mPrecip / 10);
                    canvas.drawText(WeatherIcon.WI_UMBRELLA + " " + precip + " mm",
                            xPrecip,
                            centerY + 20, mInfosPaint);
                }

                // daylight
                Log.d("Météo Wear", wd.mSunrise + " " + wd.mSunset);
                Date dSunrise, dSunset, dSunrise1;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                Calendar cRise = Calendar.getInstance();
                Calendar cSet = Calendar.getInstance();
                Calendar cRise1 = Calendar.getInstance();
                try {
                    dSunrise = sdf.parse(wd.mSunrise);
                    dSunset = sdf.parse(wd.mSunset);
                    dSunrise1 = sdf.parse(wd.mSunrise1);
                } catch (Exception e) {
                    dSunrise = new Date();
                    dSunset = new Date();
                    dSunrise1 = new Date();
                }
                cRise.setTime(dSunrise);
                cSet.setTime(dSunset);
                cRise1.setTime(dSunrise1);
                int riseHour = cRise.get(Calendar.HOUR_OF_DAY);
                int setHour = cSet.get(Calendar.HOUR_OF_DAY);
                int rise1Hour = cRise1.get(Calendar.HOUR_OF_DAY);

                int descent = (int)mTempesPaint.descent();

                // 12 hours tempes / pictos
                Boolean started = false;
                Boolean complete = false;
                for (int i = 0; i < 12; i++) {

                    int hour = wd.mFirstHour + i;
                    Boolean tomorrow = (hour > 23);
                    hour = hour % 24;
                    int hour12 = hour % 12;

                    if (!started && hour >= mTime.hour) {
                        started = true;
                        if (i == 0) {
                            complete = true;
                        }
                    }
                    if (!started)
                        continue;

                    Boolean isDay;
                    if (tomorrow) {
                        isDay = hour > rise1Hour;
                    } else if (i == 0 && (hour == riseHour || hour == setHour)) {
                        if (hour == riseHour) {
                            isDay = mTime.minute >= cRise.get(Calendar.MINUTE);
                        } else { // hour == setHour
                            isDay = mTime.minute < cSet.get(Calendar.MINUTE);
                        }
                    } else {
                        isDay = hour > riseHour && hour <= setHour;
                    }
                    if (i == 0) {
                        Log.d("Météo Wear", riseHour + " " + hour + " " + setHour);
                    }
                    float angle = ((wd.mFirstHour + i) % 12) / 6f * (float) Math.PI + 1 / 12f * (float) Math.PI;

                    Point p = angleInRect (width/2 - borderWidth + 20, angle);
                    mTempesPaint.setAlpha(255 - 10 * i);
                    canvas.drawText(wd.mTempes.get(i) + WeatherIcon.WI_CELSIUS, centerX + p.x, centerY + descent + p.y, mTempesPaint);

                    Point pPicto = angleInRect(width/2 - 20, angle);
                    final Bitmap picto;
                    if (isDay) {
                        picto = mPictoD[wd.mPictos.get(i)];
                    } else {
                        picto = mPictoN[wd.mPictos.get(i)];
                    }
                    canvas.drawBitmap(picto,
                            centerX+pPicto.x - picto.getWidth() / 2,
                            centerY + pPicto.y - picto.getHeight() / 2,
                            mPictosPaint);
                }

                if (complete) {
                    // Draw Separator
                    float hrRot1 = (mTime.hour / 6f) * (float) Math.PI;
                    Point p1 = angleInRect(width/2 - 10, hrRot1);
                    Point p2 = angleInRect(width/2 - borderWidth + 15, hrRot1);
                    canvas.drawLine(centerX + p1.x, centerY + p1.y, centerX + p2.x, centerY + p2.y,
                            mHourNowPaint);
                }

                if (!complete) {
                    sendNeedUpdate(wd);
                } else {
                    needUpdateSent = false;
                }

            } else if (!mAmbient) {
                canvas.drawText("Sélectionnez un lieu", centerX, centerY + 20, mCityPaint);
            }
            Log.d("Meteo Wear", "Drawn");
        }

        private Point angleInRect(int r1, float angle) {
            float x1 = (float) Math.sin(angle) * r1;
            float y1 = (float) -Math.cos(angle) * r1;
            if (Math.abs(x1) > Math.abs(y1)) {
                y1 = y1/x1*r1 * x1 / Math.abs(x1);
                x1 = r1 * x1 / Math.abs(x1);
            } else {
                x1 = x1/y1*r1 * y1 / Math.abs(y1);
                y1 = r1 * y1 / Math.abs(y1);
            }
            return new Point ((int)x1, (int)y1);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WeatherWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void sendNeedUpdate (WeatherData wd) {
            if (!needUpdateSent && wearClient != null && wearClient.isConnected() && !"".equals (wd.mSenderNodeId)) {
                Log.d("Meteo Wear", "Send need-update to "+wd.mSenderNodeId);
                Wearable.MessageApi.sendMessage(wearClient, wd.mSenderNodeId, "need-update", null);
                needUpdateSent = true;
            }
        }
    }
}
