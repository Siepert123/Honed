package quantumstudios.honed.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import quantumstudios.honed.data.forging.ForgingAction;
import quantumstudios.honed.network.SPacketBase;
import quantumstudios.honed.te.TileEntityForgingAnvil;

public class SPacketMinigame extends SPacketBase {

    public static final byte START = 0;
    public static final byte CYCLE_NEXT = 1;
    public static final byte CYCLE_PREV = 2;
    public static final byte APPLY_ACTION = 3;
    public static final byte COMPLETE = 4;
    public static final byte CANCEL = 5;

    @Override
    public void handle(PacketBuffer data, EntityPlayer player) {
        BlockPos pos = BlockPos.fromLong(data.readLong());
        byte action = data.readByte();
        float payload = data.readFloat();
        float clientBarPos = data.readFloat();

        // Distance check — ignore packets from players too far away
        if (player.getDistanceSq(pos) > 64) return;

        TileEntity te = player.world.getTileEntity(pos);
        if (!(te instanceof TileEntityForgingAnvil)) return;
        TileEntityForgingAnvil anvil = (TileEntityForgingAnvil) te;

        switch (action) {
            case START:       anvil.startForging(); break;
            case CYCLE_NEXT:  anvil.cycleRecipe(1); break;
            case CYCLE_PREV:  anvil.cycleRecipe(-1); break;
            case APPLY_ACTION: anvil.applyAction(ForgingAction.fromOrdinal((int) payload), clientBarPos); break;
            case COMPLETE:    anvil.completeForging(); break;
            case CANCEL:      anvil.cancelForging(); break;
        }
    }

    public static FMLProxyPacket create(TileEntityForgingAnvil te, byte action, float payload) {
        return create(te, action, payload, 0f);
    }

    public static FMLProxyPacket create(TileEntityForgingAnvil te, byte action, float payload, float clientBarPos) {
        PacketBuffer data = buf(SPacketMinigame.class);
        data.writeLong(te.getPos().toLong());
        data.writeByte(action);
        data.writeFloat(payload);
        data.writeFloat(clientBarPos);
        return build(data);
    }
}
