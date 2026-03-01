package quantumstudios.honed.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import quantumstudios.honed.gui.container.ContainerKnapping;

import javax.annotation.Nullable;

public class KnappingGuiHandler implements IGuiHandler {

    public static final int KNAPPING_GUI_ID = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == KNAPPING_GUI_ID) {
            return new ContainerKnapping(player.inventory, x);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == KNAPPING_GUI_ID) {
            return new GuiKnapping(new ContainerKnapping(player.inventory, x));
        }
        return null;
    }
}
