/*
 * MIT License
 *
 * Copyright (c) 2023 Grabsky <44530932+Grabsky@users.noreply.github.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * HORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package cloud.grabsky.dragon.commands;

import cloud.grabsky.bedrock.components.Message;
import cloud.grabsky.commands.ArgumentQueue;
import cloud.grabsky.commands.Command;
import cloud.grabsky.commands.Dependency;
import cloud.grabsky.commands.RootCommand;
import cloud.grabsky.commands.RootCommandContext;
import cloud.grabsky.commands.component.CompletionsProvider;
import cloud.grabsky.commands.exception.CommandLogicException;
import cloud.grabsky.dragon.Dragon;
import cloud.grabsky.dragon.DragonEvent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Command(name = "dragon", permission = "dragon.command.dragon")
public final class DragonCommand extends RootCommand {

    @Dependency
    private @UnknownNullability Dragon plugin; // Generally should not be null.

    private static @Nullable DragonEvent DRAGON_EVENT;

    @Override
    public @NotNull CompletionsProvider onTabComplete(final @NotNull RootCommandContext context, final int index) throws CommandLogicException {
        return CompletionsProvider.EMPTY;
    }

    @Override
    public void onCommand(final @NotNull RootCommandContext context, final @NotNull ArgumentQueue arguments) throws CommandLogicException {
        final Player sender = context.getExecutor().asPlayer();
        // ...
        if (arguments.hasNext() == false) {
            Message.of("unknown command").send(sender);
            return;
        }
        // ...
        final String literal = arguments.next(String.class).asRequired().toLowerCase();
        // ...
        switch (literal) {
            case "start" -> {
                if (sender.getWorld().getEnvironment() == World.Environment.THE_END && DRAGON_EVENT == null)
                    DRAGON_EVENT = new DragonEvent(plugin, sender.getWorld());
            }
            case "cancel" -> {
                if (DRAGON_EVENT != null)
                    DRAGON_EVENT.cancel();
                // ...
                DRAGON_EVENT = null;
            }
            case "refresh" -> {
                if (DRAGON_EVENT != null)
                    DRAGON_EVENT.refresh();
            }
            case "add_crystal" -> {
                if (DRAGON_EVENT != null) {
                    Message.of("You cannot do that while the event is running.").send(sender);
                    return;
                }
                // ...
                Message.of("Click on a block...").send(sender);
                // ...
                final Listener listener = new Listener() {

                    @EventHandler(ignoreCancelled = true)
                    public void onClick(final PlayerInteractEvent event) {
                        if (event.getPlayer() == sender && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            // ...
                            event.getHandlers().unregister(this);
                        }
                    }

                };
                // ...
                plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                // Unregistering after n ticks...
                plugin.getBedrockScheduler().run(200L, (task) -> {
                    HandlerList.unregisterAll(listener);
                });
            }
        }
    }
}
