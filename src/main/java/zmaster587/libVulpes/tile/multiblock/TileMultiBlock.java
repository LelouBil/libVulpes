package zmaster587.libVulpes.tile.multiblock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.LibVulpesBlocks;
import zmaster587.libVulpes.block.BlockMeta;
import zmaster587.libVulpes.block.BlockTile;
import zmaster587.libVulpes.block.RotatableBlock;
import zmaster587.libVulpes.block.multiblock.BlockMultiBlockComponentVisible;
import zmaster587.libVulpes.block.multiblock.BlockMultiblockStructure;
import zmaster587.libVulpes.block.multiblock.IHidableBlock;
import zmaster587.libVulpes.tile.IMultiblock;
import zmaster587.libVulpes.tile.TilePointer;
import zmaster587.libVulpes.tile.TileSchematic;
import zmaster587.libVulpes.tile.multiblock.hatch.TileFluidHatch;
import zmaster587.libVulpes.tile.multiblock.hatch.TileInputHatch;
import zmaster587.libVulpes.tile.multiblock.hatch.TileOutputHatch;
import zmaster587.libVulpes.util.IFluidHandlerInternal;
import zmaster587.libVulpes.util.Vector3F;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TileMultiBlock extends TileEntity {

	/*CanRender must be seperate from incomplete because some multiblocks must be completed on the client but
	because chunks on the client.  It is also used to determine if the block on the server has ever been complete */
	protected boolean completeStructure, canRender;
	protected byte timeAlive = 0;

	protected LinkedList<IInventory> itemInPorts = new LinkedList<>();
	protected LinkedList<IInventory> itemOutPorts = new LinkedList<>();

	protected LinkedList<IFluidHandlerInternal> fluidInPorts = new LinkedList<>();
	protected LinkedList<IFluidHandlerInternal> fluidOutPorts = new LinkedList<>();

	protected static HashMap<Character, List<BlockMeta>> charMapping = new HashMap<>();

	public TileMultiBlock(TileEntityType<?> type) {
		super(type);
		completeStructure = false;
		canRender = false;
	}

	public static void addMapping(char character, List<BlockMeta> listToAdd) {
		if(charMapping.containsKey(character))
			LibVulpes.logger.warn("Overwritting Multiblock mapping of \"" + character + "\"");
		charMapping.put(character, listToAdd);
	}

	public static List<BlockMeta> getMapping(char character) {
		return charMapping.get(character);
	}
	
	public List<IInventory> getItemInPorts() {
		for(int i = 0; i < itemInPorts.size(); i++) {
			if(itemInPorts.get(i) instanceof TileEntity) {
				TileEntity newTile = world.getTileEntity(((TileEntity)itemInPorts.get(i)).getPos());
				if (newTile instanceof IInventory)
					itemInPorts.set(i, (IInventory)newTile);
			}
		}
		return itemInPorts;
	}
	
	public List<IInventory> getItemOutPorts() {
		for(int i = 0; i < itemOutPorts.size(); i++) {
			if(itemOutPorts.get(i) instanceof TileEntity) {
				TileEntity newTile = world.getTileEntity(((TileEntity)itemOutPorts.get(i)).getPos());
				if (newTile instanceof IInventory)
					itemOutPorts.set(i, (IInventory)newTile);
			}
		}
		return itemOutPorts;
	}

	/**
	 * Note: it may be true on the server but not the client.  This is because the client needs to form the multiblock
	 * so the tile has references to other blocks in its structure for gui display etc
	 * @return true if the structure is complete
	 */
	public boolean isComplete() {
		return completeStructure;
	}

	/**
	 * 
	 * @return true if the block should be rendered as complete
	 */
	@OnlyIn(value=Dist.CLIENT)
	public boolean canRender() {
		return canRender;
	}

	/**
	 * @return the unlocalized name of the machine
	 */
	public String getMachineName() {
		return "";
	}

	public void setMachineRunning(boolean running) {
		world.setBlockState(pos, world.getBlockState(pos).with(BlockTile.STATE, running), 2);
	}

	public boolean isUsableByPlayer(PlayerEntity player) {
		return player.getDistanceSq(this.pos.getX(), this.pos.getY(), this.pos.getZ()) < 64*64;
	}


	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putBoolean("canRender", canRender);
		writeNetworkData(nbt);
		return new SUpdateTileEntityPacket(this.pos, 0, nbt);
	}

	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putBoolean("canRender", canRender);
		write(nbt);
		return nbt;
	}

	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT nbt) {

		canRender = nbt.getBoolean("canRender");
		readNetworkData(nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		CompoundNBT nbt = pkt.getNbtCompound();

		canRender = nbt.getBoolean("canRender");
		readNetworkData(nbt);
	}

	public void invalidateComponent(TileEntity tile) {
		setComplete(false);
	}

	/**Called by inventory blocks that are part of the structure
	 ** This includes recipe management etc
	 **/
	public void onInventoryUpdated() {
	}

	/**
	 * @param world world
	 * @param destroyedPos coords of destroyed block
	 * @param blockBroken set true if the block is being broken, otherwise some other means is being used to disassemble the machine
	 */
	public void deconstructMultiBlock(World world, BlockPos destroyedPos, boolean blockBroken, BlockState state) {
		canRender = completeStructure = false;
		if(this.pos.compareTo(destroyedPos) != 0 && world.getBlockState(pos).getBlock() instanceof BlockTile) 
			world.setBlockState(this.pos, world.getBlockState(pos).with(BlockTile.STATE, false));



		//UNDO all the placeholder blocks
		Direction front = getFrontDirection(state);

		Object[][][] structure = getStructure();
		Vector3F<Integer> offset = getControllerOffset(structure);


		//Mostly to make sure IMultiblocks lose their choke-hold on this machines and to revert placeholder blocks
		for(int y = 0; y < structure.length; y++) {
			for(int z = 0; z < structure[0].length; z++) {
				for(int x = 0; x< structure[0][0].length; x++) {

					int globalX = pos.getX() + (x - offset.x)*front.getZOffset() - (z-offset.z)*front.getXOffset();
					int globalY = pos.getY() - y + offset.y;
					int globalZ = pos.getZ() - (x - offset.x)*front.getXOffset()  - (z-offset.z)*front.getZOffset();


					TileEntity tile = world.getTileEntity(new BlockPos(globalX, globalY, globalZ));
					//This block is being broken anyway so don't bother
					if(blockBroken 
					&& globalX == destroyedPos.getX() 
					&& globalY == destroyedPos.getY() 
					&& globalZ == destroyedPos.getZ()) {
						if(tile instanceof IMultiblock) {
							((IMultiblock)tile).setIncomplete();
						}
						tile.remove();
						continue;
					}
					Block block = world.getBlockState(new BlockPos(globalX, globalY, globalZ)).getBlock();

					destroyBlockAt(new BlockPos(globalX, globalY, globalZ), block, tile);

				}
			}
		}

		resetCache();
		this.markDirty();
		world.notifyBlockUpdate(pos, world.getBlockState(pos),  world.getBlockState(pos), 3);
	}

	/**
	 * Called when the multiblock is being deconstructed.  This is called for each block in the structure.
	 * Provided in case of special handling
	 * @param destroyedPos coords of destroyed block
	 * @param block
	 * @param tile
	 */
	protected void destroyBlockAt(BlockPos destroyedPos, Block block, TileEntity tile) {

		//if it's an instance of multiblock structure call its destroyStructure method
		if(block instanceof BlockMultiblockStructure) {
			((BlockMultiblockStructure)block).destroyStructure(world, destroyedPos, world.getBlockState(destroyedPos));
		}
		
		if(block instanceof IHidableBlock) {
			((IHidableBlock)block).showBlock(world, destroyedPos, world.getBlockState(destroyedPos));
		}
		
		//If the the tile is a placeholder then make sure to replace it with its original block and tile
		if(tile instanceof TilePlaceholder && !(tile instanceof TileSchematic)) {
			TilePlaceholder placeholder = (TilePlaceholder)tile;

			//Must set incomplete BEFORE changing the block to prevent stack overflow!
			placeholder.setIncomplete();

			world.setBlockState(destroyedPos, placeholder.getReplacedState());

			//Dont try to set a tile if none existed
			if(placeholder.getReplacedTileEntity() != null) {
				CompoundNBT nbt = new CompoundNBT();
				placeholder.getReplacedTileEntity().write(nbt);

				world.getTileEntity(destroyedPos).deserializeNBT(nbt);
			}
		}
		//Make all pointers incomplete
		else if(tile instanceof IMultiblock) {
			((IMultiblock)tile).setIncomplete();
			tile.remove();
		}
	}

	public Direction getFrontDirection(BlockState state) {
		return RotatableBlock.getFront(state);
	}

	public Object[][][] getStructure() {
		return null;
	}

	public boolean attemptCompleteStructure(BlockState state) {
		//if(!completeStructure)
		canRender = completeStructure = completeStructure(state);
		return completeStructure;
	}

	public void setComplete(boolean complete) {
		completeStructure = complete;
	}

	public List<BlockMeta> getAllowableWildCardBlocks() {
		return new ArrayList<>();
	}

	/**
	 * Called when cached Tiles need to be cleared (batteries/IO/etc)
	 */
	public void resetCache() {
		itemInPorts.clear();
		itemOutPorts.clear();
		fluidInPorts.clear();
		fluidOutPorts.clear();
	}


	/**
	 * Use '*' to allow any kind of Hatch, or energy device or anything returned by getAllowableWildcards
	 * Use 'L' for liquid input hatches
	 * Use 'l' for liquid output hatches
	 * Use 'I' for input hatch
	 * Use 'O' for output hatch
	 * Use 'P' for power input
	 * Use 'p' for power output
	 * Use 'D' for data hatch
	 * Use 'c' for the main Block, there can only be one
	 * Use null for anything
	 * Use a Block to force the user to place that block there
	 * @return true if the structure is valid
	 */
	protected boolean completeStructure(BlockState state) {

		//Make sure the environment is clean
		resetCache();

		Object[][][] structure = getStructure();

		Vector3F<Integer> offset = getControllerOffset(structure);
		List<BlockPos> replacableBlocks = new LinkedList<>();

		Direction front = getFrontDirection(state);

		//Store tile entities for later processing so we don't risk the check failing halfway through leaving half the multiblock assigned
		LinkedList<TileEntity> tiles = new LinkedList<>();

		for(int y = 0; y < structure.length; y++) {
			for(int z = 0; z < structure[0].length; z++) {
				for(int x = 0; x< structure[0][0].length; x++) {

					//Ignore nulls
					if(structure[y][z][x] == null)
						continue;

					int globalX = pos.getX() + (x - offset.x)*front.getZOffset() - (z-offset.z)*front.getXOffset();
					int globalY = pos.getY() - y + offset.y;
					int globalZ = pos.getZ() - (x - offset.x)*front.getXOffset()  - (z-offset.z)*front.getZOffset();
					BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);

					if(!world.isBlockLoaded(globalPos))
						return false;

					TileEntity tile = world.getTileEntity(globalPos);
					BlockState blockState = world.getBlockState(globalPos);
					Block block = blockState.getBlock();
					int meta = block.getStateId(blockState);

					if(block == LibVulpesBlocks.blockPhantom)
						return false;

					if(tile != null)
						tiles.add(tile);

					//If the other block already thinks it's complete just assume valid
					if(tile instanceof TilePointer) {
						TileEntity masterBlock;
						if(((IMultiblock)tile).hasMaster() && (masterBlock = ((IMultiblock)tile).getMasterBlock()) != this) {
							
							//This ~should~ only occur with world edits and being moved to space stations and such
							if(masterBlock == null)
								((IMultiblock)tile).setMasterBlock(this.pos);
							
							if(((IMultiblock)tile).getMasterBlock().getPos().equals(getPos())) {
								((IMultiblock)tile).setMasterBlock(getPos());
								continue;
							}
						}
						else if(((IMultiblock)tile).getMasterBlock() == this) 
							continue;
					}
					//Make sure the structure is valid
					if(!(structure[y][z][x] instanceof Character && (Character)structure[y][z][x] == 'c') && !(structure[y][z][x] instanceof Block && structure[y][z][x] == Blocks.AIR && world.isAirBlock(globalPos)) && !getAllowableBlocks(structure[y][z][x]).contains(new BlockMeta(blockState))) {

						/*//Can it be replaced?
							replacableBlocks.add(globalPos);
						else {*/
							LibVulpes.proxy.spawnParticle("errorBox", world, globalX, globalY, globalZ, 0, 0, 0);
							return false;
						//}
					}
				}
			}
		}

		//Notify all blocks in the structure that it's being build and assimilate them
		for(int y = 0; y < structure.length; y++) {
			for(int z = 0; z < structure[0].length; z++) {
				for(int x = 0; x< structure[0][0].length; x++) {

					int globalX = pos.getX() + (x - offset.x)*front.getZOffset() - (z-offset.z)*front.getXOffset();
					int globalY = pos.getY() - y + offset.y;
					int globalZ = pos.getZ() - (x - offset.x)*front.getXOffset()  - (z-offset.z)*front.getZOffset();
					BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);


					TileEntity tile = world.getTileEntity(globalPos);
					BlockState blockState = world.getBlockState(globalPos);
					Block block = blockState.getBlock();
					int meta = block.getStateId(blockState);

					if(block instanceof BlockMultiBlockComponentVisible) {
						if(shouldHideBlock(world, globalPos, blockState))
							((BlockMultiBlockComponentVisible)block).hideBlock(world, globalPos, blockState);

						tile = world.getTileEntity(globalPos);

						if(tile instanceof IMultiblock)
							((IMultiblock)tile).setComplete(globalPos);
					}
					else if(block instanceof IHidableBlock) {
						if(shouldHideBlock(world, globalPos, blockState))
							((IHidableBlock)block).hideBlock(world, globalPos, blockState);
					}

					if(structure[y][z][x] != null && !block.isAir(blockState, world, globalPos) && !(tile instanceof IMultiblock) && !(tile instanceof TileMultiBlock)) {
						replaceStandardBlock(globalPos, blockState, tile);
					}
				}
			}
		}

		//Now that we know the multiblock is valid we can assign
		for(TileEntity tile : tiles) {
			integrateTile(tile);
		}
		
		//Replace and drop replaceable blocks
		for(BlockPos pos : replacableBlocks) {
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
		}
		
		markDirty();
		world.notifyBlockUpdate(pos, world.getBlockState(pos),  world.getBlockState(pos), 3);
		return true;
	}


	public List<BlockMeta> getAllowableBlocks(Object input) {
		if(input instanceof Character && (Character)input == '*') {
			return getAllowableWildCardBlocks();
		}
		else if(input instanceof Character  && charMapping.containsKey(input)) {
			return charMapping.get(input);
		}
		else if(input instanceof String) { //OreDict entry
			if(BlockTags.getCollection().getRegisteredTags().contains(new ResourceLocation((String)input)))
				return BlockTags.getCollection().get(new ResourceLocation((String)input)).getAllElements().stream().map(value -> new BlockMeta(value.getDefaultState())).collect(Collectors.toList());
			LibVulpes.logger.warn(String.format("No ore dictionary entry for '%s' in machine %s", input, getMachineName()));
		}
		else if(input instanceof ResourceLocation) { //OreDict entry
			if(BlockTags.getCollection().getRegisteredTags().contains((ResourceLocation)input))
				return BlockTags.getCollection().get((ResourceLocation)input).getAllElements().stream().map(value -> new BlockMeta(value.getDefaultState())).collect(Collectors.toList());
			LibVulpes.logger.warn(String.format("No ore dictionary entry for '%s' in machine %s", input,  getMachineName()));
		}
		else if(input instanceof Block) {
			List<BlockMeta> list = new ArrayList<>();
			list.add(new BlockMeta(((Block) input).getDefaultState(), true));
			return list;
		}
		else if(input instanceof BlockMeta) {
			List<BlockMeta> list = new ArrayList<>();
			list.add((BlockMeta) input);
			return list;
		}
		else if(input instanceof Block[]) {
			List<BlockMeta> list = new ArrayList<>();
			for(Block b : (Block[])input) list.add(new BlockMeta(b.getDefaultState(), true));
			return list;
		}
		else if(input instanceof List) {
			return (List<BlockMeta>)input;
		}
		return new ArrayList<>();
	}

	public boolean shouldHideBlock(World world, BlockPos pos, BlockState tile) {
		return false;
	}

	/**
	 * Called when replacing a block that is not specifically designed to be compatible with the multiblocks.  Eg iron black
	 * Most multiblocks have a renderer and so these blocks are converted to an invisible pointer
	 * @return
	 */
	protected void replaceStandardBlock(BlockPos newPos, BlockState state, TileEntity tile) {

		world.setBlockState(newPos, LibVulpesBlocks.blockPlaceHolder.getDefaultState());
		TilePlaceholder newTile = (TilePlaceholder)world.getTileEntity(newPos);

		newTile.setReplacedBlockState(state);
		newTile.setReplacedTileEntity(tile);
		newTile.setMasterBlock(pos);
	}

	/**
	 * This is used so classes extending this one can have their own handling of tiles without overriding the method
	 * @param tile Current tile in multiblock
	 */
	protected void integrateTile(TileEntity tile) {
		if(tile instanceof IMultiblock)
			((IMultiblock) tile).setComplete(pos);

		if(tile instanceof TileInputHatch)
			itemInPorts.add((IInventory) tile);
		else if(tile instanceof TileOutputHatch) 
			itemOutPorts.add((IInventory) tile);
		else if(tile instanceof TileFluidHatch) {
			TileFluidHatch liquidHatch = (TileFluidHatch)tile;
			if(liquidHatch.isOutputOnly())
				fluidOutPorts.add(liquidHatch);
			else
				fluidInPorts.add(liquidHatch);
		}
	}

	protected Vector3F<Integer> getControllerOffset(Object[][][] structure) {
		for(int y = 0; y < structure.length; y++) {
			for(int z = 0; z < structure[0].length; z++) {
				for(int x = 0; x< structure[0][0].length; x++) {
					if(structure[y][z][x] instanceof Character && (Character)structure[y][z][x] == 'c')
						return new Vector3F<>(x, y, z);
				}
			}
		}
		return null;
	}

	protected void writeNetworkData(CompoundNBT nbt) {

	}

	protected void readNetworkData(CompoundNBT nbt) {

	}

	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		super.write(nbt);
		writeNetworkData(nbt);
		nbt.putBoolean("completeStructure", completeStructure);
		nbt.putBoolean("canRender", canRender);
		return nbt;
	}
	
	@Override
	public void read(BlockState state, CompoundNBT nbt) {
		super.read(state, nbt);
		readNetworkData(nbt);
		completeStructure = nbt.getBoolean("completeStructure");
		canRender = nbt.getBoolean("canRender");
	}
}
