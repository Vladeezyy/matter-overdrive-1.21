package matteroverdrive.client.guide;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * 1.7.10 PageGuideDescription.loadGuideInfo: an entry's {@code guide/<lang>/<file>.xml} (falling back to en_us) parsed
 * into pages, with the entry's stylesheet ({@code mo:info/styles/guide.css} -> {@code guide/styles/guide.css}).
 */
public final class GuideDocument {
    private static final Pattern CSS_RULE = Pattern.compile("([^}{]+)(\\{[^}]+})", Pattern.DOTALL | Pattern.MULTILINE);

    public static List<GuideElements.Page> load(GuideEntry entry, int width, int height) {
        List<GuideElements.Page> pages = new ArrayList<>();
        Optional<Resource> resource = find(entry);
        if (resource.isEmpty()) return pages;
        try (InputStream in = resource.get().open()) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(in);
            document.normalize();
            Element root = (Element) document.getElementsByTagName("entry").item(0);
            Map<String, String> css = stylesheet(root);
            NodeList pageNodes = root.getElementsByTagName("page");
            for (int i = 0; i < pageNodes.getLength(); i++) {
                GuideElements.Page page = new GuideElements.Page();
                page.load(entry, (Element) pageNodes.item(i), css, width, height);
                pages.add(page);
            }
        } catch (Exception e) {
            MatterOverdrive.LOGGER.error("Guide entry {} could not be read", entry.name(), e);
        }
        return pages;
    }

    private static Optional<Resource> find(GuideEntry entry) {
        var manager = Minecraft.getInstance().getResourceManager();
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        Optional<Resource> r = manager.getResource(id("guide/" + lang + "/" + entry.file() + ".xml"));
        return r.isPresent() ? r : manager.getResource(id("guide/en_us/" + entry.file() + ".xml"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    /** 1.7.10 loadStyleSheetMap: ".class{rules}" with whitespace and comments removed. */
    private static Map<String, String> stylesheet(Element root) {
        Map<String, String> map = new HashMap<>();
        if (!root.hasAttribute("stylesheet")) return map;
        String path = root.getAttribute("stylesheet").replace("mo:info/", "guide/");
        var resource = Minecraft.getInstance().getResourceManager().getResource(id(path));
        if (resource.isEmpty()) return map;
        try (InputStream in = resource.get().open()) {
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\r|\\n|\\s+", "").replaceAll("(?s)/\\*.*?\\*/", "");
            Matcher m = CSS_RULE.matcher(raw);
            while (m.find()) map.put(m.group(1), m.group(2).substring(1, m.group(2).length() - 1));
        } catch (Exception e) {
            MatterOverdrive.LOGGER.error("Guide stylesheet {} could not be read", path, e);
        }
        return map;
    }

    private GuideDocument() {}
}
