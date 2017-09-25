package io.bdrc.xmltoldmigration.xml2files;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EtextBodyMigration {

    public static boolean oneLongString = false;
    private static final XPath xPath = EtextMigration.initXpath();
    private static final String TEI_PREFIX = EtextMigration.TEI_PREFIX;
    public static final String BDR = CommonMigration.BDR;
    public static final String BDO = CommonMigration.BDO;
    public static final String ADM = CommonMigration.ADM;
    
    public static String normalizeString(final String src, final String page, final String lineNum) {
        String res = CommonMigration.normalizeTibetan(src);
        // I don't think we want non-breakable spaces, just normal spaces
        res = res.replace('\u00A0', ' ');
        //res = res.replaceAll("\\s+", " "); // not sure it's necessary
        // TODO: replace \r, \n, etc. with space ?
        return res;
    }
    
    public static void MigrateBody(Document d, OutputStream strOutput, Model m, String eTextId) {
        final PrintStream ps = new PrintStream(strOutput);
        Element body;
        Resource etextR = m.getResource(BDR+eTextId);
        try {
            body = (Element) ((NodeList)xPath.evaluate("/tei:TEI/tei:text/tei:body/tei:div",
                    d.getDocumentElement(), XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
            return;
        }
        final NodeList pars = body.getElementsByTagNameNS(TEI_PREFIX, "p");
        int currentTotalPoints = 0;
        final StringBuilder totalStr = new StringBuilder();
        final Map<Resource,int[]> resourcesCoords = new HashMap<Resource,int[]>();
        int pageBeginChar = 0;
        for (int i = 0; i < pars.getLength(); i++) {
            Element par = (Element) pars.item(i);
            String pageNum = par.getAttribute("n");
            Resource pageR = m.createResource();
            etextR.addProperty(m.createProperty(BDO, "eTextHasPage"), pageR);
            pageR.addProperty(m.createProperty(BDO, "seqNum"), m.createLiteral(pageNum)); // TODO: convert to int, but what about image ref?
            if (oneLongString) {
                pageR.addProperty(m.createProperty(BDO, "sliceStart"), m.createLiteral("1-"+currentTotalPoints));
            } else {
                pageBeginChar = currentTotalPoints;
            }
            final NodeList children = par.getChildNodes();
            int linenum = 0;
            for (int j = 0; j < children.getLength(); j++) {
              final Node child = children.item(j);
              if (child instanceof Element) {
                  final Element milestone = (Element) child;
                  try {
                      linenum = Integer.parseInt(milestone.getAttribute("n"));
                  } catch (NumberFormatException ex) {
                      System.out.println("cannot parse line number string: "+milestone.getAttribute("n"));
                  }
              } else {
                final String s = normalizeString(child.getTextContent(), Integer.toString(linenum), pageNum);
                Resource lineR = m.createResource();
                pageR.addProperty(m.createProperty(BDO, "pageHasLine"), lineR);
                lineR.addProperty(m.createProperty(BDO, "seqNum"), m.createTypedLiteral(linenum, XSDDatatype.XSDinteger));
                final int strLen = s.codePointCount(0, s.length()); 
                if (oneLongString) {
                    lineR.addProperty(m.createProperty(BDO, "sliceStart"), m.createLiteral("1-"+currentTotalPoints));
                    lineR.addProperty(m.createProperty(BDO, "sliceEnd"), m.createLiteral("1-"+currentTotalPoints+strLen));
                } else {
                    resourcesCoords.put(lineR, new int[] {currentTotalPoints, currentTotalPoints+strLen});
                }
                currentTotalPoints += strLen;
                
                // web annotations refer to code points, not UTF-16 code units:
                if (oneLongString)
                    ps.print(s);
                else
                    totalStr.append(s);
              }
           }
           if (oneLongString) {
               pageR.addProperty(m.createProperty(BDO, "sliceEnd"), m.createLiteral("1-"+currentTotalPoints));
           } else {
               resourcesCoords.put(pageR, new int[] {pageBeginChar, currentTotalPoints});
           }
        }
        // at that point the processing is done if we're in one long string mode
        if (!oneLongString) {
            chunkString(totalStr.toString(), resourcesCoords, ps, m, eTextId, currentTotalPoints);
        }
    }
    
    public static String translatePoint(List<Integer> pointBreaks, int pointIndex) {
        int curLine = 1;
        int toSubstract = 0;
        for (final int pointBreak : pointBreaks) {
            if (pointBreak > pointIndex) {
                break;
            }
            toSubstract = pointBreak;
            curLine += 1;
        }
        return curLine+"-"+(pointIndex-toSubstract);
    }
    
    public static void chunkString(final String totalStr, final Map<Resource,int[]> resourcesCoords, final PrintStream out, final Model m, final String eTextId, final int totalPoints) {
        final List<Integer>[] breaks = TibetanStringChunker.getAllBreakingCharsIndexes(totalStr);
        final List<Integer> charBreaks = breaks[0];
        final List<Integer> pointBreaks = breaks[1];
        int previousIndex = 0;
        //final int nbBreaks = charBreaks.size();
        for (final int charBreakIndex : charBreaks) { 
            out.print(totalStr.substring(previousIndex, charBreakIndex)+'\n');
            previousIndex = charBreakIndex+1;// because we don't typeset the space
        }
        if (previousIndex != totalStr.length()) {
            out.print(totalStr.substring(previousIndex));
        }
        final Property sliceStart = m.getProperty(BDO, "sliceStart");
        final Property sliceEnd = m.getProperty(BDO, "sliceEnd");
        for (final Entry<Resource,int[]> e : resourcesCoords.entrySet()) {
            final int[] oldSet = e.getValue();
            final String start = translatePoint(pointBreaks, oldSet[0]);
            final String end = translatePoint(pointBreaks, oldSet[1]);
            m.add(e.getKey(), sliceStart, m.createLiteral(start));
            m.add(e.getKey(), sliceEnd, m.createLiteral(end));
        }
    }
    


}