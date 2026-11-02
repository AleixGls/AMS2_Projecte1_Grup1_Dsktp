package grup1.desktop;


import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

import org.w3c.dom.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class ConfigManager { //Clase para generar CONFIG.XML
    
    public static void createConfigXML(String ubicacion, String servidor) {
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

            Text nodeUbicacion = docConfigXML.createTextNode(ubicacion);
            Text nodeServidor = docConfigXML.createTextNode(servidor);

            elmUbicacion.appendChild(nodeUbicacion); //Les damos valor a los campos ubicacion y servidor
            elmServidor.appendChild(nodeServidor);



            File configFile = new File("CONFIG.XML");

            //Fabrica de transformadores 
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            // Creamos un transformador
            Transformer transformer = transformerFactory.newTransformer();
            //Creamos una instancia DOMSource a partir del documento XML
            DOMSource source = new DOMSource(docConfigXML);

            StreamResult result = new StreamResult(configFile);
            //Transoforma el documento XML especificado por source y escribe el documento XML resultante al objeto especificado por result
            transformer.transform(source, result);


        } catch (ParserConfigurationException p) {
            // No es pot crear el parser: és un error de programació
            throw new IllegalStateException(p);
        } catch (TransformerException t) {
            t.printStackTrace();
        }

    }

    public static void updateConfigXML(String ubicacion, String servidor) {
        File configFile = new File("CONFIG.XML");
        if (configFile.exists()) { //Si ya existe, actualizamos los campos
            
            try { 
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

                Document doc = dBuilder.parse(configFile);

                doc.getElementsByTagName("ubicacion").item(0).setTextContent(ubicacion);
                doc.getElementsByTagName("servidor").item(0).setTextContent(servidor); //Modificamos los campos a los nuevos introducidos


                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(configFile);
                transformer.transform(source, result);
                

            } catch (ParserConfigurationException p) {
                // No es pot crear el parser: és un error de programació
                throw new IllegalStateException(p);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException s) {
                s.printStackTrace();
            } catch (TransformerException t) {
                t.printStackTrace();
                return ; //Al fallar el guardado del archivo, no continua 
            }



            
        } else { //Si el xml no existe, lo creamos 
            createConfigXML(ubicacion, servidor);
        }
    }

}
