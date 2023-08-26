package ru.netology;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ExecutorService executorService;
    private final Map<String, Map<String, Handler>> handlers;
    private Handler handler;
    private final String validPaths = "public/messages";
    private final Handler ifNotFoundHandler = (request, out) -> {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    public Server(int threadPool) {
        this.executorService = Executors.newFixedThreadPool(threadPool);
        this.handlers = new ConcurrentHashMap<>();

    }

    public void addHandler(String method, String path, Handler handler) {
        if (handlers.get(method) == null) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    public void listen(int port) {

        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> handleConnection(socket));

            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    //
    private void handleConnection(Socket socket) {
        try (
                socket;
                final var in = socket.getInputStream();
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            var request = Request.fromInputStream(in);
            var handlerMap = handlers.get(request.getMethod());
            if (handlerMap == null) {
                ifNotFoundHandler.handle(request, out);
                return;
            }
            File fileDirectory = new File(validPaths);
            for (File f : Objects.requireNonNull(fileDirectory.listFiles())) {
                if (f.isFile() && f.getName().equals(request.getPath().substring(1))) {
                    handler = handlerMap.get("*.*");
                }
            }
            if (handler == null) handler = handlerMap.get(request.getPath());
            if (handler == null) {
                ifNotFoundHandler.handle(request, out);
                return;
            }
            handler.handle(request, out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

