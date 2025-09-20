package com.x1f4r.mmocraft.command.commands.admin;

import com.x1f4r.mmocraft.core.MMOCraftPlugin;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MMOCAdminRootCommandTest {

    @Mock
    private MMOCraftPlugin plugin;
    @Mock
    private CommandSender sender;

    @BeforeEach
    void setUp() {
        when(sender.hasPermission("mmocraft.admin")).thenReturn(true);
    }

    @Test
    void executeReloadConfigSubcommand_invokesPluginReload() {
        MMOCAdminRootCommand rootCommand = new MMOCAdminRootCommand(plugin);

        boolean handled = rootCommand.onCommand(sender, null, "mmocadm", new String[]{"reloadconfig"});

        assertTrue(handled);
        verify(plugin).reloadPluginConfig();
        verify(sender).sendMessage(contains("configuration reloaded"));
    }
}
