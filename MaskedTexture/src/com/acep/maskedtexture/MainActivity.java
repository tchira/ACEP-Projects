
package com.acep.maskedtexture;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity implements OnSeekBarChangeListener
{	
	private int mostRecentAlpha = 255;
	private ImageView background;
	private ImageView maskedTexture;
	private ImageView lens;
	private SeekBar alphaBar;
	private RelativeLayout texturedLens;
	private MaskedTextureDrawer drawer;	
	  
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initializeImageViews();
		initializeAlphaBar();
		initializeRelativeLayouts();
		
		drawer = new MaskedTextureDrawer(this);
	}	

	private void initializeImageViews()
	{
		background = (ImageView) findViewById(R.id.background);
		maskedTexture = (ImageView) findViewById(R.id.maskedTexture);
		maskedTexture.setTranslationY(maskedTexture.getTranslationY() + 20);
		lens = (ImageView) findViewById(R.id.lens);
	}
	
	private void initializeAlphaBar()
	{
		alphaBar = (SeekBar) findViewById(R.id.alphaBar);
		alphaBar.setProgress(mostRecentAlpha);
		alphaBar.setOnSeekBarChangeListener(this);
	}
	
	private void initializeRelativeLayouts()
	{
		texturedLens = (RelativeLayout) findViewById(R.id.texturedLens);
		texturedLens.setOnTouchListener(new TouchListener());
	}	
	
	@Override
	public void onStart()
	{
		super.onStart();
		loadImages();
	}
	
	private void loadImages()
	{
		background.setImageResource(R.drawable.coffee);
		lens.setImageResource(R.drawable.lens);
		MaskedTextureLoader maskedTextureLoader = new MaskedTextureLoader(drawer, maskedTexture);
		maskedTextureLoader.execute(mostRecentAlpha);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		alphaBar.setProgress(mostRecentAlpha);
		loadImages();
	}
	  
	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
	    // TODO Auto-generated method stub
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
	    // TODO Auto-generated method stub
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
	          boolean fromUser)
	{
		if (fromUser)
		{
			mostRecentAlpha = progress;
			MaskedTextureLoader maskedTextureLoader = new MaskedTextureLoader(
					drawer, maskedTexture);
			maskedTextureLoader.execute(mostRecentAlpha);
		}
	} 
	 	 
}
