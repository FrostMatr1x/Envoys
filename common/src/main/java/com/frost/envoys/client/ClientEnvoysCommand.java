package com.frost.envoys.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;

/**
 * Shared root for client-side Envoys commands.
 *
 * <p>Client commands such as {@code /envoys lua} and {@code /envoys anim} share their root
 * with server-side commands such as {@code /envoys npc}. Fabric only forwards a command to the
 * server when the root itself is unknown, so a client command tree that contains just
 * {@code lua}/{@code anim} makes {@code /envoys npc ...} fail locally with
 * "Incorrect argument for command". The greedy fallback below forwards every subcommand that is
 * not handled on the client to the server.</p>
 */
public final class ClientEnvoysCommand {

    private static final String SERVER_ARGS = "serverArgs";

    private ClientEnvoysCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal("envoys")
                .executes(context -> forwardToServer("envoys"))
                .then(Commands.argument(SERVER_ARGS, StringArgumentType.greedyString())
                        .executes(context -> forwardToServer(
                                "envoys " + StringArgumentType.getString(context, SERVER_ARGS))));
    }

    public static int forwardToServer(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return 0;
        }

        // Send the packet directly: going through sendCommand() would run it through the
        // client command dispatcher again and could recurse into this fallback.
        minecraft.getConnection().send(new ServerboundChatCommandPacket(command));
        return 1;
    }
}
