package quantumstudios.honed.te;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import quantumstudios.honed.gui.widget.WidgetDecor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import quantumstudios.honed.Honed;
import quantumstudios.honed.Tags;
import quantumstudios.honed.data.material.MaterialDefinition;
import quantumstudios.honed.data.part.PartSchema;
import quantumstudios.honed.data.tool.ToolSchema;
import quantumstudios.honed.item.part.ItemHonedPart;
import quantumstudios.honed.item.tool.ItemHonedTool;
import quantumstudios.honed.network.packet.SPacketAssembly;
import quantumstudios.honed.registry.HonedItems;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;
import quantumstudios.honed.tool.ToolStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class TileEntityAssemblyTable extends TileEntity implements IGuiHolder<PosGuiData>, IItemHandlerModifiable {

    private static final int SLOT_COUNT = 3; // handle, head, binder

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];
    private int selectedToolIndex = 0;

    // Cached ordered list of tool schema IDs (built once when needed)
    private List<String> toolSchemaIds;

    public TileEntityAssemblyTable() {
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = ItemStack.EMPTY;
    }

    // ── Tool schema selection ───────────────────────────────────────────────

    private List<String> getToolSchemaIds() {
        if (toolSchemaIds == null || toolSchemaIds.size() != HonedRegistries.TOOL_SCHEMAS.size()) {
            toolSchemaIds = new ArrayList<>(HonedRegistries.TOOL_SCHEMAS.keySet());
            toolSchemaIds.sort(String::compareTo);
        }
        return toolSchemaIds;
    }

    public ToolSchema getSelectedToolSchema() {
        List<String> ids = getToolSchemaIds();
        if (ids.isEmpty()) return null;
        return HonedRegistries.getToolSchema(ids.get(selectedToolIndex % ids.size()));
    }

    public PartSchema getSelectedPartSchema() {
        ToolSchema tool = getSelectedToolSchema();
        if (tool == null) return null;
        String toolType = tool.id.contains(":") ? tool.id.substring(tool.id.indexOf(':') + 1) : tool.id;
        return HonedRegistries.getPartSchema(toolType);
    }

    public void cycleToolType(int direction) {
        List<String> ids = getToolSchemaIds();
        if (ids.isEmpty()) return;
        selectedToolIndex = (selectedToolIndex + direction + ids.size()) % ids.size();
        markDirty();
    }

    // ── Assembly logic ──────────────────────────────────────────────────────
    public boolean canAssemble() {
        ToolSchema tool = getSelectedToolSchema();
        PartSchema partSchema = getSelectedPartSchema();
        if (tool == null || partSchema == null || partSchema.partSlots == null) return false;
        if (tool.requiredParts == null || tool.requiredParts.size() != SLOT_COUNT) return false;

        List<String> slotNames = new ArrayList<>(partSchema.partSlots.keySet());
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i >= slotNames.size()) return false;
            String slotName = slotNames.get(i);
            PartSchema.SlotDef slotDef = partSchema.partSlots.get(slotName);
            if (slotDef == null) return false;

            String expectedPartType = slotDef.getPartType(slotName);
            ItemStack stack = slots[i];
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemHonedPart)) return false;

            String actualPartType = ToolNBT.getPartType(stack);
            if (!expectedPartType.equals(actualPartType)) return false;
        }
        return true;
    }
    public void assemble() {
        if (!canAssemble()) return;
        ToolSchema tool = getSelectedToolSchema();
        PartSchema partSchema = getSelectedPartSchema();
        if (tool == null || partSchema == null) return;

        String toolType = tool.id.contains(":") ? tool.id.substring(tool.id.indexOf(':') + 1) : tool.id;
        Item toolItem = getToolItem(toolType);
        if (toolItem == null) {
            Honed.LOGGER.warn("No registered tool item for type: {}", toolType);
            return;
        }

        ItemStack result = new ItemStack(toolItem);

        // Write construction data from each part
        List<String> slotNames = new ArrayList<>(partSchema.partSlots.keySet());
        List<String> allTraitIds = new ArrayList<>();

        for (int i = 0; i < SLOT_COUNT && i < slotNames.size(); i++) {
            String slotName = slotNames.get(i);
            ItemStack part = slots[i];

            String materialId = ToolNBT.getPartMaterial(part);
            int quality = ToolNBT.getPartQuality(part);

            ToolNBT.setMaterial(result, slotName, materialId);
            ToolNBT.setQuality(result, slotName, quality);

            // Collect traits from the material
            MaterialDefinition mat = HonedRegistries.getMaterial(materialId);
            if (mat != null && mat.traitIds != null) {
                for (String traitId : mat.traitIds) {
                    if (!allTraitIds.contains(traitId)) {
                        allTraitIds.add(traitId);
                    }
                }
            }
        }

        // Mark as assembled
        if (!result.hasTagCompound()) result.setTagCompound(new NBTTagCompound());
        NBTTagCompound construction = result.getTagCompound().getCompoundTag(ToolNBT.CONSTRUCTION);
        construction.setBoolean("assembled", true);
        result.getTagCompound().setTag(ToolNBT.CONSTRUCTION, construction);

        // Set traits and calculate stats
        ToolNBT.setAffixTraits(result, allTraitIds);
        ToolStats.recalculate(result);

        // Clear input slots and place result in slot 0 (the "output")
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = ItemStack.EMPTY;
        slots[0] = result;
        markDirty();
    }
    private static Item getToolItem(String toolType) {
        switch (toolType) {
            case "sword":   return HonedItems.SWORD;
            case "pickaxe": return HonedItems.PICKAXE;
            case "axe":     return HonedItems.AXE;
            case "shovel":  return HonedItems.SHOVEL;
            case "hoe":     return HonedItems.HOE;
            default:        return null;
        }
    }
    public String getExpectedPartType(int slotIndex) {
        PartSchema partSchema = getSelectedPartSchema();
        if (partSchema == null || partSchema.partSlots == null) return "";
        List<String> slotNames = new ArrayList<>(partSchema.partSlots.keySet());
        if (slotIndex < 0 || slotIndex >= slotNames.size()) return "";
        String slotName = slotNames.get(slotIndex);
        PartSchema.SlotDef def = partSchema.partSlots.get(slotName);
        return def != null ? def.getPartType(slotName) : slotName;
    }
    public String getSlotLabel(int slotIndex) {
        PartSchema partSchema = getSelectedPartSchema();
        if (partSchema == null || partSchema.partSlots == null) return "Slot " + (slotIndex + 1);
        List<String> slotNames = new ArrayList<>(partSchema.partSlots.keySet());
        if (slotIndex < 0 || slotIndex >= slotNames.size()) return "Slot " + (slotIndex + 1);
        String name = slotNames.get(slotIndex);
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!slots[i].isEmpty()) {
                nbt.setTag("Slot" + i, slots[i].serializeNBT());
            }
        }
        nbt.setInteger("SelectedTool", selectedToolIndex);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (nbt.hasKey("Slot" + i, Constants.NBT.TAG_COMPOUND)) {
                slots[i] = new ItemStack(nbt.getCompoundTag("Slot" + i));
            } else {
                slots[i] = ItemStack.EMPTY;
            }
        }
        selectedToolIndex = nbt.getInteger("SelectedTool");
    }

    // ── GUI ─────────────────────────────────────────────────────────────────

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        IntSyncValue toolIndexSync = new IntSyncValue(
                () -> selectedToolIndex,
                v -> selectedToolIndex = v);
        syncManager.syncValue("tool_index", toolIndexSync);

        ModularPanel panel = ModularPanel.defaultPanel("assembly_table", 176, 200);

        panel.child(IKey.lang("tile.honed.assembly_table.name").asWidget()
                        .top(6).left(8))

            // ── Tool type selector frame ──────────────────────────────────────
            .child(new WidgetDecor<>()
                    .colors(0x20000000, 0x12FFFFFF)
                    .size(160, 16).left(8).top(18))
            .child(IKey.dynamic(() -> {
                        ToolSchema ts = getSelectedToolSchema();
                        return ts != null ? "\u00a7f" + ts.displayName : "\u00a78No tools";
                    }).asWidget()
                    .left(40).top(22))
            .child(new ButtonWidget<>()
                    .overlay(IKey.str("\u25c4"))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(
                                SPacketAssembly.create(this, SPacketAssembly.CYCLE_PREV));
                        return true;
                    })
                    .size(16, 14).left(10).top(19))
            .child(new ButtonWidget<>()
                    .overlay(IKey.str("\u25ba"))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(
                                SPacketAssembly.create(this, SPacketAssembly.CYCLE_NEXT));
                        return true;
                    })
                    .size(16, 14).left(150).top(19))

            // ── Part slots background frame ───────────────────────────────────
            .child(new WidgetDecor<>()
                    .colors(0x25000000, 0x12FFFFFF)
                    .topAccent(0x18AAAAFF)
                    .size(160, 34).left(8).top(38))

            // Slot 0: handle (left)
            .child(IKey.dynamic(() -> "\u00a78" + getSlotLabel(0)).asWidget()
                    .left(14).top(39))
            .child(ItemSlot.create(false).slot(this, 0)
                    .left(14).top(50))

            // Slot 1: head (center)
            .child(IKey.dynamic(() -> "\u00a78" + getSlotLabel(1)).asWidget()
                    .left(62).top(39))
            .child(ItemSlot.create(false).slot(this, 1)
                    .left(62).top(50))

            // Slot 2: binder (right)
            .child(IKey.dynamic(() -> "\u00a78" + getSlotLabel(2)).asWidget()
                    .left(110).top(39))
            .child(ItemSlot.create(false).slot(this, 2)
                    .left(110).top(50))

            // ── Status text ───────────────────────────────────────────────────
            .child(IKey.dynamic(() -> {
                        if (canAssemble()) return "\u00a7a\u2714 Ready to assemble!";
                        ToolSchema ts = getSelectedToolSchema();
                        if (ts == null) return "\u00a78Select a tool type";
                        StringBuilder sb = new StringBuilder("\u00a77Needs: ");
                        for (int i = 0; i < SLOT_COUNT; i++) {
                            if (i > 0) sb.append("\u00a77, ");
                            String expected = getExpectedPartType(i);
                            boolean filled = !slots[i].isEmpty()
                                    && ToolNBT.getPartType(slots[i]).equals(expected);
                            sb.append(filled ? "\u00a7a\u2714 " : "\u00a7c\u2716 ");
                            sb.append(expected.replace('_', ' '));
                        }
                        return sb.toString();
                    }).asWidget()
                    .left(8).top(76))

            // ── Assemble button ───────────────────────────────────────────────
            .child(new ButtonWidget<>()
                    .overlay(IKey.str("\u00a7l\u2692 Assemble"))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(
                                SPacketAssembly.create(this, SPacketAssembly.ASSEMBLE));
                        return true;
                    })
                    .setEnabledIf(w -> canAssemble())
                    .size(120, 20).left(28).top(92))

            .bindPlayerInventory();

        return panel;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(Tags.MOD_ID, mainPanel);
    }

    // ── IItemHandlerModifiable ──────────────────────────────────────────────

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            slots[slot] = stack;
        }
    }

    @Override
    public int getSlots() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return (slot >= 0 && slot < SLOT_COUNT) ? slots[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= SLOT_COUNT) return stack;
        if (!slots[slot].isEmpty()) return stack;
        if (stack.isEmpty()) return ItemStack.EMPTY;
        // Only accept part items
        if (!(stack.getItem() instanceof ItemHonedPart)) return stack;
        ItemStack copy = stack.copy();
        ItemStack inserted = copy.splitStack(1);
        if (!simulate) {
            slots[slot] = inserted;
            markDirty();
        }
        return copy;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) return ItemStack.EMPTY;
        if (slots[slot].isEmpty()) return ItemStack.EMPTY;
        ItemStack result = slots[slot].copy();
        if (!simulate) {
            slots[slot] = ItemStack.EMPTY;
            markDirty();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }
}
