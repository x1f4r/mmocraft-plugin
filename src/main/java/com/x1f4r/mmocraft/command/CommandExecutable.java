package com.x1f4r.mmocraft.command;

import org.bukkit.command.CommandSender;
import java.util.List;

public interface CommandExecutable {
    /**
     * Executes the given command, returning its success.
     *
     * @param sender Source of the command
     * @param args   Passed command arguments
     * @return true if a valid command, otherwise false
     */
    boolean onCommand(CommandSender sender, String[] args);

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender Source of the command
     * @param args   Arguments passed to the command, including final partial argument to be completed
     * @return A List of possible completions for the final argument, or null to default to normal auto-complete
     */
    List<String> onTabComplete(CommandSender sender, String[] args);
}
