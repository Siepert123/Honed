package quantumstudios.honed.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import quantumstudios.honed.data.knapping.KnappingRecipeData;
import quantumstudios.honed.data.material.MaterialDefinition;
import quantumstudios.honed.item.part.ItemHonedPart;
import quantumstudios.honed.registry.HonedItems;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;

import java.util.ArrayList;
import java.util.List;

public class ContainerKnapping extends Container {

    public IInventory craftResult = new InventoryCraftResult();
    public InventoryPlayer invPlayer;
    public boolean[][] grid = new boolean[5][5];
    public boolean hasConsumed = false;
    public int materialColor = 0x8B4513;
    public String materialId = "";
    private final int chiselingQuality;

    private final List<KnappingRecipeData> matchingRecipes = new ArrayList<>();

    public ContainerKnapping(InventoryPlayer playerInventory, int chiselingQuality) {
        this.invPlayer = playerInventory;
        this.chiselingQuality = chiselingQuality;
        lookupRecipes(playerInventory.getCurrentItem());
        resolveMaterialColor();

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 102 + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new SlotLocked(invPlayer, i, 8 + i * 18, 160));
        }

        this.addSlotToContainer(new SlotKnapping(this, this.craftResult, 0, 136, 45));
    }

    private void lookupRecipes(ItemStack stack) {
        matchingRecipes.clear();
        if (stack.isEmpty()) return;
        ResourceLocation rl = stack.getItem().getRegistryName();
        if (rl == null) return;
        String itemId = rl.toString();
        int[] oreIds = OreDictionary.getOreIDs(stack);
        for (KnappingRecipeData recipe : HonedRegistries.KNAPPING.values()) {
            if (recipe.inputItem == null) continue;
            if (recipe.inputItem.startsWith("ore:")) {
                String oreName = recipe.inputItem.substring(4);
                int targetOreId = OreDictionary.getOreID(oreName);
                for (int id : oreIds) {
                    if (id == targetOreId) {
                        matchingRecipes.add(recipe);
                        break;
                    }
                }
            } else if (recipe.inputItem.equals(itemId)) {
                matchingRecipes.add(recipe);
            }
        }
    }

    private void resolveMaterialColor() {
        if (matchingRecipes.isEmpty()) return;
        KnappingRecipeData first = matchingRecipes.get(0);
        MaterialDefinition mat = HonedRegistries.getMaterial(first.materialId);
        if (mat != null) {
            materialColor = mat.color;
            materialId = first.materialId;
        }
    }

    public void clickedOnSlot(int x, int y) {
        if (invPlayer.player.world.isRemote) return;
        if (x < 0 || x >= 5 || y < 0 || y >= 5) return;
        if (grid[x][y]) return;

        if (!hasConsumed) {
            consumeMaterial();
            hasConsumed = true;
        }

        grid[x][y] = true;

        float pitch = 0.7F + (invPlayer.player.getRNG().nextFloat() * 0.3F);
        playSound(invPlayer.player, "minecraft:block.stone.break", pitch);

        ItemStack output = getRecipeOutput();
        craftResult.setInventorySlotContents(0, output != null ? output : ItemStack.EMPTY);
        detectAndSendChanges();
    }

    private ItemStack getRecipeOutput() {
        for (KnappingRecipeData recipe : matchingRecipes) {
            if (recipe.matches(grid)) {
                ItemStack out = new ItemStack(HonedItems.PART, 1,
                        ItemHonedPart.getMetaForPartType(recipe.outputPartType));
                ToolNBT.setPartData(out, recipe.outputPartType, recipe.materialId, chiselingQuality);
                return out;
            }
        }
        return null;
    }

    public void resetKnapping() {
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                grid[x][y] = true;
            }
        }
        hasConsumed = false;
        detectAndSendChanges();
    }

    private void consumeMaterial() {
        ItemStack currentStack = invPlayer.getCurrentItem();
        if (!currentStack.isEmpty()) {
            currentStack.shrink(1);
        }
    }

    private void playSound(EntityPlayer player, String name, float pitch) {
        SoundEvent soundEvent = SoundEvent.REGISTRY.getObject(new ResourceLocation(name));
        if (soundEvent != null) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    soundEvent, SoundCategory.PLAYERS, 1.0F, pitch);
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            itemstack = stack.copy();

            int playerInvStart = 0;
            int playerInvEnd = 36;
            int resultSlot = 36;

            if (index == resultSlot) {
                if (!this.mergeItemStack(stack, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(stack, itemstack);
            } else {
                if (!this.mergeItemStack(stack, playerInvStart, playerInvEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        detectAndSendChanges();
        return itemstack;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    public static class SlotKnapping extends Slot {
        private final ContainerKnapping container;

        public SlotKnapping(ContainerKnapping container, IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.container = container;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            container.resetKnapping();
            return super.onTake(player, stack);
        }
    }

    static class SlotLocked extends Slot {
        private final InventoryPlayer player;

        public SlotLocked(InventoryPlayer inv, int index, int x, int y) {
            super(inv, index, x, y);
            this.player = inv;
        }

        @Override
        public boolean canTakeStack(EntityPlayer p) {
            return player.currentItem != getSlotIndex();
        }
    }
}
