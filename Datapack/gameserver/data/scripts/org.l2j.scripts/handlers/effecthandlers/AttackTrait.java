package handlers.effecthandlers;

import org.l2j.gameserver.model.StatsSet;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.effects.AbstractEffect;
import org.l2j.gameserver.model.items.instance.Item;
import org.l2j.gameserver.engine.skill.api.Skill;
import org.l2j.gameserver.model.stats.TraitType;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Attack Trait effect implementation.
 * @author NosBit
 */
public final class AttackTrait extends AbstractEffect {

	public final Map<TraitType, Float> attackTraits = new HashMap<>();
	
	public AttackTrait(StatsSet params) {

		if (params.isEmpty()) {
			LOGGER.warn("this effect must have parameters!");
			return;
		}
		
		for (Entry<String, Object> param : params.getSet().entrySet()) {
			attackTraits.put( TraitType.valueOf(param.getKey()), Float.parseFloat((String) param.getValue()) / 100);
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item) {
		for (Entry<TraitType, Float> trait : attackTraits.entrySet()) {
			effected.getStats().mergeAttackTrait(trait.getKey(), trait.getValue());
		}
	}

	@Override
	public void onExit(Creature effector, Creature effected, Skill skill) {
		for (Entry<TraitType, Float> trait : attackTraits.entrySet()) {
			effected.getStats().removeAttackTrait(trait.getKey(), trait.getValue());
		}
	}
}
