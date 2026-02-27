package quantumstudios.honed.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import quantumstudios.honed.Tags;

//Serverbound packet
public abstract class SPacketBase {
    public abstract String name(); //For logging purposes

    public abstract void handle(FMLProxyPacket packet, EntityPlayer player);

    protected static PacketBuffer buf(Class<? extends SPacketBase> clazz) {
        return HonedNetworkManager.buf(clazz);
    }
    protected static FMLProxyPacket build(PacketBuffer buf) {
        return new FMLProxyPacket(buf, Tags.MOD_ID);
    }
}
