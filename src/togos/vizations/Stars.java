package togos.vizations;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Stars
{
	//// Data classes
	
	static class Luminosity {
		public final float r, g, b;
		
		public Luminosity( float r, float g, float b ) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	
	static abstract class StarNode {
		final float maximumOuterRadius;
		final Luminosity totalLuminosity;
		
		abstract boolean isSolid();
		abstract Set<StarNodeBinding> getChildren();
		
		public StarNode( float r, Luminosity l ) {
			this.maximumOuterRadius = r;
			this.totalLuminosity = l;
		}
	}
	
	static class StarNodeBinding {
		final float orbitalAxisX, orbitalAxisY, orbitalAxisZ;
		final float orbitalDistance, orbitalPhase, orbitalSpeed;
		final StarNode child;
		
		public StarNodeBinding(
			float orbitalAxisX, float orbitalAxisY, float orbitalAxisZ,
			float orbitalDistance, float orbitalPhase, float orbitalSpeed,
			StarNode child
		) {
			this.orbitalAxisX = orbitalAxisX;
			this.orbitalAxisY = orbitalAxisY;
			this.orbitalAxisZ = orbitalAxisZ;
			this.orbitalDistance = orbitalDistance;
			this.orbitalPhase = orbitalPhase;
			this.orbitalSpeed = orbitalSpeed;
			this.child = child;
		}
	}
	
	static class CompoundNode extends StarNode {
		final Set<StarNodeBinding> children;
		
		static Luminosity sumLuminosity( Set<StarNodeBinding> children ) {
			float r = 0, g = 0, b = 0;
			for( StarNodeBinding c : children ) {
				r += c.child.totalLuminosity.r;
				g += c.child.totalLuminosity.g;
				b += c.child.totalLuminosity.b;
			}
			return new Luminosity(r,g,b);
		}
		
		static float sumOuterRadius( Set<StarNodeBinding> children ) {
			float total = 0;
			for( StarNodeBinding b : children ) {
				total = Math.max(total, b.orbitalDistance + b.child.maximumOuterRadius);
			}
			return total;
		}
		
		public CompoundNode( Set<StarNodeBinding> children ) {
			super( sumOuterRadius(children), sumLuminosity(children) );
			this.children = children;
		}
		
		public boolean isSolid() { return false; }
		public Set<StarNodeBinding> getChildren() { return children; }
	}
	
	static class SolidNode extends StarNode {
		public boolean isSolid() { return true; }
		public Set<StarNodeBinding> getChildren() { return Collections.emptySet(); }
		
		public SolidNode( float r, Luminosity l ) {
			super(r, l);
		}
	}
	
	//// Render
	
	static class StarRenderer {
		final int w, h;
		final float[] r, g, b;
		
		public StarRenderer( int w, int h ) {
			this.w = w; this.h = h;
			this.r = new float[w*h];
			this.g = new float[w*h];
			this.b = new float[w*h];
		}
		
		protected static int toByte( float c ) {
			int x = (int)(c * 255 + 0.5f);
			return x < 0 ? 0 : x > 255 ? 255 : x; 
		}
		
		protected static int rgb( float r, float g, float b ) {
			// Might someday do gamma correction?
			return 0xFF000000 | (toByte(r) << 16) | (toByte(g) << 8) | toByte(b);
		}
		
		public void toRGB( int[] buffer ) {
			assert buffer != null;
			assert buffer.length >= w*h;
			for( int i=w*h-1; i>=0; --i ) buffer[i] = rgb(r[i], g[i], b[i]);
		}
		
		public void clear() {
			for( int i=w*h-1; i>=0; --i ) r[i] = g[i] = b[i] = 0;
		}
		
		// Will need to be refactored for 3D, but hang with me
		public void draw( double time, StarNode n, float x, float y, float scale ) {
			float apparentRadius = n.maximumOuterRadius * scale;
			if(
				x + apparentRadius <= 0 ||
				x - apparentRadius >= w ||
				y + apparentRadius <= 0 ||
				y - apparentRadius >= h
			) return;
			
			int pixelDiam = (int)(apparentRadius * 2 + 0.5f);
			if( pixelDiam < 1 ) pixelDiam = 1;
			
			if( pixelDiam == 1 || n.isSolid() ) {
				// TODO: if numbers are huge, make them fit in an int
				// such that area can still be computed,
				// or else do some special case for hugeness.
				int minX = (int)(x - apparentRadius    );
				int maxX = (int)(x + apparentRadius + 1);
				int minY = (int)(y - apparentRadius    );
				int maxY = (int)(y + apparentRadius + 1);
				int pixelArea = (maxX-minX)*(maxY-minY);
				float luminosityPerPixel = 1 / scale / pixelArea;
				float pr = n.totalLuminosity.r * luminosityPerPixel;
				float pg = n.totalLuminosity.g * luminosityPerPixel;
				float pb = n.totalLuminosity.b * luminosityPerPixel;
				minX = minX < 0 ? 0 : minX;  maxX = maxX > w ? w : maxX;
				minY = minY < 0 ? 0 : minY;  maxY = maxY > h ? h : maxY;
				for( int py=minY; py<maxY; ++py ) for( int px=minX, i=minX+w*py; px<maxX; ++px, ++i ) {
					r[i] += pr; g[i] += pg; b[i] += pb;
				}
			} else {
				for( StarNodeBinding snb : n.getChildren() ) {
					double totalPhase = snb.orbitalPhase + time * snb.orbitalSpeed;
					draw( time, snb.child,
						x + scale * snb.orbitalDistance * (float)Math.cos( totalPhase*Math.PI*2 ),
						y + scale * snb.orbitalDistance * (float)Math.sin( totalPhase*Math.PI*2 ),
						scale
					);
				}
			}
		}
	}
	
	//// UI
	
	public static void main( String[] args ) throws InterruptedException {
		final int w = 320, h = 180;
		final BufferedImage image = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
		final int[] pixBuf = new int[w*h];
		
		final Frame f = new Frame("Stars");
		final ImageCanvas ic = new ImageCanvas();
		ic.setImage(image);
		f.add(ic);
		f.pack();
		f.setVisible(true);
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				f.dispose();
				System.exit(0);
			}
		});
		
		StarNode starNode = new SolidNode(0.01f, new Luminosity(1000,1000,1000));
		StarRenderer renderer = new StarRenderer( w, h );
		
		Set<StarNodeBinding> chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 0.2f, 0.00f, 1, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 0, 0.2f, 0.33f, 1, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 0, 0.2f, 0.66f, 1, starNode));
		
		starNode = new CompoundNode(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 0.5f, 0.00f, -1, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 0, 0.6f, 0.25f, -0.8f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 0, 0.5f, 0.50f, -1, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 0, 0.6f, 0.75f, -0.8f, starNode));
		
		starNode = new CompoundNode(chrilden);
		
		double phase = 0.0;
		while( true ) {
			renderer.clear();
			renderer.draw(phase, starNode, w/2f, h/2f, Math.min(w,h)/2f);
			renderer.toRGB(pixBuf);
			synchronized( image ) {
				image.setRGB(0, 0, w, h, pixBuf, 0, w);
			}
			ic.setImage(image);
			Thread.sleep(50);
			phase += 0.01;
		}
	}
}

/*
 * 3D rendering
 * 
 * camera quaternion -> camera transform matrix -> view transform matrix
 * view transform matrix, fov -> projection
 * 
 * projection(relative x, y, z) -> screen x, y, scale (scale = 1 for object at distance 1)  
 */
