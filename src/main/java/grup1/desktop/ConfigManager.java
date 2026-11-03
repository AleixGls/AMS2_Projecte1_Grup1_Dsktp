
package grup1.desktop;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class ConfigManager { // Clase para gestionar CONFIG.XML

    // Clase para almacenar los datos recuperados de CONFIG.XML
    public record Config(String ubicacion, String servidor) {
    }

    // Leer los datos guardados en CONFIG.XML
    public static Config readConfig() {

        File configFile = new File("CONFIG.XML");

        // Comprobar si existe el archivo de configuración
        if (!configFile.exists()) {
            return null;
        }

        try {

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(configFile);

            // Obtener los campos de ubicación y servidor
            NodeList ubicaciones = doc.getElementsByTagName("ubicacion");
            NodeList servidores = doc.getElementsByTagName("servidor");

            // Comprobar que existen los dos campos
            if (ubicaciones.getLength() == 0 || servidores.getLength() == 0) {
                return null;
            }

            // Recuperar los valores almacenados
            String ubicacion = ubicaciones.item(0).getTextContent().trim();
            String servidor = servidores.item(0).getTextContent().trim();

            return new Config(ubicacion, servidor);

        } catch (Exception e) {

            System.err.println("Error leyendo CONFIG.XML: " + e.getMessage());
            return null;
        }
    }

    // Guardar o actualizar los datos de CONFIG.XML
    public static void updateConfigXML(String ubicacion, String servidor) {

        try {

            // Crear un nuevo documento XML
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .newDocument();

            // Crear el elemento raíz config
            Element elmRoot = doc.createElement("config");
            doc.appendChild(elmRoot);

            // Crear el elemento ubicacion
            Element elmUbicacion = doc.createElement("ubicacion");
            elmUbicacion.setTextContent(ubicacion);
            elmRoot.appendChild(elmUbicacion);

            // Crear el elemento servidor
            Element elmServidor = doc.createElement("servidor");
            elmServidor.setTextContent(servidor);
            elmRoot.appendChild(elmServidor);

            // Guardar el documento XML en CONFIG.XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();

            var transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            transformer.transform(
                    new DOMSource(doc),
                    new StreamResult(new File("CONFIG.XML"))
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "No se pudo guardar CONFIG.XML", e
            );
        }
    }
}
