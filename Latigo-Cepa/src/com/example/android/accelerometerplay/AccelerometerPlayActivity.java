/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.accelerometerplay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.BitmapFactory.Options;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;


@SuppressLint("DrawAllocation")
public class AccelerometerPlayActivity extends Activity implements OnCompletionListener{

    private LatigoCepaView mSimulationView;
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private WakeLock mWakeLock;
    
    private boolean isLatigoDown;
    private boolean isLatigoUp;
	private MediaPlayer playerLatigoCepa;
	private MediaPlayer playerLatigo_1;
	private MediaPlayer playerLatigo_2;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playerLatigo_1 = getMediaPlayer("Latigo_01.WAV");
        playerLatigoCepa = getMediaPlayer("latigoCepa.aac");
        playerLatigo_2= getMediaPlayer("Latigo_01.WAV");
        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get an instance of the PowerManager
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Get an instance of the WindowManager
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();

        // Create a bright wake lock
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass()
                .getName());

        // instantiate our simulation view and set it as the activity's content
        mSimulationView = new LatigoCepaView(this);
        setContentView(mSimulationView);
    }

	@Override
    protected void onResume() {
        super.onResume();
        /*
         * when the activity is resumed, we acquire a wake-lock so that the
         * screen stays on, since the user will likely not be fiddling with the
         * screen or buttons.
         */
        mWakeLock.acquire();

        // Start the simulation
        mSimulationView.startSimulation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*
         * When the activity is paused, we make sure to stop the simulation,
         * release our sensor resources and wake locks
         */

        // Stop the simulation
        mSimulationView.stopSimulation();

        // and release our wake-lock
        mWakeLock.release();
    }

    class LatigoCepaView extends View implements SensorEventListener {
        private static final float LATIGO_DOWN = 5;
		private static final float LATIGO_UP = -5;

        private Sensor mAccelerometer;

        //private float mXDpi;
        //private float mYDpi;
        private Bitmap bullbasaur;
        private float mSensorX;
        private float mSensorY;
        private long mSensorTimeStamp;
        private long mCpuTimeStamp;
		private boolean playSoundAfterLatigoCepa = false;
		private int countRepeatLatigoFinal = 0;
		private static final int REPETICIONES_LATIGO_FINAL = 3;


        public void startSimulation() {
            /*
             * It is not necessary to get accelerometer events at a very high
             * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
             * automatic low-pass filter, which "extracts" the gravity component
             * of the acceleration. As an added benefit, we use less power and
             * CPU resources.
             */
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        public void stopSimulation() {
            mSensorManager.unregisterListener(this);
        }

        public LatigoCepaView(Context context) {
            super(context);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            //mXDpi = metrics.xdpi;
            //mYDpi = metrics.ydpi;
         
            Options opts = new Options();
            //opts.inDither = true;
            //opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inScaled = true;
            bullbasaur = BitmapFactory.decodeResource(getResources(), R.drawable.bullbasaur_background, opts);           

        }

        @SuppressLint("NewApi")
		@Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            /*
             * record the accelerometer data, the event's timestamp as well as
             * the current time. The latter is needed so we can calculate the
             * "present" time during rendering. In this application, we need to
             * take into account how the screen is rotated with respect to the
             * sensors (which always return data in a coordinate space aligned
             * to with the screen in its native orientation).
             */

            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_0:
                    mSensorX = event.values[0];
                    mSensorY = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    mSensorX = -event.values[1];
                    mSensorY = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    mSensorX = -event.values[0];
                    mSensorY = -event.values[1];
                    break;
                case Surface.ROTATION_270:
                    mSensorX = event.values[1];
                    mSensorY = -event.values[0];
                    break;
            }

            mSensorTimeStamp = event.timestamp;
            mCpuTimeStamp = System.nanoTime();
        }

        @SuppressLint("DrawAllocation")
		@Override
        protected void onDraw(Canvas canvas) {

            /*
             * draw the background
             */
            Rect dest = new Rect(0, 0, getWidth(), getHeight());
            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            canvas.drawBitmap(bullbasaur, null, dest, paint);
            /*
             * compute the new position of our object, based on accelerometer
             * data and present time.
             */            
   	         //final ParticleSystem particleSystem = mParticleSystem;
	         //final long now = mSensorTimeStamp + (System.nanoTime() - mCpuTimeStamp);
	         //final float sx = mSensorX;
	         final float sy = mSensorY;
	           
	         Boolean fustigarConLatigo = isFustigarConLatigo(sy);
	            
	         if(fustigarConLatigo){
	         	isLatigoDown = false;
	          	isLatigoUp = false;
	          	playerLatigo_1.start();
	           	playerLatigoCepa.start();
	           	//playerLatigo_2.start();    
	           	playSoundAfterLatigoCepa = true;
	           	
	         }
	         
	      	if(!playerLatigoCepa.isPlaying() && playSoundAfterLatigoCepa && 
	      			countRepeatLatigoFinal <= REPETICIONES_LATIGO_FINAL){
	      		if(!playerLatigo_2.isPlaying()){
	      			playerLatigo_2.start();
	      			countRepeatLatigoFinal++;
	      		}
	      		if(countRepeatLatigoFinal == REPETICIONES_LATIGO_FINAL){
	      			playSoundAfterLatigoCepa = false;
	      			countRepeatLatigoFinal= 0;
	      		}
           	}

	      	
            


            // and make sure to redraw asap
            invalidate();
        }
        
        /**
         * Decide si va a haber latigo esta noche y no del que tu querias.
         * 
         * @param sy
         * @return
         */
        private Boolean isFustigarConLatigo(float sy) {
        	Boolean result = false;
        	
        	if(sy>LATIGO_DOWN){
        		isLatigoDown = true;
        	}
        	
        	if(sy < LATIGO_UP){
        		isLatigoUp = true;
        	}
        	
        	if(isLatigoDown && isLatigoUp){
        		result = true;
        	}
        	
        	return result;
		}
        
        

		@Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
			//not implemented
        }
    }

	@Override
	public void onCompletion(MediaPlayer arg0) {
		playerLatigo_1.stop();
		playerLatigoCepa.stop();
		//playerLatigo_2.stop();
	}
	
    /**
     * Inicializa el fustigador
     * @return el reproductor del fustigador
     */
    private MediaPlayer getMediaPlayer(final String fileName) {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AssetManager manager = this.getAssets();
        MediaPlayer player = new MediaPlayer();
        try {
			AssetFileDescriptor descriptor = manager.openFd(fileName);
			player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
					descriptor.getLength());
			player.prepare();
		} catch (Exception e) {
			throw new InternalError();
		}
        return player;
		
	}
    
}
