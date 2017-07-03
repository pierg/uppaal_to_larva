
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
     * Read an XML code from a text file and delete the DOCTYPE tag because of bugs 
     * @param inputFile the file we want to extract the XML code from
     * @return the XML string without the DOCTYPE tag
     */
    private String readFile(File inputFile){
        String outputString = "";
        
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                outputString += sCurrentLine+"\n";
            }
        } catch (IOException e) {
            System.err.println("Exception in readFile method : "+e);
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
            String line = null;
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
    
    private List<State> getStates(Document document) {
	Element root = document.getDocumentElement();
	NodeList locations = root.getElementsByTagName("location");

	List<State> states = new ArrayList<>();

	for (int i = 0; i < locations.getLength(); i++) {
            Element location = (Element) (locations.item(i));
            String state = location.getTextContent();
            states.add(new State(location.getAttribute("id"), state));
	}

	return states;
    }
    
    private List<Transition> getTransitions(Document document, List<State> states) {
	Element root = document.getDocumentElement();
	NodeList transitionNodes = root.getElementsByTagName("transition");

	List<Transition> transitions = new ArrayList<>();

	for (int i = 0; i < transitionNodes.getLength(); i++) {
            Element transitionNode = (Element) (transitionNodes.item(i));
            String idFrom = ((Element) (transitionNode.getElementsByTagName("source").item(0))).getAttribute("ref");
            String idTo = ((Element) (transitionNode.getElementsByTagName("target").item(0))).getAttribute("ref");
            NodeList labels = transitionNode.getElementsByTagName("label");
            transitions.add(getTransitionFromNode(states, i, idFrom, idTo, labels));
        }

	return transitions;
    }

    private Transition getTransitionFromNode(List<State> states, int i, String idFrom, String idTo, NodeList labels) {
	State from = new State();
	State to = new State();
	String guard = new String();
	String assignment = new String();
	for (int j = 0; j < labels.getLength(); j++) {
            Element label = (Element) (labels.item(j));
            if (label.getAttribute("kind").equals("guard")) {
		guard = label.getTextContent();
            } else if (label.getAttribute("kind").equals("assignment")) {
                assignment = label.getTextContent();
            }
        }

	for (State state : states) {
            if (state._id.equals(idFrom)) {
                from = state;
            }

            if (state._id.equals(idTo)) {
		to = state;
		}
	}
	return new Transition(from, to, guard, assignment);
    }
    
    private String getStateDeclaration(List<State> states) {
	String stateDeclaration = "";
	for (State state : states) {
		if (!state._name.equals("start")) {
        		stateDeclaration += state + " ";
		}
	}

        return stateDeclaration;
    }        
    

    private String getResetTransitions(List<State> states) {
	String statement = "->start[reset()\\EchoServer.reward = 0;EchoServer.response();]\n";
	String resetTransition = "start" + statement;

        for (State state : states) {
            resetTransition += "\t\t\t" + state + statement;
	}

	return resetTransition;
    }
    
    private String getTransitionsCode(List<Transition> transitions) {
	String transitionsCode = "";
	for (Transition tr : transitions) {
		transitionsCode += "\t\t\t" + tr._from + "->" + tr._to;
			
		if(!tr._guard.equals("") && !tr._guard.equals("none")){
			transitionsCode += "[rlevent\\EchoServer." + tr._guard;
		}else{
                    transitionsCode += "[rlevent\\";
		}
			
		if(!tr._assignment.equals("") && !tr._assignment.equals("none")){
                    transitionsCode += "\\EchoServer." + tr._assignment + ";EchoServer.response();]\n";
		}else{  
                    transitionsCode += "\\EchoServer.response();]\n";
		}
			
	}

        return transitionsCode;
    }
    
    private String mergeCodes(String template, String upaallCode, String replacedPattern) {
	return template.replaceAll(replacedPattern, upaallCode);
    }
    
    private String getLarvaDocument(Document document) {
	String larvaTemplate = getTemplate("larva_template");

	List<State> states = getStates(document);
	List<Transition> transitions = getTransitions(document, states);

	String stateDeclaration = getStateDeclaration(states);

	String transitionsCode = getResetTransitions(states) + getTransitionsCode(transitions);

	String larvaDocument = mergeCodes(larvaTemplate, stateDeclaration, "<<<UPPAAL_STATES>>>");
	larvaDocument = mergeCodes(larvaDocument, transitionsCode, "<<<UPPAAL_TRANSITIONS>>>");

        return larvaDocument;
    }
    
    public void parseFile(String filename,String destination) throws IOException, ParserConfigurationException, SAXException {
	Document document = getXMLDocument(filename);

	String larvaDocument = getLarvaDocument(document);
		
        List<String> larvaList = Arrays.asList(larvaDocument.split("\\n"));
	
        Path larvaPath = Paths.get(destination+".lrv");
        
        Files.write(larvaPath, larvaList, Charset.forName("UTF-8"));
    }
}
