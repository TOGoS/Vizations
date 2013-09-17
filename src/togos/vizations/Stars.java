package togos.vizations;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import togos.vizations.math.FAxisAngle;
import togos.vizations.math.FMatrix;
import togos.vizations.math.MatrixMath;

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
		final float orbitalScaleX, orbitalScaleY, orbitalScaleZ;
		final float orbitalDistance, orbitalPhase, orbitalSpeed;
		final StarNode child;
		
		public StarNodeBinding(
			float orbitalAxisX, float orbitalAxisY, float orbitalAxisZ,
			float orbitalDistance, float orbitalPhase, float orbitalSpeed,
			StarNode child
		) {
			this.orbitalScaleX = orbitalAxisX;
			this.orbitalScaleY = orbitalAxisY;
			this.orbitalScaleZ = orbitalAxisZ;
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
		static final FMatrix IDENTITY_VECTOR = new FMatrix(1,4);
		static final FMatrix X_VECTOR = new FMatrix(1,4);
		static {
			IDENTITY_VECTOR.put(0,3,1);
			X_VECTOR.put(0,3,1);
			X_VECTOR.put(0,0,1);
		}
				
		final int w, h;
		final float[] r, g, b;
		final FMatrix[] xfStack = new FMatrix[64];
		int xfIndex = 0;
		final FMatrix scratchVector = new FMatrix(1,4);
		final FMatrix scratchMatrix = new FMatrix(4,4);
		final FAxisAngle scratchAxisAngle = new FAxisAngle();
		
		public StarRenderer( int w, int h ) {
			this.w = w; this.h = h;
			this.r = new float[w*h];
			this.g = new float[w*h];
			this.b = new float[w*h];
			for( int i=0; i<xfStack.length; ++i ) xfStack[i] = new FMatrix(4,4);
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
		
		public void initCamera( float z ) {
			// TODO: actually use some camera settings
			MatrixMath.identity( xfStack[0] );
			xfStack[0].put(3, 2, -z);
			xfStack[0].put(3, 1, 40);
			// x is left, y is up, z is backwards
		}
		
		// Will need to be refactored for 3D, but hang with me
		public void draw( float t, StarNode n ) {
			MatrixMath.multiply( xfStack[xfIndex], IDENTITY_VECTOR, scratchVector );
			float z = scratchVector.get(0,2); 
			if( z + n.maximumOuterRadius <= 0.1 ) return; // Entirely behind camera
			
			if( z < 0.1 && n.isSolid() ) return;
			
			float scale = h/1.75f/(z+n.maximumOuterRadius);

			float x = scratchVector.get(0,0);
			float minX = w/2 + scale*(x-n.maximumOuterRadius);
			float maxX = w/2 + scale*(x+n.maximumOuterRadius)+1;
			float y = scratchVector.get(0,1);
			float minY = h/2 + scale*(y-n.maximumOuterRadius);
			float maxY = h/2 + scale*(y+n.maximumOuterRadius)+1;
			
			if( z > 0 ) {
				if( minX >= w ) return;
				if( maxX <= 0 ) return;
				if( minY >= h ) return;
				if( maxY <= 0 ) return;
			}
			
			float pixelDiam = scale*n.maximumOuterRadius*2;
			if( z > 0 && pixelDiam <= 1 || n.isSolid() ) {
				
				if( minX < 0 ) minX = 0; if( maxX > w ) maxX = w;
				if( minY < 0 ) minY = 0; if( maxY > h ) maxY = h;
				int iMinX = (int)minX; 	int iMaxX = (int)maxX;
				int iMinY = (int)minY; 	int iMaxY = (int)maxY;
				
				float pixelArea = pixelDiam*pixelDiam;
				float marmar = n.maximumOuterRadius*n.maximumOuterRadius;
				
				float brightness = pixelArea / marmar;
				for( int py=iMinY; py<iMaxY; ++py ) for( int px=iMinX, i=py*w+px; px<iMaxX; ++px, ++i ) {
					r[i] += brightness * n.totalLuminosity.r;
					g[i] += brightness * n.totalLuminosity.g;
					b[i] += brightness * n.totalLuminosity.b;
				}
			} else {
				++xfIndex;
				for( StarNodeBinding snb : n.getChildren() ) {
					double phase = Math.PI*2*(snb.orbitalPhase + snb.orbitalSpeed * t);
					
					MatrixMath.identity(scratchMatrix);
					scratchMatrix.put(3, 0, (float)Math.sin(phase)*snb.orbitalDistance);
					scratchMatrix.put(3, 2, ((xfIndex+0)%2)*(float)Math.cos(phase)*snb.orbitalDistance);
					scratchMatrix.put(3, 1, ((xfIndex+1)%2)*(float)Math.cos(phase)*snb.orbitalDistance);
					
					MatrixMath.multiply( xfStack[xfIndex-1], scratchMatrix, xfStack[xfIndex] );
					draw( t, snb.child );
				}
				--xfIndex;
			}
		}
	}
	
	//// UI
	
	public static void main( String[] args ) throws InterruptedException {
		final int w = 640, h = 360;
		//final int w = 320, h = 180;
		//final int w = 160, h = 90;
		//final int w = 80, h = 45;
		final BufferedImage image = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
		final int[] pixBuf = new int[w*h];
		
		final Frame f = new Frame("Stars");
		final ImageCanvas ic = new ImageCanvas();
		ic.setBackground(Color.BLACK);
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
		
		StarNode starNode = new SolidNode(0.5f, new Luminosity(0.4f, 0.2f, 0.1f));
		StarRenderer renderer = new StarRenderer( w, h );
		
		Set<StarNodeBinding> chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(1, 0, 0, 2f, 0.00f, 2f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 0, 2f, 0.33f, 2f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 0, 2f, 0.66f, 2f, starNode));
		starNode = new CompoundNode(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 6, 0.00f, -1, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 6, 0.25f, -0.8f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 6, 0.50f, -1, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 6, 0.75f, -0.8f, starNode));
		starNode = new CompoundNode(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 24, 0.00f, -0.5f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 24, 0.25f, -0.5f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 24, 0.50f, -0.5f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 24, 0.75f, -0.5f, starNode));
		starNode = new CompoundNode(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 96, 0.00f, -0.2f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 96, 0.25f, -0.2f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 96, 0.50f, -0.2f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 96, 0.75f, -0.2f, starNode));
		starNode = new CompoundNode(chrilden);

		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 200, 0.00f, -0.05f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 200, 0.25f, -0.05f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 200, 0.50f, -0.05f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 200, 0.75f, -0.05f, starNode));
		starNode = new CompoundNode(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 500, 0.00f, -0.005f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 500, 0.25f, -0.005f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 500, 0.50f, -0.005f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 500, 0.75f, -0.005f, starNode));
		starNode = new CompoundNode(chrilden);
		
		float gSize = 1500;
		for( int i=0; i<10; ++i, gSize*=2 ) {
			chrilden = new HashSet<StarNodeBinding>();
			chrilden.add(new StarNodeBinding(0, 1, 0, gSize, 0.1f*i+0.00f, -0.0001f, starNode));
			chrilden.add(new StarNodeBinding(0, 1, 1, gSize, 0.1f*i+0.25f, -0.0001f, starNode));
			chrilden.add(new StarNodeBinding(1, 1, 0, gSize, 0.1f*i+0.50f, -0.0001f, starNode));
			chrilden.add(new StarNodeBinding(1, 0, 1, gSize, 0.1f*i+0.75f, -0.0001f, starNode));
			chrilden.add(new StarNodeBinding(1, 0, 1,     0,        0.00f, -0.0001f, starNode));
			starNode = new CompoundNode(chrilden);
		}

		for( int frame=0; frame<10*30*60; ++frame ) {
			float time = frame*0.01f;
			float z = time*1500 - 40000;
			renderer.clear();
			renderer.initCamera( z );
			renderer.draw(time, starNode);
			renderer.toRGB(pixBuf);
			synchronized( image ) {
				image.setRGB(0, 0, w, h, pixBuf, 0, w);
			}
			ic.setImage(image);
			//Thread.sleep(50);
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
