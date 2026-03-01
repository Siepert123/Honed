package quantumstudios.honed.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import quantumstudios.honed.gui.container.ContainerKnapping;
import quantumstudios.honed.network.SPacketBase;

public class SPacketKnappingClick extends SPacketBase {

    @Override
    public void handle(PacketBuffer data, EntityPlayer player) {
        int x = data.readByte();
        int y = data.readByte();

        if (player.openContainer instanceof ContainerKnapping) {
            ((ContainerKnapping) player.openContainer).clickedOnSlot(x, y);
        }
    }

    public static FMLProxyPacket create(int x, int y) {
        PacketBuffer data = buf(SPacketKnappingClick.class);
        data.writeByte(x);
        data.writeByte(y);
        return build(data);
    }
}
