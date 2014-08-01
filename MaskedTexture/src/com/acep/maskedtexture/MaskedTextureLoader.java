
package com.acep.maskedtexture;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

public class MaskedTextureLoader extends AsyncTask<Integer, Void, Bitmap>
{
	private final WeakReference<ImageView> imageViewReference;
	private MaskedTextureDrawer drawer;
	
	public MaskedTextureLoader(MaskedTextureDrawer drawer, ImageView imageView)
	{
		imageViewReference = new WeakReference<ImageView>(imageView);
		this.drawer = drawer;
	}
	
	@Override
	protected Bitmap doInBackground(Integer... params)
	{
		int alpha = params[0];
		return drawer.drawMask(alpha);
	}
	
	@Override
    protected void onPostExecute(Bitmap bitmap)
	{
        if (imageViewReference != null && bitmap != null)
        {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null)
            {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
