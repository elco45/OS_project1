/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javahttpserverso;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 *
 * @author Daemon
 */
class ClientTask implements Runnable {

    private final Socket clientSocket;

    ClientTask(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        System.out.println("Got a client !");
        // Do whatever required to process the client's request
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            // chaque fois qu'une donnée est lue sur le réseau on la renvoi sur
            // le flux d'écriture.
            // la donnée lue est donc retournée exactement au même client.
            String s="", firstLine = "", lengthString="";
            int contentLength=0;
            char postContents[];
            while ((s = in.readLine()) != null) {
                if (firstLine.equals("")) {
                    firstLine = s;
                }
                
                if (s.contains("Content-Length")){
                    lengthString = s;
                }
                
                System.out.println(s);
                if (s.isEmpty()) {
                    break;
                }
            }
            
            

            String[] primerHeader = firstLine.split(" ");
            
            if(primerHeader[0].equals("POST")){
            
            }
            
            System.out.println("\nsoyeltipo " + primerHeader[0]);
            System.out.println("soylaurl " + primerHeader[1] + "\n");

            if (primerHeader[0].equals("GET")) {
                File f = new File("mi_web");
                ArrayList<File> files = new ArrayList<File>(Arrays.asList(f.listFiles()));
                boolean found = false;
                int where = 0;
                String fileContents = "";

                for (File file : files) {
                    System.out.println(file.toString());
                }
                System.out.println("Entro");
                //debería hacerse dos versiones, una con / para UNIX y otra con \ para Windows

                for (File file : files) {
                    System.out.println("w0t");
                    if (!found) {
                        System.out.println("w00t");
                        found = file.toString().replace("\\", "/").contains(primerHeader[1]);
                        System.out.println("m00t");
                        System.out.println(file.toString().replace("\\", "/"));
                    }

                    if (found) {
                        break;
                    }
                    where++;
                }
                System.out.println(where + "----------------------");
                System.out.println("entro");
                if (found) {//yay, 200
                    if (primerHeader[1].endsWith("txt") || primerHeader[1].endsWith("html")) {//sólo lo escupe
                        System.out.println(files.get(where).toString());
                        Scanner sc = new Scanner(files.get(where));
                        while (sc.hasNextLine()) {
                            fileContents += sc.nextLine();
                        }
                        sc.close();
                        response(out, fileContents);
                    } else {//es un file o get vacío, envía
                        if (primerHeader[1].equals("/")) {
                            response(out);
                        } else {//file
                            String type = "";
                            if(primerHeader[1].contains("png")||primerHeader[1].contains("jpg")||primerHeader[1].contains("ico")){
                                type = "image/"+primerHeader[1].split("\\.")[1];
                            }
                            
                            byte[] byteArray = Files.readAllBytes(files.get(where).toPath());
                            String encodedImg = new String(byteArray);
                            char[] charImg = encodedImg.toCharArray();
                            String length = files.get(where).length()+"";
                            
                            response(out, byteArray, type, length);
                        }
                    }
                } else {//nay, 404
                    responseBad(out);
                }

            } else if (primerHeader[0].equals("POST")) {
                contentLength = Integer.parseInt(lengthString.split(" ")[1]);
                postContents = new char[contentLength];
                in.read(postContents);
                
                String key = "";
                String value = "";
                
                if(String.valueOf(postContents).contains("FormBoundary")){//form-data
                    key = String.valueOf(postContents).split("\n")[1].split("name=\"")[1].split("\"")[0];
                    if(String.valueOf(postContents).contains("filename")){
                        String filename = String.valueOf(postContents).split("\n")[1].split("filename=\"")[1].split("\"")[0];
                        FileOutputStream fos = new FileOutputStream("mi_web/"+filename);
                        char[] fileValue;
                        int stopHere1 = 0, stopHere2 = 0;
                        char temp1 = ' ', temp2 = ' ';
                        String temp = String.valueOf(postContents);
                        for(int i = 0; i<contentLength-1; i++){
                            if(temp.charAt(i)=='\n'){
                                if(temp.charAt(i+2)=='\n'){
                                    System.out.println("FOUND");
                                    System.out.println(temp.charAt(i-2));
                                }
                            }
                            temp1 = temp.charAt(i);
                            temp2 = temp.charAt(i+2);
                            if(temp1==temp2&&temp1=='\n'&&stopHere1==0)
                                stopHere1 = i+2;
                            if(stopHere1!=0&&temp1=='\n'&&temp2=='-'){
                                stopHere2 = i;
                                break;
                            }
                        }
                        System.out.println(stopHere1);
                        System.out.println(stopHere2);
                        fileValue = new char[((stopHere2-stopHere1))];
                        System.out.println("ñññññññññññññññ"+fileValue.length);
                        for (int i = 0;i<fileValue.length;i++){
                            fileValue[i]=temp.charAt(stopHere1+i);
                        }
                        System.out.println("sassasasasasasasas");
                        System.out.println(String.valueOf(fileValue));
                        byte writeToServer[] = new String(fileValue).getBytes();
                        fos.write(writeToServer);
                        fos.flush();
                        fos.close();
                    }else{
                        value = String.valueOf(postContents).split("\n")[3];
                        postResponse(out, key, value);
                    }
                    System.out.println("++++"+key);
                }else if(!String.valueOf(postContents).contains("\n")&&String.valueOf(postContents).contains("=")){//url-encoded
                    if(String.valueOf(postContents).contains("&")){
                        //Daemon: methinks this will need a lot of work
                        String splitty[] = String.valueOf(postContents).split("&");
                        //si sólo son 2...
                        key = splitty[0].split("=")[1];
                        value = splitty[1].split("=")[1];
                    }else{
                    key = String.valueOf(postContents).split("=")[0];
                    value = String.valueOf(postContents).split("=")[1];
                    }
                    postResponse(out, key, value);
                }else{//raw o file, guardar en .txt de todas formas
                    FileOutputStream fos = new FileOutputStream("mi_web/incrementaresto.txt");
                    byte writeToServer[] = String.valueOf(postContents).getBytes();
                    fos.write(writeToServer);
                    fos.flush();
                    fos.close();
                    response(out,"Archivo incremental.txt subido con éxito");
                }
                
                System.out.println(String.valueOf(postContents));
                

            } else if (primerHeader[0].equals("PUT")) {

            } else {
                //ERROR: NO ES GET NI POST NI PUT
            }

            /*out.write("HTTP/1.0 200 OK\r\n");
             out.write("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n");
             out.write("Server: Apache/0.8.4\r\n");
             out.write("Content-Type: text/html\r\n");
             //out.write("Content-Length: 59\r\n");
             out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
             out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
             out.write("\r\n");
             out.write("<TITLE>Exemple</TITLE>");
             out.write("<P>Ceci est une page d'exemple.</P>");*/
            // on ferme les flux.
            System.err.println("Connexion avec le client terminée");

            out.close();
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    private void responseBad(BufferedWriter out){
         try {
            out.write("HTTP/1.0 404 File not found\r\n");
            

        } catch (Exception e) {
        }
    }

    private void response(BufferedWriter out, byte[] c, String t, String l) {//response + objeto + tipo + length

        try {
           
            OutputStream stream = clientSocket.getOutputStream();
            stream.write(c);
            //stream.close();
            //out.write(c, 0, c.length);

        } catch (Exception e) {
        }

    }

    private void response(BufferedWriter out, String s) {//response + texto

        try {
            out.write("HTTP/1.0 200 OK\r\n");
            out.write("Date: Fri, 30 Dec 1999 23:59:59 GMT\r\n");
            out.write("Server: Apache/0.8.4\r\n");
            out.write("Content-Type: text/html\r\n");
            //out.write("Content-Length: 59\r\n");
            out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
            out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
            out.write("\r\n");
            out.write(s);

        } catch (Exception e) {
        }

    }

    private void response(BufferedWriter out) {//standard response
        try {
            out.write("HTTP/1.0 200 OK\r\n");
            out.write("Date: Fri, 29 Dec 1999 23:59:59 GMT\r\n");
            out.write("Server: Apache/0.8.4\r\n");
            out.write("Content-Type: text/html\r\n");
            //out.write("Content-Length: 59\r\n");
            out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
            out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
            out.write("\r\n");

            out.write("<TITLE>Exemple</TITLE>");
            out.write("<P>Ceci est une page d'exemple.</P>");

        } catch (Exception e) {
        }
    }
    
    private void postResponse(BufferedWriter out, String key, String value) {//standard response for POST (form, url-encoded)
        try {
            out.write("HTTP/1.0 200 OK\r\n");
            out.write("Date: Fri, 28 Dec 1999 23:59:59 GMT\r\n");
            out.write("Server: Apache/0.8.4\r\n");
            out.write("Content-Type: text/html\r\n");
            //out.write("Content-Length: 59\r\n");
            out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
            out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
            out.write("\r\n");

            out.write("<TITLE>Exemple</TITLE>");
            out.write("<P>Key = "+key+"</P>");
            out.write("<P>Value = "+value+"</P>");

        } catch (Exception e) {
        }
    }

}
