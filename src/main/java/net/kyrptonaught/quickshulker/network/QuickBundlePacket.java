package net.kyrptonaught.quickshulker.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.quickshulker.BundleHelper;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.List;

public record QuickBundlePacket(ItemStackWithPos itemStackWithPos) implements CustomPayload {

    public static final Id<QuickBundlePacket> ID = new Id<>(Identifier.of(QuickShulkerMod.MOD_ID, "quick_bundle_packet"));
    public static final PacketCodec<RegistryByteBuf, QuickBundlePacket> CODEC = new PacketCodec<>() {
        private static final PacketCodec<RegistryByteBuf, RegistryEntry<Item>> ITEM_PACKET_CODEC = PacketCodecs.registryEntry(RegistryKeys.ITEM);

        @Override
        public QuickBundlePacket decode(RegistryByteBuf registryByteBuf) {
            int slotId = registryByteBuf.readInt();
            int i = registryByteBuf.readVarInt();
            ItemStack itemStack1 = ItemStack.EMPTY;
            RegistryEntry<Item> registryEntry = ITEM_PACKET_CODEC.decode(registryByteBuf);
            ComponentChanges componentChanges = ComponentChanges.PACKET_CODEC.decode(registryByteBuf);
            itemStack1 = new ItemStack(registryEntry, i, componentChanges);
            return new QuickBundlePacket(new ItemStackWithPos(slotId, itemStack1));
        }

        @Override
        public void encode(RegistryByteBuf registryByteBuf, QuickBundlePacket quickBundlePacket) {
            registryByteBuf.writeInt(quickBundlePacket.itemStackWithPos.slotId);
            ItemStack itemStack = quickBundlePacket.itemStackWithPos.itemStack;
            if (itemStack.isEmpty()) {
                registryByteBuf.writeVarInt(0);
                return;
            }
            registryByteBuf.writeVarInt(itemStack.getCount());
            ITEM_PACKET_CODEC.encode(registryByteBuf, itemStack.getRegistryEntry());
            ComponentChanges.PACKET_CODEC.encode(registryByteBuf, itemStack.getComponentChanges());
        }
    };

    public static void registerReceivePacket() {
        PayloadTypeRegistry.playS2C().register(QuickBundlePacket.ID, QuickBundlePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(QuickBundlePacket.ID, QuickBundlePacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(QuickBundlePacket.ID, (payload, context) -> {
            if (context.player().isCreative()) {
                context.player().server.execute(() -> BundleHelper.bundleItemIntoStack(context.player(), context.player().getInventory().getStack(payload.itemStackWithPos.slotId), payload.itemStackWithPos.itemStack, null));
            }
        });
        UnBundlePacket.registerReceivePacket();
        BundleIntoHeld.registerReceivePacket();
    }

    @Environment(EnvType.CLIENT)
    public static void sendPacket(int slotID, ItemStack stackToBundle) {
        ClientPlayNetworking.send(new QuickBundlePacket(new ItemStackWithPos(slotID, stackToBundle)));
    }

    public static void sendCreativeSlotUpdate(ItemStack output, Slot slot) {
        MinecraftClient.getInstance().interactionManager.clickCreativeStack(output, slot.id);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return QuickBundlePacket.ID;
    }

    public static class ItemStackWithPos {
        private final ItemStack itemStack;
        private final int slotId;

        public ItemStackWithPos(int slotId, ItemStack itemStack) {
            this.itemStack = itemStack;
            this.slotId = slotId;
        }
    }


    public record BundleIntoHeld(List<ItemStack> stackList) implements CustomPayload {

        public static final Id<BundleIntoHeld> ID = new Id<>(Identifier.of(QuickShulkerMod.MOD_ID, "quick_bundleheld_packet"));

        public static final PacketCodec<RegistryByteBuf, BundleIntoHeld> CODEC = PacketCodec.tuple(ItemStack.OPTIONAL_LIST_PACKET_CODEC, BundleIntoHeld::stackList, BundleIntoHeld::new);

        public static void registerReceivePacket() {
            PayloadTypeRegistry.playS2C().register(BundleIntoHeld.ID, BundleIntoHeld.CODEC);
            PayloadTypeRegistry.playC2S().register(BundleIntoHeld.ID, BundleIntoHeld.CODEC);
            ServerPlayNetworking.registerGlobalReceiver(BundleIntoHeld.ID, (payload, context) -> {
                if (context.player().isCreative()) {
                    context.player().server.execute(() -> BundleHelper.bundleItemIntoStack(context.player(), payload.stackList.get(0), payload.stackList.get(1), null));
                }
            });
        }

        @Environment(EnvType.CLIENT)
        public static void sendPacket(ItemStack stackToBundle, ItemStack bundleStack) {
            ClientPlayNetworking.send(new BundleIntoHeld(List.of(stackToBundle, bundleStack)));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return BundleIntoHeld.ID;
        }
    }

    public record UnBundlePacket(ItemStackWithPos itemStackWithPos) implements CustomPayload {
        public static final Id<UnBundlePacket> ID = new Id<>(Identifier.of(QuickShulkerMod.MOD_ID, "quick_unbundle_packet"));
        public static final PacketCodec<RegistryByteBuf, UnBundlePacket> CODEC = new PacketCodec<>() {
            private static final PacketCodec<RegistryByteBuf, RegistryEntry<Item>> ITEM_PACKET_CODEC = PacketCodecs.registryEntry(RegistryKeys.ITEM);

            @Override
            public UnBundlePacket decode(RegistryByteBuf registryByteBuf) {
                int slotId = registryByteBuf.readInt();
                int i = registryByteBuf.readVarInt();
                ItemStack itemStack1 = ItemStack.EMPTY;
                RegistryEntry<Item> registryEntry = ITEM_PACKET_CODEC.decode(registryByteBuf);
                ComponentChanges componentChanges = ComponentChanges.PACKET_CODEC.decode(registryByteBuf);
                itemStack1 = new ItemStack(registryEntry, i, componentChanges);
                return new UnBundlePacket(new ItemStackWithPos(slotId, itemStack1));
            }

            @Override
            public void encode(RegistryByteBuf registryByteBuf, UnBundlePacket unBundlePacket) {
                registryByteBuf.writeInt(unBundlePacket.itemStackWithPos.slotId);
                ItemStack itemStack = unBundlePacket.itemStackWithPos.itemStack;
                if (itemStack.isEmpty()) {
                    registryByteBuf.writeVarInt(0);
                    return;
                }
                registryByteBuf.writeVarInt(itemStack.getCount());
                ITEM_PACKET_CODEC.encode(registryByteBuf, itemStack.getRegistryEntry());
                ComponentChanges.PACKET_CODEC.encode(registryByteBuf, itemStack.getComponentChanges());
            }
        };

        public static void registerReceivePacket() {
            PayloadTypeRegistry.playS2C().register(UnBundlePacket.ID, UnBundlePacket.CODEC);
            PayloadTypeRegistry.playC2S().register(UnBundlePacket.ID, UnBundlePacket.CODEC);
            ServerPlayNetworking.registerGlobalReceiver(UnBundlePacket.ID, (payload, context) -> {
                if (context.player().isCreative()) {
                    int playerInvSlotID = payload.itemStackWithPos.slotId;
                    ItemStack unBundleStack = payload.itemStackWithPos.itemStack;
                    context.player().server.execute(() -> {
                        ItemStack output = BundleHelper.unbundleItem(context.player(), unBundleStack);
                        if (output != null)
                            context.player().getInventory().setStack(playerInvSlotID, output);
                    });
                }
            });
        }

        @Environment(EnvType.CLIENT)
        public static void sendPacket(int slotID, ItemStack unBundleStack) {
            ClientPlayNetworking.send(new UnBundlePacket(new ItemStackWithPos(slotID, unBundleStack)));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return UnBundlePacket.ID;
        }
    }
}
