package togos.vizations;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

class ImageCanvas extends Canvas
{
    private static final long serialVersionUID = 1L;
    
	public BufferedImage image;
	
	@Override public void paint( Graphics g ) {
		BufferedImage img = this.image;
		if( img == null ) {
			g.setColor(Color.BLACK);
			Rectangle clip = g.getClipBounds(); 
			g.fillRect(clip.x, clip.y, clip.width, clip.height);
		} else {
			g.drawImage( image, 0, 0, null );
		}
	}
	
	public void setImage( BufferedImage img ) {
		this.image = img;
		repaint();
	}
	
	@Override public void update( Graphics g ) {
		paint(g);
	}
}