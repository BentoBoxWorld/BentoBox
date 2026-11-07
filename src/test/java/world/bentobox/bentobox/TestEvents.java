package world.bentobox.bentobox;

import static org.mockito.Mockito.mock;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

import com.google.common.base.Function;

/**
 * Factories for Bukkit events whose simple constructors Paper has deprecated for removal.
 * Tests only need the damagee, the cause and the base damage, so this hides the damage
 * source and modifier maps the surviving constructors require.
 */
public final class TestEvents {

    private TestEvents() {}

    /**
     * @return an EntityDamageEvent with the given base damage and a mocked damage source
     */
    public static EntityDamageEvent damage(Entity damagee, DamageCause cause, double damage) {
        return new EntityDamageEvent(damagee, cause, mock(DamageSource.class), modifiers(damage), functions());
    }

    /**
     * @return an EntityDamageByEntityEvent with the given base damage and a mocked damage source
     */
    public static EntityDamageByEntityEvent damageByEntity(Entity damager, Entity damagee, DamageCause cause,
            double damage) {
        return new EntityDamageByEntityEvent(damager, damagee, cause, mock(DamageSource.class), modifiers(damage),
                functions(), false);
    }

    private static Map<DamageModifier, Double> modifiers(double damage) {
        Map<DamageModifier, Double> modifiers = new EnumMap<>(DamageModifier.class);
        modifiers.put(DamageModifier.BASE, damage);
        return modifiers;
    }

    private static Map<DamageModifier, Function<? super Double, Double>> functions() {
        Map<DamageModifier, Function<? super Double, Double>> functions = new EnumMap<>(DamageModifier.class);
        functions.put(DamageModifier.BASE, d -> -0.0);
        return functions;
    }
}
