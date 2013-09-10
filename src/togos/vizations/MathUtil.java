package togos.vizations;

public class MathUtil
{
	static double fdmod( double num, double den ) {
		if( num == 0 || den == 0 ) return 0;
		return num - den * Math.floor(num / den);
	}	

	static int fdmod( int num, int den ) {
		return (int)fdmod( (double)num, den );
	}	
}
