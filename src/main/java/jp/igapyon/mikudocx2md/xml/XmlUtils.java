package jp.igapyon.mikudocx2md.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XmlUtils {
    private XmlUtils() {
    }

    public static Document parseXml(byte[] bytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (Exception ex) {
            throw new IllegalArgumentException("XML parse failed: " + ex.getMessage(), ex);
        }
    }

    public static Document parseXml(String xml) {
        return parseXml(xml.getBytes(StandardCharsets.UTF_8));
    }

    public static String localName(Node node) {
        if (node == null) {
            return "";
        }
        String local = node.getLocalName();
        if (local != null && local.length() > 0) {
            return local;
        }
        String name = node.getNodeName();
        int index = name == null ? -1 : name.indexOf(':');
        return index >= 0 ? name.substring(index + 1) : (name == null ? "" : name);
    }

    public static List<Element> childrenByLocalName(Node parent, String localName) {
        List<Element> result = new ArrayList<Element>();
        if (parent == null) {
            return result;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE && localName(child).equals(localName)) {
                result.add((Element) child);
            }
        }
        return result;
    }

    public static List<Element> descendantsByLocalName(Node parent, String localName) {
        List<Element> result = new ArrayList<Element>();
        collectDescendants(parent, localName, result);
        return result;
    }

    private static void collectDescendants(Node parent, String localName, List<Element> result) {
        if (parent == null) {
            return;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (localName(child).equals(localName)) {
                    result.add((Element) child);
                }
                collectDescendants(child, localName, result);
            }
        }
    }

    public static String attr(Element element, String name) {
        if (element == null) {
            return "";
        }
        if (element.hasAttribute(name)) {
            return element.getAttribute(name);
        }
        NamedNodeMap attributes = element.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            Node item = attributes.item(index);
            if (localName(item).equals(name)) {
                return item.getNodeValue();
            }
        }
        return "";
    }

    public static String namespacedAttr(Element element, String prefix, String localName) {
        if (element == null) {
            return "";
        }
        if (element.hasAttribute(prefix + ":" + localName)) {
            return element.getAttribute(prefix + ":" + localName);
        }
        return attr(element, localName);
    }

    public static Element firstChild(Node parent, String localName) {
        List<Element> children = childrenByLocalName(parent, localName);
        return children.isEmpty() ? null : children.get(0);
    }
}
