package togos.vizations.math;

import java.util.Arrays;

public final class FMatrix
{
	public final int w, h;
	private final float[] data;
	
	public FMatrix( int w, int h ) {
		this.w = w;  this.h = h;
		this.data = new float[w*h];
	}
	
	public float get( int x, int y ) {
		return data[w*y+x];
	}
	
	public void put( int x, int y, float v ) {
		data[w*y+x] = v;
	}
	
	public void clear() {
		Arrays.fill(data, 0);
	}
	
	public void fill( FMatrix b ) {
		assert b.w == this.w;
		assert b.h == this.h;
		for( int i=w*h-1; i>=0; --i ) data[i] = b.data[i];
	}
	
	public void addInPlace( FMatrix b ) {
		assert b.w == this.w;
		assert b.h == this.h;
		for( int i=w*h-1; i>=0; --i ) data[i] += b.data[i];
	}
	
	public FMatrix add( FMatrix b ) {
		FMatrix result = new FMatrix(this.w, this.h);
		MatrixMath.add(this, b, result);
		return result;
	}
	
	public FMatrix multiply( FMatrix b ) {
		FMatrix result = new FMatrix(b.w, this.h);
		MatrixMath.multiply(this, b, result);
		return result;
	}
}
