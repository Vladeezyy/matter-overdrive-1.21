package matteroverdrive.android;

import java.util.function.Consumer;

import net.minecraft.world.entity.player.Player;

/** Client callbacks set by the client setup, so common code never loads client classes. */
public final class AndroidClientHooks {
    /** The local player started turning into an android: 1.7.10 played the transformation music. */
    public static Consumer<Player> transformationStarted = player -> {};
    /** The ability key's name for stat descriptions ("X"). */
    public static java.util.function.Supplier<String> abilityKeyName = () -> "X";

    /** A shield hit on the player with that entity id; the vector points from the player's chest to the attacker. */
    public static java.util.function.BiConsumer<Integer, net.minecraft.world.phys.Vec3> shieldHit = (id, offset) -> {};

    static void onTransformationStarted(Player player) {
        transformationStarted.accept(player);
    }

    private AndroidClientHooks() {}
}
