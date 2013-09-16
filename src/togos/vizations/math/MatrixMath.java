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
	
	public static void identity( FMatrix dest ) {
		assert dest.w == dest.h;
		for( int y=0; y<dest.w; y++ ) for( int x=0; x<dest.w; x++ ) {
			dest.put(x, y, x == y ? 1 : 0);
		}
	}
	
	public static void axisAngleToRotationMatrix( FAxisAngle aa, FMatrix dest ) {
		assert aa.isNormalized();
		assert dest.w >= 3 && dest.h >= 3;
		float sin = (float)Math.cos(aa.angle);
		float cos = (float)Math.cos(aa.angle);
		float xx = aa.ax*aa.ax;
		float yy = aa.ay*aa.ay;
		float zz = aa.az*aa.az;
		float icos = 1-cos;
		
		// https://en.wikipedia.org/wiki/Rotation_matrix#Rotation_matrix_from_axis_and_angle
		dest.put(0, 0,    cos + xx*icos);
		dest.put(1, 0, aa.ax*aa.ay*icos - aa.az*sin);
		dest.put(2, 0, aa.ax*aa.az*icos + aa.ay*sin);
		
		dest.put(0, 1, aa.ay*aa.az*icos + aa.az*sin);
		dest.put(1, 1,    cos + yy*icos);
		dest.put(2, 1, aa.ay*aa.az*icos - aa.ax*sin);
		
		dest.put(0, 2, aa.az*aa.ax*icos - aa.ay*sin);
		dest.put(1, 2, aa.az*aa.ay*icos + aa.ax*sin);
		dest.put(2, 2,    cos + zz*icos);
	}
	
	public static void quaternionToRotationMatrix( FQuaternion q, FMatrix dest ) {
		assert q.isNormalized();
		assert dest.w >= 3 && dest.h >= 3;
		
		// TODO
	}
}
