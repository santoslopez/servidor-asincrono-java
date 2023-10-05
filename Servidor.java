import java.net.Socket;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.BindException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.util.Arrays;

public class Servidor{

    //private static int cantidadPaquetesEnviar = 8092;
    private static int valorFixedThreadPool = 5;
    
    private static ContentType tipos = new ContentType();
    private static ExecutorService executor = Executors.newFixedThreadPool(valorFixedThreadPool); // Número máximo de hilos
    int PUERTO = 8080;
    private static byte[] fileData;
    public void iniciar() {
        
        // recuperando el valor del archivo de la configuracion del puerto 
        int puertoAlmacenado = ValidarArchivoTexto.getInstancia().getPrimeraFilaValorArchivo("puerto.txt");
        

        try (ServerSocket serverSocket = new ServerSocket(puertoAlmacenado)) {
            System.out.println("Servidor HTTP multihilo iniciado en http://localhost:"+puertoAlmacenado+"/");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nueva conexión entrante: " + clientSocket.getInetAddress().getHostAddress());
                Runnable clientTask = () -> manejadorCliente(clientSocket);
                executor.execute(clientTask);
            }
        } catch (BindException be) {
            
            System.out.println("El puerto " + PUERTO + " ya está en uso.");
            //menu();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private static void manejadorCliente(Socket clientSocket) {
        try (
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream()
        ) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            
            // Leer la solicitud del cliente (ignorando las cabeceras por ahora)
            StringBuilder request = new StringBuilder();
            while ((bytesRead = input.read(buffer)) != -1) {
                request.append(new String(buffer, 0, bytesRead));
                if (request.toString().endsWith("\r\n\r\n")) {
                    break; // Fin de la solicitud HTTP
                }
            }

            // Analizar la solicitud para obtener la ruta del archivo solicitado
            String[] lines = request.toString().split("\r\n");
            System.out.println("lines: "+lines); 

            String[] requestLine = lines[0].split(" ");
            String ruta = requestLine[1].substring(1); // Elimina el primer '/' en la ruta
            

            System.out.println("estoy en: "+ruta); 
                    
            String valorRuta = "";
            
            // indicar el archivo index por defecto
            if (ruta.isEmpty() || ruta.equals("/")) {
                ruta = "www/index.html";
            
            }else{
                ruta = "www/"+ruta;
            }

            /**
             * Detectar si el usuario utiliza el movil o computadora
            */

            String userAgent="";
            for(String line:lines){
                if (line.startsWith("User-Agent: ")) {
                    userAgent = line.substring("User-Agent: ".length());
                    break;
                }
            }
            
            // determinar si se usa movil o computadora
            boolean esDispositivoMovil = userAgent.contains("Mobile");

            
            // Leer el archivo y enviarlo en paquetes
            Path filePath = Paths.get(ruta);
             if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                fileData = Files.readAllBytes(filePath);

                //System.out.println("estoy en fileData: "+fileData);


                String contentType = ContentType.getInstancia().recuperarContentType(ruta);
                System.out.println("contenttype: "+contentType);
                long fileLengthArchivo = fileData.length;
                double megasArchivo = (double)fileLengthArchivo/(1024*1024);

                System.out.println("Archivo solicitado: "+ megasArchivo + " MB)");

                String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: " + contentType + "\r\n" +
                                        // tratar de tener una conexion persistente
                                        "Connection: Keep-Alive\r\n"+
                                        "Keep-Alive: timeout=60, max=5\r\n"+
                                        "Cache-Control: max-age=60, public\r\n" + // 1 minuto
                                        "Content-Disposition: inline\r\n" + // Visualización en el navegador
                                        "Content-Length: " + fileData.length + "\r\n\r\n";
                                                         

                output.write(responseHeaders.getBytes());                
                //output.flush();

                
                if(esDispositivoMovil){
                    System.out.println("Es un dispositivo movil");

                    // si el paquete es menor o igual a 6 MB
                    if(megasArchivo <= 5){
                        for (int i = 0; i < fileData.length; i++) {
                            output.write(fileData[i]);
                            output.flush();
                        }
                    }else if (megasArchivo >= 5 && megasArchivo <= 19) {
                        int megasTrasferir = 2 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);
                    }else if (megasArchivo >= 20 && megasArchivo <= 50) {
                        int megasTrasferir = 4 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 51 && megasArchivo <= 100) {
                        int megasTrasferir = 5 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);
                    }else if (megasArchivo >= 101 && megasArchivo <= 189) {
                        int megasTrasferir = 6 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 190 && megasArchivo <= 220) {
                        int megasTrasferir = 8 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 221 && megasArchivo <= 340) {
                        int megasTrasferir = 9 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 341 && megasArchivo <= 450) {
                        int megasTrasferir = 7 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);
                    }else if (megasArchivo >= 451 && megasArchivo <= 949) {
                        int megasTrasferir = 11 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 950 && megasArchivo <= 1020) {
                        int megasTrasferir = 6 * 1024 * 1024; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else{

                       
                    }                
                
                
                }else{
                    System.out.println("Es una computadora");

                    // si el paquete es menor o igual a 6 MB
                    if(megasArchivo <= 8){
                        for (int i = 0; i < fileData.length; i++) {
                            output.write(fileData[i]);
                            output.flush();
                        }
                        //int megasTrasferir = 6;
                        //TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);
                    }else if (megasArchivo >= 9 && megasArchivo <= 50) {
                        int megasTrasferir = 3; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 51 && megasArchivo <= 189) {
                        int megasTrasferir = 5 ; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 190 && megasArchivo <= 219) {
                        int megasTrasferir = 6; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 220 && megasArchivo <= 340) {
                        int megasTrasferir = 7; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 341 && megasArchivo <= 450) {
                        int megasTrasferir = 8; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 451 && megasArchivo <= 949) {
                        int megasTrasferir = 9; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else if (megasArchivo >= 950 && megasArchivo <= 1020) {
                        int megasTrasferir = 10; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);

                    }else{
  int megasTrasferir = 1; // Tamaño del paquete en bytes (2 MB)
                    
                        TransferenciaPaquetes.getInstancia().envio(megasTrasferir,fileData,output,clientSocket);
                       
                    }
                
                }


            } else {
                // Archivo no encontrado, enviar respuesta 404
                String respuesta404 = "HTTP/1.1 404 Not Found\r\n\r\n";
                output.write(respuesta404.getBytes());
                output.flush();
            }
        } catch (OutOfMemoryError e) {
            // Maneja la excepción aquí
            e.printStackTrace(); // o cualquier otro manejo adecuado
        } catch (SocketException se) {
            System.out.println("Se produjo una excepción de SocketException: " + se.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                // para liberar memoria, pero falta ver si funciona bien
                
                //fileData = null;
                System.gc();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
