package org.l2j.gameserver.network.clientpackets;

import org.l2j.gameserver.model.actor.Npc;
import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.network.serverpackets.html.NpcHtmlMessage;
import org.l2j.gameserver.util.GameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lets drink to code!
 *
 * @author zabbix, HorridoJoho
 */
public final class RequestLinkHtml extends ClientPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLinkHtml.class);
    private String _link;

    @Override
    public void readImpl() {
        _link = readString();
    }

    @Override
    public void runImpl() {
        final Player actor = client.getPlayer();
        if (actor == null) {
            return;
        }

        if (_link.isEmpty()) {
            LOGGER.warn("Player " + actor.getName() + " sent empty html link!");
            return;
        }

        if (_link.contains("..")) {
            LOGGER.warn("Player " + actor.getName() + " sent invalid html link: link " + _link);
            return;
        }

        final int htmlObjectId = actor.validateHtmlAction("link " + _link);
        if (htmlObjectId == -1) {
            LOGGER.warn("Player " + actor.getName() + " sent non cached  html link: link " + _link);
            return;
        }

        if ((htmlObjectId > 0) && !GameUtils.isInsideRangeOfObjectId(actor, htmlObjectId, Npc.INTERACTION_DISTANCE)) {
            // No logging here, this could be a common case
            return;
        }

        final String filename = "data/html/" + _link;
        final NpcHtmlMessage msg = new NpcHtmlMessage(htmlObjectId);
        msg.setFile(actor, filename);
        actor.sendPacket(msg);
    }
}
