package quantumstudios.honed.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import quantumstudios.honed.config.HonedConfig;
import quantumstudios.honed.tool.ToolNBT;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
public class CommandHeat extends CommandBase {

    @Override
    @Nonnull
    public String getName() {
        return "honed";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/honed <heat [°C]|cool>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    @Nonnull
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, net.minecraft.util.math.BlockPos pos) {
        if (args.length == 1) return Arrays.asList("heat", "cool");
        return java.util.Collections.emptyList();
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender,
                        @Nonnull String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "Must be run by a player."));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "Usage: " + getUsage(sender)));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack held = player.getHeldItemMainhand();

        if (held.isEmpty()) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "Hold an item first."));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "heat": {
                float maxK = (float) HonedConfig.smelteryMaxTemp;
                float targetK;
                if (args.length >= 2) {
                    try {
                        float targetC = Float.parseFloat(args[1]);
                        targetK = targetC + 273f;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(new TextComponentString(
                                TextFormatting.RED + "Invalid temperature: " + args[1]));
                        return;
                    }
                } else {
                    targetK = maxK;
                }
                targetK = Math.min(targetK, maxK);
                ToolNBT.setTemperature(held, targetK);
                int displayC = Math.round(targetK) - 273;
                sender.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "Heated to " + displayC + " °C."));
                break;
            }
            case "cool": {
                ToolNBT.setTemperature(held, 0f);
                sender.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "Cooled to 0 °C."));
                break;
            }
            default:
                sender.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "Usage: " + getUsage(sender)));
        }
    }
}
