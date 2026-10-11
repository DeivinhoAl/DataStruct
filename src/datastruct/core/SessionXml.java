package datastruct.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** Small, explicit XML format inspired by editor projects: no Java object serialization. */
public final class SessionXml {
    private SessionXml() { }

    public static void save(Simulator simulator, Path path) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().newDocument();
        Element root = document.createElement("datastruct");
        root.setAttribute("version", "2");
        root.setAttribute("vectorCapacity", Integer.toString(simulator.vectorCapacity()));
        document.appendChild(root);
        for (StructureType type : StructureType.values()) {
            Element module = document.createElement("structure");
            module.setAttribute("type", type.name());
            for (int value : simulator.storageValues(type)) {
                Element item = document.createElement("value");
                item.setTextContent(Integer.toString(value));
                module.appendChild(item);
            }
            root.appendChild(module);
        }
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(document), new StreamResult(path.toFile()));
    }

    public static Simulator load(Path path) throws Exception {
        if (Files.size(path) > 1_000_000) throw new IOException("Arquivo de sessão acima do limite de 1 MB.");
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override public void fatalError(SAXParseException error) throws SAXException { throw error; }
        });
        Document document = builder.parse(path.toFile());
        Element root = document.getDocumentElement();
        if (!"datastruct".equals(root.getTagName()) || !("1".equals(root.getAttribute("version")) || "2".equals(root.getAttribute("version"))))
            throw new IOException("Arquivo de sessão DataStruct inválido ou versão incompatível.");
        var states = new EnumMap<StructureType, List<Integer>>(StructureType.class);
        NodeList modules = root.getChildNodes();
        for (int i = 0; i < modules.getLength(); i++) {
            Node raw = modules.item(i);
            if (!(raw instanceof Element module) || !"structure".equals(module.getTagName())) continue;
            StructureType type = StructureType.valueOf(module.getAttribute("type"));
            if (states.containsKey(type)) throw new IOException("Estrutura duplicada: " + type);
            List<Integer> values = new ArrayList<>();
            NodeList children = module.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child instanceof Element element && "value".equals(element.getTagName()))
                    values.add(Integer.parseInt(element.getTextContent().trim()));
            }
            states.put(type, values);
        }
        Simulator simulator = new Simulator();
        if ("2".equals(root.getAttribute("version"))) simulator.resizeVector(Integer.parseInt(root.getAttribute("vectorCapacity")));
        for (StructureType type : StructureType.values()) {
            if (!states.containsKey(type)) throw new IOException("Estrutura ausente: " + type);
            simulator.load(type, states.get(type));
        }
        return simulator;
    }
}
