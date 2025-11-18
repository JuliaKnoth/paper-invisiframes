package eu.b1o.mc.paperinvisiframes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

public class PaperInvisiframes extends JavaPlugin implements Listener
{
    private NamespacedKey invisibleRecipe;
    private NamespacedKey invisibleGlowingRecipe;
    private static NamespacedKey invisibleKey;
    
    public static NamespacedKey getInvisibleKey() {
        return invisibleKey;
    }
    
    private boolean framesGlow;
    private boolean firstLoad = true;
    
    // Stays null if not in 1.17
    private Material glowInkSac = null;
    private Material glowFrame = null;
    private EntityType glowFrameEntity = null;
    
    @Override
    public void onEnable()
    {
        invisibleRecipe = new NamespacedKey(this, "invisible-recipe");
        invisibleGlowingRecipe = new NamespacedKey(this, "invisible-glowing-recipe");
        invisibleKey = new NamespacedKey(this, "invisible");
        
    
        try
        {
            glowInkSac = Material.valueOf("GLOW_INK_SAC");
            glowFrame = Material.valueOf("GLOW_ITEM_FRAME");
            glowFrameEntity = EntityType.valueOf("GLOW_ITEM_FRAME");
        }
        catch(IllegalArgumentException ignored) {}
        
        reload();
        
        getServer().getPluginManager().registerEvents(this, this);
        InvisiFramesCommand invisiFramesCommand = new InvisiFramesCommand(this);
        getCommand("iframe").setExecutor(invisiFramesCommand);
        getCommand("iframe").setTabCompleter(invisiFramesCommand);
    }
    
    @Override
    public void onDisable()
    {
        // Remove added recipes on plugin disable
        removeRecipe();
    }
    
    private void removeRecipe()
    {
        Iterator<Recipe> iter = getServer().recipeIterator();
        while(iter.hasNext())
        {
            Recipe check = iter.next();
            if(isInvisibleRecipe(check) || isGlowingInvisibleRecipe(check))
            {
                iter.remove();
            }
        }
    }
    
    public void reload()
    {
        saveDefaultConfig();
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        removeRecipe();
        
        if(firstLoad)
        {
            firstLoad = false;
            framesGlow = !getConfig().getBoolean("item-frames-glow");
        }
        if(getConfig().getBoolean("item-frames-glow") != framesGlow)
        {
            framesGlow = getConfig().getBoolean("item-frames-glow");
            forceRecheck();
        }
    
        ItemStack invisibleItem = generateInvisibleItemFrame();
        invisibleItem.setAmount(1);
        
        ShapedRecipe invisRecipe = new ShapedRecipe(invisibleRecipe, invisibleItem);
        invisRecipe.shape("FFF", "FPF", "FFF");
        invisRecipe.setIngredient('F', Material.GLASS_PANE);
        invisRecipe.setIngredient('P', Material.ITEM_FRAME);
        Bukkit.addRecipe(invisRecipe);

        if (glowInkSac != null && glowFrame != null) {
            ItemStack invisibleGlowingItem = generateInvisibleItemFrame().withType(glowFrame);
            ItemMeta meta = invisibleGlowingItem.getItemMeta();
            meta.displayName(Component.text("Glow Invisible Item Frame", NamedTextColor.LIGHT_PURPLE));
            invisibleGlowingItem.setItemMeta(meta);

            ShapedRecipe invisGlowRecipe = new ShapedRecipe(invisibleGlowingRecipe, invisibleGlowingItem);
            invisGlowRecipe.shape("FFF", "FPF", "FFF");
            invisGlowRecipe.setIngredient('F', Material.GLASS_PANE);
            invisGlowRecipe.setIngredient('P', glowFrame);
            Bukkit.addRecipe(invisGlowRecipe);
        }
    }
    
    public void forceRecheck()
    {
        for(World world : Bukkit.getWorlds())
        {
            for(ItemFrame frame : world.getEntitiesByClass(ItemFrame.class))
            {
                if(frame.getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE))
                {
                    if(frame.getItem().getType() == Material.AIR && framesGlow)
                    {
                        frame.setGlowing(true);
                        frame.setVisible(true);
                    }
                    else if(frame.getItem().getType() != Material.AIR)
                    {
                        frame.setGlowing(false);
                        frame.setVisible(false);
                    }
                }
            }
        }
    }
    
    private boolean isInvisibleRecipe(Recipe recipe)
    {
        return (recipe instanceof ShapedRecipe && ((ShapedRecipe) recipe).getKey().equals(invisibleRecipe));
    }

    private boolean isGlowingInvisibleRecipe(Recipe recipe) {
        return (recipe instanceof ShapedRecipe && ((ShapedRecipe) recipe).getKey().equals(invisibleGlowingRecipe));
    }
    
    private boolean isFrameEntity(Entity entity)
    {
        return (entity != null && (entity.getType() == EntityType.ITEM_FRAME ||
                (glowFrameEntity != null && entity.getType() == glowFrameEntity)));
    }
    
    public static ItemStack generateInvisibleItemFrame()
    {
        ItemStack item = new ItemStack(Material.ITEM_FRAME, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.DURABILITY, 1 ,true);
        meta.displayName(Component.text("Invisible Item Frame", NamedTextColor.LIGHT_PURPLE));
        meta.getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onHangingPlace(HangingPlaceEvent event)
    {
        if(!isFrameEntity(event.getEntity()) || event.getPlayer() == null)
        {
            return;
        }
        
        // Get the frame item that the player placed
        ItemStack frame;
        Player p = event.getPlayer();
        if(p.getInventory().getItemInMainHand().getType() == Material.ITEM_FRAME ||
                (glowFrame != null && p.getInventory().getItemInMainHand().getType() == glowFrame))
        {
            frame = p.getInventory().getItemInMainHand();
        }
        else if(p.getInventory().getItemInOffHand().getType() == Material.ITEM_FRAME ||
                (glowFrame != null && p.getInventory().getItemInOffHand().getType() == glowFrame))
        {
            frame = p.getInventory().getItemInOffHand();
        }
        else
        {
            return;
        }
        
        // If the frame item has the invisible tag, make the placed item frame invisible
        if(frame.getItemMeta().getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE))
        {
            if(!p.hasPermission("paperinvisiframes.place"))
            {
                event.setCancelled(true);
                return;
            }
            ItemFrame itemFrame = (ItemFrame) event.getEntity();
            if(framesGlow)
            {
                itemFrame.setVisible(true);
                itemFrame.setGlowing(true);
            }
            else
            {
                itemFrame.setVisible(false);
            }
            event.getEntity().getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onHangingBreak(HangingBreakEvent event)
    {
        if(!isFrameEntity(event.getEntity()) || !event.getEntity().getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE))
        {
            return;
        }

        event.setCancelled(true); // We will handle the drop ourselves

        ItemFrame frame = (ItemFrame) event.getEntity();
        Location loc = frame.getLocation();
        World world = frame.getWorld();

        ItemStack drop;
        if (glowFrameEntity != null && frame.getType() == glowFrameEntity) {
            drop = generateInvisibleItemFrame().withType(glowFrame);
            ItemMeta meta = drop.getItemMeta();
            meta.displayName(Component.text("Glow Invisible Item Frame", NamedTextColor.WHITE));
            drop.setItemMeta(meta);
        } else {
            drop = generateInvisibleItemFrame();
        }

        world.dropItemNaturally(loc, drop);
        frame.remove();
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if(!framesGlow)
        {
            return;
        }
        
        if(isFrameEntity(event.getRightClicked()) &&
                event.getRightClicked().getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE))
        {
            ItemFrame frame = (ItemFrame) event.getRightClicked();
            Bukkit.getScheduler().runTaskLater(this, () ->
            {
                if(frame.getItem().getType() != Material.AIR)
                {
                    frame.setGlowing(false);
                    frame.setVisible(false);
                }
            }, 1L);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if(!framesGlow)
        {
            return;
        }
        
        if(isFrameEntity(event.getEntity()) &&
                event.getEntity().getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE))
        {
            ItemFrame frame = (ItemFrame) event.getEntity();
            Bukkit.getScheduler().runTaskLater(this, () ->
            {
                if(frame.getItem().getType() == Material.AIR)
                {
                    if(framesGlow)
                    {
                        frame.setGlowing(true);
                        frame.setVisible(true);
                    }
                }
            }, 1L);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onCraft(PrepareItemCraftEvent event)
    {
        if(isInvisibleRecipe(event.getRecipe()) && !event.getView().getPlayer().hasPermission("paperinvisiframes.craft"))
        {
            event.getInventory().setResult(null);
        }
        else if(isGlowingInvisibleRecipe(event.getRecipe()) && !event.getView().getPlayer().hasPermission("paperinvisiframes.craft"))
        {
            event.getInventory().setResult(null);
        }
    }
}
