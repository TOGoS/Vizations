package togos.vizations;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import togos.vizations.math.FAxisAngle;
import togos.vizations.math.FMatrix;
import togos.vizations.math.MatrixMath;

public class Stars
{
	//// Data classes
	
	static class FColor {
		public final float r, g, b;
		
		public FColor( float r, float g, float b ) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	
	static abstract class StarNode {
		final float maximumOuterRadius;
		final FColor totalLuminance; // Pixel color per area ('area' being rad*rad*4) 
		
		abstract boolean isSolid();
		abstract Set<StarNodeBinding> getChildren();
		
		public StarNode( float r, FColor l ) {
			this.maximumOuterRadius = r;
			this.totalLuminance = l;
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
		
		static FColor sumLuminosity( Set<StarNodeBinding> children, float agRad ) {
			float r = 0, g = 0, b = 0;
			for( StarNodeBinding c : children ) {
				float area = c.child.maximumOuterRadius*c.child.maximumOuterRadius;
				r += c.child.totalLuminance.r * area;
				g += c.child.totalLuminance.g * area;
				b += c.child.totalLuminance.b * area;
			}
			float agArea = agRad*agRad;
			return new FColor(r/agArea,g/agArea,b/agArea);
		}
		
		static float sumOuterRadius( Set<StarNodeBinding> children ) {
			float total = 0;
			for( StarNodeBinding b : children ) {
				total = Math.max(total, b.orbitalDistance + b.child.maximumOuterRadius);
			}
			return total;
		}
		
		public static CompoundNode aggregate( Set<StarNodeBinding> children ) {
			float rad = sumOuterRadius(children);
			FColor lum = sumLuminosity(children, rad);
			return new CompoundNode( rad, lum, children );
		}
		
		private CompoundNode( float rad, FColor lum, Set<StarNodeBinding> children ) {
			super( rad, lum );
			this.children = children;
		}
		
		public boolean isSolid() { return false; }
		public Set<StarNodeBinding> getChildren() { return children; }
	}
	
	static class SolidNode extends StarNode {
		public boolean isSolid() { return true; }
		public Set<StarNodeBinding> getChildren() { return Collections.emptySet(); }
		
		public SolidNode( float r, FColor l ) {
			super(r, l);
		}
	}
	
	//// Render
	
	static class RenderBuffer {
		final int w, h;
		final float[] r, g, b;
		
		public RenderBuffer( int w, int h ) {
			this.w = w; this.h = h;
			this.r = new float[w*h];
			this.g = new float[w*h];
			this.b = new float[w*h];
		}
		
		public void copyFrom( RenderBuffer oth ) {
			assert oth.w == this.w;
			assert oth.h == this.h;
			
			for( int i=w*h-1; i>=0; --i ) {
				r[i] = oth.r[i];
				g[i] = oth.g[i];
				b[i] = oth.b[i];
			}
		}
		
		public void addFrom( RenderBuffer oth, float scale ) {
			assert oth.w == this.w;
			assert oth.h == this.h;
			
			for( int i=w*h-1; i>=0; --i ) {
				r[i] += oth.r[i] * scale;
				g[i] += oth.g[i] * scale;
				b[i] += oth.b[i] * scale;
			}
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
		
		public void multiply( float s ) {
			for( int i=w*h-1; i>=0; --i ) {
				r[i] *= s;
				g[i] *= s;
				b[i] *= s;
			}
		}
	}
	
	static class StarRenderer extends RenderBuffer {
		static final FMatrix IDENTITY_VECTOR = new FMatrix(1,4);
		static final FMatrix X_VECTOR = new FMatrix(1,4);
		static {
			IDENTITY_VECTOR.put(0,3,1);
			X_VECTOR.put(0,3,1);
			X_VECTOR.put(0,0,1);
		}
				
		final FMatrix[] xfStack = new FMatrix[64];
		int xfIndex = 0;
		final FMatrix scratchVector = new FMatrix(1,4);
		final FMatrix scratchMatrix = new FMatrix(4,4);
		final FAxisAngle scratchAxisAngle = new FAxisAngle();
		
		public StarRenderer( int w, int h ) {
			super(w, h);
			for( int i=0; i<xfStack.length; ++i ) xfStack[i] = new FMatrix(4,4);
		}
		
		public void initCamera( float z ) {
			// TODO: actually use some camera settings
			MatrixMath.identity( xfStack[0] );
			xfStack[0].put(3, 2, -z);
			xfStack[0].put(3, 1, 40);
			// x is left, y is up, z is backwards
		}
		
		float nearZ = 0.1f, farZ = Float.POSITIVE_INFINITY;
		public void setDrawRange( float nearZ, float farZ ) {
			this.nearZ = nearZ;
			this.farZ = farZ;
		}
		
		// Will need to be refactored for 3D, but hang with me
		public void draw( float t, StarNode n ) {
			MatrixMath.multiply( xfStack[xfIndex], IDENTITY_VECTOR, scratchVector );
			float z = scratchVector.get(0,2); 
			if( z + n.maximumOuterRadius <= nearZ ) return; // Entirely behind camera
			if( z - n.maximumOuterRadius >=  farZ ) return; // Entirely outside range
			
			if( z < nearZ && n.isSolid() ) return;
			
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
				float brightnessPerPixel = pixelArea / ((iMaxX-iMinX)*(iMaxY-iMinY));
				float pr = brightnessPerPixel * n.totalLuminance.r;
				float pg = brightnessPerPixel * n.totalLuminance.g;
				float pb = brightnessPerPixel * n.totalLuminance.b;
				
				if( pixelArea == 1 ) {
					int i = iMinY*w+iMinX;
					r[i] += pr;
					g[i] += pg;
					b[i] += pb;
				} else {
					// Draw a nice circle!
					// TODO: Note that this code is awful.
					float cpx = w/2 + scale*x;
					float cpy = h/2 + scale*y;
					for( int py=iMinY; py<iMaxY; ++py ) {
						float ppy = py+0.5f;
						float sin = Math.abs(2*(cpy - ppy)/pixelDiam);
						if( sin > 1 ) sin = 1;
						float cos = (float)Math.sqrt(1 - sin*sin);
						iMinX = (int)(cpx-cos*pixelDiam/2  );
						if( iMinX < 0 ) iMinX = 0;
						iMaxX = (int)(cpx+cos*pixelDiam/2+1);
						if( iMaxX > w ) iMaxX = w;
						for( int px=iMinX, i=py*w+px; px<iMaxX; ++px, ++i ) {
							r[i] += pr;
							g[i] += pg;
							b[i] += pb;
						}
					}
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
	
	public static void main( String[] args ) throws InterruptedException, IOException {
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
		
		StarNode starNode = new SolidNode(0.5f, new FColor(4f, 2f, 1f));
		StarRenderer renderer = new StarRenderer( w, h );
		
		Set<StarNodeBinding> chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(1, 0, 0, 2f, 0.00f, 2f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 0, 2f, 0.33f, 2f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 0, 2f, 0.66f, 2f, starNode));
		starNode = CompoundNode.aggregate(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 6, 0.00f, -1, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 6, 0.25f, -0.8f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 6, 0.50f, -1, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 6, 0.75f, -0.8f, starNode));
		starNode = CompoundNode.aggregate(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 24, 0.00f, -0.5f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 24, 0.25f, -0.5f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 24, 0.50f, -0.5f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 24, 0.75f, -0.5f, starNode));
		starNode = CompoundNode.aggregate(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 96, 0.00f, -0.2f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 96, 0.25f, -0.2f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 96, 0.50f, -0.2f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 96, 0.75f, -0.2f, starNode));
		starNode = CompoundNode.aggregate(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 200, 0.00f, -0.05f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 200, 0.25f, -0.05f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 200, 0.50f, -0.05f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 200, 0.75f, -0.05f, starNode));
		starNode = CompoundNode.aggregate(chrilden);
		
		chrilden = new HashSet<StarNodeBinding>();
		chrilden.add(new StarNodeBinding(0, 1, 0, 500, 0.00f, -0.005f, starNode));
		chrilden.add(new StarNodeBinding(0, 1, 1, 500, 0.25f, -0.005f, starNode));
		chrilden.add(new StarNodeBinding(1, 1, 0, 500, 0.50f, -0.005f, starNode));
		chrilden.add(new StarNodeBinding(1, 0, 1, 500, 0.75f, -0.005f, starNode));
		starNode = CompoundNode.aggregate(chrilden);
		
		float gSize = 1500;
		for( int i=0; i<10; ++i, gSize*=2 ) {
			chrilden = new HashSet<StarNodeBinding>();
			chrilden.add(new StarNodeBinding(0, 1, 0, gSize, 0.1f*i+0.00f, -0.001f, starNode));
			chrilden.add(new StarNodeBinding(0, 1, 1, gSize, 0.1f*i+0.33f, -0.001f, starNode));
			chrilden.add(new StarNodeBinding(1, 1, 0, gSize, 0.1f*i+0.66f, -0.001f, starNode));
			chrilden.add(new StarNodeBinding(1, 0, 1,     0,        0.00f, -0.001f, starNode));
			starNode = CompoundNode.aggregate(chrilden);
		}
		
		float dt = 0.01f;
		final int totalFrameCount = 10*30*60;
		final int framesPerSuperframe = 3;
		int lastSuperframe = -1;
		final int miniframesPerFrame = 5;
		final int microframesPerFrame = 20;
		File outputDir = new File("output/stars3");
		if( !outputDir.exists() ) outputDir.mkdirs();
		
		final RenderBuffer background = new RenderBuffer(w,h);
		
		for( int frame=0; frame<totalFrameCount; ++frame ) {
			File outputFile = new File(outputDir, String.format("frame%08d.png", frame));
			if( outputFile.exists() ) continue;
			
			f.setTitle("Stars (Rendering frame "+frame+" of "+totalFrameCount+")");
			
			// Draw really far away stuff only once per superframe
			if( lastSuperframe >=0 && frame - lastSuperframe < framesPerSuperframe ) {
				renderer.copyFrom(background);
			} else {
				float time = (frame+framesPerSuperframe/2)*dt;
				float z = time*1500 - 40000;
				renderer.initCamera( z );
				renderer.clear();
				renderer.setDrawRange(30000, Float.POSITIVE_INFINITY);
				renderer.draw(time, starNode);
				background.copyFrom(renderer);
				lastSuperframe = frame;
			}
			
			// Draw medium-distance stuff once per frame
			renderer.setDrawRange(1500, 30000);
			{
				float time = (frame+0.5f)*dt;
				float z = time*1500 - 40000;
				renderer.initCamera( z );
				renderer.draw(time, starNode);
			}

			renderer.multiply(miniframesPerFrame);
			// Draw close stuff over and over
			renderer.setDrawRange(500f, 1500);
			for( int i=0; i<miniframesPerFrame; ++i ) {
				float time = (frame+(float)i/miniframesPerFrame)*dt;
				float z = time*1500 - 40000;
				renderer.initCamera( z );
				renderer.draw(time, starNode);
			}
			renderer.multiply(1f/miniframesPerFrame);
			
			renderer.multiply(microframesPerFrame);
			// Draw closer stuff over and overer
			renderer.setDrawRange(0.1f, 500);
			for( int i=0; i<microframesPerFrame; ++i ) {
				float time = (frame+(float)i/microframesPerFrame)*dt;
				float z = time*1500 - 40000;
				renderer.initCamera( z );
				renderer.draw(time, starNode);
			}
			renderer.multiply(1f/microframesPerFrame);
			
			renderer.multiply(10f);
			
			renderer.toRGB(pixBuf);
			synchronized( image ) {
				image.setRGB(0, 0, w, h, pixBuf, 0, w);
			}
			ic.setImage(image);
			
			ImageIO.write( image, "png", outputFile );
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
