package org.l2j.gameserver.network.serverpackets;

import io.github.joealisson.mmocore.StaticPacket;
import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;

/**
 * @author UnAfraid
 */
@StaticPacket
public class ExChangeClientEffectInfo extends ServerPacket {
    public static final ExChangeClientEffectInfo STATIC_FREYA_DEFAULT = new ExChangeClientEffectInfo(0, 0, 1);
    public static final ExChangeClientEffectInfo STATIC_FREYA_DESTROYED = new ExChangeClientEffectInfo(0, 0, 2);

    private final int _type;
    private final int _key;
    private final int _value;

    /**
     * @param type  <ul>
     *              <li>0 - ChangeZoneState</li>
     *              <li>1 - SetL2Fog</li>
     *              <li>2 - postEffectData</li>
     *              </ul>
     * @param key
     * @param value
     */
    private ExChangeClientEffectInfo(int type, int key, int value) {
        _type = type;
        _key = key;
        _value = value;
    }

    @Override
    public void writeImpl(GameClient client) {
        writeId(ServerExPacketId.EX_CLIENT_EFFECT_INFO);

        writeInt(_type);
        writeInt(_key);
        writeInt(_value);
    }

}
