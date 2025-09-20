package com.x1f4r.mmocraft.demo.skill;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import com.x1f4r.mmocraft.demo.pet.DemoCompanionPets;
import com.x1f4r.mmocraft.pet.model.ActiveCompanionPet;
import com.x1f4r.mmocraft.pet.model.CompanionPetDefinition;
import com.x1f4r.mmocraft.pet.service.CompanionPetService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LuckySpriteSummonSkillTest {

    @Mock
    private MMOCraftPlugin plugin;
    @Mock
    private CompanionPetService petService;
    @Mock
    private PlayerProfile profile;
    @Mock
    private Player player;

    @Test
    void execute_whenNoPetActive_summonsLuckySprite() {
        UUID playerId = UUID.randomUUID();
        when(plugin.getCompanionPetService()).thenReturn(petService);
        when(profile.getPlayerUUID()).thenReturn(playerId);
        when(petService.getActivePet(playerId)).thenReturn(Optional.empty());

        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);
        when(player.getLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        lenient().doNothing().when(world).spawnParticle(Mockito.any(Particle.class), Mockito.any(Location.class), Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble());
        lenient().doNothing().when(world).playSound(Mockito.any(Location.class), Mockito.any(Sound.class), Mockito.anyFloat(), Mockito.anyFloat());
        doNothing().when(profile).consumeMana(Mockito.anyLong());
        when(player.getUniqueId()).thenReturn(playerId);

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(player);

            LuckySpriteSummonSkill skill = new LuckySpriteSummonSkill(plugin);
            skill.execute(profile, null, null);

            ArgumentCaptor<CompanionPetDefinition> definitionCaptor = ArgumentCaptor.forClass(CompanionPetDefinition.class);
            verify(petService).summonPet(eq(player), definitionCaptor.capture());
            assertEquals(DemoCompanionPets.LUCKY_SPRITE_ID, definitionCaptor.getValue().id());
        }
    }

    @Test
    void execute_whenPetActive_dismissesWithoutMana() {
        UUID playerId = UUID.randomUUID();
        when(plugin.getCompanionPetService()).thenReturn(petService);
        when(profile.getPlayerUUID()).thenReturn(playerId);

        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);
        when(player.getLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        when(player.getUniqueId()).thenReturn(playerId);
        CompanionPetDefinition definition = DemoCompanionPets.luckySprite();
        ActiveCompanionPet activePet = new ActiveCompanionPet(playerId, null, definition, "status:lucky_sprite");
        when(petService.getActivePet(playerId)).thenReturn(Optional.of(activePet));

        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return mock(BukkitTask.class);
        });

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(player);

            LuckySpriteSummonSkill skill = new LuckySpriteSummonSkill(plugin);
            skill.execute(profile, null, null);

            verify(petService).dismissPet(player);
            verify(profile, times(0)).consumeMana(Mockito.anyLong());
            verify(profile).setSkillCooldown(LuckySpriteSummonSkill.SKILL_ID, 0);
        }
    }
}

