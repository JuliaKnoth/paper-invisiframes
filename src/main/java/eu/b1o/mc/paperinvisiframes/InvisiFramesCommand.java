package eu.b1o.mc.paperinvisiframes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InvisiFramesCommand implements CommandExecutor, TabCompleter
{
    private final PaperInvisiframes paperInvisiframes;
    
    public InvisiFramesCommand(PaperInvisiframes paperInvisiframes)
    {
        this.paperInvisiframes = paperInvisiframes;
    }
    
    private void sendNoPermissionMessage(CommandSender sender)
    {
        sender.sendMessage(Component.text("I'm sorry, but you do not have permission to perform this " +
                "command. Please contact the server administrators if you believe that this is a mistake.", NamedTextColor.RED));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            sender.sendMessage(Component.text("PaperInvisiframes v" + paperInvisiframes.getPluginMeta().getVersion() + " by " + String.join(" & ", paperInvisiframes.getPluginMeta().getAuthors()), NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Available subcommands:", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/iframe get - Gives the player an invisible item frame", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/iframe reload - Reloads the config", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/iframe force-recheck - Forces all invisible frames to re-check their visibility", NamedTextColor.GOLD));
            return true;
        }
        if(args[0].equalsIgnoreCase("reload"))
        {
            if(!sender.hasPermission("paperinvisiframes.reload"))
            {
                sendNoPermissionMessage(sender);
                return true;
            }
            paperInvisiframes.reload();
            sender.sendMessage(Component.text("Reloaded!", NamedTextColor.GREEN));
            return true;
        }
        else if(args[0].equalsIgnoreCase("force-recheck"))
        {
            if(!sender.hasPermission("paperinvisiframes.forcerecheck"))
            {
                sendNoPermissionMessage(sender);
                return true;
            }
            paperInvisiframes.forceRecheck();
            sender.sendMessage(Component.text("Rechecked invisible item frames", NamedTextColor.GREEN));
            return true;
        }
        else if(args[0].equalsIgnoreCase("get"))
        {
            if(!sender.hasPermission("paperinvisiframes.get"))
            {
                sendNoPermissionMessage(sender);
                return true;
            }
            if(!(sender instanceof Player))
            {
                sender.sendMessage(Component.text("This command can only be run by a player", NamedTextColor.RED));
                return true;
            }
            Player player = (Player) sender;
            ItemStack item = PaperInvisiframes.generateInvisibleItemFrame();
            player.getInventory().addItem(item);
            sender.sendMessage(Component.text("You have been given an invisible item frame", NamedTextColor.GREEN));
            return true;
        }
        return false;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length != 1)
        {
            return Collections.emptyList();
        }
        List<String> options = new ArrayList<>();
        options.add("help");
        if(sender.hasPermission("paperinvisiframes.get"))
        {
            options.add("get");
        }
        if(sender.hasPermission("paperinvisiframes.reload"))
        {
            options.add("reload");
        }
        if(sender.hasPermission("paperinvisiframes.forcerecheck"))
        {
            options.add("force-recheck");
        }
        List<String> completions = new ArrayList<>();
        StringUtil.copyPartialMatches(args[0], options, completions);
        return completions;
    }
}
