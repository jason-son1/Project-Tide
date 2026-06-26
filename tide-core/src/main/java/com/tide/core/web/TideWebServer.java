package com.tide.core.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tide.core.TideCorePlugin;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class TideWebServer {

    private final TideCorePlugin plugin;
    private final int port;
    private HttpServer server;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public TideWebServer(TideCorePlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/configs", new ConfigsHandler());
            server.createContext("/api/logs", new LogsHandler());
            server.createContext("/api/reload", new ReloadHandler());
            server.createContext("/api/database", new DatabaseHandler());
            server.createContext("/", new StaticHandler());
            server.setExecutor(null); // default executor
            server.start();
            plugin.getLogger().info("내장 웹 서버가 포트 " + port + "에서 시작되었습니다.");
        } catch (IOException e) {
            plugin.getLogger().severe("내장 웹 서버 시작 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("내장 웹 서버가 중지되었습니다.");
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendTextResponse(HttpExchange exchange, int statusCode, String contentType, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendOptionsResponse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(204, -1);
    }

    private File getPluginsDirectory() {
        return plugin.getDataFolder().getParentFile();
    }

    /**
     * Handler for /api/configs and /api/configs/{category}/{id}
     */
    private class ConfigsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendOptionsResponse(exchange);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            // Expected patterns:
            // GET /api/configs -> list all configs
            // GET /api/configs/{category}/{id} -> read a specific config
            // POST /api/configs/{category}/{id} -> write a specific config

            String relativePath = path.substring("/api/configs".length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            if (relativePath.isEmpty()) {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleList(exchange);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } else {
                String[] parts = relativePath.split("/", 2);
                if (parts.length < 2) {
                    sendJsonResponse(exchange, 400, Map.of("error", "잘못된 요청 형식입니다. /api/configs/{category}/{id}"));
                    return;
                }
                String category = parts[0];
                String id = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());

                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleRead(exchange, category, id);
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleWrite(exchange, category, id);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        }

        private void handleList(HttpExchange exchange) throws IOException {
            List<Map<String, String>> configs = new ArrayList<>();
            File pluginsDir = getPluginsDirectory();

            // Mapping categories to directories
            Map<String, String> dirMap = Map.of(
                "item", "TideRPG/items",
                "rune", "TideRPG/runes",
                "mob", "TideMobs/mobs",
                "affix", "TideMobs/affixes",
                "altar", "TideMobs/altars"
            );

            for (Map.Entry<String, String> entry : dirMap.entrySet()) {
                File dir = new File(pluginsDir, entry.getValue());
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
                    if (files != null) {
                        for (File file : files) {
                            Map<String, String> configMeta = new HashMap<>();
                            String fileId = file.getName().substring(0, file.getName().lastIndexOf('.'));
                            configMeta.put("id", fileId);
                            configMeta.put("category", entry.getKey());
                            configMeta.put("name", file.getName());
                            configMeta.put("path", entry.getValue() + "/" + file.getName());
                            configs.add(configMeta);
                        }
                    }
                }
            }

            // Add global configs
            File coreConfig = new File(pluginsDir, "TideCore/config.yml");
            if (coreConfig.exists()) {
                configs.add(Map.of("id", "core", "category", "global", "name", "config.yml (TideCore)", "path", "TideCore/config.yml"));
            }
            File rpgConfig = new File(pluginsDir, "TideRPG/config.yml");
            if (rpgConfig.exists()) {
                configs.add(Map.of("id", "rpg", "category", "global", "name", "config.yml (TideRPG)", "path", "TideRPG/config.yml"));
            }

            sendJsonResponse(exchange, 200, configs);
        }

        private void handleRead(HttpExchange exchange, String category, String id) throws IOException {
            File file = getConfigFile(category, id);
            if (file == null || !file.exists()) {
                sendJsonResponse(exchange, 404, Map.of("error", "설정 파일을 찾을 수 없습니다: " + category + "/" + id));
                return;
            }

            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            sendJsonResponse(exchange, 200, Map.of("id", id, "category", category, "yaml", content));
        }

        private void handleWrite(HttpExchange exchange, String category, String id) throws IOException {
            File file = getConfigFile(category, id);
            if (file == null) {
                sendJsonResponse(exchange, 400, Map.of("error", "잘못된 카테고리 또는 ID입니다."));
                return;
            }

            // Read raw body
            String yamlContent;
            try (InputStream is = exchange.getRequestBody();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                yamlContent = sb.toString().trim();
            }

            // Ensure parent directories exist
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Save file
            Files.writeString(file.toPath(), yamlContent, StandardCharsets.UTF_8);

            // Execute hot reload on main thread
            String reloadTarget = getReloadTarget(category, id);
            if (reloadTarget != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Integer count = plugin.getReloadManager().reload(reloadTarget);
                    plugin.getLogger().info("웹 콘솔 요청으로 '" + reloadTarget + "'이 리로드되었습니다. 결과: " + count + "개 로드.");
                });
            }

            sendJsonResponse(exchange, 200, Map.of(
                "success", true,
                "message", "설정이 성공적으로 저장되었습니다.",
                "reload_target", reloadTarget != null ? reloadTarget : "none"
            ));
        }

        private File getConfigFile(String category, String id) {
            File pluginsDir = getPluginsDirectory();
            return switch (category.toLowerCase()) {
                case "item" -> new File(pluginsDir, "TideRPG/items/" + id + ".yml");
                case "rune" -> new File(pluginsDir, "TideRPG/runes/" + id + ".yml");
                case "mob" -> new File(pluginsDir, "TideMobs/mobs/" + id + ".yml");
                case "affix" -> new File(pluginsDir, "TideMobs/affixes/" + id + ".yml");
                case "altar" -> new File(pluginsDir, "TideMobs/altars/" + id + ".yml");
                case "global" -> {
                    if ("core".equalsIgnoreCase(id)) {
                        yield new File(pluginsDir, "TideCore/config.yml");
                    } else if ("rpg".equalsIgnoreCase(id)) {
                        yield new File(pluginsDir, "TideRPG/config.yml");
                    }
                    yield null;
                }
                default -> null;
            };
        }

        private String getReloadTarget(String category, String id) {
            return switch (category.toLowerCase()) {
                case "item" -> "items";
                case "rune" -> "runes";
                case "mob", "altar" -> "mobs"; // Mobs/Altars reloaded in TideMobs under mobs/altars
                case "affix" -> "affixes";
                case "global" -> {
                    if ("core".equalsIgnoreCase(id)) yield "config";
                    if ("rpg".equalsIgnoreCase(id)) yield "items"; // reload rpg items/config
                    yield null;
                }
                default -> null;
            };
        }
    }

    /**
     * Handler for /api/logs
     */
    private class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendOptionsResponse(exchange);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            File logFile = new File(getPluginsDirectory().getParentFile(), "logs/latest.log");
            if (!logFile.exists()) {
                sendJsonResponse(exchange, 404, Map.of("error", "로그 파일을 찾을 수 없습니다."));
                return;
            }

            // Read last 100 lines
            List<String> lastLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
                Queue<String> queue = new LinkedList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (queue.size() >= 100) {
                        queue.poll();
                    }
                    queue.offer(line);
                }
                lastLines.addAll(queue);
            }

            sendJsonResponse(exchange, 200, Map.of("logs", lastLines));
        }
    }

    /**
     * Handler for /api/reload/{target}
     */
    private class ReloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendOptionsResponse(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String target = path.substring("/api/reload".length());
            if (target.startsWith("/")) {
                target = target.substring(1);
            }
            if (target.isEmpty()) {
                sendJsonResponse(exchange, 400, Map.of("error", "리로드 대상을 지정해야 합니다. /api/reload/{target}"));
                return;
            }
            
            final String finalTarget = target;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Integer count = plugin.getReloadManager().reload(finalTarget);
                if (count == null) {
                    plugin.getLogger().warning("웹 API 요청: 알 수 없는 리로드 대상 '" + finalTarget + "'");
                } else {
                    plugin.getLogger().info("웹 API 요청으로 '" + finalTarget + "'이 리로드되었습니다. 결과: " + count + "개 로드.");
                }
            });
            
            sendJsonResponse(exchange, 200, Map.of("success", true, "target", target));
        }
    }

    /**
     * Handler for static files (React web app)
     */
    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (!"GET".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            // First try loading from physical directory plugins/TideCore/web/
            File file = new File(plugin.getDataFolder(), "web" + path);
            if (file.exists() && file.isFile()) {
                serveFile(exchange, file);
            } else {
                // Fallback to loading from JAR resources
                serveResource(exchange, path);
            }
        }

        private void serveFile(HttpExchange exchange, File file) throws IOException {
            String mimeType = getMimeType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(file)) {
                fis.transferTo(os);
            }
        }

        private void serveResource(HttpExchange exchange, String path) throws IOException {
            // Static files inside resource JAR: /web/...
            try (InputStream is = TideWebServer.class.getResourceAsStream("/web" + path)) {
                if (is == null) {
                    // Serve a fallback index.html if it's a sub-route (for client-side routing)
                    try (InputStream indexIs = TideWebServer.class.getResourceAsStream("/web/index.html")) {
                        if (indexIs != null) {
                            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                            byte[] bytes = indexIs.readAllBytes();
                            exchange.sendResponseHeaders(200, bytes.length);
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(bytes);
                            }
                            return;
                        }
                    }
                    sendTextResponse(exchange, 404, "text/plain", "404 Not Found");
                    return;
                }

                String mimeType = getMimeType(path);
                exchange.getResponseHeaders().set("Content-Type", mimeType);
                byte[] bytes = is.readAllBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }

        private String getMimeType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
            if (lower.endsWith(".css")) return "text/css; charset=utf-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (lower.endsWith(".json")) return "application/json; charset=utf-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }

    private List<Map<String, Object>> getNemesisRecordsReflectively() {
        List<Map<String, Object>> records = new ArrayList<>();
        try {
            Class<?> nemesisManagerClass = Class.forName("com.tide.mobs.nemesis.NemesisManager");
            var registration = Bukkit.getServicesManager().getRegistration(nemesisManagerClass);
            if (registration != null) {
                Object manager = registration.getProvider();
                java.lang.reflect.Method getAllRecordsMethod = nemesisManagerClass.getMethod("getAllRecords");
                java.util.Collection<?> col = (java.util.Collection<?>) getAllRecordsMethod.invoke(manager);
                if (col != null) {
                    for (Object record : col) {
                        if (record == null) continue;
                        Class<?> recClass = record.getClass();
                        
                        UUID mobUuid = (UUID) recClass.getMethod("getMobUuid").invoke(record);
                        UUID playerUuid = (UUID) recClass.getMethod("getPlayerUuid").invoke(record);
                        String originalName = (String) recClass.getMethod("getOriginalName").invoke(record);
                        String affixesCsv = (String) recClass.getMethod("getAffixesCsv").invoke(record);
                        int killCount = (Integer) recClass.getMethod("getKillCount").invoke(record);
                        boolean active = (Boolean) recClass.getMethod("isActive").invoke(record);

                        Map<String, Object> map = new HashMap<>();
                        map.put("mobUuid", mobUuid != null ? mobUuid.toString() : "");
                        map.put("playerUuid", playerUuid != null ? playerUuid.toString() : "");
                        map.put("originalName", originalName);
                        map.put("affixesCsv", affixesCsv);
                        map.put("killCount", killCount);
                        map.put("active", active);
                        records.add(map);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error querying nemesis records reflectively: " + e.getMessage());
        }
        return records;
    }

    private class DatabaseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendOptionsResponse(exchange);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String relativePath = path.substring("/api/database".length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            if ("economy".equalsIgnoreCase(relativePath)) {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleGetEconomy(exchange);
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handlePostEconomy(exchange);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } else if ("nemesis".equalsIgnoreCase(relativePath)) {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleGetNemesis(exchange);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } else {
                sendJsonResponse(exchange, 404, Map.of("error", "알 수 없는 API 경로입니다: /api/database/" + relativePath));
            }
        }

        private void handleGetEconomy(HttpExchange exchange) throws IOException {
            try {
                List<Map<String, Object>> list = new ArrayList<>();
                for (com.tide.core.economy.PlayerEconomy economy : plugin.getEconomyManager().getAllPlayers().values()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("uuid", economy.getUuid().toString());
                    map.put("clam", economy.getClam());
                    map.put("pearl", economy.getPearl());
                    map.put("rep", economy.getRep());
                    map.put("hardMode", economy.isHardMode());
                    org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(economy.getUuid());
                    map.put("name", offlinePlayer != null ? offlinePlayer.getName() : "Unknown");
                    list.add(map);
                }
                sendJsonResponse(exchange, 200, list);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("error", "economy 목록을 가져오지 못했습니다: " + e.getMessage()));
            }
        }

        private void handlePostEconomy(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                
                Map<String, Object> body = gson.fromJson(reader, Map.class);
                if (body == null || !body.containsKey("uuid")) {
                    sendJsonResponse(exchange, 400, Map.of("error", "uuid는 필수 입력 항목입니다."));
                    return;
                }

                String uuidStr = (String) body.get("uuid");
                UUID uuid = UUID.fromString(uuidStr);

                Long clam = null;
                if (body.containsKey("clam")) {
                    clam = ((Number) body.get("clam")).longValue();
                }
                Long pearl = null;
                if (body.containsKey("pearl")) {
                    pearl = ((Number) body.get("pearl")).longValue();
                }
                Integer rep = null;
                if (body.containsKey("rep")) {
                    rep = ((Number) body.get("rep")).intValue();
                }
                Boolean hardMode = null;
                if (body.containsKey("hardMode")) {
                    hardMode = (Boolean) body.get("hardMode");
                }

                final Long finalClam = clam;
                final Long finalPearl = pearl;
                final Integer finalRep = rep;
                final Boolean finalHardMode = hardMode;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getEconomyManager().updatePlayerEconomy(uuid, finalClam, finalPearl, finalRep, finalHardMode);
                });

                sendJsonResponse(exchange, 200, Map.of("success", true, "message", "플레이어 정보가 업데이트되었습니다."));
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("error", "업데이트 실패: " + e.getMessage()));
            }
        }

        private void handleGetNemesis(HttpExchange exchange) throws IOException {
            try {
                List<Map<String, Object>> records = getNemesisRecordsReflectively();
                sendJsonResponse(exchange, 200, records);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("error", "nemesis 목록을 가져오지 못했습니다: " + e.getMessage()));
            }
        }
    }
}
