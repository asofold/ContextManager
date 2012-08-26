package me.asofold.bpl.contextmanager.plshared.blocks;

/**
 * Simple hashable block location, all final.
 * @license public domain
 * @author mc_dev
 *
 */
public final class FBlockPos {
	
	private static final int p1 = 73856093;
    private static final int p2 = 19349663;
	private static final int p3 = 83492791;
	
	public static final int getHash(final int x, final int y, final int z) {
		return p1 * x ^ p2 * y ^ p3 * z;
	}

	public final int x;
	public final int y;
	public final int z;
	public final String w;
	final int h;
	@Override
	public final int hashCode(){
		return h;
	}
	@Override
	public final boolean equals(final Object obj) {
		if ( obj instanceof FBlockPos){
			FBlockPos other = (FBlockPos) obj;
			if (h != other.h) return false;
			return (x==other.x) && (z==other.z) && (y==other.y) && w.equals(other.w);
		}
		return false;
	}
	
	public FBlockPos(final String world, final int x, final int y, final int z){
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = world;
		this.h = getHash(x, y, z);
	}
	
	public final String toString(){
		return w+","+x+","+y+","+z;
	}
}
