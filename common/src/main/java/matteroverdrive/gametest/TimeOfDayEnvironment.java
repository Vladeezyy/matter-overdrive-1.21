package matteroverdrive.gametest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.clock.WorldClock;

/**
 * A fixed time of day for the tests of one environment. 26.x keeps time on world clocks; vanilla's clock_time
 * environment needs the clock holder, which code registering tests (NeoForge) can't get before the world loads, so
 * this one sets the level's default clock (Fabric's test environment JSON uses the same type).
 */
public record TimeOfDayEnvironment(int time) implements TestEnvironmentDefinition<Long> {
    public static final MapCodec<TimeOfDayEnvironment> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TimeOfDayEnvironment::time)).apply(i, TimeOfDayEnvironment::new));

    @Override
    public Long setup(ServerLevel level) {
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        long previous = level.getServer().clockManager().getInstance(clock).totalTicks();
        level.getServer().clockManager().setTotalTicks(clock, time);
        return previous;
    }

    @Override
    public void teardown(ServerLevel level, Long previous) {
        level.getServer().clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(), previous);
    }

    @Override
    public MapCodec<TimeOfDayEnvironment> codec() {
        return CODEC;
    }
}
