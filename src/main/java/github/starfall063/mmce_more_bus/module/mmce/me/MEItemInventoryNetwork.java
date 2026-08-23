package github.starfall063.mmce_more_bus.module.mmce.me;

import github.starfall063.mmce_more_bus.MMCEMoreBus;
import github.starfall063.mmce_more_bus.tile.*;
import io.netty.buffer.ByteBuf;
import appeng.container.AEBaseContainer;
import mekanism.api.gas.GasStack;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Server-authoritative configuration updates for the ME inventory input bus.
 */
public final class MEItemInventoryNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("sfc_me_inventory");

    private static boolean initialized;

    private MEItemInventoryNetwork() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        CHANNEL.registerMessage(SetMinStackSizeMessage.Handler.class, SetMinStackSizeMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(RequestOutputViewportMessage.Handler.class, RequestOutputViewportMessage.class, 1, Side.SERVER);
        CHANNEL.registerMessage(OutputViewportMessage.Handler.class, OutputViewportMessage.class, 2, Side.CLIENT);
        CHANNEL.registerMessage(SetOreDictionaryConfigMessage.Handler.class, SetOreDictionaryConfigMessage.class, 3, Side.SERVER);
        CHANNEL.registerMessage(SetFluidMarkerMessage.Handler.class, SetFluidMarkerMessage.class, 4, Side.SERVER);
        CHANNEL.registerMessage(SetGasMarkerMessage.Handler.class, SetGasMarkerMessage.class, 5, Side.SERVER);
        CHANNEL.registerMessage(SetUniversalFluidMarkerMessage.Handler.class, SetUniversalFluidMarkerMessage.class, 6, Side.SERVER);
        CHANNEL.registerMessage(SetUniversalGasMarkerMessage.Handler.class, SetUniversalGasMarkerMessage.class, 7, Side.SERVER);
    }

    static boolean acceptsMinimumStock(TileEntity tile) {
        return tile instanceof MEItemInventoryInputBus
                || tile instanceof MEFluidInventoryInputBus
                || tile instanceof MEGasInventoryInputBus
                || tile instanceof MEOreDictionaryInputBus
                || tile instanceof MEUniversalInventoryInputBus;
    }

    static boolean isOpenContainerFor(Container openContainer, TileEntity target) {
        if (!(openContainer instanceof AEBaseContainer) || target == null) return false;
        AEBaseContainer container = (AEBaseContainer) openContainer;
        return container.getTarget() == target || container.getTileEntity() == target;
    }

    private static <T extends TileEntity> T editableTile(
            EntityPlayerMP player,
            BlockPos position,
            Class<T> expectedType
    ) {
        if (!player.getServerWorld().isBlockLoaded(position)
                || player.getDistanceSq(position) > 64.0D) return null;

        TileEntity tile = player.getServerWorld().getTileEntity(position);
        if (!expectedType.isInstance(tile) || !isOpenContainerFor(player.openContainer, tile)) return null;
        return expectedType.cast(tile);
    }

    public static final class SetMinStackSizeMessage implements IMessage {
        private long position;
        private int minStackSize;

        public SetMinStackSizeMessage() {
        }

        public SetMinStackSizeMessage(BlockPos position, int minStackSize) {
            this.position = position.toLong();
            this.minStackSize = minStackSize;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            position = buffer.readLong();
            minStackSize = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(position);
            buffer.writeInt(minStackSize);
        }

        public static final class Handler implements IMessageHandler<SetMinStackSizeMessage, IMessage> {
            private static void updateMinStackSize(EntityPlayerMP player, SetMinStackSizeMessage message) {
                BlockPos position = BlockPos.fromLong(message.position);
                TileEntity tile = editableTile(player, position, TileEntity.class);
                if (!acceptsMinimumStock(tile)) return;
                if (tile instanceof MEItemInventoryInputBus bus) {
                    bus.setMinStackSize(message.minStackSize);
                } else if (tile instanceof MEFluidInventoryInputBus bus) {
                    bus.setMinStackSize(message.minStackSize);
                } else if (tile instanceof MEGasInventoryInputBus bus) {
                    bus.setMinStackSize(message.minStackSize);
                } else if (tile instanceof MEOreDictionaryInputBus bus) {
                    bus.setMinStackSize(message.minStackSize);
                } else if (tile instanceof MEUniversalInventoryInputBus bus) {
                    bus.setMinStackSize(message.minStackSize);
                }
            }

            @Override
            public IMessage onMessage(SetMinStackSizeMessage message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> updateMinStackSize(player, message));
                return null;
            }
        }
    }

    public static final class SetOreDictionaryConfigMessage implements IMessage {
        private long position;
        private String oreDictionaryName;
        private int pullMode;
        private int minStackSize;
        private int matchingMode;

        public SetOreDictionaryConfigMessage() {
        }

        public SetOreDictionaryConfigMessage(BlockPos position, String oreDictionaryName, int pullMode,
                                             int minStackSize, int matchingMode) {
            this.position = position.toLong();
            this.oreDictionaryName = oreDictionaryName == null ? "" : oreDictionaryName;
            this.pullMode = pullMode;
            this.minStackSize = minStackSize;
            this.matchingMode = matchingMode;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            position = buffer.readLong();
            oreDictionaryName = ByteBufUtils.readUTF8String(buffer);
            pullMode = buffer.readInt();
            minStackSize = buffer.readInt();
            matchingMode = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(position);
            ByteBufUtils.writeUTF8String(buffer, oreDictionaryName == null ? "" : oreDictionaryName);
            buffer.writeInt(pullMode);
            buffer.writeInt(minStackSize);
            buffer.writeInt(matchingMode);
        }

        public static final class Handler implements IMessageHandler<SetOreDictionaryConfigMessage, IMessage> {
            private static void updateConfiguration(EntityPlayerMP player, SetOreDictionaryConfigMessage message) {
                BlockPos position = BlockPos.fromLong(message.position);
                MEOreDictionaryInputBus bus = editableTile(player, position, MEOreDictionaryInputBus.class);
                if (bus == null) return;
                bus.setOreDictionaryName(message.oreDictionaryName);
                bus.setPullMode(message.pullMode);
                bus.setMinStackSize(message.minStackSize);
                bus.setMatchingMode(message.matchingMode);
            }

            @Override
            public IMessage onMessage(SetOreDictionaryConfigMessage message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> updateConfiguration(player, message));
                return null;
            }
        }
    }

    public static final class SetFluidMarkerMessage implements IMessage {
        private long position;
        private int slot;
        private NBTTagCompound marker;

        public SetFluidMarkerMessage() {
        }

        public SetFluidMarkerMessage(BlockPos position, int slot, FluidStack marker) {
            this.position = position.toLong();
            this.slot = slot;
            this.marker = marker == null ? null : marker.writeToNBT(new NBTTagCompound());
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            position = buffer.readLong();
            slot = buffer.readInt();
            marker = buffer.readBoolean() ? ByteBufUtils.readTag(buffer) : null;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(position);
            buffer.writeInt(slot);
            buffer.writeBoolean(marker != null);
            if (marker != null) ByteBufUtils.writeTag(buffer, marker);
        }

        public static final class Handler implements IMessageHandler<SetFluidMarkerMessage, IMessage> {
            private static void update(EntityPlayerMP player, SetFluidMarkerMessage message) {
                BlockPos position = BlockPos.fromLong(message.position);
                if (message.slot < 0 || message.slot >= MEFluidInventoryInputBus.SLOT_COUNT)
                    return;
                MEFluidInventoryInputBus bus = editableTile(player, position, MEFluidInventoryInputBus.class);
                if (bus == null) return;
                bus.clearMarker(message.slot);
                if (message.marker != null) {
                    FluidStack fluid = FluidStack.loadFluidStackFromNBT(message.marker);
                    if (fluid != null) bus.setMarker(message.slot, fluid);
                }
            }

            @Override
            public IMessage onMessage(SetFluidMarkerMessage message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> update(player, message));
                return null;
            }
        }
    }

    public static final class SetGasMarkerMessage implements IMessage {
        private long position;
        private int slot;
        private NBTTagCompound marker;

        public SetGasMarkerMessage() {
        }

        public SetGasMarkerMessage(BlockPos position, int slot, GasStack marker) {
            this.position = position.toLong();
            this.slot = slot;
            this.marker = marker == null ? null : marker.write(new NBTTagCompound());
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            position = buffer.readLong();
            slot = buffer.readInt();
            marker = buffer.readBoolean() ? ByteBufUtils.readTag(buffer) : null;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(position);
            buffer.writeInt(slot);
            buffer.writeBoolean(marker != null);
            if (marker != null) ByteBufUtils.writeTag(buffer, marker);
        }

        public static final class Handler implements IMessageHandler<SetGasMarkerMessage, IMessage> {
            private static void update(EntityPlayerMP player, SetGasMarkerMessage message) {
                BlockPos position = BlockPos.fromLong(message.position);
                if (message.slot < 0 || message.slot >= MEGasInventoryInputBus.SLOT_COUNT)
                    return;
                MEGasInventoryInputBus bus = editableTile(player, position, MEGasInventoryInputBus.class);
                if (bus == null) return;
                bus.clearMarker(message.slot);
                if (message.marker != null) {
                    GasStack gas = GasStack.readFromNBT(message.marker);
                    if (gas != null) bus.setMarker(message.slot, gas);
                }
            }

            @Override
            public IMessage onMessage(SetGasMarkerMessage message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> update(player, message));
                return null;
            }
        }
    }

    public static final class SetUniversalFluidMarkerMessage implements IMessage {
        private long position;
        private int slot;
        private NBTTagCompound marker;

        public SetUniversalFluidMarkerMessage() {
        }

        public SetUniversalFluidMarkerMessage(BlockPos position, int slot, FluidStack marker) {
            this.position = position.toLong();
            this.slot = slot;
            this.marker = marker == null ? null : marker.writeToNBT(new NBTTagCompound());
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            position = buffer.readLong();
            slot = buffer.readInt();
            marker = buffer.readBoolean() ? ByteBufUtils.readTag(buffer) : null;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(position);
            buffer.writeInt(slot);
            buffer.writeBoolean(marker != null);
            if (marker != null) ByteBufUtils.writeTag(buffer, marker);
        }

        public static final class Handler implements IMessageHandler<SetUniversalFluidMarkerMessage, IMessage> {
            private static void update(EntityPlayerMP player, SetUniversalFluidMarkerMessage message) {
                BlockPos position = BlockPos.fromLong(message.position);
                if (message.slot < 0 || message.slot >= MEUniversalInventoryInputBus.SLOT_COUNT)
                    return;
                MEUniversalInventoryInputBus bus = editableTile(player, position, MEUniversalInventoryInputBus.class);
                if (bus == null) return;
                bus.clearMarker(message.slot);
                if (message.marker != null) {
                    FluidStack fluid = FluidStack.loadFluidStackFromNBT(message.marker);
                    if (fluid != null) bus.setFluidMarker(message.slot, fluid);
                }
            }

            @Override
            public IMessage onMessage(SetUniversalFluidMarkerMessage message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> update(player, message));
                return null;
            }
        }
    }

    public static final class SetUniversalGasMarkerMessage implements IMessage {
        private long position;
        private int slot;
        private NBTTagCompound marker;

        public SetUniversalGasMarkerMessage() {
        }

        public SetUniversalGasMarkerMessage(BlockPos position, int slot, GasStack marker) {
            this.position = position.toLong();
            this.slot = slot;
            this.marker = marker == null ? null : marker.write(new NBTTagCompound());
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            position = buffer.readLong();
            slot = buffer.readInt();
            marker = buffer.readBoolean() ? ByteBufUtils.readTag(buffer) : null;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(position);
            buffer.writeInt(slot);
            buffer.writeBoolean(marker != null);
            if (marker != null) ByteBufUtils.writeTag(buffer, marker);
        }

        public static final class Handler implements IMessageHandler<SetUniversalGasMarkerMessage, IMessage> {
            private static void update(EntityPlayerMP player, SetUniversalGasMarkerMessage message) {
                BlockPos position = BlockPos.fromLong(message.position);
                if (message.slot < 0 || message.slot >= MEUniversalInventoryInputBus.SLOT_COUNT)
                    return;
                MEUniversalInventoryInputBus bus = editableTile(player, position, MEUniversalInventoryInputBus.class);
                if (bus == null) return;
                bus.clearMarker(message.slot);
                if (message.marker != null) {
                    GasStack gas = GasStack.readFromNBT(message.marker);
                    if (gas != null) bus.setGasMarker(message.slot, gas);
                }
            }

            @Override
            public IMessage onMessage(SetUniversalGasMarkerMessage message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> update(player, message));
                return null;
            }
        }
    }

    public static final class RequestOutputViewportMessage implements IMessage {
        private long position;
        private int rowOffset;
        private int knownRevision;
        private int knownRowOffset;

        public RequestOutputViewportMessage() {
        }

        public RequestOutputViewportMessage(BlockPos position, int rowOffset) {
            this(position, rowOffset, -1, -1);
        }

        public RequestOutputViewportMessage(BlockPos position, int rowOffset, int knownRevision, int knownRowOffset) {
            this.position = position.toLong();
            this.rowOffset = rowOffset;
            this.knownRevision = knownRevision;
            this.knownRowOffset = knownRowOffset;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            position = buffer.readLong();
            rowOffset = buffer.readInt();
            knownRevision = buffer.readInt();
            knownRowOffset = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(position);
            buffer.writeInt(rowOffset);
            buffer.writeInt(knownRevision);
            buffer.writeInt(knownRowOffset);
        }

        public static final class Handler implements IMessageHandler<RequestOutputViewportMessage, IMessage> {
            @Override
            public IMessage onMessage(RequestOutputViewportMessage message, MessageContext context) {
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    BlockPos position = BlockPos.fromLong(message.position);
                    MEUniversalOutputBus bus = editableTile(player, position, MEUniversalOutputBus.class);
                    if (bus == null) return;
                    CHANNEL.sendTo(new OutputViewportMessage(
                            position,
                            bus.createViewportState(message.rowOffset, message.knownRevision, message.knownRowOffset)
                    ), player);
                });
                return null;
            }
        }
    }

    public static final class OutputViewportMessage implements IMessage {
        private long position;
        private NBTTagCompound viewport;

        public OutputViewportMessage() {
        }

        private OutputViewportMessage(BlockPos position, NBTTagCompound viewport) {
            this.position = position.toLong();
            this.viewport = viewport;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            position = buffer.readLong();
            viewport = ByteBufUtils.readTag(buffer);
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(position);
            ByteBufUtils.writeTag(buffer, viewport);
        }

        public static final class Handler implements IMessageHandler<OutputViewportMessage, IMessage> {
            @Override
            public IMessage onMessage(OutputViewportMessage message, MessageContext context) {
                FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() -> {
                    if (message.viewport != null) {
                        MMCEMoreBus.proxy.applyMEUniversalOutputViewport(
                                BlockPos.fromLong(message.position),
                                message.viewport
                        );
                    }
                });
                return null;
            }
        }
    }
}
