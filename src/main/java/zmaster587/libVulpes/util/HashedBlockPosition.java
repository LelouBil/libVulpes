package zmaster587.libVulpes.util;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public class HashedBlockPosition  {
	public int x,z;
	public short y;
	
	
	public HashedBlockPosition(int x, int y,int z) {
		this.x = x;
		this.y = (short)y;
		this.z = z;
	}
	
	public HashedBlockPosition(BlockPos pos) {
		this.x = pos.getX();
		this.y = (short)pos.getY();
		this.z = pos.getZ();
	}
	
	public HashedBlockPosition(Vector3d pos) {
		this.x = (int) pos.getX();
		this.y = (short) pos.getY();
		this.z = (int) pos.getZ();
	}
	
	public BlockPos getBlockPos() {
		return new BlockPos(x,y,z);
	}
	
	/**
	 * @param dx x offset
	 * @param dy y offset
	 * @param dz z offset
	 * @return a new object containing the coordinates of that offset
	 */
	public HashedBlockPosition getPositionAtOffset(int dx, int dy, int dz) {
		return new HashedBlockPosition(dx + x, dy + y, dz + z);
	}
	
	public HashedBlockPosition getPositionAtOffset(Direction facing) {
		return new HashedBlockPosition(facing.getXOffset() + x, facing.getYOffset() + y, facing.getZOffset() + z);
	}
	
	public double getDistance(HashedBlockPosition otherPos) {
		return Math.sqrt(Math.pow(x-otherPos.x, 2) + Math.pow(y-otherPos.y, 2) + Math.pow(z-otherPos.z, 2));
	}
	
	@Override
	public int hashCode() {
		return (x*179425423) ^ (z*179426549)  ^ (y*179424691);
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj instanceof HashedBlockPosition) {
			return this.x == ((HashedBlockPosition) obj).x && this.y == ((HashedBlockPosition) obj).y && this.z == ((HashedBlockPosition) obj).z;
		}
		
		return super.equals(obj);
	}
	
	@Override
	public String toString() {
		return this.x + " " + this.y + " " + this.z;
	}
}
