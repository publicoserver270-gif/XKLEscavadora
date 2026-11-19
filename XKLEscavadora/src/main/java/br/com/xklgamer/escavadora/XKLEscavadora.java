package br.com.xklgamer.escavadora;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class XKLEscavadora extends JavaPlugin implements Listener {
    private NamespacedKey toolKey;
    private final Set<UUID> processing = new HashSet<>();

    @Override
    public void onEnable() {
        toolKey = new NamespacedKey(this, "escavadora_3x3");
        getServer().getPluginManager().registerEvents(this, this);
        registerRecipe();
        getLogger().info("Picareta Escavadora 3x3 ativada.");
    }

    private ItemStack createPickaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Picareta Escavadora 3x3"));
        meta.lore(List.of(
                net.kyori.adventure.text.Component.text("§7Minera uma área de §d3x3§7 blocos."),
                net.kyori.adventure.text.Component.text("§7Respeita as proteções do servidor."),
                net.kyori.adventure.text.Component.text("§8Ferramenta exclusiva do XKL Gamer")
        ));
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void registerRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(this, "picareta_escavadora_3x3");
        getServer().removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createPickaxe());
        recipe.shape("DND", "ABA", " B ");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('B', Material.BREEZE_ROD);
        getServer().addRecipe(recipe);
    }

    private boolean isOurPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE || !item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (processing.contains(player.getUniqueId())) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isOurPickaxe(tool)) return;

        Block center = event.getBlock();
        if (!isMineable(center)) return;

        processing.add(player.getUniqueId());
        try {
            for (Block extra : get3x3Neighbors(center, player)) {
                if (tool.getType() == Material.AIR || !isMineable(extra)) continue;

                // Dispara o evento Bukkit para que plugins de proteção, incluindo plugins de claims,
                // possam cancelar a quebra deste bloco adicional.
                BlockBreakEvent extraEvent = new BlockBreakEvent(extra, player);
                getServer().getPluginManager().callEvent(extraEvent);
                if (extraEvent.isCancelled() || extra.getType() == Material.AIR) continue;

                if (extraEvent.isDropItems()) {
                    extra.breakNaturally(tool);
                    if (extraEvent.getExpToDrop() > 0) {
                        extra.getWorld().spawn(extra.getLocation(), ExperienceOrb.class,
                                orb -> orb.setExperience(extraEvent.getExpToDrop()));
                    }
                } else {
                    extra.setType(Material.AIR);
                }
                damageForExtraBlock(player, tool);
            }
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    private boolean isMineable(Block block) {
        Material type = block.getType();
        if (type.isAir() || !Tag.MINEABLE_PICKAXE.isTagged(type)) return false;
        // Não tocar em baús, fornalhas, spawners e outros blocos com inventário/estado especial.
        return !(block.getState() instanceof TileState);
    }

    private List<Block> get3x3Neighbors(Block center, Player player) {
        List<Block> blocks = new ArrayList<>(8);
        float pitch = player.getLocation().getPitch();
        if (Math.abs(pitch) >= 60.0f) {
            // Olhando bastante para cima/baixo: plano horizontal.
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    blocks.add(center.getRelative(x, 0, z));
                }
            }
        } else {
            // Plano vertical perpendicular à direção horizontal do olhar.
            double yaw = Math.toRadians(player.getLocation().getYaw());
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            int sideX = Math.abs(forwardZ) >= Math.abs(forwardX) ? 1 : 0;
            int sideZ = sideX == 1 ? 0 : 1;
            for (int vertical = -1; vertical <= 1; vertical++) {
                for (int side = -1; side <= 1; side++) {
                    if (vertical == 0 && side == 0) continue;
                    blocks.add(center.getRelative(sideX * side, vertical, sideZ * side));
                }
            }
        }
        return blocks;
    }

    private void damageForExtraBlock(Player player, ItemStack tool) {
        if (tool.getType() == Material.AIR || !tool.hasItemMeta()) return;
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int unbreaking = meta.getEnchantLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && ThreadLocalRandom.current().nextInt(unbreaking + 1) != 0) return;

        int nextDamage = damageable.getDamage() + 1;
        if (nextDamage >= tool.getType().getMaxDurability()) {
            player.getInventory().setItem(EquipmentSlot.HAND, null);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return;
        }
        damageable.setDamage(nextDamage);
        tool.setItemMeta(meta);
    }
}
