package io.bdrc.xmltoldmigration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class OutlineMigration {

	private static final String OP = CommonMigration.OUTLINE_PREFIX;
	private static final String WP = CommonMigration.WORK_PREFIX;
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
	private static final String OXSDNS = "http://www.tbrc.org/models/outline#";

	public static Map<String,Boolean> ridsToIgnore = new HashMap<>();
	static {
        ridsToIgnore.put("O2MS4765", true);
        ridsToIgnore.put("O2MS5129", true);
        ridsToIgnore.put("O1TLMXXX000011", true);
        ridsToIgnore.put("O1TLMXXX000012", true);
        ridsToIgnore.put("O3JW10074", true);
        ridsToIgnore.put("O3JW11025", true);
        ridsToIgnore.put("O3JW11874", true);
        ridsToIgnore.put("O3JW13595", true);
        ridsToIgnore.put("O3JW14444", true);
        ridsToIgnore.put("O3JW15385", true);
        ridsToIgnore.put("O3JW18061", true);
        ridsToIgnore.put("O3JW18930", true);
        ridsToIgnore.put("O3JW19779", true);
        ridsToIgnore.put("O4CTX297", true);
        ridsToIgnore.put("O3JW17161", true);
        ridsToIgnore.put("O2MS4381", true);
        ridsToIgnore.put("O4JW33589", true);
        ridsToIgnore.put("O3JW5309", true);
        ridsToIgnore.put("O5TAX003", true);
        ridsToIgnore.put("OTX2", true);
        ridsToIgnore.put("OTX5", true);
        ridsToIgnore.put("O4CTX325", true);
        ridsToIgnore.put("O4CTX313", true);
        ridsToIgnore.put("O4JW296", true);
        ridsToIgnore.put("O4JW313", true);
        ridsToIgnore.put("O4CTX298", true);
        ridsToIgnore.put("O4JW33649", true);
        ridsToIgnore.put("O10MS13722", true);
        ridsToIgnore.put("O2MS24613", true);
        ridsToIgnore.put("O1", true);
	    ridsToIgnore.put("O5JW1123", true);
	    ridsToIgnore.put("O5JW1071", true);
	    ridsToIgnore.put("O9TAXTBRC201605", true);
	    ridsToIgnore.put("O3JW16234", true);
	    ridsToIgnore.put("O3JW8867", true);
	    ridsToIgnore.put("O4JW33751", true);
	    ridsToIgnore.put("O4CTX296", true);
	    ridsToIgnore.put("O9TAXTBRC201605S", true);
	    ridsToIgnore.put("O9TAXTBRC201602", true);
	    ridsToIgnore.put("O9TAXTBRC201605DLD", true);
	    ridsToIgnore.put("OTX3", true);
	    ridsToIgnore.put("O9TAXTBRC201604", true);
	    ridsToIgnore.put("O5JW1109", true);
	    ridsToIgnore.put("O1HU51", true);
	    ridsToIgnore.put("O3JW20628", true);
	    ridsToIgnore.put("O4JW33653", true);
	    ridsToIgnore.put("O3JW7994", true);
        ridsToIgnore.put("O5TAX004", true);
        ridsToIgnore.put("O4JW33840", true);
        ridsToIgnore.put("OTX4", true);
        ridsToIgnore.put("O5JW18", true);
        ridsToIgnore.put("O10MS19652", true);
        ridsToIgnore.put("OTX1", true);
        ridsToIgnore.put("O3JW12746", true);
        ridsToIgnore.put("O5TAX006", true);
        ridsToIgnore.put("O4JW33844", true);
        ridsToIgnore.put("O4JW33784", true);
        ridsToIgnore.put("O4JW33827", true);
        ridsToIgnore.put("O5TAX005", true);
        ridsToIgnore.put("O5TAX002", true);
        ridsToIgnore.put("O5TAX007", true);
        ridsToIgnore.put("O5TAX007", true);
        ridsToIgnore.put("O5TAX001", true);
        ridsToIgnore.put("O5TAX008", true);
        ridsToIgnore.put("O4JW5431", true);
	}
	
	static class CurNodeInt{
	    public int i = 0;
	}
	
	// TODO: ignore outlines with type enumeration and taxonomy
	public static Model MigrateOutline(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Resource main = m.createResource(OP + root.getAttribute("RID"));
		CommonMigration.addStatus(m, main, root.getAttribute("status"));

		CurNodeInt curNodeInt = new CurNodeInt();
		curNodeInt.i = 0;
		
		// fetch type in isOutlineOf
		NodeList nodeList = root.getElementsByTagNameNS(OXSDNS, "isOutlineOf");
        String value = null;
        String workId = "";
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            
            value = current.getAttribute("type").trim();
            if (value.isEmpty()) {
                value = "NoType";
            }
            value = CommonMigration.normalizePropName(value, null);
            m.add(main, RDF.type, m.createResource(OP + value));
            if (!value.equals("Outline"))
                m.add(main, RDF.type, m.createResource(OP + "Outline"));
            
            value = current.getAttribute("work").trim();
            if (!value.isEmpty()) {
                m.add(main, m.getProperty(OP+"isOutlineOf"), m.createProperty(WP+value));
            } else {
            	CommonMigration.addException(m, main, "outline does not reference the corresponding work");
            }
            workId = value;
        }
        
        value = root.getAttribute("pagination").trim();
        if (value.isEmpty()) value = "relative";
        m.add(main, m.getProperty(OP+"pagination"), m.createLiteral(value));
        
		CommonMigration.addNotes(m, root, main, OXSDNS);
		CommonMigration.addExternals(m, root, main, OXSDNS);
		CommonMigration.addLog(m, root, main, OXSDNS);
		CommonMigration.addDescriptions(m, root, main, OXSDNS);
		CommonMigration.addLocations(m, main, root, OXSDNS, workId);
		
		addCreators(m, main, root);
		
		addNodes(m, main, root, workId, curNodeInt);
		
		return m;
	}
	
	public static void addCreators(Model m, Resource r, Element e) {
        // creator
        // originally supposed to be a wrk:creator property, but unlike the latter, it
        // never maps to a per:Person, because ontologies are BDRC staff and not per:Persons
        // so a new out:authorship data property has been created to capture the textcontent
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "creator");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            Property prop = m.createProperty(ADM+"outlineAuthorStatement");
            Literal l = CommonMigration.getLiteral(current, "en", m, "catalogInfo", r.getLocalName(), null);
            if (l == null) continue;
            r.addProperty(prop,  l);
        }
	}
	
	public static CommonMigration.LocationVolPage addNode(Model m, Resource r, Element e, int i, String workId, CurNodeInt curNode, CommonMigration.
LocationVolPage previousEndLocVP) {
	    curNode.i = curNode.i+1;
	    String value = String.format("%04d", curNode.i);	    
        Resource node = m.createResource(value);
        String RID = e.getAttribute("RID").trim();
        if (!value.isEmpty()) {
            node.addProperty(m.getProperty(ADM, "workOldOutlineRID"), RID);
        }
        value = e.getAttribute("type");
        if (value.isEmpty()) {
            value = "Node";// TODO: ?
        }
        value = CommonMigration.normalizePropName(value, "Class");
        m.add(node, RDF.type, m.getResource(BDO+"Outline"));
        //m.add(node, RDF.type, m.getResource(OP+value));
        m.add(r, m.getProperty(OP+"hasNode"), node);
        
        value = e.getAttribute("parent").trim();
        if (!value.isEmpty())
            m.add(r, m.getProperty(BDO, "workPartOf"), m.createResource(BDR+value));
        
        m.add(r, m.getProperty(BDO, "workHasPart"), node);
        
        CommonMigration.addNames(m, e, node, OXSDNS, true);
        CommonMigration.addDescriptions(m, e, node, OXSDNS);
        CommonMigration.addTitles(m, node, e, OXSDNS, false);
        
        CommonMigration.LocationVolPage locVP =
                CommonMigration.addLocations(m, node, e, OXSDNS, workId);
        
        // check if outlines cross
        if (locVP != null && previousEndLocVP != null) {
            if (previousEndLocVP.endVolNum > locVP.beginVolNum
                    || (previousEndLocVP.endVolNum == locVP.beginVolNum && previousEndLocVP.endPageNum > locVP.beginPageNum)) {
                ExceptionHelper.logException(ExceptionHelper.ET_OUTLINE, workId, RID, "starts (v."+locVP.beginVolNum+", p. "+locVP.beginPageNum+") before the end of previous node ["+
                        previousEndLocVP.RID+"](https://www.tbrc.org/#!rid="+RID+"|"+workId+") (v."+previousEndLocVP.endVolNum+", p. "+previousEndLocVP.endPageNum+")\n");
            }
        }
        
        CommonMigration.addSubjects(m, node, e, OXSDNS);
        
        List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "site");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            
            //value = CommonMigration.getSubResourceName(node, OP, "Site");
            Resource site = m.createResource();
            value = e.getAttribute("type");
            if (value.isEmpty()) 
                value = "Site";// TODO: ?
            else {
                value = "WorkSiteType" + value.substring(0, 1).toUpperCase() + value.substring(1);
                m.add(site, m.getProperty(BDO, "workSiteType"), m.getResource(BDR+value));
            }
            m.add(node, m.getProperty(BDO, "workHasSite"), site);
            
            addSimpleAttr(current.getAttribute("circa"), BDO+"onOrAbout", m, site);
            
            value = current.getAttribute("place").trim();
            if (!value.isEmpty())
                m.add(site, m.getProperty(BDO, "workSitePlace"), m.getResource(BDR+value));

            // TODO: what about current.getTextContent()?
        }
        
        addCreators(m, node, e);
        
        // sub nodes
        addNodes(m, node, e, workId, curNode);
        
        return locVP;
        
	}
	

	
	public static void addNodes(Model m, Resource r, Element e, String workId, CurNodeInt curNode) {
	    CommonMigration.LocationVolPage endLocVP = null;
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "node");
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            endLocVP = addNode(m, r, current, i, workId, curNode, endLocVP);
        }
	}

   public static void addSimpleAttr(String attrValue, String propUrl, Model m, Resource r) {
        attrValue = attrValue.trim();
        if (attrValue.isEmpty()) return;
        Property prop = m.getProperty(propUrl);
        m.add(r, prop, m.createLiteral(attrValue));
    }
	
}
