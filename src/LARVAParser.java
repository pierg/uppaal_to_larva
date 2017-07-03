
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Brissy Maxence & Duplessis Vincent
 */
public class LARVAParser {

    private Document getXMLDocument(String filename) throws ParserConfigurationException, SAXException, IOException {
        File xmlFile = new File(filename);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(readFile(xmlFile)));

        return builder.parse(inputSource);
    }

    /**
     * Read an XML code from a text file and delete the DOCTYPE tag because of
     * bugs
     *
     * @param inputFile the file we want to extract the XML code from
     * @return the XML string without the DOCTYPE tag
     */
    private String readFile(File inputFile) {
        String outputString = "";

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                outputString += sCurrentLine + "\n";
            }
        } catch (IOException e) {
            System.err.println("Exception in readFile method : " + e);
        }

        outputString = outputString.replaceFirst("<!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.1//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_2.dtd'>", "");

        return outputString;
    }

    /*
     * Get the content of the template file
     */
    private String getTemplate(String templateName) {
        BufferedReader reader;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(templateName));
            String line;
            String ls = System.getProperty("line.separator");

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Cannot find the JAVA template.");
        }
        return stringBuilder.toString();
    }

    private String mergeCodes(String template, String upaallCode, String replacedPattern) {
        return template.replaceAll(replacedPattern, upaallCode);
    }

    private List<UppaalTemplate> getUppaalTemplates(Document document) {
        List<UppaalTemplate> uppaalTemplates = new ArrayList<>();

        Element root = document.getDocumentElement();
        NodeList templates = root.getElementsByTagName("template");

        for (int i = 0; i < templates.getLength(); i++) {
            Element template = (Element) templates.item(i);

            Element name = (Element) (template.getElementsByTagName("name").item(0));
            
            if(name.getTextContent().matches("Sim_(.*)")){
                continue;
            }
            
            UppaalTemplate uppaalTemplate = new UppaalTemplate(name.getTextContent());
            uppaalTemplate.retrieveStates(template);
            uppaalTemplate.retrieveTransitions(template);
            uppaalTemplates.add(uppaalTemplate);
        }
        
        return uppaalTemplates;
    }

    private String getLarvaDocument(Document document) {
        String larvaTemplate = getTemplate("larva_template");
        String larvaPropertyTemplate = getTemplate("larva_property_template");
        String larvaProperties = new String();
        
        List<UppaalTemplate> uppaalTemplates = getUppaalTemplates(document);

        larvaProperties = uppaalTemplates.stream().map((UppaalTemplate uppaalTemplate) -> {
            String stateDeclaration = uppaalTemplate.getStateDeclaration();
            String transitionsCode = uppaalTemplate.getResetTransitions() + uppaalTemplate.getTransitionsCode() + uppaalTemplate.getDefaultTransitions();
            String larvaProperty;
            larvaProperty = mergeCodes(larvaPropertyTemplate, uppaalTemplate.getProperty(), "<<<UPPAAL_PROPERTY>>>");
            larvaProperty = mergeCodes(larvaProperty, stateDeclaration, "<<<UPPAAL_STATES>>>");
            larvaProperty = mergeCodes(larvaProperty, transitionsCode, "<<<UPPAAL_TRANSITIONS>>>");

            return larvaProperty;
        }).map((larvaProperty) -> larvaProperty).reduce(larvaProperties, String::concat);


        return mergeCodes(larvaTemplate, larvaProperties, "<<<UPPAAL_PROPERTIES>>>");
    }

    public void parseFile(String filename, String destination) throws IOException, ParserConfigurationException, SAXException {
        Document document = getXMLDocument(filename);

        String larvaDocument = getLarvaDocument(document);

        List<String> larvaList = Arrays.asList(larvaDocument.split("\\n"));

        Path larvaPath = Paths.get(destination + ".lrv");

        Files.write(larvaPath, larvaList, Charset.forName("UTF-8"));
    }
}
