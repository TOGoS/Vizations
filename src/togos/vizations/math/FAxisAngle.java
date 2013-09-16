package togos.vizations.math;

public final class FAxisAngle
{
	public float ax, ay, az, angle;

	public boolean isNormalized() {
		double len = Math.sqrt(ax*ax + ay*ay + az*az); 
		return len > 0.99999 && len < 1.00001; 
    } 
}
