package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.instancemanager.CastleManorManager;
import org.l2j.gameserver.model.CropProcure;
import org.l2j.gameserver.model.L2Seed;
import org.l2j.gameserver.model.itemcontainer.PcInventory;
import org.l2j.gameserver.model.items.instance.Item;
import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

import java.util.HashMap;
import java.util.Map;

/**
 * @author l3x
 */
public final class ExShowSellCropList extends ServerPacket {
    private final int _manorId;
    private final Map<Integer, Item> _cropsItems = new HashMap<>();
    private final Map<Integer, CropProcure> _castleCrops = new HashMap<>();

    public ExShowSellCropList(PcInventory inventory, int manorId) {
        _manorId = manorId;
        for (int cropId : CastleManorManager.getInstance().getCropIds()) {
            final Item item = inventory.getItemByItemId(cropId);
            if (item != null) {
                _cropsItems.put(cropId, item);
            }
        }

        for (CropProcure crop : CastleManorManager.getInstance().getCropProcure(_manorId, false)) {
            if (_cropsItems.containsKey(crop.getId()) && (crop.getAmount() > 0)) {
                _castleCrops.put(crop.getId(), crop);
            }
        }
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.EX_SHOW_SELL_CROP_LIST);

        writeInt(_manorId); // manor id
        writeInt(_cropsItems.size()); // size
        for (Item item : _cropsItems.values()) {
            final L2Seed seed = CastleManorManager.getInstance().getSeedByCrop(item.getId());
            writeInt(item.getObjectId()); // Object id
            writeInt(item.getId()); // crop id
            writeInt(seed.getLevel()); // seed level
            writeByte((byte) 0x01);
            writeInt(seed.getReward(1)); // reward 1 id
            writeByte((byte) 0x01);
            writeInt(seed.getReward(2)); // reward 2 id
            if (_castleCrops.containsKey(item.getId())) {
                final CropProcure crop = _castleCrops.get(item.getId());
                writeInt(_manorId); // manor
                writeLong(crop.getAmount()); // buy residual
                writeLong(crop.getPrice()); // buy price
                writeByte((byte) crop.getReward()); // reward
            } else {
                writeInt(0xFFFFFFFF); // manor
                writeLong(0x00); // buy residual
                writeLong(0x00); // buy price
                writeByte((byte) 0x00); // reward
            }
            writeLong(item.getCount()); // my crops
        }
    }

}