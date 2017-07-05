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
import java.util.regex.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Brissy Maxence & Duplessis Vincent
 */
public class UppaalParser {
        
    @SuppressWarnings("empty-statement")
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Please enter a filename and a destination folder.");
            return;
	}
        
	try {         
            Runtime rt = Runtime.getRuntime();
            String [] tab = {"/bin/sh", "-c", "mkdir "+args[1]};
            rt.exec(tab);
            
            String [] tab2 = {"/bin/sh", "-c", "mkdir "+args[1]+"/SocketServerPackage"};
            rt.exec(tab2);
            
            new JavaParser().parseFile(args[0], args[1]+"/SocketServerPackage");
            new LARVAParser().parseFile(args[0], args[1]);
            
            String [] tab3 = {"/bin/sh", "-c", "cp ./Makefile "+args[1]+"/"};
            rt.exec(tab3);
            
	} catch (IOException e) {
            System.err.println("Problem with the file.");
            System.err.println(e);
	} catch (ParserConfigurationException | SAXException e) {
            System.err.println("Error while reading XML file.");
	}
        
     }
}
