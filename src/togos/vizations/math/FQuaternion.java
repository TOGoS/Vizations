package togos.vizations.math;

public final class FQuaternion
{
	public float w, x, y, z;
	
	public boolean isNormalized() {
		double l = w*w + x*x + y*y + z*z;
		return l > 0.99999 && l < 1.00001;
	}
}
