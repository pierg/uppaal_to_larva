import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
public class UppaalParser {
    final private String _filename;
    final private String _destination;

    public UppaalParser(String filename, String destination) {
	_filename = filename;
	_destination = destination;
    }

    public void parseFile() throws IOException, ParserConfigurationException, SAXException {
	Document document = getXMLDocument();

	String javaDocument = getJavaDocument(document);
	String larvaDocument = getLarvaDocument(document);
                
        System.out.println(javaDocument);
        System.out.println(larvaDocument);
		
		/*System.out.println(larvaDocument);
		List<String> list = Arrays.asList(javaDocument.split("\\n"));
		for(String s : list){
			System.out.println(s);
		}
		Path javaFile = Paths.get(_destination + "EchoServer.java");*/
		//Files.write(javaFile, list, Charset.forName("UTF-8"));

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

    private String getStateDeclaration(List<State> states) {
	String stateDeclaration = "";
	for (State state : states) {
		if (!state._name.equals("start")) {
        		stateDeclaration += state + " ";
		}
	}

        return stateDeclaration;
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

    private String getJavaDocument(Document document) {
	String code = getUppaalCode(document);
	String javaTemplate = getTemplate("java_template");

	return mergeCodes(javaTemplate, code, "<<<UPPAAL_CODE>>>");
    }

    private String getResetTransitions(List<State> states) {
	String statement = "->start[reset()\\EchoServer.reward = 0;EchoServer.response();]\n";
	String resetTransition = "start" + statement;

        for (State state : states) {
            resetTransition += "\t\t\t" + state + statement;
	}

	return resetTransition;
    }

    private Document getXMLDocument() throws ParserConfigurationException, SAXException, IOException {
        File xmlFile = new File(_filename);
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document document = (Document) dBuilder.parse(xmlFile);
	
        return document;
    }

    private String mergeCodes(String template, String upaallCode, String replacedPattern) {
	return template.replaceAll(replacedPattern, upaallCode);
    }

	/*
	 * Get the code of the declarations of variables and functions in Uppaal and
	 * transform it in Java
	 */
    private String getUppaalCode(Document document) {
	Element root = document.getDocumentElement();
	Element gobalDeclaration = (Element) (root.getElementsByTagName("declaration").item(0));
	Element template = (Element) (root.getElementsByTagName("template").item(0));
	Element localDeclaration = (Element) (template.getElementsByTagName("declaration").item(0));

	String code = gobalDeclaration.getTextContent() + localDeclaration.getTextContent();

	code = code.replaceAll("int", "public static int");
	code = code.replaceAll("double", "public static double");
	code = code.replaceAll("void", "public static void");
	code = code.replaceAll("bool", "public static bool");
	code = code.replaceAll("\\[(.*?,.*?)\\]", "");

	return code;
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

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Please enter a filename and a destination folder.");
            return;
	}

	try {
            new UppaalParser(args[0], args[1]).parseFile();
	} catch (IOException e) {
            System.err.println("Problem with the file.");
	} catch (ParserConfigurationException e) {
            System.err.println("Error while reading XML file.");
	} catch (SAXException e) {
            System.err.println("Error while reading XML file.");
	}
    }
}
