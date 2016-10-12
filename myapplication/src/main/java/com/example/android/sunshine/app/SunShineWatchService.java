package com.example.android.sunshine.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SunShineWatchService extends CanvasWatchFaceService implements DataApi.DataListener,GoogleApiClient.ConnectionCallbacks{

    private GoogleApiClient googleApiClient;
    private String TAG ="SunShineWatchService";
    private Bitmap mBackgroundBitmap, mWeatherBitmap = null;
    private int highTemp;
    private int lowTemp;
    private Paint mTextPaint = null;
    private SunShineWatchServiceEngine sunShineWatchServiceEngine;


    @Override
    public Engine onCreateEngine() {

        Log.d(TAG, "onCreateEngine");
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        googleApiClient.connect();
        sunShineWatchServiceEngine= new SunShineWatchServiceEngine();
        return sunShineWatchServiceEngine;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG,"Google play services connected");
        PendingResult<Status> pendingResult= Wearable.DataApi.addListener(googleApiClient, this);
        pendingResult.setResultCallback(new ResultCallback() {
            @Override
            public void onResult(@NonNull Result result) {
                Log.d(TAG,"Added Data Listener API with result =" + result.getStatus().isSuccess());
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }
    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG,"onDataChanged");
        for(int i=0;i<dataEventBuffer.getCount();i++) {
            DataEvent dataEvent= dataEventBuffer.get(i);
            if(dataEvent.getType() == DataEvent.TYPE_CHANGED &&
                    dataEvent.getDataItem().getUri().getPath().equals("/wearable")) {
                DataItem dataItem = dataEvent.getDataItem();
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                Asset asset = dataMap.getAsset("profileImage");
                highTemp = (int)dataMap.getDouble("high");
                lowTemp = (int)dataMap.getDouble("low");
                updateWatchFaceBitmap(asset);
            }
        }
    }
    public void updateWatchFaceBitmap(Asset asset) {

        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        Wearable.DataApi.getFdForAsset(googleApiClient,asset).setResultCallback(new ResultCallbacks<DataApi.GetFdForAssetResult>() {
            @Override
            public void onSuccess(@NonNull DataApi.GetFdForAssetResult getFdForAssetResult) {
                InputStream inputStream= getFdForAssetResult.getInputStream();
                mWeatherBitmap = BitmapFactory.decodeStream(inputStream);
                Log.d(TAG,"mWeatherBitmap.height:"+mWeatherBitmap.getHeight()+" mWeatherBitmap.width:"+mWeatherBitmap.getWidth());
            }

            @Override
            public void onFailure(@NonNull Status status) {
                Log.w(TAG, "Requested an unknown Asset.");
                return;

            }
        });

    }

    private class SunShineWatchServiceEngine extends CanvasWatchFaceService.Engine  {

        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mTickAndCirclePaint;
        private int mWatchHandColor;
        private int mWatchHandHighlightColor;
        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;
        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;
        private final String TAG = "SunShineWatchServiceE";
        private boolean mAbient;
        private float mCenterX;
        private float mCenterY;

        private float mSecondHandLength;
        private float mMinuteHandLength;
        private float mHourHandLength;
        private Calendar mCalendar;




        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunShineWatchService.this)
                                .setAcceptsTapEvents(true)
                                .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                                .setShowSystemUiTime(false)
                                .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                                .build()
                            );



            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_cool_blue);


            /* Set defaults for colors */
            mWatchHandColor = Color.BLACK;
            mWatchHandHighlightColor = Color.RED;

            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);

            mCalendar = Calendar.getInstance();

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            // text color - #3D3D3D
            mTextPaint.setColor(Color.rgb(0,0,0));
            // text size in pixels
            Resources resources = getApplicationContext().getResources();
            float scale = resources.getDisplayMetrics().density;
            mTextPaint.setTextSize((int) (24 * scale));
            // text shadow
            mTextPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
             /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (float) (mCenterX * 0.875);
            mMinuteHandLength = (float) (mCenterX * 0.75);
            mHourHandLength = (float) (mCenterX * 0.5);


        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.v(TAG, "onDraw");

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            canvas.drawBitmap(mBackgroundBitmap, 0, 0,null);


            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);
            }
            if(!mAbient) {
                if (mWeatherBitmap != null) {
                    canvas.drawBitmap(mWeatherBitmap, mCenterX - (mWeatherBitmap.getWidth() / 2), 10, null);
                }
                if (highTemp != 0) {
                    String highTempStr = String.valueOf(highTemp) + (char) 0x00B0;
                    canvas.drawText(highTempStr, mCenterX - (mCenterX / 2), mCenterY + (mCenterY / 2), mTextPaint);
                }
                if (lowTemp != 0) {
                    String lowTempStr = String.valueOf(lowTemp) + (char) 0x00B0;
                    canvas.drawText(lowTempStr, mCenterX + 30, mCenterY + (mCenterY / 2), mTextPaint);
                }
            }
            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;


            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);

            }
            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mTickAndCirclePaint);

            /* Restore the canvas' original orientation. */
            canvas.restore();



            /* Draw every frame as long as we're visible and in interactive mode. */
            if ((isVisible()) && (!mAbient)) {
                invalidate();
            }
        }

        @Override
        // Called every minute in ambient mode.
        public void onTimeTick() {
            super.onTimeTick();
            Log.d(TAG,"onTimeTick");
            invalidate();
        }

        @Override
        // Called when the mode changes from Ambient to interactive and vice versa.
        public void onAmbientModeChanged(boolean inAmbientMode) {
            Log.d(TAG, "onAmbientModeChanged");
            super.onAmbientModeChanged(inAmbientMode);
            mAbient = inAmbientMode;
            updateResources(mAbient);

        }
        private void updateResources(boolean mAbient) {

            if(mAbient) {
                mBackgroundBitmap =getGrayscaleBitmap(mBackgroundBitmap);
                mWatchHandColor = Color.GRAY;
                mTextPaint.setColor(Color.GRAY);
            }
            else {
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_cool_blue);
                mWatchHandColor = Color.BLACK;
                mTextPaint.setColor(Color.rgb(0,0,0));

            }
            mHourPaint.setColor(mWatchHandColor);
            mMinutePaint.setColor(mWatchHandColor);

        }
        private Bitmap getGrayscaleBitmap(Bitmap src){
            int width = src.getWidth();
            int height = src.getHeight();

            Bitmap dest = Bitmap.createBitmap(width, height,
                    Bitmap.Config.RGB_565);

            Canvas canvas = new Canvas(dest);
            Paint paint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0); //value of 0 maps the color to gray-scale
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            paint.setColorFilter(filter);
            canvas.drawBitmap(src, 0, 0, paint);

            return dest;
        }


    }

}