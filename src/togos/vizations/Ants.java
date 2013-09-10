package togos.vizations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Ants
{
	interface EntityCheck {
		public boolean apply( Entity e );
	}
	interface Entity {
		public void update( Map m, int w, int h );
		public Color getColor();
	}
	static class SCEntity implements Entity {
		final Color c;
		public SCEntity(Color c) {
			this.c = c;
		}
		@Override public void update( Map m, int x, int y ) {}
		@Override public Color getColor() {
			return c;
		}
	}
	
	static int[] lookForDX = new int[] {-1, 0, 1, 1, 1, 0, -1, -1};
	static int[] lookForDY = new int[] {-1, -1, -1, 0, 1, 1, 1, 0};
	
	static class ActiveEntity extends SCEntity {
		int targetX, targetY;
		
		public ActiveEntity( Color c ) {
	        super(c);
        }
		
		static Random r = new Random();
		
		protected boolean lookFor( Map m, int x, int y, EntityCheck target ) {
			int init = r.nextInt(8);
			for( int d=0; d<8; ++d ) {
				int o = (d+init)&7;
				targetX = x+lookForDX[o];
				targetY = y+lookForDY[o];
				if( target.apply(m.get( targetX, targetY )) ) return true; 
			}
			return false;
		}
		
		public void putAtTarget( Map m, Entity thing ) {
			m.put( targetX, targetY, thing ); 
		}
		
		public void walkThereAndBecomeLeaving( Map m, int x, int y, Entity newSelf, Entity leaveBehind ) {
			m.put( x, y, leaveBehind );
			m.put( targetX, targetY, newSelf );
		}
		
		public void walkThereAndBecome( Map m, int x, int y, Entity newSelf ) {
			walkThereAndBecomeLeaving( m, x, y, newSelf, m.get(targetX, targetY) );
		}
		
		public void walkThere( Map m, int x, int y ) {
			walkThereAndBecome( m, x, y, this );
		}
	}
	
	static final EntityCheck IS_UNMARKED_GRASS = new EntityCheck() {
		@Override public boolean apply( Entity e ) {
	        return e == GRASS;
        }
	};
	static final EntityCheck IS_FOOD = new EntityCheck() {
		@Override public boolean apply( Entity e ) {
	        return e == FOOD;
        }
	};
	static final EntityCheck IS_NEAR_FOOD = new EntityCheck() {
		@Override public boolean apply( Entity e ) {
	        return e == GRASS_WITH_FOOD_MARKER;
        }
	};
	static final EntityCheck IS_NEAR_FEEDEE = new EntityCheck() {
		@Override public boolean apply( Entity e ) {
	        return e == GRASS_WITH_FEEDEE_MARKER;
        }
	};
	static final EntityCheck IS_FEEDEE = new EntityCheck() {
		@Override public boolean apply( Entity e ) {
	        return e == FEEDEE;
        }
	};
	static final EntityCheck IS_WALKABLE = new EntityCheck() {
		@Override public boolean apply( Entity e ) {
	        return e == GRASS || e == GRASS_WITH_FOOD_MARKER || e == GRASS_WITH_FEEDEE_MARKER;
        }
	};
	
	static final Entity GRASS = new SCEntity(new Color(0.1f, 0.6f, 0.1f));
	static final Entity FOOD = new SCEntity(new Color(0.6f, 0.6f, 0.1f));
	static final Entity FEEDEE = new SCEntity(new Color(0.8f, 0.3f, 0.1f));
	static final Entity FED_FEEDEE = new ActiveEntity(new Color(1, 0.6f, 0.6f)) {
		@Override public void update( Map m, int x, int y ) {
			if( r.nextInt(20) < 1 ) {
				m.put( x, y, FEEDEE );
			}
		}
	};
	static final Entity GRASS_WITH_FOOD_MARKER = new ActiveEntity(new Color(0.6f, 0.8f, 0.3f)) {
		@Override public void update( Map m, int x, int y ) {
			if( r.nextInt(100) < 1 ) {
				m.put( x, y, GRASS );
			}
		}
	};
	static final Entity GRASS_WITH_FEEDEE_MARKER = new ActiveEntity(new Color(0.3f, 0.6f, 0.6f)) {
		@Override public void update( Map m, int x, int y ) {
			if( r.nextInt(100) < 1 ) {
				m.put( x, y, GRASS );
			}
		}
	};
	static final Entity WATER = new SCEntity(new Color(0.1f, 0.1f, 0.5f));
	static final Entity ANT_WITH_FOOD = new ActiveEntity(new Color(0.3f, 0, 0)) {
		@Override public void update( Map m, int x, int y ) {
			if( lookFor(m, x, y, IS_FEEDEE) ) {
				m.put( targetX, targetY, FED_FEEDEE );
				m.put( x, y, ANT_EXITED_ABOUT_JOB );
			} else if( (r.nextInt(10) < 8 && lookFor(m, x, y, IS_NEAR_FEEDEE)) || lookFor(m, x, y, IS_WALKABLE) ) {
				walkThere( m, x, y );
			}
		}
	};
	static final Entity EXCITED_ANT_WITH_FOOD = new ActiveEntity(new Color(0.3f, 0, 0)) {
		@Override public void update( Map m, int x, int y ) {
			Entity newSelf = r.nextInt(15) < 1 ? ANT_WITH_FOOD : this;
			if( lookFor(m, x, y, IS_FEEDEE) ) {
				m.put( targetX, targetY, FED_FEEDEE );
				while( lookFor(m, x, y, IS_UNMARKED_GRASS) ) {
					putAtTarget(m, GRASS_WITH_FEEDEE_MARKER);
				}
				m.put( x, y, ANT_EXITED_ABOUT_JOB );
			} else if( (r.nextInt(10) < 8 && lookFor(m, x, y, IS_NEAR_FEEDEE)) || lookFor(m, x, y, IS_WALKABLE) ) {
				walkThereAndBecomeLeaving(m, x, y, newSelf, GRASS_WITH_FOOD_MARKER);
			}
		}
	};
	static final Entity ANT_EXITED_ABOUT_JOB = new ActiveEntity(new Color(0.3f, 0, 0)) {
		@Override public void update( Map m, int x, int y ) {
			Entity newSelf = r.nextInt(15) < 1 ? ANT : this;
			if( lookFor(m, x, y, IS_FOOD)) {
				m.put(targetX, targetY, GRASS_WITH_FOOD_MARKER);
				m.put(x, y, EXCITED_ANT_WITH_FOOD);
			} else if( (r.nextInt(10) < 8 && lookFor(m, x, y, IS_NEAR_FOOD)) ) {
				walkThereAndBecome( m, x, y, newSelf );
			} else if( lookFor(m, x, y, IS_WALKABLE) ) {
				walkThereAndBecomeLeaving( m, x, y, newSelf, GRASS_WITH_FEEDEE_MARKER );
			}
		}
	};
	static final Entity ANT = new ActiveEntity(new Color(0, 0, 0)) {
		@Override public void update( Map m, int x, int y ) {
			if( lookFor(m, x, y, IS_FOOD)) {
				m.put(targetX, targetY, GRASS_WITH_FOOD_MARKER);
				m.put(x, y, EXCITED_ANT_WITH_FOOD);
			} else if( (r.nextInt(10) < 8 && lookFor(m, x, y, IS_NEAR_FOOD)) || lookFor(m, x, y, IS_WALKABLE) ) {
				walkThere( m, x, y );
			}
		}
	};
	
	static class Map {
		final int w, h;
		final Entity[] data;
		
		public Map( int w, int h ) {
			this.w = w;
			this.h = h;
			this.data = new Entity[w*h];
			for( int i=0; i<data.length; ++i ) {
				data[i] = GRASS;
			}
		}
		
		public Entity get( int x, int y ) {
			x = MathUtil.fdmod(x, w);
			y = MathUtil.fdmod(y, h);
			return data[w*y+x];
		}
		
		public void put( int x, int y, Entity dat ) {
			x = MathUtil.fdmod(x, w);
			y = MathUtil.fdmod(y, h);
			data[w*y+x] = dat;
		}
	}
	
	static void draw( Map m, Graphics g ) {
		for( int y=0; y<m.h; ++y ) for( int x=0; x<m.w; ++x ) {
			g.setColor(m.data[m.w*y+x].getColor());
			g.fillRect(x*4, y*4, 4, 4);
		}
	}
	
	Random r = new Random();
	final Map map = new Map( 640/4, 360/4 );
	
	void initMap() {
		int rx = r.nextInt(map.w);
		int ry = r.nextInt(map.h);
		for( int i=0; i<16384; ++i ) {
			map.put(rx, ry, WATER);
			switch( r.nextInt(4) ) {
			case 0: ++rx; break;
			case 1: --rx; break;
			case 2: ++ry; break;
			case 3: --ry; break;
			}
		}
		
		for( int i=0; i<256; ++i ) {
			Entity put;
			switch( r.nextInt(3) ) {
			case 0: put = ANT; break;
			default: put = FEEDEE; break;
			}
			map.put( r.nextInt(map.w), r.nextInt(map.h), put );
		}
		
		for( int i=0; i<64; ++i ) {
			int cx = r.nextInt(map.w);
			int cy = r.nextInt(map.h);
			for( int dy=0; dy<5; ++dy ) for( int dx=0; dx<5; ++dx ) {
				map.put(cx+dx, cy+dy, FOOD);
			}
		}
	}
	
	void updateMap() {
		for( int y=0; y<map.h; ++y ) for( int x=0; x<map.w; ++x ) {
			map.get(x, y).update(map, x, y);
		}
	}
	
	public void runInWindow() throws Exception {
		final Frame f = new Frame("PPP1");
		final ImageCanvas c = new ImageCanvas();
		c.setPreferredSize( new Dimension(640, 360) );
		f.add(c);
		f.pack();
		f.addWindowListener( new WindowAdapter() {
			@Override public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});
		f.setVisible(true);
		
		initMap();
		
		BufferedImage img = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
		for( int frame=0; frame<Integer.MAX_VALUE; ++frame ) {
			draw(map, img.getGraphics());
			c.setImage(img);
			updateMap();
			Thread.sleep(10);
		}
	}
	
	public static void main(String[] args) {
		try {
	        new Ants().runInWindow();
        } catch( Exception e ) {
	        e.printStackTrace();
	        System.exit(1);
        }
	}	
}
