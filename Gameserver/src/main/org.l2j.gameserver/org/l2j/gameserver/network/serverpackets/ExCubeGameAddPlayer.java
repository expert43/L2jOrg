package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;

/**
 * @author mrTJO
 */
public class ExCubeGameAddPlayer extends ServerPacket {
    Player _player;
    boolean _isRedTeam;

    /**
     * Add Player To Minigame Waiting List
     *
     * @param player    Player Instance
     * @param isRedTeam Is Player from Red Team?
     */
    public ExCubeGameAddPlayer(Player player, boolean isRedTeam) {
        _player = player;
        _isRedTeam = isRedTeam;
    }

    @Override
    public void writeImpl(GameClient client) {
        writeId(ServerExPacketId.EX_BLOCK_UPSET_LIST);

        writeInt(0x01);

        writeInt(0xffffffff);

        writeInt(_isRedTeam ? 0x01 : 0x00);
        writeInt(_player.getObjectId());
        writeString(_player.getName());
    }

}
