
package com.acep.maskedtexture;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

public class MaskedTextureDrawer 
{
	private Bitmap texture;
	private Bitmap mask;
	private Context context;
	
	public MaskedTextureDrawer(Context context)
	{
		this.context = context;
		this.initializeResources();
	}
	
	private void initializeResources()
	{
		Resources resources = this.context.getResources();
		BitmapFactory.Options options = new BitmapFactory.Options();		  
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		
		Bitmap texture = BitmapFactory.decodeResource(resources, R.drawable.fog_texture, options);
		texture.setHasAlpha(true);
		this.texture = texture;
		  
		Bitmap mask = BitmapFactory.decodeResource(resources, R.drawable.mask);
		this.mask = mask;
	}
	
	public Bitmap drawMask(int alpha)
	{
		int width = this.mask.getWidth();
	    int height = this.mask.getHeight();
	    Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);  
	    
	    Canvas canvas = new Canvas(output);
	    Paint paint = new Paint();
	    
	    paint.setAlpha(alpha);
	    canvas.drawBitmap(texture, 0, 0, paint);
		paint.setAlpha(255);
	    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
		canvas.drawBitmap(mask, 0, 0, paint);
		
		return output;
	}
}
