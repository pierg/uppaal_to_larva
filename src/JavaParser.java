
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class JavaParser {

    final private String _startParsing;
    final private String _endParsing;
    final private String _startSimulationValues;
    final private String _endSimulationValues;
    private ArrayList<String> _doubles;
    final private String _startDoublesDeclaration;
    final private String _endDoublesDeclaration;
    final private String _startEnvironmentValues;
    final private String _endEnvironmentValues;
    final private String _startFunctions;
    final private String _endFunctions;
    
    JavaParser(){
        _startParsing = "\\/\\*start parsing\\*\\/";
        _endParsing = "\\/\\*end parsing\\*\\/";
         _startSimulationValues = "\\/\\*start simulation values\\*\\/";
        _endSimulationValues = "\\/\\*end simulation values\\*\\/";
        _startDoublesDeclaration = "\\/\\*start doubles declaration\\*\\/";
        _endDoublesDeclaration = "\\/\\*end doubles declaration\\*\\/";
        _doubles = new ArrayList<>();
        _startEnvironmentValues = "\\/\\*start environment values\\*\\/";
        _endEnvironmentValues = "\\/\\*end environment values\\*\\/";
        _startFunctions = "\\/\\*start functions\\*\\/";
        _endFunctions = "\\/\\*end functions\\*\\/";
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
    
    private Document getXMLDocument(String filename) throws ParserConfigurationException, SAXException, IOException {
        File xmlFile = new File(filename);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(readFile(xmlFile)));
        
        return builder.parse(inputSource);
    }
    
    
    private int getPropertiesNumber(Document document){
        Element root = document.getDocumentElement();
        
        NodeList nl = root.getElementsByTagName("template");

        int propertiesNumber = nl.getLength();
        
        for(int i=0; i<nl.getLength();i++){
            Element template = (Element) (root.getElementsByTagName("template").item(i));
            if(template.getElementsByTagName("name").item(0).getTextContent().startsWith("Sim_")){
               propertiesNumber--; 
            }
        }
        
        return propertiesNumber;
    }
    
    private void getDoubles(String inputString){
        Pattern parsingPattern = Pattern.compile("(?="+_startEnvironmentValues+")(?s).*?("+_endEnvironmentValues+")");
        Matcher parsingMatcher = parsingPattern.matcher(inputString);
        
        while(parsingMatcher.find()){
            String stringToParse = parsingMatcher.group();
            Pattern p = Pattern.compile("int .*");
            Matcher m = p.matcher(stringToParse);
            while(m.find()){
                String[] tab = m.group().split(" |;|\\[");
                _doubles.add(tab[1]);
            }
        }
    }
    
    /**
     * Delete all the size of the array for java
     * @param string the input string
     * @return the string without the array sizes
     */
    private String convertArray(String string){
        Pattern arrayPattern = Pattern.compile("int .*\\[\\d+\\];");
        Matcher arrayMatcher = arrayPattern.matcher(string);
        
        while(arrayMatcher.find()){
            String matchedString = arrayMatcher.group();
            matchedString = matchedString.replaceFirst("\\[\\d+\\]", "\\[\\]");
            string = string.replaceFirst("int .*\\[\\d+\\];",matchedString);
        }
        
        return string;
    }
    
    /**
     * Return the factor needed to divide all the integers from the document to convert them in doubles
     * @param document we extract the factor from
     * @return the factor
     */
    private int getFactor(Document document){
        Element root = document.getDocumentElement();
        Element globalDeclaration = (Element) (root.getElementsByTagName("declaration")).item(0);
        
        String code = globalDeclaration.getTextContent();
        
        Pattern p = Pattern.compile("int factor=[0-9]{1,}");
        Matcher m = p.matcher(code);
        if(m.find()){
            String[] strings = m.group().split("=|;");
            
            return Integer.parseInt(strings[1]);
        }else{
            return 1;
        }
    }
    
    /**
     * Divide all the integers of a string by a factor and convert them in doubles
     * @param factor the factor we divide the integers with
     * @param nonConvertedString the string we want to convert
     * @return the string converted
     */
    private String convertAllIntegersInDoubles(int factor, String nonConvertedString){
        String convertedString="";
        
        Pattern intPattern = Pattern.compile("[0-9]+");
        Matcher intMatcher = intPattern.matcher(nonConvertedString);
        if(intMatcher.find()){
            int intResult = Integer.parseInt(intMatcher.group());
            nonConvertedString = nonConvertedString.replaceAll("((?<!\\[)(?<!\\.))\\d+(?!\\])(?!\\.)", Double.toString((double)intResult/factor));
        }
        convertedString+=nonConvertedString;

        return convertedString;
    }
    
    private String convertIntegersInDoubles(int factor, String inputString){
        String outputString = "";
       
        try (BufferedReader br = new BufferedReader(new StringReader(inputString))) {

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                boolean bool = false;
                String[] tab = sCurrentLine.split(" |;|\\[|\\]|=|\\)|\\(");
                for(int i=0;i<tab.length;i++){
                    if(_doubles.contains(tab[i])){
                        bool = true;
                    }
                }
                if(bool){
                    outputString += convertAllIntegersInDoubles(factor, sCurrentLine) + "\n";
                }else{
                    outputString += sCurrentLine + "\n";
                }
            }
        } catch (IOException e) {
            System.err.println("Exception in readFile method : "+e);
        }
        
        //System.out.println(outputString);
        
        return outputString;
    }
    
    private String modifyDeclarations(String inputString, String type){
        
        Pattern p = Pattern.compile(type+" \\w+(?=\\()");
        Matcher m = p.matcher(inputString);
        
        while(m.find()){
            String s = m.group();
            if(type.equals("int")){
                s = s.replaceFirst("int", "double");
            }
            inputString = inputString.replaceAll("(?<!public static )"+m.group(),"public static "+s);
        }
        
        return inputString;
    }
    
    private String fonction(String inputString){
        String outputString = "";
        
        Pattern parsingPattern = Pattern.compile("(?="+_startDoublesDeclaration+")(?s).*?("+_endDoublesDeclaration+")");
        Matcher parsingMatcher = parsingPattern.matcher(inputString);
        
        while(parsingMatcher.find()){
            outputString = parsingMatcher.group().replaceAll("int","public static double");
        }
        
        inputString = inputString.replaceAll("(?="+_startDoublesDeclaration+")(?s).*?("+_endDoublesDeclaration+")",outputString);
        
        return inputString;
    }
    
    
    private String getUppaalCode(Document document) {        
	Element root = document.getDocumentElement();
	
        Element globalDeclaration = (Element) (root.getElementsByTagName("declaration").item(0));
        
        NodeList nl = root.getElementsByTagName("template");
        String localDeclarations = "";
         
        for(int i=0; i<nl.getLength(); i++){
            Element template = (Element) (root.getElementsByTagName("template").item(i));
            Element localDeclaration = (Element) (template.getElementsByTagName("declaration")).item(0);
            if(!template.getElementsByTagName("name").item(0).getTextContent().startsWith("Sim_")){
                localDeclarations += localDeclaration.getTextContent();
            }
        }
        
        int factor = getFactor(document);
        
        String code = "int propertiesNumber = "+getPropertiesNumber(document)+";\n"+ globalDeclaration.getTextContent() + localDeclarations;
        
        
        getDoubles(code);
        code = convertIntegersInDoubles(factor,code);

        code = convertArray(code);
        
        Pattern p = Pattern.compile("(?="+_startFunctions+")(?s).*?("+_endFunctions+")");
        Matcher m = p.matcher(code);
        String fonctions = "";
        
        while(m.find()){
            fonctions += m.group();
        }
        
        code = code.replaceAll(_startFunctions+"(?s).*?"+_endFunctions, "");
        
        code = code.replaceAll("int", "public static double");
        
        //code = fonction(code);
        
	code = code.replaceAll("\\[(.*?,.*?)\\]", "");
        
        code = code.replaceAll(_startSimulationValues+"(?s).*"+_endSimulationValues, "");
        
        code += fonctions;
        
        code = modifyDeclarations(code,"int");
        code = modifyDeclarations(code, "void");
        code = modifyDeclarations(code, "bool");
        
        code = code.replaceAll("bool","boolean");
        
        return code;
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
    
    private String mergeCodes(String template, String upaallCode, String replacedPattern) {
	return template.replaceAll(replacedPattern, upaallCode);
    }
    
    private String getJavaDocument(Document document) {
	String code = getUppaalCode(document);
	String javaTemplate = getTemplate("java_template");

	return mergeCodes(javaTemplate, code, "<<<UPPAAL_CODE>>>");
    }
    
    public void parseFile(String filename, String destination) throws IOException, ParserConfigurationException, SAXException {
	Document document = getXMLDocument(filename);

	String javaDocument = getJavaDocument(document);
		
        List<String> javaList = Arrays.asList(javaDocument.split("\\n"));
	
        Path javaPath = Paths.get(destination+".java");
        
        Files.write(javaPath, javaList, Charset.forName("UTF-8"));
    }
}
