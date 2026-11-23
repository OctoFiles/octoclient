package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Port of Meteor's Reach: adds to the vanilla block and entity interaction range. */
public class Reach extends Module implements Module.Tickable {
    private static final Identifier ENTITY_RANGE =
            Identifier.fromNamespaceAndPath("octoclient", "reach_entity");
    private static final Identifier BLOCK_RANGE =
            Identifier.fromNamespaceAndPath("octoclient", "reach_block");

    private final NumberSetting extraBlockReach = new NumberSetting("extra-block-reach",
            "The distance to add to your block reach.", 0.0, 0.0, 1.0, 0.05);
    private final NumberSetting extraEntityReach = new NumberSetting("extra-entity-reach",
            "The distance to add to your entity reach.", 0.0, 0.0, 1.0, 0.05);

    public Reach() {
        super("Reach", "Gives you super long arms.", 'R');
        addSetting(extraBlockReach);
        addSetting(extraEntityReach);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player != null) {
            apply(client.player);
        }
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            apply(client.player);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        AttributeInstance entities = client.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        AttributeInstance blocks = client.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (entities != null) {
            entities.removeModifier(ENTITY_RANGE);
        }
        if (blocks != null) {
            blocks.removeModifier(BLOCK_RANGE);
        }
    }

    private void apply(net.minecraft.world.entity.player.Player player) {
        AttributeInstance entities = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entities != null) {
            entities.addOrUpdateTransientModifier(new AttributeModifier(ENTITY_RANGE,
                    extraEntityReach.getValue(), AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance blocks = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blocks != null) {
            blocks.addOrUpdateTransientModifier(new AttributeModifier(BLOCK_RANGE,
                    extraBlockReach.getValue(), AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
