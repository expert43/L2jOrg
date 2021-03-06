package org.l2j.gameserver.network.clientpackets;

import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.model.holders.MovieHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author JIV
 */
public final class EndScenePlayer extends ClientPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndScenePlayer.class);
    private int _movieId;

    @Override
    public void readImpl() {
        _movieId = readInt();
    }

    @Override
    public void runImpl() {
        final Player activeChar = client.getPlayer();
        if ((activeChar == null) || (_movieId == 0)) {
            return;
        }

        final MovieHolder holder = activeChar.getMovieHolder();
        if ((holder == null) || (holder.getMovie().getClientId() != _movieId)) {
            LOGGER.warn("Player " + client + " sent EndScenePlayer with wrong movie id: " + _movieId);
            return;
        }
        activeChar.stopMovie();
    }
}