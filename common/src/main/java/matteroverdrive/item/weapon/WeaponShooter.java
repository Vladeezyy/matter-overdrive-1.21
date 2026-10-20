package matteroverdrive.item.weapon;

/**
 * A mob that fires energy weapons (1.7.10 IRangedEnergyWeaponAttackMob): its shots are scaled by difficulty.
 */
public interface WeaponShooter {
    /** Multiplies the weapon's damage. */
    float weaponDamageScale();

    /** Added to the weapon's inaccuracy. */
    float weaponAccuracyAdd();
}
