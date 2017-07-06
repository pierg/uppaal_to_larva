import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

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
            String [] mkdirRoot = {"/bin/sh", "-c", "mkdir "+args[1]};
            rt.exec(mkdirRoot);
            
            String [] mkdirJavaFolder = {"/bin/sh", "-c", "mkdir "+args[1]+"/SocketServerPackage"};
            rt.exec(mkdirJavaFolder);
               
            
            List<UppaalTemplate> templates = new LARVAParser().parseFile(args[0], args[1]);
            new JavaParser(templates).parseFile(args[0], args[1]+"/SocketServerPackage");
            
            String [] cpMakefile = {"/bin/sh", "-c", "cp ./Makefile "+args[1]+"/"};
            rt.exec(cpMakefile);
            
	} catch (IOException e) {
            System.err.println("Problem with the file.");
            System.err.println(e);
	} catch (ParserConfigurationException | SAXException e) {
            System.err.println("Error while reading XML file.");
	}
        
     }
}
