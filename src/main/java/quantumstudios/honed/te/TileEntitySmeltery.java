package quantumstudios.honed.te;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import quantumstudios.honed.gui.widget.WidgetSmelteryBars;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import quantumstudios.honed.Tags;
import quantumstudios.honed.config.HonedConfig;
import quantumstudios.honed.tool.ToolNBT;

public class TileEntitySmeltery extends TileEntity implements ITickable, IGuiHolder<PosGuiData>, IItemHandlerModifiable {

    private ItemStack inputStack = ItemStack.EMPTY;
    private ItemStack fuelStack = ItemStack.EMPTY;
    private float temperature = 0;
    private int burnTimeRemaining = 0;
    private int currentBurnTime = 0;

    public float getTemperature() {
        return temperature;
    }

    public int getBurnTimeRemaining() {
        return burnTimeRemaining;
    }

    public int getCurrentBurnTime() {
        return currentBurnTime;
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        boolean dirty = false;

        if (burnTimeRemaining > 0) {
            burnTimeRemaining--;
            if (!inputStack.isEmpty()) {
                float maxTemp = (float) HonedConfig.smelteryMaxTemp;
                temperature = Math.min(temperature + (float) HonedConfig.smelteryHeatRate, maxTemp);
                dirty = true;
            }
        } else if (!fuelStack.isEmpty()) {
            int fuelBurn = TileEntityFurnace.getItemBurnTime(fuelStack);
            if (fuelBurn > 0) {
                burnTimeRemaining = fuelBurn;
                currentBurnTime = fuelBurn;
                fuelStack.shrink(1);
                dirty = true;
            }
        }

        if (burnTimeRemaining <= 0 && temperature > 0) {
            temperature = Math.max(0, temperature - (float) HonedConfig.smelteryCoolRate);
            dirty = true;
        }

        if (!inputStack.isEmpty() && temperature > 0) {
            // Gradually bring the item's temperature toward the smeltery temperature
            // so heating takes time, and a pre-heated ingot continues from its current temp.
            float itemTemp = ToolNBT.getTemperature(inputStack);
            float newItemTemp = Math.min(itemTemp + (float) HonedConfig.smelteryHeatRate, temperature);
            if (newItemTemp > itemTemp) {
                ToolNBT.setTemperature(inputStack, newItemTemp);
                dirty = true;
            }
        }

        if (dirty) {
            markDirty();
            if (world != null) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            }
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!inputStack.isEmpty()) nbt.setTag("Input", inputStack.serializeNBT());
        if (!fuelStack.isEmpty()) nbt.setTag("Fuel", fuelStack.serializeNBT());
        nbt.setFloat("Temp", temperature);
        nbt.setInteger("BurnTime", burnTimeRemaining);
        nbt.setInteger("MaxBurnTime", currentBurnTime);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        inputStack = nbt.hasKey("Input", Constants.NBT.TAG_COMPOUND) ? new ItemStack(nbt.getCompoundTag("Input")) : ItemStack.EMPTY;
        fuelStack = nbt.hasKey("Fuel", Constants.NBT.TAG_COMPOUND) ? new ItemStack(nbt.getCompoundTag("Fuel")) : ItemStack.EMPTY;
        temperature = nbt.getFloat("Temp");
        burnTimeRemaining = nbt.getInteger("BurnTime");
        currentBurnTime = nbt.getInteger("MaxBurnTime");
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        // Sync temperature and fuel for live bar updates
        syncManager.syncValue("temp_x100", new IntSyncValue(
                () -> (int) (temperature * 100),
                v -> temperature = v / 100f));
        syncManager.syncValue("burn_time", new IntSyncValue(
                () -> burnTimeRemaining,
                v -> burnTimeRemaining = v));
        syncManager.syncValue("max_burn", new IntSyncValue(
                () -> currentBurnTime,
                v -> currentBurnTime = v));

        ModularPanel panel = ModularPanel.defaultPanel("smeltery", 176, 176);
        panel.child(IKey.lang("tile.honed.smeltery.name").asWidget()
                        .top(6).left(8))

                // Input slot with label
                .child(IKey.str("\u00a77Input").asWidget()
                        .left(12).top(22))
                .child(ItemSlot.create(false).slot(this, 0)
                        .left(12).top(33))

                // Fuel slot with label
                .child(IKey.str("\u00a77Fuel").asWidget()
                        .left(12).top(57))
                .child(ItemSlot.create(false).slot(this, 1)
                        .left(12).top(68))

                // Temperature and fuel bars
                .child(new WidgetSmelteryBars<>(this)
                        .left(50).top(22).size(118, 68))

                .bindPlayerInventory();
        return panel;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(Tags.MOD_ID, mainPanel);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            inputStack = stack;
            if (inputStack.isEmpty()) temperature = 0;
        } else {
            fuelStack = stack;
        }
        markDirty();
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? inputStack : fuelStack;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack current = slot == 0 ? inputStack : fuelStack;
        if (!current.isEmpty()) return stack;
        if (stack.isEmpty()) return ItemStack.EMPTY;

        if (slot == 1 && TileEntityFurnace.getItemBurnTime(stack) <= 0) return stack;

        ItemStack copy = stack.copy();
        ItemStack inserted = copy.splitStack(slot == 0 ? 1 : copy.getCount());
        if (!simulate) {
            if (slot == 0) {
                inputStack = inserted;
                if (inputStack.isEmpty()) temperature = 0;
            } else {
                fuelStack = inserted;
            }
            markDirty();
        }
        return copy;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack current = slot == 0 ? inputStack : fuelStack;
        if (current.isEmpty()) return ItemStack.EMPTY;

        ItemStack extracted;
        if (slot == 0) {
            extracted = current.copy();
            if (!simulate) {
                inputStack = ItemStack.EMPTY;
                temperature = 0;
                markDirty();
            }
        } else {
            int toExtract = Math.min(amount, current.getCount());
            extracted = current.copy();
            extracted.setCount(toExtract);
            if (!simulate) {
                fuelStack.shrink(toExtract);
                if (fuelStack.isEmpty()) fuelStack = ItemStack.EMPTY;
                markDirty();
            }
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == 0 ? 1 : 64;
    }
}
