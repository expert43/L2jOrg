package org.l2j.gameserver.data.xml.impl;

import org.l2j.gameserver.model.StatsSet;
import org.l2j.gameserver.model.base.ClassId;
import org.l2j.gameserver.model.items.L2Henna;
import org.l2j.gameserver.model.skills.Skill;
import org.l2j.gameserver.util.IGameXmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class holds the henna related information.<br>
 * Cost and required amount to add the henna to the player.<br>
 * Cost and retrieved amount for removing the henna from the player.<br>
 * Allowed classes to wear each henna.
 *
 * @author Zoey76, Mobius
 */
public final class HennaData implements IGameXmlReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(HennaData.class);

    private final Map<Integer, L2Henna> _hennaList = new HashMap<>();

    /**
     * Instantiates a new henna data.
     */
    protected HennaData() {
        load();
    }

    /**
     * Gets the single instance of HennaData.
     *
     * @return single instance of HennaData
     */
    public static HennaData getInstance() {
        return SingletonHolder._instance;
    }

    @Override
    public void load() {
        _hennaList.clear();
        parseDatapackFile("data/stats/hennaList.xml");
        LOGGER.info(getClass().getSimpleName() + ": Loaded " + _hennaList.size() + " Henna data.");
    }

    @Override
    public void parseDocument(Document doc, File f) {
        for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
            if ("list".equals(n.getNodeName())) {
                for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                    if ("henna".equals(d.getNodeName())) {
                        parseHenna(d);
                    }
                }
            }
        }
    }

    /**
     * Parses the henna.
     *
     * @param d the d
     */
    private void parseHenna(Node d) {
        final StatsSet set = new StatsSet();
        final List<ClassId> wearClassIds = new ArrayList<>();
        final List<Skill> skills = new ArrayList<>();
        NamedNodeMap attrs = d.getAttributes();
        Node attr;
        for (int i = 0; i < attrs.getLength(); i++) {
            attr = attrs.item(i);
            set.set(attr.getNodeName(), attr.getNodeValue());
        }

        for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
            final String name = c.getNodeName();
            attrs = c.getAttributes();
            switch (name) {
                case "stats": {
                    for (int i = 0; i < attrs.getLength(); i++) {
                        attr = attrs.item(i);
                        set.set(attr.getNodeName(), attr.getNodeValue());
                    }
                    break;
                }
                case "wear": {
                    attr = attrs.getNamedItem("count");
                    set.set("wear_count", attr.getNodeValue());
                    attr = attrs.getNamedItem("fee");
                    set.set("wear_fee", attr.getNodeValue());
                    break;
                }
                case "cancel": {
                    attr = attrs.getNamedItem("count");
                    set.set("cancel_count", attr.getNodeValue());
                    attr = attrs.getNamedItem("fee");
                    set.set("cancel_fee", attr.getNodeValue());
                    break;
                }
                case "duration": {
                    attr = attrs.getNamedItem("time"); // in minutes
                    set.set("duration", attr.getNodeValue());
                    break;
                }
                case "skill": {
                    skills.add(SkillData.getInstance().getSkill(parseInteger(attrs, "id"), parseInteger(attrs, "level")));
                    break;
                }
                case "classId": {
                    wearClassIds.add(ClassId.getClassId(Integer.parseInt(c.getTextContent())));
                    break;
                }
            }
        }
        final L2Henna henna = new L2Henna(set);
        henna.setSkills(skills);
        henna.setWearClassIds(wearClassIds);
        _hennaList.put(henna.getDyeId(), henna);
    }

    /**
     * Gets the henna.
     *
     * @param id of the dye.
     * @return the dye with that id.
     */
    public L2Henna getHenna(int id) {
        return _hennaList.get(id);
    }

    /**
     * Gets the henna list.
     *
     * @param classId the player's class Id.
     * @return the list with all the allowed dyes.
     */
    public List<L2Henna> getHennaList(ClassId classId) {
        final List<L2Henna> list = new ArrayList<>();
        for (L2Henna henna : _hennaList.values()) {
            if (henna.isAllowedClass(classId)) {
                list.add(henna);
            }
        }
        return list;
    }

    private static class SingletonHolder {
        protected static final HennaData _instance = new HennaData();
    }
}