package quantumstudios.honed.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import quantumstudios.honed.Honed;
import quantumstudios.honed.gui.container.ContainerKnapping;
import quantumstudios.honed.network.packet.SPacketKnappingClick;
import quantumstudios.honed.util.ColorHelper;

import java.io.IOException;

public class GuiKnapping extends GuiContainer {

    private final ContainerKnapping containerKnapping;

    private static final int GRID_X = 8;
    private static final int GRID_Y = 8;
    private static final int CELL = 16;
    private static final int PITCH = 18;
    private static final int SIZE = 5;

    // Dark metallic palette
    private static final int PNL_BG = 0xFF22222A;
    private static final int PNL_EDGE_HI = 0xFF3E3E4A;
    private static final int PNL_EDGE_LO = 0xFF111116;
    private static final int GRID_FRAME = 0xFF151519;
    private static final int GRID_INNER = 0xFF1E1E26;
    private static final int CARVED_BG = 0xFF0A0A0E;
    private static final int CARVED_SHADOW = 0xFF050508;
    private static final int CARVED_EDGE = 0xFF131318;
    private static final int HOVER_OVL = 0x35FFFFFF;
    private static final int SLOT_DARK = 0xFF18181E;
    private static final int SLOT_LIGHT = 0xFF3C3C48;
    private static final int SLOT_MID = 0xFF2A2A34;
    private static final int SEP_LINE = 0xFF333340;
    private static final int ARROW_BODY = 0xFF55555E;
    private static final int ARROW_HI = 0xFF77778A;
    private static final int OUTPUT_ACCENT = 0xFFAA8833;

    public GuiKnapping(Container container) {
        super(container);
        this.containerKnapping = (ContainerKnapping) container;
        this.xSize = 186;
        this.ySize = 184;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();
        int gx = (this.width - this.xSize) / 2;
        int gy = (this.height - this.ySize) / 2;

        drawPanel(gx, gy);
        drawGrid(gx, gy, mouseX, mouseY);
        drawArrow(gx, gy);
        drawAllSlots(gx, gy);
        drawSeparator(gx, gy);
    }

    private void drawPanel(int gx, int gy) {
        // Outer border
        Gui.drawRect(gx, gy, gx + xSize, gy + ySize, PNL_EDGE_LO);
        // Main background
        Gui.drawRect(gx + 1, gy + 1, gx + xSize - 1, gy + ySize - 1, PNL_BG);
        // Top + left bevel highlight
        Gui.drawRect(gx + 1, gy + 1, gx + xSize - 1, gy + 2, PNL_EDGE_HI);
        Gui.drawRect(gx + 1, gy + 2, gx + 2, gy + ySize - 1, PNL_EDGE_HI);
        // Bottom + right bevel shadow
        Gui.drawRect(gx + 2, gy + ySize - 2, gx + xSize - 1, gy + ySize - 1, PNL_EDGE_LO);
        Gui.drawRect(gx + xSize - 2, gy + 2, gx + xSize - 1, gy + ySize - 2, PNL_EDGE_LO);
    }

    private void drawGrid(int gx, int gy, int mx, int my) {
        int matColor = ColorHelper.opaque(containerKnapping.materialColor);
        int gridX = gx + GRID_X;
        int gridY = gy + GRID_Y;
        int total = SIZE * PITCH;

        // Inset frame
        Gui.drawRect(gridX - 2, gridY - 2, gridX + total + 2, gridY + total + 2, GRID_FRAME);
        Gui.drawRect(gridX - 1, gridY - 1, gridX + total + 1, gridY + total + 1, GRID_INNER);

        // Hover detection
        int relX = mx - gridX;
        int relY = my - gridY;
        int hx = (relX >= 0 && relX < total) ? relX / PITCH : -1;
        int hy = (relY >= 0 && relY < total) ? relY / PITCH : -1;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                int cx = gridX + x * PITCH + 1;
                int cy = gridY + y * PITCH + 1;
                boolean carved = containerKnapping.grid[x][y];

                if (carved) {
                    // Deep carved inset
                    Gui.drawRect(cx, cy, cx + CELL, cy + CELL, CARVED_BG);
                    Gui.drawRect(cx, cy, cx + CELL, cy + 1, CARVED_SHADOW);
                    Gui.drawRect(cx, cy, cx + 1, cy + CELL, CARVED_SHADOW);
                    Gui.drawRect(cx + 1, cy + CELL - 1, cx + CELL, cy + CELL, CARVED_EDGE);
                    Gui.drawRect(cx + CELL - 1, cy + 1, cx + CELL, cy + CELL, CARVED_EDGE);
                } else {
                    // Material cell with 3D bevel
                    int lighter = ColorHelper.blend(matColor, 0xFFFFFF, 0.28f) | 0xFF000000;
                    int darker = ColorHelper.blend(matColor, 0x000000, 0.35f) | 0xFF000000;
                    int midHi = ColorHelper.blend(matColor, 0xFFFFFF, 0.10f) | 0xFF000000;
                    Gui.drawRect(cx, cy, cx + CELL, cy + CELL, matColor);
                    Gui.drawRect(cx, cy, cx + CELL, cy + 1, lighter);
                    Gui.drawRect(cx, cy + 1, cx + 1, cy + CELL - 1, midHi);
                    Gui.drawRect(cx, cy + CELL - 1, cx + CELL, cy + CELL, darker);
                    Gui.drawRect(cx + CELL - 1, cy + 1, cx + CELL, cy + CELL - 1, darker);

                    // Hover highlight
                    if (x == hx && y == hy) {
                        Gui.drawRect(cx, cy, cx + CELL, cy + CELL, HOVER_OVL);
                    }
                }
            }
        }
    }

    private void drawArrow(int gx, int gy) {
        int gridTotal = SIZE * PITCH;
        int ax = gx + 104;
        int ay = gy + GRID_Y + gridTotal / 2;
        // Shaft
        Gui.drawRect(ax, ay - 1, ax + 20, ay + 2, ARROW_BODY);
        Gui.drawRect(ax, ay, ax + 18, ay + 1, ARROW_HI);
        // Arrowhead
        Gui.drawRect(ax + 16, ay - 4, ax + 18, ay - 1, ARROW_BODY);
        Gui.drawRect(ax + 18, ay - 2, ax + 20, ay, ARROW_BODY);
        Gui.drawRect(ax + 16, ay + 2, ax + 18, ay + 5, ARROW_BODY);
        Gui.drawRect(ax + 18, ay + 1, ax + 20, ay + 3, ARROW_BODY);
        Gui.drawRect(ax + 20, ay - 1, ax + 22, ay + 2, ARROW_BODY);
    }

    private void drawAllSlots(int gx, int gy) {
        for (Slot slot : this.inventorySlots.inventorySlots) {
            boolean isOutput = slot.xPos == 136 && slot.yPos == 45;
            if (isOutput) {
                drawOutputSlot(gx, gy, slot);
            } else {
                drawSlotBg(gx, gy, slot);
            }
        }
    }

    private void drawSlotBg(int gx, int gy, Slot slot) {
        int sx = gx + slot.xPos;
        int sy = gy + slot.yPos;
        // Inset border
        Gui.drawRect(sx - 1, sy - 1, sx + 17, sy, SLOT_DARK);
        Gui.drawRect(sx - 1, sy - 1, sx, sy + 17, SLOT_DARK);
        Gui.drawRect(sx - 1, sy + 16, sx + 17, sy + 17, SLOT_LIGHT);
        Gui.drawRect(sx + 16, sy - 1, sx + 17, sy + 17, SLOT_LIGHT);
        Gui.drawRect(sx, sy, sx + 16, sy + 16, SLOT_MID);
    }

    private void drawOutputSlot(int gx, int gy, Slot slot) {
        int sx = gx + slot.xPos;
        int sy = gy + slot.yPos;
        // Golden accent ring
        Gui.drawRect(sx - 3, sy - 3, sx + 19, sy + 19, OUTPUT_ACCENT);
        Gui.drawRect(sx - 2, sy - 2, sx + 18, sy + 18, 0xFF332A11);
        drawSlotBg(gx, gy, slot);
    }

    private void drawSeparator(int gx, int gy) {
        Gui.drawRect(gx + 6, gy + 99, gx + xSize - 6, gy + 100, SEP_LINE);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);

        int gx = (this.width - this.xSize) / 2;
        int gy = (this.height - this.ySize) / 2;
        int gridX = gx + GRID_X;
        int gridY = gy + GRID_Y;
        int total = SIZE * PITCH;

        int relX = mouseX - gridX;
        int relY = mouseY - gridY;

        if (relX >= 0 && relX < total && relY >= 0 && relY < total) {
            int cx = relX / PITCH;
            int cy = relY / PITCH;
            if (cx < SIZE && cy < SIZE && !containerKnapping.grid[cx][cy]) {
                containerKnapping.grid[cx][cy] = true;
                Honed.NETWORK.sendToServer(SPacketKnappingClick.create(cx, cy));
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}
