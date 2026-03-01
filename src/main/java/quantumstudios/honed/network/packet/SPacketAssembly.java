package quantumstudios.honed.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import quantumstudios.honed.network.SPacketBase;
import quantumstudios.honed.te.TileEntityAssemblyTable;

public class SPacketAssembly extends SPacketBase {

    public static final byte CYCLE_NEXT = 0;
    public static final byte CYCLE_PREV = 1;
    public static final byte ASSEMBLE   = 2;

    @Override
    public void handle(PacketBuffer data, EntityPlayer player) {
        BlockPos pos = BlockPos.fromLong(data.readLong());
        byte action = data.readByte();

        if (player.getDistanceSq(pos) > 64) return;

        TileEntity te = player.world.getTileEntity(pos);
        if (!(te instanceof TileEntityAssemblyTable)) return;
        TileEntityAssemblyTable table = (TileEntityAssemblyTable) te;

        switch (action) {
            case CYCLE_NEXT: table.cycleToolType(1);  break;
            case CYCLE_PREV: table.cycleToolType(-1); break;
            case ASSEMBLE:   table.assemble();        break;
        }
    }

    public static FMLProxyPacket create(TileEntityAssemblyTable te, byte action) {
        PacketBuffer data = buf(SPacketAssembly.class);
        data.writeLong(te.getPos().toLong());
        data.writeByte(action);
        return build(data);
    }
}
