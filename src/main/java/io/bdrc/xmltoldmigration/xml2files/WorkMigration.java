package io.bdrc.xmltoldmigration.xml2files;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;


public class WorkMigration {

    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
	private static final String WXSDNS = "http://www.tbrc.org/models/work#";
	
	public static boolean splitItems = true;
	
	public static boolean addItemForWork = false;
	public static boolean addWorkHasItem = true;
	
	public static Map<String,List<String>> productWorks = new HashMap<>();

	private static String getUriFromTypeSubtype(String type, String subtype) {
	    switch (type) {
	    case "creator":
	        if (subtype.startsWith("has"))
	            subtype = subtype.substring(3);
	        return BDO+"creator"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
	    default:
	        return "";
	    }
	}
	    
	// testing only
    public static Model MigrateWork(Document xmlDocument) {
        Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m, "work");
        return MigrateWork(xmlDocument, m, new HashMap<>());
    }
	    
	public static Model MigrateWork(Document xmlDocument, Model m, Map<String,Model> itemModels) {
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(BDR + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(BDO + "Work"));
		
		CommonMigration.addStatus(m, main, root.getAttribute("status"));
		
		String value = null;
		Literal lit = null;
		Property prop;
		
		CommonMigration.addNotes(m, root, main, WXSDNS);
	    CommonMigration.addExternals(m, root, main, WXSDNS);
	    CommonMigration.addLog(m, root, main, WXSDNS);
		
	    CommonMigration.addTitles(m, main, root, WXSDNS, true);
	    CommonMigration.addSubjects(m, main, root, WXSDNS);
	    Map<String,Model> itemModelsFromDesc = CommonMigration.addDescriptions(m, root, main, WXSDNS);
	    if (itemModelsFromDesc != null) {
	        if (splitItems == false) {
	            for (Model itemModel : itemModelsFromDesc.values()) {
	                m.add(itemModel);
	            }
	        } else {
	            itemModels.putAll(itemModelsFromDesc);
	        }
	    }
		
		// archiveInfo
		
		NodeList nodeList = root.getElementsByTagNameNS(WXSDNS, "archiveInfo");
		boolean hasAccess = false;
		boolean hasLicense = false;
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("license").trim();
            if (!value.isEmpty()) {
                if (value.equals("ccby")) value = BDR+"WorkLicenseTypeCCBY";
                else value = BDR+"WorkLicenseTypeCopyrighted";
                m.add(main, m.getProperty(ADM+"license"), m.createResource(value));
                hasLicense = true;
            }
            
            value = current.getAttribute("access").trim();
            switch (value) {
            case "openAccess": value = "WorkAccessOpen"; break;
            case "fairUse": value = "WorkAccessFairUse"; break;
            case "restrictedSealed": value = "WorkAccessRestrictedSealed"; break;
            case "temporarilyRestricted": value = "WorkAccessTemporarilyRestricted"; break;
            case "restrictedByQuality": value = "WorkAccessRestrictedByQuality"; break;
            case "restrictedByTbrc": value = "WorkAccessRestrictedByTbrc"; break;
            case "restrictedInChina": value = "WorkAccessRestrictedInChina"; break;
            default: value = ""; break;
            }
            if (!value.isEmpty()) {
                m.add(main, m.getProperty(ADM, "access"), m.createResource(BDR+value));
                hasAccess = true;
            }

            try {
                int nbvols = Integer.parseUnsignedInt(current.getAttribute("vols").trim());
                if (nbvols != 0) {
                    prop = m.getProperty(BDO, "workNumberOfVolumes");
                    lit = m.createTypedLiteral(nbvols, XSDDatatype.XSDinteger);
                    m.add(main, prop, lit);
                }
            } catch (NumberFormatException e) {}
        }
        if (!hasAccess)
            m.add(main, m.getProperty(ADM, "workHasAccess"), m.createResource(BDR+"WorkAccessOpen"));
        
        if (!hasLicense)
            m.add(main, m.getProperty(ADM+"workLicense"), m.createResource(BDR+"WorkLicenseTypeCCBY"));

        // info
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "info");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            String nodeType = current.getAttribute("nodeType");
            switch (nodeType) {
            case "unicodeText": value = BDR+"WorkTypeUnicodeText";
            case "conceptualWork": value = BDR+"WorkTypeConceptualWork";
            case "publishedWork": value = BDR+"WorkTypePublishedWork";
            case "series": value = BDR+"WorkTypeSeries";
            default: value = "";
            }
            if (!value.isEmpty()) {
                main.addProperty(m.getProperty(BDO, "workType"), m.getResource(value));
            }
            boolean numbered = false;
            // will be overwritten when reading the pubinfo
            value = current.getAttribute("number");
            if (!value.isEmpty()) {
                main.addProperty(m.getProperty(BDO, "workSeriesNumber"), m.createLiteral(value));
                numbered = true;
            };
            value = current.getAttribute("numbered"); 
            if (!value.isEmpty()) {
                numbered = true;
            }
            if (numbered) {
                prop = m.getProperty(BDO, "workIsNumbered");
                m.add(main, prop, m.createTypedLiteral(true));
            }
            value = current.getAttribute("parent");
            if (!value.isEmpty() && !value.contains("LEGACY")) {
                if (numbered) {
                    SymetricNormalization.addSymetricProperty(m, "workNumberOf", main.getLocalName(), value, null);
                } else {
                    SymetricNormalization.addSymetricProperty(m, "workExpressionOf", main.getLocalName(), value, null);
                }
            }
        }
        
        // creator
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "creator");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("type").trim();
            if (value.isEmpty()) {
                value = "hasMainAuthor";
            }
            String person = current.getAttribute("person").trim(); // required
            if (person.isEmpty()) continue;
            if (person.equals("Add to DLMS")) {
                person = current.getTextContent().trim();
                if (!person.isEmpty())
                    ExceptionHelper.logException(ExceptionHelper.ET_MISSING, main.getLocalName(), main.getLocalName(), "creator", "needs to be added to dlms: `"+value+"`");
            } else {
                String uri = getUriFromTypeSubtype("creator", value);
                main.addProperty(m.getProperty(uri), m.createResource(BDR+person));
            }
                
        }
        
        // inProduct
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "inProduct");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("pid").trim();
            List<String> worksForProduct = productWorks.computeIfAbsent(value, x -> new ArrayList<String>());
            worksForProduct.add(main.getLocalName());
            //m.add(main, m.getProperty(ADM, "workInProduct"), m.createResource(BDR+value));
        }
        
        // catalogInfo
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "catalogInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            Literal l = CommonMigration.getLiteral(current, "en", m, "catalogInfo", main.getLocalName(), null);
            if (l == null) continue;
            main.addProperty(m.getProperty(BDO, "workCatalogInfo"), l);
        }
        
        // scanInfo
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "scanInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            Literal l = CommonMigration.getLiteral(current, "en", m, "scanInfo", main.getLocalName(), null);
            if (l == null) continue;
            main.addProperty(m.getProperty(BDO, "workScanInfo"), l);
        }
        
        Map<String,String> imageGroupList = getImageGroupList(xmlDocument);
        if (!imageGroupList.isEmpty()) {
            Resource item = m.createResource(BDR+"I"+root.getAttribute("RID").substring(1)+"_001");
            if (WorkMigration.addWorkHasItem)
                m.add(main, m.getProperty(BDO, "workHasItem"), item);
        }
        
        SymetricNormalization.insertMissingTriplesInModel(m, root.getAttribute("RID"));
        
		return m;
		
	}
	
	public static Map<String,String> getImageGroupList(Document d) {
	    Map<String,String> res = new HashMap<String,String>(); 
	    
	    Element root = d.getDocumentElement();
        NodeList volumes = root.getElementsByTagNameNS(WXSDNS, "volume");
        for (int j = 0; j < volumes.getLength(); j++) {
            Element volume = (Element) volumes.item(j);
            String value = volume.getAttribute("imagegroup").trim();
            if (value.isEmpty()) continue;
            if (!value.startsWith("I")) {
                System.err.println("Image group doesn't start with I! "+value);
                continue;
            }
            res.put(value, volume.getAttribute("num").trim());
        }
	    return res;
	}
	
}
