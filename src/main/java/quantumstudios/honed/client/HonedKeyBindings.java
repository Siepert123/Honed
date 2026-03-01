package quantumstudios.honed.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public final class HonedKeyBindings {

    public static final KeyBinding KNAP = new KeyBinding(
            "key.honed.knap",
            Keyboard.KEY_K,
            "key.categories.honed"
    );

    private HonedKeyBindings() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(KNAP);
    }
}
