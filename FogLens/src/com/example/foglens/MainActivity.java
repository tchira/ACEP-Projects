package com.example.foglens;

import android.app.Activity;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.example.drawtools.DrawTools;

public class MainActivity extends Activity implements OnSeekBarChangeListener {
  
  private SurfaceView preview=null;
  private SurfaceHolder previewHolder=null;
  private Camera camera=null;
  private boolean inPreview=false;
  private boolean cameraConfigured=false;
  private SeekBar mOpacityBar=null;
  private ImageView fogView,lensView;
  private DrawTools dTools;
  private Thread rThread;
  private int numCrossing,p;
  private short audioData[];
  private int bufferSize;
  private int frequency;
  private int amplitude;
  
  private double mEMA;
  
  private AudioRecord recorder;
  //private MediaRecorder recorder;
  
  
  //Recorder constants
  private static final int SAMPLE_DELAY = 75;
  private static final int MIN_AMP_LIMIT=16000;
  //private static final int MIN_AMP_LIMIT=2300;  //for median amp
  private static final int MAX_FREQ_LIMIT=150;
  private static final int MAX_AMP_LIMIT=18000; 
 // private static final int MAX_AMP_LIMIT=3000; //for median amp
  private static final int MIN_FREQ_LIMIT=40;
  private static final int SAMPLE_RATE=48000;
  
 

  
  //
  //Layout initialisation
  //
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    this.initViews();

   bufferSize=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,
	AudioFormat.ENCODING_PCM_16BIT)*3; //get the buffer size to use with this audio record
   audioData = new short [bufferSize]; //short array that pcm data is put into.
    }

 
 private void initViews(){
	  
	    dTools=new DrawTools(this);
	    //fog texture
	    fogView=(ImageView)this.findViewById(R.id.maskedTexture);
	    fogView.setImageBitmap(dTools.getMaskedBitmap(105));	//align lens with texture
	    fogView.setTranslationY(fogView.getTranslationY()+20);
	    
	    //lens overlay
	    lensView=(ImageView)this.findViewById(R.id.lens);
	    lensView.setImageResource(R.drawable.lens);
	    
	    //camera preview
	    preview=(SurfaceView)findViewById(R.id.preview);
	    previewHolder=preview.getHolder();
	    previewHolder.addCallback(surfaceCallback);
	   
	    RelativeLayout rl=(RelativeLayout)findViewById(R.id.foglens);	//set dragging for the lens
	    rl.setOnTouchListener(new TouchListener());
	    
	    //slider
	    mOpacityBar=(SeekBar)this.findViewById(R.id.seekBarID);
	    mOpacityBar.setProgress(105);
	    mOpacityBar.setOnSeekBarChangeListener(this);
}
 

private void initRecorder(){
	 			recorder = new AudioRecord (AudioSource.MIC,SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT,bufferSize); //instantiate the AudioRecorder 
				                
				recorder.startRecording();
	 
				
	 	rThread = new Thread(new Runnable() {
				        public void run() {
				        	while(rThread != null && !rThread.isInterrupted()){
				        		try{ Thread.sleep(75); } catch (Exception e){e.printStackTrace();} 
				        		
				        		readAudioBuffer();
								
								runOnUiThread(new Runnable() {
									
									@Override
									public void run() {
										
										getMaxAmplitude();
										//getMedianAmplitude();
										Log.e("FREQ",Double.toString(getFrequency()));
										Log.e("AMP",Double.toString(amplitude));
										
										if(amplitude>MIN_AMP_LIMIT&&amplitude<MAX_AMP_LIMIT)
										{
											if(getFrequency()<MAX_FREQ_LIMIT&&getFrequency()>MIN_FREQ_LIMIT)
												Log.d("LOG", "Blow detected");
												
													fogView.setImageBitmap(dTools.getMaskedBitmap(mOpacityBar.getProgress()+10));
													mOpacityBar.setProgress(mOpacityBar.getProgress()+10);
													
										}
										
									}
								});
				        	}
				        }
				    });
				rThread.start();
 }
  
 private void readAudioBuffer() {
	 		
	 		audioData = new short [bufferSize]; //short array that pcm data is put into.
			recorder.read(audioData,0,bufferSize); //read the PCM audio data into the audioData array
	          
 }

 
 public double getFrequency(){
		double freq;
		numCrossing=0; //initialize your number of zero crossings to 0
	    for (p=0;p<bufferSize/4;p+=4) {
	           if (audioData[p]>0 && audioData[p+1]<=0) numCrossing++;
	            if (audioData[p]<0 && audioData[p+1]>=0) numCrossing++;
	            if (audioData[p+1]>0 && audioData[p+2]<=0) numCrossing++;
	            if (audioData[p+1]<0 && audioData[p+2]>=0) numCrossing++;
	            if (audioData[p+2]>0 && audioData[p+3]<=0) numCrossing++;
	            if (audioData[p+2]<0 && audioData[p+3]>=0) numCrossing++;
	            if (audioData[p+3]>0 && audioData[p+4]<=0) numCrossing++;
	            if (audioData[p+3]<0 && audioData[p+4]>=0) numCrossing++;
	            }//for p
	    
	      for (p=(bufferSize/4)*4;p<bufferSize-1;p++) {
	            if (audioData[p]>0 && audioData[p+1]<=0) numCrossing++;
	            if (audioData[p]<0 && audioData[p+1]>=0) numCrossing++;
	            }
	                                            
	     
	      freq=(SAMPLE_RATE/bufferSize)*(numCrossing/2);
	    return freq;
}
	      
public void getMaxAmplitude(){		     
	   
		amplitude=audioData[0]; 
		for (short s : audioData) { 
			if (Math.abs(s)>amplitude) amplitude=Math.abs(s); 
		}
		
}

public void getMedianAmplitude(){
	int sum=0;
	for (short s : audioData) { 
		sum+=Math.abs(s);
	}
	amplitude=sum/audioData.length;
	
}
 
 
 
 //
  //Camera preview implementation
  //
  
  @Override
  public void onResume() {
	  Log.d("RESUME","RESUME");
	  super.onResume();
    camera=Camera.open();
    startPreview();
    initRecorder();
	
  }
    
  
  
  @Override
  public void onPause() {
	  //intrerrupt camera and recorder thread
	  Log.d("PAUSE","PAUSE");
	  super.onPause();
	  if (inPreview) {
      camera.stopPreview();
    }
    rThread.interrupt();
    rThread=null;
    try{
    	if(recorder!=null){
    		recorder.stop();
    		recorder.release();
    		recorder=null;
    	}
    }
    catch(Exception e){e.printStackTrace();}
    
    
 
    
    camera.release();
    camera=null;
    inPreview=false;
          
   
  }
  
  private Camera.Size getBestPreviewSize(int width, int height,
                                         Camera.Parameters parameters) {
    Camera.Size result=null;
    
    for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
      if (size.width<=width && size.height<=height) {
        if (result==null) {
          result=size;
        }
        else {
          int resultArea=result.width*result.height;
          int newArea=size.width*size.height;
          
          if (newArea>resultArea) {
            result=size;
          }
        }
      }
    }
    
    return(result);
  }
  
  private void initPreview(int width, int height) {
    if (camera!=null && previewHolder.getSurface()!=null) {
      try {
        camera.setPreviewDisplay(previewHolder);
      }
      catch (Throwable t) {
        Log.e("PreviewDemo-surfaceCallback",
              "Exception in setPreviewDisplay()", t);
        Toast
          .makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG)
          .show();
      }

      if (!cameraConfigured) {
        Camera.Parameters parameters=camera.getParameters();
        Camera.Size size=getBestPreviewSize(width, height,
                                            parameters);
        
        if (size!=null) {
          parameters.setPreviewSize(size.width, size.height);
          camera.setParameters(parameters);
          cameraConfigured=true;
        }
      }
    }
  }
  
  private void startPreview() {
    if (cameraConfigured && camera!=null) {
      camera.startPreview();
      inPreview=true;
    }
  }
  
  SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
    public void surfaceCreated(SurfaceHolder holder) {
      // no-op -- wait until surfaceChanged()
    }
    
    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width,
                               int height) {
      initPreview(width, height);
      startPreview();
    }
    
    public void surfaceDestroyed(SurfaceHolder holder) {
      // no-op
    }
  };
  
  
  
  
  
  
  //
  //SeekBar Listener implementation
  //
  
  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
      // TODO Auto-generated method stub

  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
      // TODO Auto-generated method stub

  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress,
          boolean fromUser) {
      // TODO Auto-generated method stub
	 int opacity=progress;
	 if(fogView!=null)
		 fogView.setImageBitmap(dTools.getMaskedBitmap(opacity));
	 
  }
  
  
  
 
}