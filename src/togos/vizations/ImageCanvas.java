package togos.vizations;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

class ImageCanvas extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	public ImageCanvas() {
		setImage(null);
	}
	
	BufferedImage image;
	public void setImage( BufferedImage img ) {
		this.image = img;
		setPreferredSize( img == null ? new Dimension(512,384) : new Dimension(img.getWidth(), img.getHeight()));
		repaint();
	}
	
	protected int offX, offY;
	protected float scale;
	
	/**
	 * Adjust scale and offsets and return the image so
	 * that this.image can be safely rebound from another thread
	 */
	protected BufferedImage initOffsets() {
		BufferedImage img = image;
		
		if( img == null ) return img;
		
		scale = 1;
		while( img.getWidth() * scale > getWidth() || img.getHeight() * scale > getHeight() ) {
			scale /= 2;
		}
		while( img.getWidth() * scale * 2 <= getWidth() && img.getHeight() * 2 <= getHeight() ) {
			scale *= 2;
		}
		
		offX = (int)(getWidth() - img.getWidth() * scale) / 2;
		offY = (int)(getHeight() - img.getHeight() * scale) / 2;
		
		return img;
	}
	
	@Override public void paint( Graphics g ) {
		g.setColor(getBackground());
		BufferedImage img = initOffsets();
		if( img == null || scale == 0 ) {
			g.fillRect(0,0,getWidth(),getHeight());
		} else {
			int scaledImageWidth  = (int)(img.getWidth()  * scale);
			int scaledImageHeight = (int)(img.getHeight() * scale);
			int right  = offX + scaledImageWidth; 
			int bottom = offY + scaledImageHeight;
			synchronized( img ) {
				g.drawImage( img, offX, offY, scaledImageWidth, scaledImageHeight, null );
			}
			g.fillRect(    0, offY,             offX, scaledImageHeight);
			g.fillRect(right, offY, getWidth()-right, scaledImageHeight);
			g.fillRect(0,      0, getWidth(), offY);
			g.fillRect(0, bottom, getWidth(), getHeight()-bottom);
		}
	}
	
	@Override public void update( Graphics g ) { paint(g); }
}
