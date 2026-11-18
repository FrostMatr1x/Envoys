package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionCommand;
import com.frost.envoys.npc.entity.BaseNPC;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class CommandActionHandler implements NpcActionHandler<ActionCommand> {

    @Override
    public void execute(ActionCommand action, ActionContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            ServerLevel level = serverPlayer.serverLevel();
            MinecraftServer server = level.getServer();

            if (server != null) {
                Entity entity = level.getEntity(context.npcUuid());
                CommandSourceStack source;

                if (entity instanceof BaseNPC npc) {
                    source = npc.createCommandSourceStack()
                            .withPermission(4)
                            .withSuppressedOutput();
                } else {
                    String npcName = context.manager().passport.npcName;
                    Component displayName = (npcName != null && !npcName.trim().isEmpty())
                            ? Component.literal(npcName)
                            : Component.literal("NPC");

                    source = new CommandSourceStack(
                            CommandSource.NULL,
                            serverPlayer.position(),
                            serverPlayer.getRotationVector(),
                            level,
                            4,
                            displayName.getString(),
                            displayName,
                            server,
                            null
                    ).withSuppressedOutput();
                }

                for (String cmd : action.commands) {
                    if (cmd != null && !cmd.trim().isEmpty()) {
                        String formattedCmd = cmd.trim();

                        if (formattedCmd.startsWith("/")) {
                            formattedCmd = formattedCmd.substring(1);
                        }

                        server.getCommands().performPrefixedCommand(source, formattedCmd);
                    }
                }
            }
        }

        context.advance(action.nextActionId);
    }
}
