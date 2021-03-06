package org.l2j.gameserver.network.serverpackets.costume;

import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;
import org.l2j.gameserver.network.serverpackets.ServerPacket;

/**
 * @author JoeAlisson
 */
public class ExChooseCostumeItem extends ServerPacket {

    private final int itemId;

    public ExChooseCostumeItem(int itemId) {
        this.itemId = itemId;
    }

    @Override
    protected void writeImpl(GameClient client)  {
        writeId(ServerExPacketId.EX_CHOOSE_COSTUME_ITEM);
        writeInt(itemId);
    }
}
