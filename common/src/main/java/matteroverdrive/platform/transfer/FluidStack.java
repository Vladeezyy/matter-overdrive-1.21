package matteroverdrive.platform.transfer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * An amount of a fluid in millibuckets (NeoForge's unit; Fabric's adapter converts droplets). Saves as
 * {@code {id, amount}} like NeoForge's {@code FluidStack}, so saved rats and curdlers keep their milk.
 */
public final class FluidStack {
    public static final int BUCKET_VOLUME = 1000;
    public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, 0);

    public static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(i -> i.group(
        BuiltInRegistries.FLUID.holderByNameCodec().fieldOf("id").forGetter(s -> s.fluid.builtInRegistryHolder()),
        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidStack::getAmount)
    ).apply(i, (fluid, amount) -> new FluidStack(fluid.value(), amount)));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = StreamCodec.of(
        (buf, stack) -> {
            buf.writeVarInt(stack.isEmpty() ? 0 : stack.amount);
            if (!stack.isEmpty()) {
                ByteBufCodecs.holderRegistry(Registries.FLUID).encode(buf, stack.fluid.builtInRegistryHolder());
            }
        },
        buf -> {
            int amount = buf.readVarInt();
            if (amount <= 0) {
                return EMPTY;
            }
            Holder<Fluid> fluid = ByteBufCodecs.holderRegistry(Registries.FLUID).decode(buf);
            return new FluidStack(fluid.value(), amount);
        });

    private final Fluid fluid;
    private int amount;

    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public static FluidStack parseOptional(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.isEmpty()) {
            return EMPTY;
        }
        return CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(EMPTY);
    }

    public Tag saveOptional(HolderLookup.Provider provider) {
        return this.isEmpty() ? new CompoundTag() : this.save(provider);
    }

    public Tag save(HolderLookup.Provider provider) {
        return CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    public Fluid getFluid() {
        return this.isEmpty() ? Fluids.EMPTY : this.fluid;
    }

    public Holder<Fluid> getFluidHolder() {
        return this.getFluid().builtInRegistryHolder();
    }

    public int getAmount() {
        return this.isEmpty() ? 0 : this.amount;
    }

    public void setAmount(int amount) {
        if (this != EMPTY) {
            this.amount = amount;
        }
    }

    public void grow(int amount) {
        this.setAmount(this.amount + amount);
    }

    public void shrink(int amount) {
        this.setAmount(this.amount - amount);
    }

    public boolean isEmpty() {
        return this == EMPTY || this.fluid == Fluids.EMPTY || this.amount <= 0;
    }

    public FluidStack copy() {
        return this.isEmpty() ? EMPTY : new FluidStack(this.fluid, this.amount);
    }

    public FluidStack copyWithAmount(int amount) {
        return this.isEmpty() || amount <= 0 ? EMPTY : new FluidStack(this.fluid, amount);
    }

    public boolean is(Fluid fluid) {
        return this.getFluid().isSame(fluid);
    }

    /** Same fluid (NeoForge's isFluidEqual) */
    public boolean isFluidEqual(FluidStack other) {
        return this.getFluid() == other.getFluid();
    }

    public static boolean isSameFluid(FluidStack a, FluidStack b) {
        return a.getFluid() == b.getFluid();
    }

    /** The fluid's name: its block's, or the bucket's for fluids without a block */
    public Component getHoverName() {
        if (this.isEmpty()) {
            return Component.empty();
        }
        var block = this.fluid.defaultFluidState().createLegacyBlock().getBlock();
        if (block != net.minecraft.world.level.block.Blocks.AIR) {
            return block.getName();
        }
        return Component.translatable(this.fluid.getBucket().getDescriptionId());
    }

    @Override
    public String toString() {
        return this.getAmount() + " mB " + BuiltInRegistries.FLUID.getKey(this.getFluid());
    }
}
