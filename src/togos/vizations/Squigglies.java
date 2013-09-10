package togos.vizations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Squigglies
{
	public int frame = 0;
	public int w = 640, h = 360;
	
	Random r = new Random(1231);
	
	int squiggleCount = 20;
	int[] estarts = new int[squiggleCount];
	float[] sizes = new float[squiggleCount];
	float[] speeds = new float[squiggleCount];
	Color[] colors = new Color[squiggleCount];
	
	int dustCount = 512;
	int[] dustPositions = new int[dustCount];
	float[] dustSizes = new float[dustCount];
	float[] dustSpeeds = new float[dustCount];
	
	public Squigglies() {
		for( int j=0; j<squiggleCount; ++j ) {
			estarts[j] = r.nextInt(w);
			sizes[j] = 1+r.nextFloat()*3;
			speeds[j] = 4/sizes[j];
			colors[j] = new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 0.5f);
		}
		
		for( int j=0; j<dustCount; ++j ) {
			dustPositions[j] = r.nextInt(w);
			dustSizes[j] = 0.1f + r.nextFloat()*r.nextFloat()*4;
			dustSpeeds[j] = dustSizes[j]+r.nextFloat();
		}
	}
	
	static int wrap( int num, int den, int border ) {
		return MathUtil.fdmod( num, den + border*2 ) - border;
	}
	
	public void paint( Graphics g ) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, w, h);
		
		g.setColor(new Color(1, 1, 1, 0.5f));
		for( int j=0; j<dustCount; ++j ) {
			int cx = wrap( (int)(dustPositions[j] - frame*dustSpeeds[j]), w, 20 );
			int cy = wrap( (int)(dustPositions[j] * 43 + dustSpeeds[j]*Math.sin(j+frame*0.1)), h, 20 );
			
			int rad = (int)Math.ceil(dustSizes[j]);
			g.fillRect( cx-rad, cy-rad, rad*2, rad*2 );
		}
		
		for( int j=0; j<squiggleCount; ++j ) {
			g.setColor(colors[j]);
			for( int i=0; i<40; ++i ) {
				int absX = (int)(estarts[j] - i*sizes[j] + frame * speeds[j]);
				int cx = wrap( absX, w, 20 );
				int cy = wrap( (estarts[j] * 43) + (int)Math.round(80/speeds[j]*Math.sin(absX * 0.01 * speeds[j])), h, 20);
				
				int rad = (int)Math.ceil(50*sizes[j] / (i+10));
				int top = (int)Math.round(cy - rad);
				int left = (int)Math.round(cx - rad);
				
				g.fillOval( left, top, rad*2, rad*2 );
			}
		}
	}
	
	public void runInWindow() throws Exception {
		final Frame f = new Frame("PPP1");
		final ImageCanvas c = new ImageCanvas();
		c.setPreferredSize( new Dimension(w, h) );
		f.add(c);
		f.pack();
		f.addWindowListener( new WindowAdapter() {
			@Override public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});
		f.setVisible(true);
		
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for( frame=0; frame<Integer.MAX_VALUE; ++frame ) {
			paint(img.getGraphics());
			c.setImage(img);
			Thread.sleep(10);
		}
	}
	
	public static void main(String[] args) {
		try {
	        new Squigglies().runInWindow();
        } catch( Exception e ) {
	        e.printStackTrace();
	        System.exit(1);
        }
	}
}
