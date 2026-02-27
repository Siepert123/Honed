package quantumstudios.honed.registry;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import quantumstudios.honed.Honed;

import javax.annotation.Nullable;

public enum HonedGUIs {
    FORGING_ANVIL;

    private final int index = this.ordinal();
    public int index() {
        return this.index;
    }

    public void open(World world, EntityPlayer player, int x, int y, int z) {
        player.openGui(Honed.instance(), this.index(), world, x, y, z);
    }
    public void open(World world, EntityPlayer player) {
        this.open(world, player, (int)player.posX, (int)player.posY, (int)player.posZ);
    }
    public void open(EntityPlayer player) {
        this.open(player.world, player);
    }

    public static class Handler implements IGuiHandler {
        @Nullable
        @Override
        public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            return null;
        }

        @Nullable
        @Override
        public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            return null;
        }
    }
}
