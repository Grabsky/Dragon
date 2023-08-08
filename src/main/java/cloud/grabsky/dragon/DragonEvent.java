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
package cloud.grabsky.dragon;

import cloud.grabsky.bedrock.components.ComponentBuilder;
import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent;
import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DragonEvent implements Listener {

    private final @NotNull Dragon plugin;

    @Getter(AccessLevel.PUBLIC)
    private final @NotNull World world;

    @Getter(AccessLevel.PUBLIC)
    private @Nullable EnderDragon dragon;

    @Getter(AccessLevel.PUBLIC)
    private @Nullable BossBar bossBar;

    @Getter(AccessLevel.PUBLIC)
    private int currentPhase;

    @Getter(AccessLevel.PUBLIC)
    private int crystalsLeftToDestroy = 4;

    public DragonEvent(final @NotNull Dragon plugin, final @NotNull World world) {
        this.plugin = plugin;
        this.world = world;
        // ...
        this.refresh();
        // ...
        world.spawnEntity(new Location(world, 0, 120, 0), EntityType.ENDER_DRAGON, CreatureSpawnEvent.SpawnReason.CUSTOM, (entity) -> {
            this.bossBar = Bukkit.createBossBar("Ender Dragon", BarColor.PURPLE, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY);
            // ...
            final EnderDragon dragon = (EnderDragon) entity;
            // ...
            final AttributeInstance attr = dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(400.0d);
                dragon.setHealth(attr.getBaseValue());
            }
            dragon.setPhase(EnderDragon.Phase.CIRCLING);
            // ...
            this.dragon = dragon;
            // ...
            world.getPlayers().forEach(player -> bossBar.addPlayer(player));
        });
    }

    public void refresh() {
        HandlerList.unregisterAll(this);
        // ...
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void cancel() {
        HandlerList.unregisterAll(this);
        // ...
        if (this.dragon != null)
            this.dragon.remove();
        // ...
        if (this.bossBar != null)
            this.bossBar.removeAll();
    }

    @EventHandler
    public void onDragonGetHit(final EntityDamageEvent event) {
        if (this.dragon != null && event.getEntity() == this.dragon && this.bossBar != null) {
            plugin.getComponentLogger().info(ComponentBuilder.of("Dragon has been damaged by ").append(event.getDamage() + "", NamedTextColor.YELLOW).append("... It's now at " + this.dragon.getHealth() + "HP").build());
            // ...
            final AttributeInstance attr = this.dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) {
                final double progress = this.dragon.getHealth() / attr.getBaseValue();
                if (currentPhase == 0 && progress <= 0.66) {
                    currentPhase = 1;
                    // ...
                } else if (currentPhase == 1 && progress <= 0.33) {
                    currentPhase = 2;
                    // ...
                }
                // Delaying this for the next tick in case dragon decides to die.
                plugin.getBedrockScheduler().run(1L, (task) -> {
                    this.bossBar.setProgress(dragon.getHealth() > 1.0 ? progress : 0);
                });
            }
        }
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        if (event.getPlayer().getWorld() == this.world)
            if (this.bossBar != null)
                this.bossBar.addPlayer(event.getPlayer());
            else if (event.getFrom() == this.world && this.bossBar != null)
                bossBar.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onDragonDeath(final EntityDeathEvent event) {
        if (event.getEntity() == this.dragon && this.bossBar != null) {
            this.bossBar.removeAll();
        }
    }

    @EventHandler
    public void onDragonFireballShoot(final EnderDragonShootFireballEvent event) {
        // ...
    }

    @EventHandler
    public void onDragonPhaseChange(final EnderDragonChangePhaseEvent event) {
        plugin.getComponentLogger().info(ComponentBuilder.of("Trying to change phase to ").append(event.getNewPhase() + "...", NamedTextColor.GRAY).build());
        // Letting dragon do his thing after he's already dead...
        if (event.getEntity().getHealth() <= 1.0)
            return;
        // ...
        if (event.getNewPhase() != EnderDragon.Phase.DYING && event.getNewPhase() != EnderDragon.Phase.CIRCLING)
            event.setNewPhase(EnderDragon.Phase.CIRCLING);
        // ...
        plugin.getComponentLogger().info(ComponentBuilder.of("Dragon state changed to ").append(event.getNewPhase() + "", NamedTextColor.GREEN).build());
    }

    @EventHandler
    public void onDragonFireBall(final EnderDragonFireballHitEvent event) {
        // ...
    }

}
