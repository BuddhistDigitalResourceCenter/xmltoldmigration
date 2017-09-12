package io.bdrc.xmltoldmigration.xml2files;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OfficeMigration {
	
	private static final String OXSDNS = "http://www.tbrc.org/models/office#";
	
	public static Model MigrateOffice(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m, "office");
		Element root = xmlDocument.getDocumentElement();
		Resource main = m.createResource(CommonMigration.BDR + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(CommonMigration.BDO + "Role"));
		CommonMigration.addStatus(m, main, root.getAttribute("status"));
		
		CommonMigration.addNotes(m, root, main, OXSDNS);
		
		CommonMigration.addExternals(m, root, main, OXSDNS);
		
		CommonMigration.addLog(m, root, main, OXSDNS);
		
		CommonMigration.addDescriptions(m, root, main, OXSDNS, true);
		
		return m;
	}

}