package quantumstudios.honed.network;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import quantumstudios.honed.Honed;

public final class HonedNetworkManager {
    private static final Object2ByteMap<Class<? extends CPacketBase>> clientboundPacketIDs = new Object2ByteArrayMap<>();
    private static final Byte2ObjectMap<CPacketBase> clientboundPackets = new Byte2ObjectArrayMap<>();
    private static final Object2ByteMap<Class<? extends SPacketBase>> serverboundPacketIDs = new Object2ByteArrayMap<>();
    private static final Byte2ObjectMap<SPacketBase> serverboundPackets = new Byte2ObjectArrayMap<>();
    private static boolean packetsRegistered = false;
    private static void registerPackets() {
        packetsRegistered = true;
    }
    private static void register(CPacketBase packet, int id) {
        clientboundPacketIDs.put(packet.getClass(), (byte) id);
        clientboundPackets.put((byte) id, packet);
    }
    private static void register(SPacketBase packet, int id) {
        serverboundPacketIDs.put(packet.getClass(), (byte) id);
        serverboundPackets.put((byte) id, packet);
    }

    public HonedNetworkManager() {
        if (packetsRegistered) {
            Honed.LOGGER.warn("Honed network manager created twice?");
        } else {
            Honed.LOGGER.info("Honed network manager created");
            registerPackets();
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void receivedClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        FMLProxyPacket packet = event.getPacket();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;

        if (packet == null || packet.payload() == null) {
            Honed.LOGGER.warn("Received empty clientbound packet");
            return;
        }
        byte type = packet.payload().readByte();

        CPacketBase handler = clientboundPackets.get(type);
        if (handler == null) {
            Honed.LOGGER.warn("Received unregistered clientbound packet (ID: {})", type);
            return;
        }

        try {
            handler.handle(packet, player, mc);
        } catch (Exception e) {
            Honed.LOGGER.error("Exception handling clientbound packet {}: {}", handler.name(), e.toString());
        }
    }

    @SubscribeEvent
    public void receivedServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        FMLProxyPacket packet = event.getPacket();
        EntityPlayer player = ((NetHandlerPlayServer)event.getHandler()).player;

        if (packet == null || packet.payload() == null) {
            Honed.LOGGER.warn("Received empty serverbound packet");
            return;
        }
        byte type = packet.payload().readByte();

        SPacketBase handler = serverboundPackets.get(type);
        if (handler == null) {
            Honed.LOGGER.warn("Received unregistered serverbound packet (ID: {})", type);
            return;
        }

        try {
            handler.handle(packet, player);
        } catch (Exception e) {
            Honed.LOGGER.error("Exception handling serverbound packet {}: {}", handler.name(), e.toString());
        }
    }

    public static PacketBuffer buf(Class<?> clazz) {
        byte id;
        if (CPacketBase.class.isAssignableFrom(clazz)) {
            id = clientboundPacketIDs.get(clazz);
        } else if (SPacketBase.class.isAssignableFrom(clazz)) {
            id = serverboundPacketIDs.get(clazz);
        } else throw new IllegalArgumentException("Class is not a packet class: " + clazz.getSimpleName());
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeByte(id);
        return buf;
    }
}
