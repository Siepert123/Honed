package quantumstudios.honed.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import quantumstudios.honed.Tags;

//Clientbound packet
public abstract class CPacketBase {
    public abstract String name(); //Name for logging

    @SideOnly(Side.CLIENT)
    public abstract void handle(FMLProxyPacket packet, EntityPlayerSP player, Minecraft mc);

    protected static PacketBuffer buf(Class<? extends CPacketBase> clazz) {
        return HonedNetworkManager.buf(clazz);
    }
    protected static FMLProxyPacket build(PacketBuffer buf) {
        return new FMLProxyPacket(buf, Tags.MOD_ID);
    }
}
