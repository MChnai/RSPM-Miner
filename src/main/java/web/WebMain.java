package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/** Starts a localhost-only dashboard: http://localhost:8765 */
public final class WebMain {
    private static MiningBenchmarkService service;

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        service = new MiningBenchmarkService(root);
        int port = Integer.getInteger("rspm.port", 8765);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/", WebMain::api);
        server.createContext("/", WebMain::assets);
        server.setExecutor(Executors.newFixedThreadPool(6));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            service.shutdown();
            server.stop(0);
        }));
        server.start();
        System.out.println("RSPM Dashboard: http://localhost:" + port);
        System.out.println("Project root: " + root);
    }

    private static void api(HttpExchange x) throws IOException {
        try {
            String path = x.getRequestURI().getPath();
            String method = x.getRequestMethod();
            if (path.equals("/api/datasets") && method.equals("GET")) {
                List<String> values = new ArrayList<>();
                for (String name : service.datasets()) values.add(Json.q(name));
                send(x, 200, "application/json", Json.array(values));
                return;
            }
            if (path.equals("/api/runs") && method.equals("GET")) {
                List<String> values = new ArrayList<>();
                for (RunRecord r : service.history()) values.add(r.json());
                send(x, 200, "application/json", Json.array(values));
                return;
            }
            if (path.equals("/api/runs") && method.equals("POST")) {
                // Small form body, not arbitrary JSON or filesystem paths.
                byte[] raw = x.getRequestBody().readNBytes(4097);
                if (raw.length > 4096) { error(x, 413, "Dữ liệu form quá lớn"); return; }
                Map<String, String> fields = form(new String(raw, StandardCharsets.UTF_8));
                String dataset = fields.get("dataset");
                double minSup = Double.parseDouble(fields.getOrDefault("minSup", ""));
                RunRecord r = service.submit(dataset, minSup);
                send(x, 202, "application/json", r.json());
                return;
            }
            if (path.startsWith("/api/runs/") && method.equals("GET")) {
                long id = Long.parseLong(path.substring("/api/runs/".length()));
                RunRecord r = service.find(id);
                if (r == null) error(x, 404, "Không tìm thấy lần chạy");
                else send(x, 200, "application/json", r.json());
                return;
            }
            if (path.equals("/api/export.csv") && method.equals("GET")) {
                byte[] data = Files.readAllBytes(service.csvFile());
                x.getResponseHeaders().set("Content-Disposition", "attachment; filename=web_benchmark_results.csv");
                x.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                x.sendResponseHeaders(200, data.length);
                try (var out = x.getResponseBody()) { out.write(data); }
                return;
            }
            error(x, 404, "Không có API này");
        } catch (IllegalArgumentException e) {
            error(x, 400, e.getMessage() == null ? "Tham số không hợp lệ" : e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            error(x, 500, "Lỗi máy chủ: " + e.getClass().getSimpleName());
        }
    }

    private static Map<String, String> form(String body) {
        Map<String, String> result = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) result.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    private static void assets(HttpExchange x) throws IOException {
        if (!x.getRequestMethod().equals("GET")) { error(x, 405, "Method not allowed"); return; }
        String path = x.getRequestURI().getPath();
        String filename = switch (path) {
            case "/", "/index.html" -> "index.html";
            case "/style.css" -> "style.css";
            case "/app.js" -> "app.js";
            default -> null;
        };
        if (filename == null) { error(x, 404, "Not found"); return; }
        try (InputStream resource = WebMain.class.getClassLoader().getResourceAsStream("web/" + filename)) {
            if (resource == null) { error(x, 500, "Không tìm thấy resources/web/" + filename); return; }
            byte[] bytes = resource.readAllBytes();
            String mime = filename.endsWith(".html") ? "text/html; charset=utf-8"
                    : filename.endsWith(".css") ? "text/css; charset=utf-8" : "text/javascript; charset=utf-8";
            x.getResponseHeaders().set("Content-Type", mime);
            x.getResponseHeaders().set("Cache-Control", "no-cache");
            x.sendResponseHeaders(200, bytes.length);
            try (var out = x.getResponseBody()) { out.write(bytes); }
        }
    }

    private static void error(HttpExchange x, int status, String msg) throws IOException {
        send(x, status, "application/json", "{\"error\":" + Json.q(msg) + "}");
    }

    private static void send(HttpExchange x, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", type + (type.contains("charset") ? "" : "; charset=utf-8"));
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(status, bytes.length);
        try (var out = x.getResponseBody()) { out.write(bytes); }
    }
}
