package togos.vizations.math;

public class MatrixMath
{
	private MatrixMath() { }
	
	public static void add( FMatrix a, FMatrix b, FMatrix dest ) {
		dest.fill(a);
		dest.addInPlace(b);
	}
	
	public static void multiply( FMatrix a, FMatrix b, FMatrix dest ) {
		assert a.w == b.h;
		assert dest.h == a.h;
		assert dest.w == b.w;
		
		for( int y=0; y<dest.h; ++y ) for( int x=0; x<dest.w; ++x ) {
			float v = 0;
			for( int d=0; d<a.w; ++d ) {
				v += a.get(y, d) * b.get(x, d);
			}
			dest.put(x, y, v);
		}
	}
}
