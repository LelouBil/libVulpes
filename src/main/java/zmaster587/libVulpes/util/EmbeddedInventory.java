package zmaster587.libVulpes.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.ItemStackHandler;
import zmaster587.libVulpes.interfaces.IInventoryUpdateCallback;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class EmbeddedInventory extends ItemStackHandler implements ISidedInventory {

	ItemStackHandler handler;

	IInventoryUpdateCallback tile;

	NonNullList<Boolean> slotInsert;
	NonNullList<Boolean> slotExtract;

	public EmbeddedInventory(int size) {
		this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
		this.slotInsert = NonNullList.withSize(size, true);
		this.slotExtract = NonNullList.withSize(size, true);
	}

	public EmbeddedInventory(int size, IInventoryUpdateCallback tile) {
		this(size);
		this.tile = tile;
	}

	public void writeToNBT(NBTTagCompound nbt) {

		nbt.setInteger("size", this.stacks.size());

		NBTTagList list = new NBTTagList();
		for(int i = 0; i < this.stacks.size(); i++)
		{
			ItemStack stack = this.stacks.get(i);

			if(!stack.isEmpty()) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte)(i));
				stack.writeToNBT(tag);
				list.appendTag(tag);
			}
		}

		nbt.setTag("outputItems", list);

		ArrayList list2 = new ArrayList<Byte>();
		for(int i = 0; i < this.slotInsert.size(); i++) {
			list2.set(i, (slotInsert.get(i) == true) ? 1 : 0);
		}
		nbt.setTag("slotInsert", new NBTTagByteArray(list2));

		ArrayList list3 = new ArrayList<Byte>();
		for(int i = 0; i < this.slotExtract.size(); i++) {
			list3.set(i, (slotExtract.get(i) == true) ? 1 : 0);
		}
		nbt.setTag("slotExtract", new NBTTagByteArray(list3));

	}

	@Override
	protected void onContentsChanged(int slot) {
		super.onContentsChanged(slot);

		if(tile != null)
			tile.onInventoryUpdated(slot);

	}

	public void readFromNBT(NBTTagCompound nbt) {
		NBTTagList list = nbt.getTagList("outputItems", 10);
		this.stacks = NonNullList.withSize(Math.max(nbt.getInteger("size") == 0 ? 4 : nbt.getInteger("size"), this.stacks.size()), ItemStack.EMPTY);
		handler = new ItemStackHandler(this.stacks);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = (NBTTagCompound) list.getCompoundTagAt(i);
			byte slot = tag.getByte("Slot");
			if (slot >= 0 && slot < this.stacks.size()) {
				this.stacks.set(slot, new ItemStack(tag));
			}
		}
		
		
		byte[] list2 = nbt.getByteArray("slotInsert");
		this.slotInsert = NonNullList.withSize(list2.length, false);
		for (int i = 0; i < list2.length; i++) {
			this.slotInsert.set(i, (list2[i] == 1) ? true : false);
		}
		byte[] list3 = nbt.getByteArray("slotExtract");
		this.slotExtract = NonNullList.withSize(list3.length, false);
		for (int i = 0; i < list3.length; i++) {
			this.slotExtract.set(i, (list3[i] == 1) ? true : false);
		}

		//Backcompat, to allow older worlds to load
        if (this.slotInsert.isEmpty()) {
			this.slotInsert = NonNullList.withSize(4, false);
		}
		if (this.slotExtract.isEmpty()) {
			this.slotExtract = NonNullList.withSize(4, false);
		}
	}


	public int getSizeInventory() {
		return this.stacks.size();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if(slot >= this.stacks.size())
			return ItemStack.EMPTY;

		return this.stacks.get(slot);
	}

	@Override
	public ItemStack decrStackSize(int slot, int amt) {
		ItemStack stack = this.stacks.get(slot);
		if(!stack.isEmpty()) {
			ItemStack stack2 = stack.splitStack(Math.min(amt, stack.getCount()));
			if(stack.getCount() == 0)
				this.stacks.set(slot, ItemStack.EMPTY);

			return stack2;
		}
		return null;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		this.stacks.set(slot, stack);
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	public void setCanInsertSlot(int index, boolean bool) {
		this.slotInsert.set(index, bool);
	}

	public void setCanExtractSlot(int index, boolean bool) {
		this.slotExtract.set(index, bool);
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return true;
	}

	@Override
	public boolean isEmpty() {
		for(ItemStack i : this.stacks) {
			if(i != null && !i.isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public void openInventory(EntityPlayer player) {

	}

	@Override
	public void closeInventory(EntityPlayer player) {

	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack item) {
		return this.stacks.get(slot).isEmpty() || (this.stacks.get(slot).isItemEqual(item) && this.stacks.get(slot).getMaxStackSize() != this.stacks.get(slot).getCount());
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		int array[] = new int[this.stacks.size()];

		for(int i = 0; i < this.stacks.size(); i++) {
			array[i] = i;
		}
		return array;
	}



	public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
		return this.slotInsert.get(index);
	}


	public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
		return this.slotExtract.get(index);
	}

	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (!slotInsert.isEmpty() && slotInsert.get(slot) == true ){
			return super.insertItem(slot, stack, simulate);
		}
		return stack;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (!slotExtract.isEmpty() && slotExtract.get(slot) == true ){
			return super.extractItem(slot, amount, simulate);
		}
		return ItemStack.EMPTY;
	}

	public String getName() {
		return "";
	}

	@Override
	public void markDirty() {
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack stack = this.stacks.get(index);
		this.stacks.set(index, ItemStack.EMPTY);
		return stack;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {

	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {

	}

	@Override
	public ITextComponent getDisplayName() {
		return new TextComponentString("Inventory");
	}
}
