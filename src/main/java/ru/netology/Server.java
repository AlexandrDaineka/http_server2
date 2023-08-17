package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
  private static final int THREAD_POOL_SIZE = 64;
  private static final List<String> VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

  public static void main(String[] args) {
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    try (ServerSocket serverSocket = new ServerSocket(9999)) {
      while (true) {
        try {
          Socket socket = serverSocket.accept();
          executorService.execute(() -> handleConnection(socket));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      executorService.shutdown();
    }
  }

  private static void handleConnection(Socket socket) {
    try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
    ) {
      String requestLine = in.readLine();
      String[] parts = requestLine.split(" ");

      if (parts.length != 3) {
        return;
      }

      String path = parts[1];
      if (!VALID_PATHS.contains(path)) {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();

        Logger.logRequest(requestLine, 404);

        return;
      }

      Path filePath = Path.of(".", "public", path);
      String mimeType = Files.probeContentType(filePath);

      if (path.equals("/classic.html")) {
        String template = Files.readString(filePath);
        byte[] content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
        return;
      }

      final var length = Files.size(filePath);
      out.write((
              "HTTP/1.1 200 OK\r\n" +
                      "Content-Type: " + mimeType + "\r\n" +
                      "Content-Length: " + length + "\r\n" +
                      "Connection: close\r\n" +
                      "\r\n"
      ).getBytes());
      Files.copy(filePath, out);
      out.flush();
      Logger.logRequest(requestLine, 200);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

