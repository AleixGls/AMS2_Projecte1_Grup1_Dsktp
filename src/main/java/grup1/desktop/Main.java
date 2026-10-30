package grup1.desktop;


public class Main {

    public static CtrlConnexio ctrlConnexio;
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }

    public static void connectToServer() { //Metodo del cliente para conectarse al servidor 
        String location = ctrlConnexio.getComboBoxValue();
        String server = ctrlConnexio.getTextServidor();

        //String serverUrl = "ws://" + server + ":" + puerto;

        if (location.isEmpty() || server.isEmpty()) {
            System.out.println("Rellena los campos");
        }


    }
}