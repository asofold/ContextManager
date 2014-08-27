package me.asofold.bpl.contextmanager.plshared;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import me.asofold.bpl.contextmanager.plshared.blocks.FBlockPos;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;


public class Blocks {
	/**
	 * Simply the orthogonal directions. It is final, but the elements could be chnaged - please don't do that...
	 */
	public static final BlockFace[] orthogonalFaces = new BlockFace[]{
			BlockFace.DOWN,
			BlockFace.UP,
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.EAST,
			BlockFace.WEST,
	};
	
	/**
	 * @deprecated use static{}
	 */
	public static void init(){
		
	}
	
	
	
	/**
	 * return a list with neighbouring blocks of given material - orthogonal neighbours.
	 * @param block
	 * @param mat
	 * @return
	 */
	public static final List<Block> getNeighbourBlocks(final Block block, final Material mat){
		final List<Block> blocks = new LinkedList<Block>();
		Block neighbour;
		for ( final BlockFace face : Blocks.orthogonalFaces){
			 neighbour = block.getRelative(face); 
			 if (neighbour.getType().compareTo(mat) == 0 ){
				 blocks.add(neighbour);
			 }
		}
		return blocks;
	}

	/**
	 * Check if the block is one of the blocks you step on for nether portal, with all other blocks being in place too.
	 * @param block
	 * @return
	 */
	public static final boolean isNetherPortalPossible(final Block base) {
		if ( base.getType()!=Material.OBSIDIAN) return false;
		Block block;
		for ( final BlockFace startFace : new BlockFace[ ]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH}){
			block = base;
			boolean air = true;
			for ( int i = 1; i<4; i++){
				if ( block.getRelative(BlockFace.UP, i).getType()!=Material.AIR) {
					air=false;
					break;
				}
			}
			if (!air) continue;
			final BlockFace oppositeFace = startFace.getOppositeFace();
			block = block.getRelative(startFace);
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			air = true;
			for ( int i = 1; i<4; i++){
				if ( block.getRelative(BlockFace.UP, i).getType()!=Material.AIR) {
					air=false;
					break;
				}
			}
			if (!air) continue;
			block = block.getRelative(startFace); block = block.getRelative(BlockFace.UP);
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			block = block.getRelative(BlockFace.UP);
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			block = block.getRelative(BlockFace.UP);
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			block = block.getRelative(BlockFace.UP);block = block.getRelative(oppositeFace); 
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			block = block.getRelative(oppositeFace); 
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			block = block.getRelative(oppositeFace); block = block.getRelative(BlockFace.DOWN);
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			block = block.getRelative(BlockFace.DOWN);
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			block = block.getRelative(BlockFace.DOWN);
			if ( block.getType()!=Material.OBSIDIAN ) continue;
			return true;
		}
		return false;
	}
	
	/**
	 * check if all blocks are of the same material.
	 * @return
	 */
	public static final boolean checkMaterial(final Collection<Block> blocks, final Material material){
		for ( Block block : blocks ){
			if (block.getType() != material) return false;
		}
		return true;
	}
	
	public static final boolean sameCoords( final double x1, final double y1, final double z1, final double x2, final double y2, final double z2, final double tol){
		return ((Math.abs(x1-x2)<=tol) && (Math.abs(y1-y2)<=tol) && (Math.abs(z1-z2)) <= tol);
	}
	
	public static final String format(final double x, final double y, final double z ){
		return format(x,y,z,new DecimalFormat("#.##"));
	}
	public static final String format(final double x, final double y, final double z , final DecimalFormat f){
		return "x="+f.format(x)+" y="+f.format(y)+" z="+f.format(z);
	}

	public static final boolean sameCoords(final Location loc1, final Location loc2, final double tol) {
		if ( (loc1 == null) || (loc2==null) ) return false;
		if ( !loc1.getWorld().getName().equals(loc2.getWorld().getName())) return false;
		return sameCoords(loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getX(), loc2.getY(), loc2.getZ(), tol);
	}

	/**
	 * SPECIAL : blockx except for Y
	 * @param loc
	 * @param lastFake
	 * @param d
	 * @return
	 */
	public static final boolean sameYCoords(final Location loc1, final Location loc2, final double tol) {
		if ( (loc1 == null) || (loc2==null) ) return false;
		if ( !loc1.getWorld().getName().equals(loc2.getWorld().getName())) return false;
		return (loc1.getBlockX()==loc2.getBlockX()) && (loc1.getBlockZ()==loc2.getBlockZ()) && (Math.abs(loc1.getBlockY()-loc2.getBlockY())<=tol);
	}
	
	public static final String blockString(final Location loc){
		return loc.getWorld().getName()+","+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
	}
	public static final String blockString(final Block loc){
		return loc.getWorld().getName()+","+loc.getX()+","+loc.getY()+","+loc.getZ();
	}

	public static final boolean sameYCoords(final Block block, final Location loc) {
		if (loc == null) return false;
		return (loc.getBlockY()==block.getY());
	}

	public static final boolean sameCoords(final Block block, final Block block2) {
		return (block.getX()==block2.getX()) && (block.getY()==block2.getY()) && (block.getZ()==block2.getZ());
	}
	
	/**
	 * Block distance.
	 * @param x1
	 * @param y1
	 * @param z1
	 * @param x2
	 * @param y2
	 * @param z2
	 * @param d
	 * @return
	 */
	public static final boolean exceedsDistance(final int x1, final int y1, final int z1, final int x2 , final int y2, final int z2, final int d){
		if (Math.abs(x1-x2)>d) return true;
		if (Math.abs(y1-y2)>d) return true;
		if (Math.abs(z1-z2)>d) return true;
		return false;
	}
	
	public static final boolean exceedsDistance(final FBlockPos pos1, final String w2, final int x2 , final int y2, final int z2, final int d ){
		if ( exceedsDistance(pos1.x, pos1.y, pos1.z,x2,y2,z2,d)) return true;
		if (pos1.w.equals(w2)) return true;
		return false;
	}
	
	public static final boolean exceedsDistance(final FBlockPos pos1, final FBlockPos pos2, final int d){
		return exceedsDistance(pos1, pos2.w, pos2.x, pos2.y, pos2.z, d);
	}
	
	public static final FBlockPos FBlockPos(final Location loc){
		return new FBlockPos(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public static final FBlockPos FBlockPos(final Block block) {
		return new FBlockPos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}
	
	public static final boolean exceedsDistance(final Location loc1, final Location loc2,
			double dist) {
		if (!loc1.getWorld().equals(loc2.getWorld()) ) return true;
		final double ref = loc1.distance(loc2);
		return Double.isNaN(ref)|| (ref > dist);
	}
	
	/**
	 * Block coordinate for double, especially important for negative numbers.
	 * (Adapted From Bukkit/NumberConversions.)
	 * @param x
	 * @return
	 */
	public static final int floor(final double x) {
        final int floor = (int) x;
        return (floor == x)? floor : floor - (int) (Double.doubleToRawLongBits(x) >>> 63);
    }
}
