package io.bdrc.xmltoldmigration;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class ImagegroupMigration {

	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String VP = CommonMigration.VOLUMES_PREFIX;
	private static final String IGXSDNS = "http://www.tbrc.org/models/imagegroup#";

	public static Model MigrateImagegroup(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m);
        Resource volume = m.createResource(VP+"TestVolume");
        m.add(volume, RDF.type, m.getResource(VP+"Volume"));
        MigrateImagegroup(xmlDocument, m, volume);
        return m;
	}
	
	public static void MigrateImagegroup(Document xmlDocument, Model m, Resource main) {
		
		Element root = xmlDocument.getDocumentElement();
                
		CommonMigration.addDescriptions(m, root, main, IGXSDNS, false, false);
        CommonMigration.addLog(m, root, main, IGXSDNS);
        
        NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "images");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("intro").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(VP+"pages_intro"), m.createTypedLiteral(value, XSDDatatype.XSDnonNegativeInteger));
            
            value = current.getAttribute("tbrcintro").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(VP+"pages_tbrcintro"), m.createTypedLiteral(value, XSDDatatype.XSDnonNegativeInteger));
            
            value = current.getAttribute("text").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(VP+"pages_text"), m.createTypedLiteral(value, XSDDatatype.XSDnonNegativeInteger));
            
            value = current.getAttribute("total").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(VP+"pages_total"), m.createTypedLiteral(value, XSDDatatype.XSDnonNegativeInteger));
        }
        
        nodeList = root.getElementsByTagNameNS(IGXSDNS, "qc");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            addSimpleElement("qcperson", "qcperson", current, m, main);
            addSimpleElement("qcdate", "qcdate", current, m, main);
            addSimpleElement("qcnotes", "qcnotes", current, m, main);
        }

	}

	public static void addSimpleElement(String elementName, String propName, Element root, Model m, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, elementName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getTextContent().trim();
            if (value.isEmpty()) {
                return;
            }
            m.add(main, m.createProperty(VP+propName), m.createLiteral(value));
        }
    }
	
}
