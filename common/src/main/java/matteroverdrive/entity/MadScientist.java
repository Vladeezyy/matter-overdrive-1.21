package matteroverdrive.entity;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.dialog.DialogMessage;
import matteroverdrive.dialog.DialogMessages;
import matteroverdrive.dialog.DialogNpc;
import matteroverdrive.dialog.DialogPayloads;
import matteroverdrive.dialog.DialogRegistry;
import matteroverdrive.dialog.TalkToPlayerGoal;
import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.init.MOSounds;
import matteroverdrive.quest.PlayerQuests;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.quest.QuestStack;
import matteroverdrive.quest.Quests;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;

/**
 * 1.7.10 EntityVillagerMadScientist (villager profession 666): talking to him opens a conversation instead of trades.
 * Humans may ask to become androids ("Puny Humans": bring a full set of rogue android parts), androids learn what
 * happened to them and can trade; half of them are junkies who give the "Cocktail of Ascension" quest and turn into a
 * mutant scientist when it is done. Trades: 1.7.10 TradeHandlerMadScientist.
 */
public class MadScientist extends AbstractVillager implements DialogNpc {
    private static final EntityDataAccessor<Boolean> JUNKIE = SynchedEntityData.defineId(MadScientist.class, EntityDataSerializers.BOOLEAN);
    /** 1.7.10 MerchantRecipe default maxTradeUses. */
    private static final int MAX_USES = 7;

    private @Nullable Player dialogPlayer;
    private @Nullable DialogMessage startMessage;

    public MadScientist(EntityType<? extends MadScientist> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(JUNKIE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TalkToPlayerGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8, 0.6, 0.6));
        goalSelector.addGoal(1, new PanicGoal(this, 0.6));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3, 1));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8));
    }

    public boolean isJunkie() {
        return entityData.get(JUNKIE);
    }

    public void setJunkie(boolean junkie) {
        entityData.set(JUNKIE, junkie);
        if (junkie) setCustomName(Component.translatable("entity." + MatterOverdrive.MODID + ".mad_scientist.junkie"));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData data) {
        setJunkie(random.nextBoolean());
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ValueOutput output = ValueOutput.of(tag, registryAccess());
        output.putBoolean("junkie", isJunkie());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ValueInput input = ValueInput.of(tag, registryAccess());
        entityData.set(JUNKIE, input.getBooleanOr("junkie", false));
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        MadScientist child = MOEntities.MAD_SCIENTIST.get().create(level, EntitySpawnReason.BREEDING);
        if (child != null) child.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.BREEDING, null);
        return child;
    }

    // --- trades (1.7.10 TradeHandlerMadScientist) ----------------------------------------------------

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        RandomSource r = random;
        sell(offers, r, new ItemStack(MOItems.ROGUE_ANDROID_HEAD.get()), 0.5f, 16 * 2, 8 * 2);
        sell(offers, r, new ItemStack(MOItems.ROGUE_ANDROID_ARMS.get()), 0.4f, 18 * 2, 10 * 2);
        sell(offers, r, new ItemStack(MOItems.ROGUE_ANDROID_LEGS.get()), 0.3f, 20 * 2, 12 * 2);
        sell(offers, r, new ItemStack(MOItems.ROGUE_ANDROID_CHEST.get()), 0.2f, 32 * 2, 8 * 2);
        sell(offers, r, new ItemStack(MOItems.ANDROID_PILL_BLUE.get()), 1, 16, 16);
        sell(offers, r, new ItemStack(MOItems.ANDROID_PILL_YELLOW.get()), 1, 16, 16);
        sell(offers, r, new ItemStack(MOItems.H_COMPENSATOR.get()), 0.5f, 32, 18);
        if (r.nextFloat() < 1) {
            offers.add(new MerchantOffer(new ItemCost(MOItems.DILITHIUM_CRYSTAL.get()), new ItemStack(Items.EMERALD, 8 + r.nextInt(8)),
                    MAX_USES, 0, 0));
        }
        sell(offers, r, new ItemStack(MOItems.BARREL_DAMAGE.get()), 1, 8, 4);
        sell(offers, r, new ItemStack(MOItems.BARREL_FIRE.get()), 1, 8, 4);
        sell(offers, r, new ItemStack(MOItems.BARREL_EXPLOSION.get()), 1, 16, 16);
        sell(offers, r, new ItemStack(MOItems.EARL_GRAY_TEA.get()), 1, 3, 2);
    }

    /** 1.7.10 addSellItemStack: emeralds (price + random variation) for the stack, with the given chance. */
    private static void sell(MerchantOffers offers, RandomSource r, ItemStack stack, float chance, int price, int variation) {
        if (r.nextFloat() < chance) {
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, price + r.nextInt(variation)), stack, MAX_USES, 0, 0));
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), 3 + random.nextInt(4)));
    }

    /** A villager: never despawns (his house would be empty). */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    // --- conversation (1.7.10 IDialogNpc) ---------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || isBaby() || player.getItemInHand(hand).is(Items.VILLAGER_SPAWN_EGG)) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer server) DialogPayloads.startConversation(server, this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable DialogMessage getStartDialogMessage(Player player) {
        return startMessage;
    }

    @Override
    public void setDialogPlayer(@Nullable Player player) {
        dialogPlayer = player;
        startMessage = player != null ? Dialogs.start(player, this) : null;
    }

    @Override
    public @Nullable Player getDialogPlayer() {
        return dialogPlayer;
    }

    /** 1.7.10 canTalkTo: not while the player is turning into an android. */
    @Override
    public boolean canTalkTo(Player player) {
        return !Android.get(player).isTurning();
    }

    @Override
    public Mob getEntity() {
        return this;
    }

    @Override
    public void onPlayerInteract(Player player, @Nullable DialogMessage message) {
        if (!(level() instanceof ServerLevel server)) return;
        if (message == Dialogs.cocktailComplete) {
            // the cocktail turns him into a mutant scientist
            addEffect(new MobEffectInstance(MobEffects.WITHER, 1000, 1));
            server.playSound(null, getX(), getY(), getZ(), MOSounds.FAILED_ANIMAL_DIE.get(), SoundSource.NEUTRAL, 1, 1);
            discard();
            var mutant = MOEntities.MUTANT_SCIENTIST.get().create(server, EntitySpawnReason.CONVERSION);
            if (mutant != null) {
                mutant.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
                mutant.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.CONVERSION, null);
                server.addFreshEntity(mutant);
                mutant.spawnAnim();
            }
        } else if (message == Dialogs.convertMe) {
            QuestStack stack = PlayerQuests.get(player).findActive(Quests.PUNY_HUMANS);
            if (stack != null && player instanceof ServerPlayer serverPlayer) {
                stack.markCompleted(player, false);
                Android.startTransformation(serverPlayer);
                PlayerQuests.sync(player);
            }
        }
    }

    /** 1.7.10 giveQuest: the quest goes to the player with this scientist as its giver. */
    @Override
    public void giveQuest(DialogMessage message, QuestStack stack, Player player) {
        if (player instanceof ServerPlayer server && stack.getQuest() != null && stack.getQuest().canBeAccepted(stack, player)) {
            QuestStack copy = stack.copy();
            copy.setGiver(this);
            QuestEvents.addQuest(server, copy);
        }
    }

    /** 1.7.10 MatterOverdriveDialogs + EntityVillagerMadScientist.registerDialogMessages (registration order = ids). */
    public static final class Dialogs {
        public static final DialogMessage back = DialogRegistry.register(new DialogMessages.Back()).question("dialog.generic.back.questions").random()
                .holoIcon("mini_quit");
        public static final DialogMessage quit = DialogRegistry.register(new DialogMessages.Quit()).question("dialog.generic.quit.questions").random()
                .holoIcon("mini_quit");
        public static final DialogMessage backHome = DialogRegistry.register(new DialogMessages.BackToMain())
                .question("dialog.generic.back_home.questions").random().holoIcon("mini_quit");
        public static final DialogMessage trade = DialogRegistry.register(new DialogMessages.Trade()).question("dialog.generic.trade.questions").random()
                .holoIcon("trade");
        // human
        public static final DialogMessage convertMe = DialogRegistry.register(new DialogMessages.QuestOnObjectivesCompleted(Quests.PUNY_HUMANS, 0))
                .question("dialog.mad_scientist.convert.question");
        public static final DialogMessage canYouConvert = DialogRegistry.register(new DialogMessages.QuestGive(Quests.PUNY_HUMANS))
                .message("dialog.mad_scientist.requirements.line").question("dialog.mad_scientist.requirements.question")
                .addOption(convertMe).addOption(backHome);
        // android
        public static final DialogMessage undo = DialogRegistry.register(new DialogMessage()).message("dialog.mad_scientist.undo.line")
                .question("dialog.mad_scientist.undo.question").addOption(trade).addOption(backHome);
        public static final DialogMessage whatDidYouDo = DialogRegistry.register(new DialogMessages.AndroidOnly())
                .message("dialog.mad_scientist.whatDidYouDo.line").question("dialog.mad_scientist.whatDidYouDo.question")
                .addOption(undo).addOption(backHome);
        // junkie
        public static final DialogMessage acceptCocktail = DialogRegistry.register(new DialogMessages.QuestGive(Quests.COCKTAIL_OF_ASCENSION)
                .returnToMain()).question("dialog.mad_scientist.junkie.cocktail_quest.question.accept");
        public static final DialogMessage declineCocktail = DialogRegistry.register(new DialogMessages.BackToMain())
                .question("dialog.mad_scientist.junkie.cocktail_quest.question.decline");
        public static final DialogMessage[] cocktailQuest = DialogRegistry.multipleLines(
                new DialogMessages.QuestStart().setQuest(Quests.COCKTAIL_OF_ASCENSION), "dialog.mad_scientist.junkie.cocktail_quest", 8,
                ". . . . . .");
        public static final DialogMessage cocktailOfAscension = cocktailQuest[0];
        public static final DialogMessage cocktailComplete = DialogRegistry.register(
                        new DialogMessages.QuestOnObjectivesCompleted(Quests.COCKTAIL_OF_ASCENSION, 0, 1, 2))
                .message("dialog.mad_scientist.junkie.cocktail_quest.line").question("dialog.mad_scientist.junkie.cocktail_quest.complete.question");
        public static final DialogMessage areYouOk = DialogRegistry.register(new DialogMessages.Quit())
                .question("dialog.mad_scientist.junkie.cocktail_quest.are_you_ok.question");

        static {
            DialogMessage last = cocktailQuest[cocktailQuest.length - 1];
            last.addOption(acceptCocktail).addOption(declineCocktail);
            cocktailComplete.addOption(areYouOk);
        }

        /** Loads the messages (ids) on both sides at setup. */
        public static void init() {}

        /** 1.7.10 assembleStartingMessage. */
        static DialogMessage start(Player player, MadScientist npc) {
            if (npc.isJunkie()) {
                DialogMessage main = new DialogMessage().message("dialog.mad_scientist.junkie.main.line").random();
                for (DialogMessage option : new DialogMessage[] {canYouConvert, trade, cocktailOfAscension, cocktailComplete, quit}) {
                    if (option.isVisible(npc, player)) main.addOption(option);
                }
                return main;
            }
            if (Android.isAndroid(player)) {
                return new DialogMessage().message("dialog.mad_scientist.main.line.android").random().addOption(whatDidYouDo).addOption(trade)
                        .addOption(quit);
            }
            return new DialogMessage().message("dialog.mad_scientist.main.line.human").random().addOption(canYouConvert).addOption(quit);
        }

        private Dialogs() {}
    }
}
