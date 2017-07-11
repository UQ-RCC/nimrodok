package au.edu.uq.rcc.nimrod.optim.build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtils {

	public static String NIMRODOK_KAR_LSID = "urn:lsid:au.edu.uq.rcc.nimrod.optim:kar:1:1";

	public static void buildActorXML(Path dir, ActorDefinition actor) throws IOException, ParserConfigurationException, TransformerException {
		Path xmlFile = dir.resolve(actor.xmlName);

		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

		Element entity = document.createElement("entity");
		entity.setAttribute("name", actor.name);
		entity.setAttribute("class", "ptolemy.kernel.ComponentEntity");
		document.appendChild(entity);

		Element prop = document.createElement("property");
		prop.setAttribute("name", "entityId");
		prop.setAttribute("value", actor.entity);
		prop.setAttribute("class", "org.kepler.moml.NamedObjId");
		entity.appendChild(prop);

		prop = document.createElement("property");
		prop.setAttribute("name", "class");
		prop.setAttribute("value", actor.klass);
		prop.setAttribute("class", "ptolemy.kernel.util.StringAttribute");
		entity.appendChild(prop);

		Element id = document.createElement("property");
		id.setAttribute("name", "id");
		id.setAttribute("value", actor.entity);
		id.setAttribute("class", "ptolemy.kernel.util.StringAttribute");
		prop.appendChild(id);

		prop = document.createElement("property");
		prop.setAttribute("name", "semanticType00");
		prop.setAttribute("value", "urn:lsid:localhost:onto:4:1#NimrodOKActors");
		prop.setAttribute("class", "org.kepler.sms.SemanticType");
		entity.appendChild(prop);

		try(OutputStream os = Files.newOutputStream(xmlFile)) {
			dumpXML(document, os);
		}
	}

	public static void generateActorManifest(Path manifest, List<ActorDefinition> actors) throws IOException {
		try(PrintStream ps = new PrintStream(Files.newOutputStream(manifest))) {
			ps.printf("Manifest-Version: 1.4.2\n");
			ps.printf("KAR-Version: 2.0\n");
			ps.printf("lsid: %s\n", NIMRODOK_KAR_LSID);
			ps.printf("openable: false\n");

			for(ActorDefinition actor : actors) {
				ps.printf("\nName: %s\n", actor.xmlName);
				ps.printf("type: ptolemy.kernel.ComponentEntity\n");
				ps.printf("lsid: %s\n", actor.entity);
				ps.printf("handler: org.kepler.kar.handlers.ActorMetadataKAREntryHandler\n");
			}
		}
	}

	public static void addModulesTXTEntry(String fileName, String entry, String insertBefore) throws IOException {

		HashSet<String> dup = new LinkedHashSet<>();
		/* <== LinkedHashSet preserves order. */

		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			for(String line; (line = br.readLine()) != null;) {
				dup.add(line.trim());
			}
		}

		/* Remove us if we already exist. */
		dup.remove(entry);

		try(FileWriter w = new FileWriter(fileName)) {
			boolean inserted = false;
			for(String s : dup) {
				if(s.equals(insertBefore)) {
					w.write(entry);
					w.write('\n');
					inserted = true;
				}
				w.write(s);
				w.write('\n');
			}

			/* If we weren't inserted, just dump us at the end. */
			if(!inserted) {
				w.write(entry);
				w.write('\n');
			}
		}

	}

	public static void addOrUpdatePair(Node config, String name, String value) {
		NodeList l = config.getChildNodes();
		Node pair = null;

		for(int i = 0; i < l.getLength(); ++i) {
			Node node = l.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			if(isPair((Element) node, name)) {
				pair = node;
				break;
			}

		}

		if(pair == null) {
			pair = config.getOwnerDocument().createElement("pair");
			config.appendChild(pair);
		}

		/* Kind of a hack, just remove all children. We're re-creating them anyway. */
		l = pair.getChildNodes();
		while(l.getLength() > 0) {
			pair.removeChild(l.item(0));
		}

		Document document = config.getOwnerDocument();

		Element nameElement = document.createElement("name");
		nameElement.setTextContent(name);
		pair.appendChild(nameElement);

		Element valueElement = document.createElement("value");
		valueElement.setTextContent(value);
		pair.appendChild(valueElement);

	}

	public static boolean isPair(Element pair, String name) {
		if(!pair.getNodeName().equals("pair")) {
			return false;
		}

		Element nameElement = null;
		NodeList kvList = pair.getChildNodes();
		for(int j = 0; j < kvList.getLength(); ++j) {
			Node keyOrValue = kvList.item(j);

			if(keyOrValue.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			/* Find the "name" */
			if(keyOrValue.getNodeName().equals("name")) {
				nameElement = (Element) keyOrValue;
				break;
			}
		}

		/* No name element, don't do anything. */
		if(nameElement == null) {
			return false;
		}

		return nameElement.getTextContent().equals(name);
	}

	public static void addOrUpdateOntology(Node ontologies, String owlFile, boolean library) {
		NodeList l = ontologies.getChildNodes();
		boolean found = false;
		for(int i = 0; i < l.getLength(); ++i) {
			Node node = l.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element ontology = (Element) l.item(i);
			/* Ignore any non-ontology nodes, we don't care about invalid stuff
			 * that's not ours. */
			if(!ontology.getNodeName().equals("ontology")) {
				continue;
			}

			String filename = ontology.getAttribute("filename");

			/* If we exist, double check our library value is correct and break. */
			if(filename.equals(owlFile)) {
				ontology.setAttribute("library", library ? "true" : "false");
				found = true;
				break;
			}
		}

		/* If we're already there, just return. */
		if(found) {
			return;
		}

		/* Otherwise, add it! */
		Element newOntology = ontologies.getOwnerDocument().createElement("ontology");

		newOntology.setAttribute("filename", owlFile);
		newOntology.setAttribute("library", library ? "true" : "false");
		ontologies.appendChild(newOntology);
	}

	public static Node findOrCreateNode(Node root, String nodeName) {
		NodeList l = root.getChildNodes();

		Node node = null;
		for(int i = 0; i < l.getLength(); ++i) {
			Node n = l.item(i);
			if(n.getNodeName().equals(nodeName)) {
				node = n;
				break;
			}
		}

		if(node == null) {
			if(root instanceof Document) {
				node = ((Document) root).createElement(nodeName);
			} else {
				node = root.getOwnerDocument().createElement(nodeName);
			}

			root.appendChild(node);
		}

		return node;
	}

	public static void buildActorIconMappings(Document document, List<ActorDefinition> actors) {
		Node root = findOrCreateNode(document, "config");

		for(ActorDefinition actor : actors) {
			addOrUpdatePair(root, actor.klass, actor.icon);
		}
	}

	public static void dumpXML(Document document, OutputStream out) throws TransformerConfigurationException, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		tf.setAttribute("indent-number", 4);

		Transformer serializer = tf.newTransformer();
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		serializer.transform(new DOMSource(document), new StreamResult(out));
	}
}
