package grup1.desktop;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class ConfigManager { //Clase para generar CONFIG.XML
    


    public static void createConfigXML() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document docConfigXML = dBuilder.newDocument();

            Element elmRoot = docConfigXML.createElement("config");
            docConfigXML.appendChild(elmRoot); //Metemos el elemento raiz config

            Element elmUbicacion = docConfigXML.createElement("ubicacion"); //Ponemos el resto de campos dentro del root 
            Element elmServidor = docConfigXML.createElement("servidor");
            elmRoot.appendChild(elmUbicacion);
            elmRoot.appendChild(elmServidor);



        } catch (ParserConfigurationException p) {
            // No es pot crear el parser: és un error de programació
            throw new IllegalStateException(p);
        }
    }
}
