package org.l2j.scripts.handler.bbs.custom;

import org.l2j.scripts.handler.bbs.ScriptsCommunityHandler;
import org.l2j.commons.dbutils.DbUtils;
import org.l2j.gameserver.data.htm.HtmCache;
import org.l2j.gameserver.data.xml.holder.ResidenceHolder;
import org.l2j.commons.database.L2DatabaseFactory;
import org.l2j.gameserver.model.Player;
import org.l2j.gameserver.model.Zone.ZoneType;
import org.l2j.gameserver.model.actor.instances.player.BookMarkList;
import org.l2j.gameserver.model.base.TeamType;
import org.l2j.gameserver.model.entity.residence.Castle;
import org.l2j.gameserver.network.l2.components.SystemMsg;
import org.l2j.gameserver.network.l2.s2c.ShowBoardPacket;
import org.l2j.gameserver.settings.ServerSettings;
import org.l2j.gameserver.utils.*;
import io.github.joealisson.primitive.maps.IntObjectMap;
import io.github.joealisson.primitive.maps.impl.HashIntObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.StringTokenizer;

import static org.l2j.commons.configuration.Configurator.getSettings;

public class CommunityTeleport extends ScriptsCommunityHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunityTeleport.class);

    private static final IntObjectMap<TeleportInfo> _teleportsInfo = new HashIntObjectMap<TeleportInfo>();

    private static TeleportList _mainTeleportList = null;

    @Override
    public void onInit()
    {
        super.onInit();
        loadTeleportList();
    }

    @Override
    public String[] getBypassCommands()
    {
        return new String[]
                {
                        "_cbbsteleport",
                        "_cbbstpsavepoint",
                        "_cbbsteleportdelete"
                };
    }

    @Override
    protected void doBypassCommand(Player player, String bypass)
    {
        if(BBSConfig.TELEPORT_SERVICE_COST_ITEM_ID == 0)
        {
            player.sendMessage("This service disallowed.");
            player.sendPacket(ShowBoardPacket.CLOSE);
            return;
        }

        StringTokenizer st = new StringTokenizer(bypass, "_");
        String cmd = st.nextToken();
        String html = "";

        if("cbbsteleport".equals(cmd))
        {
            int pointId = 0;
            if(st.hasMoreTokens())
                pointId = Integer.parseInt(st.nextToken());

            TeleportInfo info = _teleportsInfo.get(pointId);
            if(info == null)
                return;

            if(info instanceof TeleportPoint)
            {
                if(player.getKarma() < 0 && !BBSConfig.TELEPORT_SERVICE_TELEPORT_IF_PK)
                    html = HtmCache.getInstance().getHtml("scripts/org.l2j.scripts.handler/bbs/pages/teleports-pk.htm", player);
                else
                {
                    if(!BBSConfig.GLOBAL_USE_FUNCTIONS_CONFIGS && !checkUseCondition(player))
                    {
                        onWrongCondition(player);
                        return;
                    }

                    teleport(player, (TeleportPoint) info);
                    player.sendPacket(ShowBoardPacket.CLOSE);
                    return;
                }
            }
            else if(info instanceof TeleportList)
            {
                html = HtmCache.getInstance().getHtml("scripts/org.l2j.scripts.handler/bbs/pages/teleports.htm", player);
                html = html.replace("<?price?>", Util.formatAdena(BBSConfig.TELEPORT_SERVICE_COST_ITEM_COUNT));
                html = html.replace("<?item_name?>", HtmlUtils.htmlItemName(BBSConfig.TELEPORT_SERVICE_COST_ITEM_ID));
                html = html.replace("<?teleport_list?>", generateTeleportList(player, (TeleportList) info));
                html = html.replace("<?bm_tp_list?>", generateBMTeleportList(player));
                html = html.replace("<?save_tp_price?>", Util.formatAdena(BBSConfig.TELEPORT_SERVICE_BM_SAVE_COST_ITEM_COUNT));
                html = html.replace("<?save_tp_name?>", HtmlUtils.htmlItemName(BBSConfig.TELEPORT_SERVICE_BM_SAVE_COST_ITEM_ID));
            }
        }
        else if("cbbsteleportpoint".equals(cmd))
        {
            if(player.getKarma() < 0 && !BBSConfig.TELEPORT_SERVICE_TELEPORT_IF_PK)
                html = HtmCache.getInstance().getHtml("scripts/org.l2j.scripts.handler/bbs/pages/teleports-pk.htm", player);
            else
            {
                if(!BBSConfig.GLOBAL_USE_FUNCTIONS_CONFIGS && !checkUseCondition(player))
                {
                    onWrongCondition(player);
                    return;
                }

                int x = Integer.parseInt(st.nextToken());
                int y = Integer.parseInt(st.nextToken());
                int z = Integer.parseInt(st.nextToken());

                if(!BookMarkList.checkFirstConditions(player) || !BookMarkList.checkTeleportConditions(player))
                    return;

                if(ItemFunctions.getItemCount(player, BBSConfig.TELEPORT_SERVICE_BM_COST_ITEM_ID) < BBSConfig.TELEPORT_SERVICE_BM_COST_ITEM_COUNT)
                {
                    player.sendPacket(SystemMsg.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS);
                    return;
                }

                Location loc = Location.findPointToStay(new Location(x, y, z), 50, 100, player.getGeoIndex());
                player.teleToLocation(loc);
                ItemFunctions.deleteItem(player, BBSConfig.TELEPORT_SERVICE_BM_COST_ITEM_ID, BBSConfig.TELEPORT_SERVICE_BM_COST_ITEM_COUNT);
                player.sendPacket(ShowBoardPacket.CLOSE);
                return;
            }
        }
        else if("cbbsteleportdelete".equals(cmd))
        {
            String name = st.nextToken();

            Connection con = null;
            PreparedStatement statement = null;
            try
            {
                con = L2DatabaseFactory.getInstance().getConnection();
                statement = con.prepareStatement("DELETE FROM bbs_teleport_bm WHERE char_id=? AND name=?");
                statement.setInt(1, player.getObjectId());
                statement.setString(2, name);
                statement.execute();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                DbUtils.closeQuietly(con, statement);
            }

            onBypassCommand(player, "_cbbsteleport");
            return;

        }
        else if("cbbstpsavepoint".equals(cmd))
        {
            if(!st.hasMoreTokens())
            {
                onBypassCommand(player, "_cbbsteleport");
                return;
            }
            String bmName = st.nextToken();
            if(bmName.equals(" ") || bmName.isEmpty())
            {
                player.sendMessage("You have not entered the name of the teleport.");
                onBypassCommand(player, "_cbbsteleport");
                return;
            }

            if(tpNameExist(player, bmName))
            {
                player.sendMessage("You cannot use the same teleport name twice.");
                onBypassCommand(player, "_cbbsteleport");
                return;
            }

            if(!checkCond(player, true))
            {
                onBypassCommand(player, "_cbbsteleport");
                return;
            }

            if(ItemFunctions.getItemCount(player, BBSConfig.TELEPORT_SERVICE_BM_SAVE_COST_ITEM_ID) < BBSConfig.TELEPORT_SERVICE_BM_SAVE_COST_ITEM_COUNT)
            {
                player.sendPacket(SystemMsg.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS);
                return;
            }

            ItemFunctions.deleteItem(player, BBSConfig.TELEPORT_SERVICE_BM_SAVE_COST_ITEM_ID, BBSConfig.TELEPORT_SERVICE_BM_SAVE_COST_ITEM_COUNT);

            Connection con = null;
            PreparedStatement statement = null;

            try
            {
                con = L2DatabaseFactory.getInstance().getConnection();
                statement = con.prepareStatement("INSERT INTO bbs_teleport_bm (char_id,name,x,y,z) VALUES (?,?,?,?,?)");
                statement.setInt(1, player.getObjectId());
                statement.setString(2, bmName);
                statement.setInt(3, player.getX());
                statement.setInt(4, player.getY());
                statement.setInt(5, player.getZ());
                statement.execute();
            }
            catch(Exception e)
            {
                LOGGER.warn("CommunityTeleport: cannot save tp book mark for player "+player.getName()+"");
                e.printStackTrace();
            }
            finally
            {
                player.sendMessage("You have successfully saved a teleport point.");
                DbUtils.closeQuietly(con, statement);
            }
            onBypassCommand(player, "_cbbsteleport");
            return;
        }
        ShowBoardPacket.separateAndSend(html, player);
    }

    public class CBteleport
    {
        public int PlayerId = 0; // charID
        public String TpName = ""; // Loc name
        public int xC = 0; // Location coords
        public int yC = 0; //
        public int zC = 0; //
    }

    private boolean checkCond(Player player, boolean save)
    {
        if(player.isDead())
            return false;

        if(player.getTeam() !=	TeamType.NONE)
            return false;

        if(player.isFlying() || player.isInFlyingTransform())
            return false;

        if(player.isInBoat())
            return false;

        if(player.isInStoreMode() || player.isInTrade())
            return false;

        if(player.isInDuel())
            return false;

        if(save)
        {
            if(!player.getReflection().isMain() || player.isInSiegeZone() || player.isInZone(ZoneType.RESIDENCE) || player.isInZone(ZoneType.HEADQUARTER) || player.isInZone(ZoneType.battle_zone) ||player.isInZone(ZoneType.ssq_zone) || player.isInZone(ZoneType.no_restart) || player.isInZone(ZoneType.offshore) || player.isInZone(ZoneType.epic) || player.isInOlympiadMode() || player.isInSiegeZone())
            {
                player.sendMessage("You can not make a teleport to the location in which are at the moment.");
                return false;
            }

            if(ItemFunctions.getItemCount(player, BBSConfig.TELEPORT_SERVICE_BM_SAVE_COST_ITEM_ID) < BBSConfig.TELEPORT_SERVICE_BM_SAVE_COST_ITEM_COUNT)
            {
                player.sendMessage("You have not enough item to procced the operation.");
                return false;
            }

            if(getCountTP(player) >= BBSConfig.TELEPORT_SERVICE_BM_SAVE_LIMIT)
            {
                player.sendMessage("You have reached the limit of maximum savings of the teleport bookmarks.");
                return false;
            }
        }
        else
        {
            if(ItemFunctions.getItemCount(player, BBSConfig.TELEPORT_SERVICE_BM_COST_ITEM_ID) < BBSConfig.TELEPORT_SERVICE_BM_COST_ITEM_COUNT)
            {
                player.sendMessage("You do not have the necessary items to carry teleport.");
                return false;
            }
        }
        return true;
    }

    private int getCountTP(Player player)
    {
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        int i = 0;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("SELECT name FROM bbs_teleport_bm WHERE char_id=?");
            statement.setInt(1, player.getObjectId());
            rset = statement.executeQuery();
            while(rset.next())
                i++;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            DbUtils.closeQuietly(con, statement, rset);
        }

        return i;

    }
    private boolean tpNameExist(Player player, String bmName)
    {
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        boolean isExist = false;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("SELECT name FROM bbs_teleport_bm WHERE char_id=?");
            statement.setInt(1, player.getObjectId());
            rset = statement.executeQuery();
            while(rset.next())
            {
                String name = rset.getString("name");
                if(name.equals(bmName))
                    isExist = true;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            DbUtils.closeQuietly(con, statement, rset);
        }
        return isExist;
    }
    @Override
    protected void doWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
    {
        //
    }

    private void teleport(Player player, TeleportPoint point)
    {
        if(!BookMarkList.checkFirstConditions(player) || !BookMarkList.checkTeleportConditions(player))
            return;

        if(player.getReflection().isDefault())
        {
            int castleId = point.getCastleId();
            Castle castle = castleId > 0 ? ResidenceHolder.getInstance().getResidence(Castle.class, castleId) : null;
            // Нельзя телепортироваться в города, где идет осада
            if(castle != null && castle.getSiegeEvent().isInProgress())
            {
                player.sendPacket(SystemMsg.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
                return;
            }
        }

        int itemId = point.getItemId();
        long itemCount = point.getItemCount();
        if(ItemFunctions.getItemCount(player, itemId) < itemCount)
        {
            player.sendPacket(SystemMsg.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS);
            return;
        }

        Location loc = Location.findPointToStay(point.getLocation(), 50, 100, player.getGeoIndex());
        player.teleToLocation(loc);
        ItemFunctions.deleteItem(player, itemId, itemCount);
        player.sendPacket(ShowBoardPacket.CLOSE);
    }

    public void loadTeleportList() {

        var filePath = getSettings(ServerSettings.class).dataPackRootPath().resolve("data/bbs_teleports.xml");

        if(Files.notExists(filePath)) {
            LOGGER.warn("bbs_teleports.xml file is missing.");
            return;
        }

        Document doc = null;

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            doc = factory.newDocumentBuilder().parse(filePath.toFile());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            parseTeleportList(doc);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void parseTeleportList(Document doc)
    {
        for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if("list".equalsIgnoreCase(n.getNodeName()))
            {
                _mainTeleportList = parseTeleportList(n);
            }
        }
    }

    private TeleportList parseTeleportList(Node d)
    {
        TeleportList teleportList = new TeleportList();
        for(Language lang : Language.VALUES)
        {
            if(d.getAttributes().getNamedItem("name_" + lang.getShortName()) != null)
                teleportList.addName(lang, d.getAttributes().getNamedItem("name_" + lang.getShortName()).getNodeValue());
        }
        for(Node t = d.getFirstChild(); t != null; t = t.getNextSibling())
        {
            if("point".equalsIgnoreCase(t.getNodeName()))
            {
                final int x = Integer.parseInt(t.getAttributes().getNamedItem("x").getNodeValue());
                final int y = Integer.parseInt(t.getAttributes().getNamedItem("y").getNodeValue());
                final int z = Integer.parseInt(t.getAttributes().getNamedItem("z").getNodeValue());
                final int castleId = t.getAttributes().getNamedItem("castle_id") == null ? 0 : Integer.parseInt(t.getAttributes().getNamedItem("castle_id").getNodeValue());
                final int itemId = t.getAttributes().getNamedItem("item_id") == null ? BBSConfig.TELEPORT_SERVICE_COST_ITEM_ID : Integer.parseInt(t.getAttributes().getNamedItem("item_id").getNodeValue());
                final long itemCount = t.getAttributes().getNamedItem("item_count") == null ? BBSConfig.TELEPORT_SERVICE_COST_ITEM_COUNT : Integer.parseInt(t.getAttributes().getNamedItem("item_count").getNodeValue());
                TeleportPoint teleportPoint = new TeleportPoint(new Location(x, y, z), castleId, itemId, itemCount);
                for(Language lang : Language.VALUES)
                {
                    if(t.getAttributes().getNamedItem("name_" + lang.getShortName()) != null)
                        teleportPoint.addName(lang, t.getAttributes().getNamedItem("name_" + lang.getShortName()).getNodeValue());
                }
                teleportList.addPoint(teleportPoint);
            }
            else if("teleport_list".equalsIgnoreCase(t.getNodeName()))
            {
                teleportList.addPoint(parseTeleportList(t));
            }
        }
        return teleportList;
    }

    private String generateTeleportList(Player player, TeleportList list)
    {
        int parrentId = 0;

        StringBuilder result = new StringBuilder();
        result.append("<table>");
        if(list == null || list.getPointsIds().length == 0)
        {
            result.append("<tr><td align=center>");
            result.append("Error! A disabled list teleports.");
            result.append("</td></tr>");
        }
        else
        {
            parrentId = list.getParrentId();
            int[] pointsIds = list.getPointsIds();
            Arrays.sort(pointsIds);
            for(int pointId : pointsIds)
            {
                TeleportInfo info = list.getPoint(pointId);
                result.append("<tr>");
                result.append("<td align=center><button value=\"");
                result.append(info.getName(player.getLanguage()));
                result.append("\" action=\"bypass _cbbsteleport_" + info.getId() + "\" width=200 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
                result.append("</tr>");
            }
        }
        if(list != _mainTeleportList)
        {
            result.append("<tr><td align=center>&nbsp;</td></tr>");
            result.append("<tr><td align=center><button value=\"");
            result.append("Back");
            result.append("\" action=\"bypass _cbbsteleport_" + parrentId + "\" width=100 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
        }
        result.append("</table>");
        return result.toString();
    }

    private String generateBMTeleportList(Player player)
    {
        StringBuilder teleports = new StringBuilder();

        CBteleport tp;
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("SELECT * FROM bbs_teleport_bm WHERE char_id=?;");
            statement.setLong(1, player.getObjectId());
            rs = statement.executeQuery();
            int i = 0;
            while(rs.next())
            {
                tp = new CBteleport();
                tp.PlayerId = rs.getInt("char_id");
                tp.TpName = rs.getString("name");
                tp.xC = rs.getInt("x");
                tp.yC = rs.getInt("y");
                tp.zC = rs.getInt("z");

                if(i % 2 == 0)
                    teleports.append("<table width=288 bgcolor=000000>");
                else
                    teleports.append("<table width=288>");
                teleports.append("<tr>");
                teleports.append("<td width=185 align=center><button value=\"" + tp.TpName + "\" action=\"bypass _cbbsteleportpoint_" + tp.xC + "_" + tp.yC + "_" + tp.zC + "\" width=180 height=25 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>");
                teleports.append("<td width=70 align=center><button value=\"");
                teleports.append("Delete");
                teleports.append("\" action=\"bypass _cbbsteleportdelete_" + tp.TpName + "\" width=65 height=25 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>");
                teleports.append("</tr></table>");
                i++;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            DbUtils.closeQuietly(con, statement, rs);
        }

        StringBuilder result = new StringBuilder();
        if(teleports.length() > 0)
        {
            result.append("<table>");
            result.append("<tr><td align=center>Стоимость личного телепорта: <font color=\"LEVEL\">");
            result.append(Util.formatAdena(BBSConfig.TELEPORT_SERVICE_BM_COST_ITEM_COUNT));
            result.append(" ");
            result.append(HtmlUtils.htmlItemName(BBSConfig.TELEPORT_SERVICE_BM_COST_ITEM_ID));
            result.append("</font></td></tr>");
            result.append("<tr><td align=center><table width=288 bgcolor=3D3D3D><tr><td width=260 align=center></td></tr></table></td></tr>");
            result.append("<tr><td align=center>");
            result.append(teleports.toString());
            result.append("</td></tr>");
            result.append("<tr><td align=center><table width=288 bgcolor=3D3D3D><tr><td width=260 align=center></td></tr></table></td></tr>");
            result.append("</table><br><br><br>");
        }
        return result.toString();
    }

    private static abstract class TeleportInfo
    {
        private final int _id;
        private final IntObjectMap<String> _names = new HashIntObjectMap<String>();
        private int _parrentId = 0;

        public TeleportInfo()
        {
            _id = _teleportsInfo.size();
            _teleportsInfo.put(_id, this);
        }

        public int getId()
        {
            return _id;
        }

        public void setParrentId(int value)
        {
            _parrentId = value;
        }

        public int getParrentId()
        {
            return _parrentId;
        }

        public void addName(Language lang, String name)
        {
            _names.put(lang.ordinal(), name);
        }

        public String getName(Language lang)
        {
            String name = _names.get(lang.ordinal());
            if(name == null)
            {
                if(lang == Language.ENGLISH)
                    name = _names.get(Language.RUSSIAN.ordinal());
                else
                    name = _names.get(Language.ENGLISH.ordinal());
            }
            return name;
        }
    }

    private static class TeleportList extends TeleportInfo
    {
        private final HashIntObjectMap<TeleportInfo> _points = new HashIntObjectMap<TeleportInfo>();

        public TeleportList()
        {
            super();
        }

        public void addPoint(TeleportInfo point)
        {
            _points.put(point.getId(), point);
            point.setParrentId(getId());
        }

        public int[] getPointsIds()
        {
            return _points.keySet().toArray();
        }

        public TeleportInfo getPoint(int id)
        {
            return _points.get(id);
        }
    }

    private static class TeleportPoint extends TeleportInfo
    {
        private final Location _loc;
        private final int _castleId;
        private final IntObjectMap<String> _names = new HashIntObjectMap<String>();
        private final int _itemId;
        private final long _itemCount;

        public TeleportPoint(Location loc, int castleId, int itemId, long itemCount)
        {
            super();
            _loc = loc;
            _castleId = castleId;
            _itemId = itemId;
            _itemCount = itemCount;
        }

        public Location getLocation()
        {
            return _loc;
        }

        public int getCastleId()
        {
            return _castleId;
        }

        public int getItemId()
        {
            return _itemId;
        }

        public long getItemCount()
        {
            return _itemCount;
        }
    }
}