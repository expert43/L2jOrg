package handlers.effecthandlers;

import org.l2j.gameserver.model.StatsSet;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.effects.AbstractEffect;
import org.l2j.gameserver.model.items.instance.Item;
import org.l2j.gameserver.engine.skill.api.Skill;
import org.l2j.gameserver.model.stats.Stat;

import static org.l2j.gameserver.util.GameUtils.isPlayer;

/**
 * @author Sdw
 */
public class PolearmSingleTarget extends AbstractEffect {
	public PolearmSingleTarget(StatsSet params) {
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item) {
		if (isPlayer(effected)) {
			effected.getStats().addFixedValue(Stat.PHYSICAL_POLEARM_TARGET_SINGLE, 1.0);
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill) {
		if (isPlayer(effected)) {
			effected.getStats().removeFixedValue(Stat.PHYSICAL_POLEARM_TARGET_SINGLE);
		}
	}
}
