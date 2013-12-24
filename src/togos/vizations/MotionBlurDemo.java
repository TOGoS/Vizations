package togos.vizations;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MotionBlurDemo
{
	static class Spark {
		public float x, y, vx, vy;
		int color = 0xFF444422;
		float brightness = 1;
		boolean destroy;
	}
	
	protected static final int clampByte( int b ) {
		return b < 0 ? 0 : b > 255 ? 255 : b;
	}
	protected static final int clampByte( float b ) {
		return b < 0 ? 0 : b > 255 ? 255 : (int)b;
	}
	protected static final int addComponent( int a, int b, int shift ) {
		return clampByte( ((a >> shift)&0xFF) + ((b >> shift)&0xFF) ) << shift;  
	}
	protected static final int addColor( int a, int b ) {
		return addComponent(a,b,24) | addComponent(a,b,16) | addComponent(a,b,8) | addComponent(a,b,0);
	}
	
	protected static final int scaleColor( int c, float s ) {
		return 0xFF000000 |
			(clampByte( ((c >> 16) & 0xFF) * s ) << 16) |
			(clampByte( ((c >>  8) & 0xFF) * s ) <<  8) |
			(clampByte( ((c >>  0) & 0xFF) * s ) <<  0);
	}
	
	List<Spark> sparks = new ArrayList<Spark>();
	List<Spark> things = new ArrayList<Spark>();
	
	//float gravX = 400, gravY = 300;
	//float gravStren = 100;
	float rate = 0.1f;
	
	protected void gravitate( Spark s, float cx, float cy, float g ) {
		float dx = s.x - cx;
		float dy = s.y - cy;
		float dist = (float)Math.sqrt(dx*dx + dy*dy);
		if( dist == 0 ) return;
		dx = dx/dist;
		dy = dy/dist;
		s.vx -= g * dx/(dist*dist);
		s.vy -= g * dy/(dist*dist);
	}
	
	class LeCanv extends Canvas {
		private static final long serialVersionUID = 1L;
		
		BufferedImage img;
		int[] pix;
		int bufWidth, bufHeight;
		long ts = 0;
		
		public LeCanv( int w, int h ) {
			this.setPreferredSize( new Dimension(w, h) );
			img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			pix = new int[w*h];
			this.bufWidth = w;
			this.bufHeight = h;
			setBackground(new Color(0.1f, 0.1f, 0.1f));
		}
		
		protected void drawRect( int x, int y, int w, int h, int color ) {
			color = scaleColor(color, rate);
			if( x < 0 ) { w += x; x = 0; }
			if( y < 0 ) { h += y; y = 0; }
			if( w+x > bufWidth ) { w = bufWidth - x; }
			if( y+h > bufHeight ) { h = bufHeight - y; }
			for( int dy=0; dy<h; ++dy ) for( int j=bufWidth*(y+dy)+x, dx=0; dx<w; ++dx, ++j ) {
				pix[j] = addColor( pix[j], color );
			}
		}
		
		protected void updateSparks() {
			for( Iterator<Spark> i=sparks.iterator(); i.hasNext(); ) {
				Spark s = i.next();
				
				updateThingPosition(s);
				updateThingVelocity(s);
				
				s.brightness -= 0.001*rate;
				if( s.brightness < 0 || s.x < -100 || s.y < -100 || s.x > bufWidth + 100 || s.y > bufHeight + 100 ) i.remove();
			}
		}
		
		final Random r = new Random();
		
		protected float nextGaus() {
			return (float)r.nextGaussian();
		}
		
		protected void makeSparks( float x, float y, float vx, float vy, int color, float dist ) {
			for( int i=0; i<r.nextInt(100); ++i ) {
				Spark s = new Spark();
				s.x = x; s.y = y;
				s.vx = vx + nextGaus();
				s.vy = vy + nextGaus();
				s.x += vx * dist;
				s.y += vy * dist;
				s.color = color;
				sparks.add(s);
			}
		}
		
		protected void makeSparks( float x, float y, float vx, float vy ) {
			makeSparks( x, y, vx, vy, 0xFF443322, 10 );
		}
		
		protected void draw() {
			for( Spark s : sparks ) {
				drawRect( (int)s.x - 3, (int)s.y - 3, 6, 6, scaleColor(s.color, s.brightness) );
			}
			for( Spark thing : things ) {
				int posX = (int)(10 * Math.cos( ts * 0.01 ));
				int posY = (int)(10 * Math.sin( ts * 0.01 ));
				drawRect( (int)thing.x-posX-5, (int)thing.y-posY-5, 10, 10, 0xFF221111 );
				drawRect( (int)thing.x     -5, (int)thing.y     -5, 10, 10, 0xFF111122 );
				drawRect( (int)thing.x+posX-5, (int)thing.y+posY-5, 10, 10, 0xFF112211 );
			}
		}
		
		protected void updateThingPosition( Spark thing ) {
			thing.x += thing.vx * rate;
			thing.y += thing.vy * rate;
		}
		
		protected void updateThingVelocity( Spark thing ) {
			//thing.vx *= 0.999;
			//thing.vy *= 0.999;
			for( Spark other : things ) {
				if( other == thing ) continue;
				float dx = thing.x - other.x;
				float dy = thing.y - other.y;
				float dist = (float)Math.sqrt(dx*dx + dy*dy);
				if( dist < 10 ) thing.destroy = true;
				gravitate(thing, other.x, other.y, 10);
			}
			
			thing.vy += 0.001;
		}
		
		protected void update() {
			for( Spark thing : things ) {
				updateThingPosition(thing);
				
				if( thing.x < 0 && thing.vx < 0 ) { thing.vx = -thing.vx; makeSparks(thing.x, thing.y, 1, 0); }
				if( thing.y < 0 && thing.vy < 0 ) { thing.vy = -thing.vy; makeSparks(thing.x, thing.y, 0, 1); }
				if( thing.x > bufWidth && thing.vx > 0 ) { thing.vx = -thing.vx; makeSparks(thing.x, thing.y, -1, 0); }
				if( thing.y > bufHeight && thing.vy > 0 ) { thing.vy = -thing.vy; makeSparks(thing.x, thing.y, 0, -1); }
			}
			for( Spark t : things ) updateThingVelocity(t);
			for( Iterator<Spark> i=things.iterator(); i.hasNext(); ) {
				Spark t = i.next();
				if( t.destroy ) {
					i.remove();
					makeSparks(t.x, t.y, t.vx, t.vy, 0xFF112244, 0);
				}
			}
			updateSparks();
			++ts;
			draw();
		}
		
		protected void drawOuter() {
			for( int i=pix.length-1; i>=0; --i ) pix[i] = 0xFF000000;
			
			for( int i=0; i<50; ++i ) {
				update();
				draw();
			}
		}
		
		public void paint(Graphics g) {
			drawOuter();
			img.setRGB(0, 0, img.getWidth(), img.getHeight(), pix, 0, img.getWidth());
			g.drawImage(img, (getWidth()-img.getWidth())/2, (getHeight()-img.getHeight())/2, null);
		}
		
		@Override public void update(Graphics g) {
			paint(g);
		}

		public float componentToWorldX( int x ) {
			return x - (getWidth()-img.getWidth())/2;
		}
		public float componentToWorldY( int y ) {
			return y - (getHeight()-img.getHeight())/2;
		}

	}
	
	public static void main( String[] args ) throws InterruptedException {
		final MotionBlurDemo mbd = new MotionBlurDemo(); 
		final LeCanv lc = mbd.new LeCanv(800, 600);
		Frame f = new Frame();
		f.add(lc);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent wEvt) {
				System.exit(0);
			}
		});
		// Since rendering is also done in the AWT event handling thread,
		// no need to worry about concurrency issues, tee-hee.
		lc.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				Spark t = new Spark();
				t.x = 0;
				t.y = 300;
				t.vx = (float)Math.random()*10;
				t.vy = (float)Math.random()*4-2;
				mbd.things.add(t);
			}
		});
		lc.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mEvt) {
				Spark t = new Spark();
				t.x = lc.componentToWorldX(mEvt.getX());
				t.y = lc.componentToWorldY(mEvt.getY());
				mbd.things.add(t);
				//mbd.gravX = mEvt.getX();
				//mbd.gravY = mEvt.getY();
			}
		});
		f.setVisible(true);
		while(true) {
			lc.repaint();
			Thread.sleep(50);
		}
	}
}
