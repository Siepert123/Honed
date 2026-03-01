package quantumstudios.honed.te;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.BooleanConsumer;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.oredict.OreDictionary;
import quantumstudios.honed.Honed;
import quantumstudios.honed.Tags;
import quantumstudios.honed.config.HonedConfig;
import quantumstudios.honed.data.forging.ForgingAction;
import quantumstudios.honed.data.forging.ForgingRecipe;
import quantumstudios.honed.data.material.MaterialDefinition;
import quantumstudios.honed.gui.widget.WidgetDecor;
import quantumstudios.honed.gui.widget.WidgetForgingMinigameV2;
import quantumstudios.honed.item.part.ItemHonedPart;
import quantumstudios.honed.network.packet.SPacketMinigame;
import quantumstudios.honed.registry.HonedItems;
import quantumstudios.honed.registry.HonedRegistries;
import quantumstudios.honed.tool.ToolNBT;

import java.util.ArrayList;
import java.util.List;

public class TileEntityForgingAnvil extends TileEntity implements ITickable, IGuiHolder<PosGuiData>, IItemHandlerModifiable {

    // ── Per-pass minigame constants ─────────────────────────────────────────
    public static final int TOTAL_PASSES = 20;
    private static final int[] PASS_DURATIONS = {
        60, 57, 54, 51, 48, 46, 44, 42, 40, 38,
        36, 34, 32, 30, 28, 27, 26, 25, 24, 23
    };
    private ItemStack inputStack = ItemStack.EMPTY;
    private final List<ForgingRecipe> matchingRecipes = new ArrayList<>();
    private int selectedRecipeIndex = 0;
    private boolean forging = false;

    // Pass-based rhythm minigame state
    private int currentPass = -1;
    private int requiredActionOrdinal = 0;
    private long passStartTick = 0;
    private int passDuration = 60;
    private final int[] passScores = new int[TOTAL_PASSES]; // 0=miss, 1=good, 2=perfect
    private int passHitMask = 0;
    private int passPerfectMask = 0;
    private boolean actionFiredThisPass = false;

    // Target zone for current pass (synced to client)
    private float targetCenter = 0.5f;
    private float targetHalfWidth = 0.13f;

    // Feedback
    private long lastActionTick = -100;
    private int lastActionScore = 0; // 0=miss, 1=good, 2=perfect

    // ── Getters ──────────────────────────────────────────────────────────────
    public int getLastActionTick()       { return (int) lastActionTick; }
    public int getLastActionScore()      { return lastActionScore; }
    public int getCurrentPass()          { return currentPass; }
    public int getRequiredActionOrdinal(){ return requiredActionOrdinal; }
    public int getPassStartTickLow()     { return (int) passStartTick; }
    public int getPassDuration()         { return passDuration; }
    public float getTargetCenter()       { return targetCenter; }
    public float getTargetHalfWidth()    { return targetHalfWidth; }
    public int getPassScore(int index) {
        if (index < 0 || index >= TOTAL_PASSES) return 0;
        if ((passHitMask & (1 << index)) == 0) return 0;
        return (passPerfectMask & (1 << index)) != 0 ? 2 : 1;
    }

    private float evaluateBarCurve(float t) {
        long seed = (passStartTick * 31L) ^ (currentPass * 73856093L);
        float freq1 = 2.0f + ((seed & 0xF) / 15.0f) * 3.0f;
        float freq2 = 1.0f + (((seed >> 4) & 0xF) / 15.0f) * 2.0f;
        float phase1 = ((seed >> 8) & 0xFF) / 255.0f * (float)(Math.PI * 2);
        float phase2 = ((seed >> 16) & 0xFF) / 255.0f * (float)(Math.PI * 2);
        float envelope = Math.min(t / 0.12f, 1f);
        float bar = 0.5f
                + envelope * 0.42f * (float) Math.sin(t * freq1 * Math.PI * 2 + phase1)
                + envelope * 0.14f * (float) Math.sin(t * freq2 * Math.PI * 2 + phase2);
        return Math.max(0f, Math.min(1f, bar));
    }

    public float computeBarPos(long worldTime) {
        if (currentPass < 0 || currentPass >= TOTAL_PASSES) return 0f;
        long elapsed = (worldTime & 0xFFFFFFFFL) - (passStartTick & 0xFFFFFFFFL);
        if (elapsed < 0) elapsed += 0x100000000L;
        return evaluateBarCurve(Math.min((float) elapsed / passDuration, 1f));
    }

    @SideOnly(Side.CLIENT)
    public float getClientBarPos() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.world == null || currentPass < 0 || currentPass >= TOTAL_PASSES) return 0f;
        long worldTime = mc.world.getTotalWorldTime();
        float pt = mc.getRenderPartialTicks();
        long elapsed = (worldTime & 0xFFFFFFFFL) - (passStartTick & 0xFFFFFFFFL);
        if (elapsed < 0) elapsed += 0x100000000L;
        return evaluateBarCurve(Math.min((elapsed + pt) / passDuration, 1f));
    }

    public boolean isForging() {
        return forging;
    }

    public ForgingRecipe getSelectedRecipe() {
        if (matchingRecipes.isEmpty()) return null;
        return matchingRecipes.get(selectedRecipeIndex % matchingRecipes.size());
    }

    public void cycleRecipe(int direction) {
        if (matchingRecipes.isEmpty() || forging) return;
        selectedRecipeIndex = (selectedRecipeIndex + direction + matchingRecipes.size()) % matchingRecipes.size();
    }

    public boolean canForge() {
        if (matchingRecipes.isEmpty()) return false;
        ForgingRecipe recipe = getSelectedRecipe();
        if (recipe == null) return false;
        MaterialDefinition mat = HonedRegistries.getMaterial(recipe.materialId);
        if (mat == null) return false;
        if (!"FORGE".equals(mat.processingType)) return true;
        float temp = ToolNBT.getTemperature(inputStack);
        return temp >= mat.workingTempMin;
    }

    public void startForging() {
        if (forging || !canForge()) return;
        forging = true;
        for (int i = 0; i < TOTAL_PASSES; i++) passScores[i] = 0;
        passHitMask = 0;
        passPerfectMask = 0;
        beginPass(0);
    }
    private void beginPass(int pass) {
        currentPass = pass;
        ForgingRecipe recipe = getSelectedRecipe();
        int[] pattern = (recipe != null) ? recipe.getActionPattern()
                : ForgingRecipe.defaultPatternFor(null);
        requiredActionOrdinal = pattern[pass % pattern.length];
        passStartTick = world.getTotalWorldTime();
        passDuration = pass < PASS_DURATIONS.length ? PASS_DURATIONS[pass] : PASS_DURATIONS[PASS_DURATIONS.length - 1];
        long tSeed = (passStartTick * 37L) ^ (pass * 12345L) ^ 0xDEADBEEFL;
        targetCenter = 0.20f + ((tSeed >> 24) & 0xFF) / 255.0f * 0.60f;
        float progression = (float) pass / (TOTAL_PASSES - 1);
        targetHalfWidth = 0.13f - progression * 0.07f;
        actionFiredThisPass = false;
        markDirty();
    }
    private void advancePass() {
        int next = currentPass + 1;
        if (next < TOTAL_PASSES) {
            beginPass(next);
        } else {
            currentPass = TOTAL_PASSES;
            markDirty();
        }
    }
    public void applyAction(ForgingAction action, float clientBarPos) {
        if (!forging || currentPass < 0 || currentPass >= TOTAL_PASSES) return;
        if (actionFiredThisPass) return;

        int score = 0;
        if (action.ordinal() == requiredActionOrdinal) {
            float serverPos = computeBarPos(world.getTotalWorldTime());
            float barPos = (clientBarPos > 0f && Math.abs(clientBarPos - serverPos) < 0.25f)
                    ? clientBarPos : serverPos;
            float dist = Math.abs(barPos - targetCenter);
            if (dist <= targetHalfWidth * 0.45f) {
                score = 2; // Perfect
            } else if (dist <= targetHalfWidth) {
                score = 1; // Good
            }
        }
        passScores[currentPass] = score;
        if (score > 0) passHitMask |= (1 << currentPass);
        if (score == 2) passPerfectMask |= (1 << currentPass);
        actionFiredThisPass = true;
        lastActionTick = world.getTotalWorldTime();
        lastActionScore = score;
        advancePass();
    }
    public void completeForging() {
        if (!forging || currentPass != TOTAL_PASSES) return;
        ForgingRecipe recipe = getSelectedRecipe();
        if (recipe == null) { resetForging(); return; }

        ItemStack output = new ItemStack(HonedItems.PART, 1,
                ItemHonedPart.getMetaForPartType(recipe.outputPartType));
        ToolNBT.setPartData(output, recipe.outputPartType, recipe.materialId,
                calculateQuality());
        inputStack = output;
        resetForging();
        markDirty();
    }

    private int calculateQuality() {
        int totalScore = 0;
        for (int s : passScores) totalScore += s;
        if (totalScore >= 38) return 8; // Legendary
        if (totalScore >= 34) return 7; // Masterwork
        if (totalScore >= 30) return 6; // Excellent
        if (totalScore >= 25) return 5; // Good
        if (totalScore >= 20) return 4; // Fine
        if (totalScore >= 15) return 3; // Decent
        if (totalScore >= 10) return 2; // Poor
        if (totalScore >= 5)  return 1; // Crude
        return 0;                        // Ruined
    }
    public void cancelForging() {
        if (forging) {
            resetForging();
            markDirty();
        }
    }

    private void resetForging() {
        forging = false;
        currentPass = -1;
        actionFiredThisPass = false;
        passHitMask = 0;
        passPerfectMask = 0;
    }

    private void lookupRecipes() {
        matchingRecipes.clear();
        selectedRecipeIndex = 0;
        if (inputStack.isEmpty() || inputStack.getItem().getRegistryName() == null) return;
        String itemId = inputStack.getItem().getRegistryName().toString();
        int[] oreIds = OreDictionary.getOreIDs(inputStack);
        for (ForgingRecipe recipe : HonedRegistries.FORGING.values()) {
            if (recipe.inputItem == null) continue;
            MaterialDefinition mat = HonedRegistries.getMaterial(recipe.materialId);
            if (mat == null || !"FORGE".equals(mat.processingType)) continue;
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

    @Override
    public void update() {
        if (world.isRemote) return;

        // Gradually cool the ingot while forging (once per second)
        if (forging && !inputStack.isEmpty() && world.getTotalWorldTime() % 20 == 0) {
            float temp = ToolNBT.getTemperature(inputStack);
            if (temp > 0) {
                ToolNBT.setTemperature(inputStack,
                        Math.max(0, temp - (float) HonedConfig.ambientCoolRate * 5));
            }
        }

        // Auto-advance the pass when the timer expires (missed click = miss)
        if (forging && currentPass >= 0 && currentPass < TOTAL_PASSES) {
            long elapsed = world.getTotalWorldTime() - passStartTick;
            if (elapsed >= passDuration) {
                // passScores[currentPass] remains 0 (miss)
                advancePass();
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (!inputStack.isEmpty()) {
            nbt.setTag("Input", inputStack.serializeNBT());
        }
        // Persist forging state so chunk unload/reload doesn't silently lose progress
        nbt.setBoolean("Forging", forging);
        nbt.setInteger("CurrentPass", currentPass);
        nbt.setInteger("RequiredAction", requiredActionOrdinal);
        nbt.setLong("PassStartTick", passStartTick);
        nbt.setInteger("PassDuration", passDuration);
        nbt.setBoolean("ActionFired", actionFiredThisPass);
        nbt.setInteger("PassHitMask", passHitMask);
        nbt.setInteger("PassPerfectMask", passPerfectMask);
        nbt.setFloat("TargetCenter", targetCenter);
        nbt.setFloat("TargetHalfWidth", targetHalfWidth);
        for (int i = 0; i < TOTAL_PASSES; i++) {
            nbt.setInteger("PassScore" + i, passScores[i]);
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("Input", Constants.NBT.TAG_COMPOUND)) {
            inputStack = new ItemStack(nbt.getCompoundTag("Input"));
        } else {
            inputStack = ItemStack.EMPTY;
        }
        forging = nbt.getBoolean("Forging");
        currentPass = nbt.getInteger("CurrentPass");
        requiredActionOrdinal = nbt.getInteger("RequiredAction");
        passStartTick = nbt.getLong("PassStartTick");
        passDuration = nbt.getInteger("PassDuration");
        actionFiredThisPass = nbt.getBoolean("ActionFired");
        passHitMask = nbt.getInteger("PassHitMask");
        passPerfectMask = nbt.getInteger("PassPerfectMask");
        targetCenter = nbt.getFloat("TargetCenter");
        targetHalfWidth = nbt.getFloat("TargetHalfWidth");
        for (int i = 0; i < TOTAL_PASSES; i++) {
            passScores[i] = nbt.getInteger("PassScore" + i);
        }
        lookupRecipes();
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        // Sync forging state to client so setEnabledIf conditions are accurate
        BooleanSyncValue forgingSync = new BooleanSyncValue(
                () -> forging,
                (BooleanConsumer) v -> forging = v);
        syncManager.syncValue("forging", forgingSync);

        // Sync canForge so Start button enables correctly without client-side recipe lookup
        BooleanSyncValue canForgeSync = new BooleanSyncValue(
                this::canForge,
                (BooleanConsumer) v -> {});
        syncManager.syncValue("can_forge", canForgeSync);

        // Sync recipe count so Prev/Next buttons enable correctly
        BooleanSyncValue multiRecipeSync = new BooleanSyncValue(
                () -> matchingRecipes.size() > 1,
                (BooleanConsumer) v -> {});
        syncManager.syncValue("multi_recipe", multiRecipeSync);

        // Sync selected recipe index so the recipe label tracks after Prev/Next
        IntSyncValue recipeIndexSync = new IntSyncValue(
                () -> selectedRecipeIndex,
                v -> selectedRecipeIndex = v);
        syncManager.syncValue("recipe_index", recipeIndexSync);

        // Sync last action timing so minigame bar feedback works client-side
        IntSyncValue lastTickSync = new IntSyncValue(
                () -> (int) lastActionTick,
                v -> lastActionTick = (long) v);
        syncManager.syncValue("last_action_tick", lastTickSync);

        syncManager.syncValue("last_action_score",
                new IntSyncValue(() -> lastActionScore, v -> lastActionScore = v));

        // Sync per-pass minigame state
        syncManager.syncValue("current_pass",
                new IntSyncValue(() -> currentPass, v -> currentPass = v));
        syncManager.syncValue("required_action",
                new IntSyncValue(() -> requiredActionOrdinal, v -> requiredActionOrdinal = v));
        syncManager.syncValue("pass_start_tick",
                new IntSyncValue(() -> (int) passStartTick,
                        v -> passStartTick = (long) v & 0xFFFFFFFFL));
        syncManager.syncValue("pass_duration",
                new IntSyncValue(() -> passDuration, v -> passDuration = v));

        // Sync target zone for the rhythm bar widget
        syncManager.syncValue("target_center",
                new IntSyncValue(() -> (int)(targetCenter * 10000),
                        v -> targetCenter = v / 10000f));
        syncManager.syncValue("target_half_width",
                new IntSyncValue(() -> (int)(targetHalfWidth * 10000),
                        v -> targetHalfWidth = v / 10000f));

        // Sync per-pass score bitmasks for the pass strip display
        syncManager.syncValue("pass_hits",
                new IntSyncValue(() -> passHitMask, v -> passHitMask = v));
        syncManager.syncValue("pass_perfects",
                new IntSyncValue(() -> passPerfectMask, v -> passPerfectMask = v));

        // Panel: 246 × 266
        //   Title          top=6
        //   Bar            top=18  h=28 → ends 46
        //   Slot + label   top=50       → ends 68
        //   Prev/Next      top=70  h=14 → ends 84
        //   Action frame   top=86  h=56 → ends 142
        //   Action row 1   top=90  h=22 → ends 112
        //   Action row 2   top=116 h=22 → ends 138
        //   Status hint    top=146
        //   Start/Complete top=160 h=20 → ends 180
        //   Inventory      top=184 (266-82)
        ModularPanel panel = ModularPanel.defaultPanel("forging_anvil", 246, 266);

        panel.child(IKey.lang("tile.honed.forging_anvil.name").asWidget()
                        .top(6).left(8))

            // ── Timing bar ────────────────────────────────────────────────────
            .child(new WidgetForgingMinigameV2<>(this)
                    .left(8).top(18).size(230, 28))

            // ── Input slot + recipe label ─────────────────────────────────────
            .child(ItemSlot.create(false).slot(this, 0)
                    .left(8).top(50))
            .child(IKey.dynamic(() -> {
                        ForgingRecipe r = getSelectedRecipe();
                        if (r == null) return "\u00a78No recipe (insert ingot)";
                        String part = r.outputPartType.replace('_', ' ');
                        String mat  = r.materialId.contains(":")
                                ? r.materialId.substring(r.materialId.indexOf(':') + 1)
                                : r.materialId;
                        return "\u00a7f" + part + " \u00b7 " + mat;
                    }).asWidget()
                    .left(32).top(54))

            // ── Prev / Next recipe ────────────────────────────────────────────
            .child(new ButtonWidget<>()
                    .overlay(IKey.str("\u25c4 Prev"))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(
                                SPacketMinigame.create(this, SPacketMinigame.CYCLE_PREV, 0f));
                        return true;
                    })
                    .setEnabledIf(w -> multiRecipeSync.getBoolValue() && !forgingSync.getBoolValue())
                    .size(46, 14).left(32).top(70))
            .child(new ButtonWidget<>()
                    .overlay(IKey.str("Next \u25ba"))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(
                                SPacketMinigame.create(this, SPacketMinigame.CYCLE_NEXT, 0f));
                        return true;
                    })
                    .setEnabledIf(w -> multiRecipeSync.getBoolValue() && !forgingSync.getBoolValue())
                    .size(46, 14).left(82).top(70))

            // ── Actions section background frame ──────────────────────────────
            .child(new WidgetDecor<>()
                    .colors(0x28000000, 0x15FFFFFF)
                    .topAccent(0x20AAAAFF)
                    .size(230, 56).left(8).top(86))

            // ── Action buttons row 1: Light Hit | Medium Hit | Heavy Hit ──────
            .child(new ButtonWidget<>()
                    .overlay(IKey.dynamic(() -> {
                        if (!forgingSync.getBoolValue() || getCurrentPass() >= TOTAL_PASSES) return "\u2694 Light";
                        return getRequiredActionOrdinal() == ForgingAction.HIT_LIGHT.ordinal()
                                ? "\u00a7a\u00a7l\u2694 Light" : "\u00a77\u2694 Light";
                    }))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(SPacketMinigame.create(this,
                                SPacketMinigame.APPLY_ACTION, ForgingAction.HIT_LIGHT.ordinal(), getClientBarPos()));
                        return true;
                    })
                    .setEnabledIf(w -> forgingSync.getBoolValue() && getCurrentPass() < TOTAL_PASSES)
                    .size(74, 22).left(8).top(90))
            .child(new ButtonWidget<>()
                    .overlay(IKey.dynamic(() -> {
                        if (!forgingSync.getBoolValue() || getCurrentPass() >= TOTAL_PASSES) return "\u2694 Medium";
                        return getRequiredActionOrdinal() == ForgingAction.HIT_MEDIUM.ordinal()
                                ? "\u00a7a\u00a7l\u2694 Medium" : "\u00a77\u2694 Medium";
                    }))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(SPacketMinigame.create(this,
                                SPacketMinigame.APPLY_ACTION, ForgingAction.HIT_MEDIUM.ordinal(), getClientBarPos()));
                        return true;
                    })
                    .setEnabledIf(w -> forgingSync.getBoolValue() && getCurrentPass() < TOTAL_PASSES)
                    .size(74, 22).left(84).top(90))
            .child(new ButtonWidget<>()
                    .overlay(IKey.dynamic(() -> {
                        if (!forgingSync.getBoolValue() || getCurrentPass() >= TOTAL_PASSES) return "\u2694 Heavy";
                        return getRequiredActionOrdinal() == ForgingAction.HIT_HEAVY.ordinal()
                                ? "\u00a7a\u00a7l\u2694 Heavy" : "\u00a77\u2694 Heavy";
                    }))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(SPacketMinigame.create(this,
                                SPacketMinigame.APPLY_ACTION, ForgingAction.HIT_HEAVY.ordinal(), getClientBarPos()));
                        return true;
                    })
                    .setEnabledIf(w -> forgingSync.getBoolValue() && getCurrentPass() < TOTAL_PASSES)
                    .size(74, 22).left(160).top(90))

            // ── Action buttons row 2: Draw | Punch | Upset ────────────────────
            .child(new ButtonWidget<>()
                    .overlay(IKey.dynamic(() -> {
                        if (!forgingSync.getBoolValue() || getCurrentPass() >= TOTAL_PASSES) return "\u2692 Draw";
                        return getRequiredActionOrdinal() == ForgingAction.DRAW.ordinal()
                                ? "\u00a7a\u00a7l\u2692 Draw" : "\u00a77\u2692 Draw";
                    }))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(SPacketMinigame.create(this,
                                SPacketMinigame.APPLY_ACTION, ForgingAction.DRAW.ordinal(), getClientBarPos()));
                        return true;
                    })
                    .setEnabledIf(w -> forgingSync.getBoolValue() && getCurrentPass() < TOTAL_PASSES)
                    .size(74, 22).left(8).top(116))
            .child(new ButtonWidget<>()
                    .overlay(IKey.dynamic(() -> {
                        if (!forgingSync.getBoolValue() || getCurrentPass() >= TOTAL_PASSES) return "\u2692 Punch";
                        return getRequiredActionOrdinal() == ForgingAction.PUNCH.ordinal()
                                ? "\u00a7a\u00a7l\u2692 Punch" : "\u00a77\u2692 Punch";
                    }))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(SPacketMinigame.create(this,
                                SPacketMinigame.APPLY_ACTION, ForgingAction.PUNCH.ordinal(), getClientBarPos()));
                        return true;
                    })
                    .setEnabledIf(w -> forgingSync.getBoolValue() && getCurrentPass() < TOTAL_PASSES)
                    .size(74, 22).left(84).top(116))
            .child(new ButtonWidget<>()
                    .overlay(IKey.dynamic(() -> {
                        if (!forgingSync.getBoolValue() || getCurrentPass() >= TOTAL_PASSES) return "\u2692 Upset";
                        return getRequiredActionOrdinal() == ForgingAction.UPSET.ordinal()
                                ? "\u00a7a\u00a7l\u2692 Upset" : "\u00a77\u2692 Upset";
                    }))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(SPacketMinigame.create(this,
                                SPacketMinigame.APPLY_ACTION, ForgingAction.UPSET.ordinal(), getClientBarPos()));
                        return true;
                    })
                    .setEnabledIf(w -> forgingSync.getBoolValue() && getCurrentPass() < TOTAL_PASSES)
                    .size(74, 22).left(160).top(116))

            // ── Status hint ─────────────────────────────────────────────────
            .child(IKey.dynamic(() -> {
                        if (!forgingSync.getBoolValue()) {
                            return canForgeSync.getBoolValue()
                                    ? "\u00a7aReady \u2014 press Start"
                                    : "\u00a78Insert a hot ingot";
                        }
                        int pass = getCurrentPass();
                        if (pass >= TOTAL_PASSES) return "\u00a7aAll passes done \u2014 press Complete!";
                        return "\u00a7fPass " + (pass + 1) + "/" + TOTAL_PASSES
                                + "  \u00a7eStrike the \u00a7agreen\u00a7e action in the zone!";
                    }).asWidget()
                    .left(8).top(146))

            // ── Start / Cancel / Complete (same row, mutually exclusive) ─────
            .child(new ButtonWidget<>()
                    .overlay(IKey.str("\u00a7l\u00a7a\u25b6 Start Forging"))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(
                                SPacketMinigame.create(this, SPacketMinigame.START, 0f));
                        return true;
                    })
                    .setEnabledIf(w -> !forgingSync.getBoolValue() && canForgeSync.getBoolValue())
                    .size(120, 20).left(63).top(160))
            .child(new ButtonWidget<>()
                    .overlay(IKey.str("\u00a7c\u2716 Cancel"))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(
                                SPacketMinigame.create(this, SPacketMinigame.CANCEL, 0f));
                        return true;
                    })
                    .setEnabledIf(w -> forgingSync.getBoolValue() && getCurrentPass() < TOTAL_PASSES)
                    .size(50, 20).left(8).top(160))
            .child(new ButtonWidget<>()
                    .overlay(IKey.str("\u00a7l\u00a7a\u2714 Complete"))
                    .onMouseTapped(btn -> {
                        Honed.NETWORK.sendToServer(
                                SPacketMinigame.create(this, SPacketMinigame.COMPLETE, 0f));
                        return true;
                    })
                    .setEnabledIf(w -> forgingSync.getBoolValue() && getCurrentPass() == TOTAL_PASSES)
                    .size(120, 20).left(63).top(160))

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
        // BUG FIX: MUI calls this on the client every time the slot syncs (e.g. after
        // temperature NBT changes from cooling).  Only react when the item type truly
        // changes (not NBT-only updates).  lookupRecipes runs on BOTH sides so the
        // client's matchingRecipes list stays populated for the recipe label/cycling.
        ItemStack old = inputStack;
        inputStack = stack;
        if (world != null && !ItemStack.areItemsEqual(old, stack)) {
            if (!world.isRemote && forging) resetForging();
            lookupRecipes();
            if (!world.isRemote) markDirty();
        }
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inputStack;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!inputStack.isEmpty()) return stack;
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        ItemStack inserted = copy.splitStack(1);
        if (!simulate) {
            inputStack = inserted;
            if (world != null && !world.isRemote) {
                if (forging) resetForging();
                lookupRecipes();
                markDirty();
            }
        }
        return copy;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || inputStack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = inputStack.copy();
        if (!simulate) {
            inputStack = ItemStack.EMPTY;
            if (forging) resetForging();
            matchingRecipes.clear();
            markDirty();
        }
        return copy;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }
}
