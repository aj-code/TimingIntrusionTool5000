//The code within this file is released into the public domain, see http://unlicense.org/.

import java.io.*;
import java.net.*;
import java.security.*;

public class socketServerHash {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        ServerSocket serverSocket = new ServerSocket(60000);

        while (true) {
            Socket clientSocket = serverSocket.accept();

            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            byte[] buff = new byte[512];
            int numRead = in.read(buff);

            String password = new String(buff, 0, numRead).trim();
            String hash = sha1(password);

            if (hash.equals("b1b3773a05c0ed0176787a4f1574ff0075f7521e")) //qwerty
                out.write("Success".getBytes());
            else
                out.write("Fail".getBytes());

            out.close();
            in.close();
            clientSocket.close();

        }

    }

    public static String sha1(String pwd) throws NoSuchAlgorithmException {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(pwd.getBytes("UTF-8"));

            return toHex(digest);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static String toHex(byte[] bytes) { //cheers maybewecouldstealavan (http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java)
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
