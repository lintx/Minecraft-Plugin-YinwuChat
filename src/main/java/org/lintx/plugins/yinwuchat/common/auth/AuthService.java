package org.lintx.plugins.yinwuchat.common.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import java.security.spec.MGF1ParameterSpec;

public class AuthService {
    private static final Map<String, AuthService> INSTANCES = new HashMap<>();
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 16;

    private final AuthUserStore userStore;
    private final CaptchaManager captchaManager;
    private final HandshakeManager handshakeManager;
    private final ResetManager resetManager;
    private final KeyPair keyPair;
    private final String rsaOaepHash;
    private final String rsaFallbackHash;
    private final Gson gson = new Gson();

    private AuthService(File dataFolder) {
        if (dataFolder != null && !dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.userStore = new AuthUserStore(dataFolder);
        this.captchaManager = new CaptchaManager();
        this.handshakeManager = new HandshakeManager();
        this.resetManager = new ResetManager();
        this.keyPair = CryptoUtil.generateRsaKeyPair();
        String chosenHash = "SHA-256";
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPPadding");
            javax.crypto.spec.OAEPParameterSpec spec = new javax.crypto.spec.OAEPParameterSpec(
                    chosenHash,
                    "MGF1",
                    new MGF1ParameterSpec(chosenHash),
                    javax.crypto.spec.PSource.PSpecified.DEFAULT
            );
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keyPair.getPrivate(), spec);
        } catch (Exception e) {
            chosenHash = "SHA-1";
        }
        this.rsaOaepHash = chosenHash;
        this.rsaFallbackHash = chosenHash.equals("SHA-256") ? "SHA-1" : "SHA-256";
    }

    public static synchronized AuthService getInstance(File dataFolder) {
        String key = dataFolder == null ? "default" : dataFolder.getAbsolutePath();
        return INSTANCES.computeIfAbsent(key, k -> new AuthService(dataFolder));
    }

    public JsonObject createHandshakeResponse() {
        HandshakeManager.Handshake handshake = handshakeManager.createHandshake();
        JsonObject json = new JsonObject();
        json.addProperty("ok", true);
        json.addProperty("handshakeId", handshake.id);
        json.addProperty("publicKey", CryptoUtil.publicKeyToPem(keyPair.getPublic()));
        json.addProperty("nonce", handshake.nonce);
        json.addProperty("hmacKey", Base64.getEncoder().encodeToString(handshake.hmacKey));
        json.addProperty("oaepHash", rsaOaepHash);
        json.addProperty("expiresIn", handshakeManager.getTtlMillis());
        return json;
    }

    public JsonObject createCaptchaResponse() {
        CaptchaManager.CaptchaResponse captcha = captchaManager.createCaptcha();
        JsonObject json = new JsonObject();
        json.addProperty("ok", true);
        json.addProperty("captchaId", captcha.id);
        json.addProperty("image", captcha.imageBase64);
        return json;
    }

    public String toJson(JsonObject object) {
        return gson.toJson(object);
    }

    public JsonObject handleRegister(String body) {
        try {
            JsonObject request = parseBody(body);
            AuthRequest auth = decryptAndVerify(request);
            if (auth == null) {
                return error("鉴权失败，请刷新后重试");
            }
            if (!captchaManager.validate(auth.captchaId, auth.captchaText)) {
                return error("验证码不正确或已过期");
            }
            if (!isValidUsername(auth.username)) {
                return error("用户名格式不正确，请使用 3-16 位字母数字下划线");
            }
            if (!isValidPasswordHash(auth.passwordHash)) {
                return error("密码格式不正确");
            }
            if (!userStore.register(auth.username, auth.passwordHash)) {
                return error("该用户名已被注册");
            }
            JsonObject res = new JsonObject();
            res.addProperty("ok", true);
            res.addProperty("message", "注册成功");
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return error("注册失败: " + safeErrorMessage(e));
        }
    }

    public JsonObject handleLogin(String body) {
        try {
            JsonObject request = parseBody(body);
            AuthRequest auth = decryptAndVerify(request);
            if (auth == null) {
                return error("鉴权失败，请刷新后重试");
            }
            if (!isValidUsername(auth.username)) {
                return error("用户名或密码错误");
            }
            if (!isValidPasswordHash(auth.passwordHash)) {
                return error("用户名或密码错误");
            }
            if (!userStore.verify(auth.username, auth.passwordHash)) {
                return error("用户名或密码错误");
            }
            AuthUserStore.BanInfo banInfo = userStore.getBanInfo(auth.username);
            if (banInfo.banned) {
                String durationText = formatDuration(banInfo.permanent ? -1L : banInfo.remainingMillis);
                JsonObject res = new JsonObject();
                res.addProperty("ok", false);
                res.addProperty("action", "ban_notice");
                res.addProperty("message", "该账号已被封禁");
                res.addProperty("duration", durationText.isEmpty() ? "永久" : durationText);
                res.addProperty("reason", banInfo.reason == null ? "" : banInfo.reason);
                res.addProperty("by", banInfo.bannedBy == null ? "" : banInfo.bannedBy);
                res.addProperty("target", auth.username);
                return res;
            }
            JsonObject res = new JsonObject();
            res.addProperty("ok", true);
            res.addProperty("message", "登录成功");
            res.addProperty("username", userStore.getDisplayName(auth.username));
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return error("登录失败: " + safeErrorMessage(e));
        }
    }

    public JsonObject handleResetToken(String body, TokenDeleter tokenDeleter) {
        try {
            JsonObject request = parseBody(body);
            AuthRequest auth = decryptAndVerify(request);
            if (auth == null) {
                return error("鉴权失败，请刷新后重试");
            }
            if (!userStore.verify(auth.username, auth.passwordHash)) {
                return error("密码错误");
            }
            String playerName = getBoundPlayerName(auth.username);
            if (playerName == null || playerName.isEmpty()) {
                return error("该账号尚未绑定玩家，无需重置");
            }
            if (tokenDeleter != null) {
                tokenDeleter.deleteToken(playerName);
            }
            JsonObject res = new JsonObject();
            res.addProperty("ok", true);
            res.addProperty("message", "Token 已重置，请重新连接并在游戏内绑定");
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return error("重置失败: " + safeErrorMessage(e));
        }
    }

    public JsonObject handleDeleteAccount(String body, TokenDeleter tokenDeleter) {
        try {
            JsonObject request = parseBody(body);
            AuthRequest auth = decryptAndVerify(request);
            if (auth == null) {
                return error("鉴权失败，请刷新后重试");
            }
            if (!userStore.verify(auth.username, auth.passwordHash)) {
                return error("用户名或密码错误");
            }
            
            // 账户删除
            boolean deleted = userStore.delete(auth.username);
            if (!deleted) {
                return error("账户不存在");
            }
            
            // Token 删除 (由具体平台实现)
            if (tokenDeleter != null && auth.playerName != null && !auth.playerName.isEmpty()) {
                tokenDeleter.deleteToken(auth.playerName);
            }
            
            JsonObject res = new JsonObject();
            res.addProperty("ok", true);
            res.addProperty("message", "账户注销成功，对应 Token 已失效");
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return error("注销失败: " + safeErrorMessage(e));
        }
    }

    public JsonObject handleRequestReset(String body, PlayerVerifier playerVerifier) {
        try {
            JsonObject request = parseBody(body);
            AuthRequest auth = decryptAndVerify(request);
            if (auth == null) {
                return error("鉴权失败，请刷新后重试");
            }
            if (!userStore.exists(auth.username)) {
                return error("该 Web 账户名不存在");
            }
            if (auth.playerName == null || auth.playerName.isEmpty()) {
                return error("请提供绑定的 Minecraft 玩家名");
            }
            
            // 验证该玩家是否确实绑定了该 Web 账户 (即玩家是否有该账户名关联的 Token)
            if (playerVerifier != null && !playerVerifier.isBound(auth.username, auth.playerName)) {
                return error("该玩家名与账户不匹配，或未在游戏中完成绑定");
            }
            
            // 生成重置码
            String code = resetManager.createSession(auth.username, auth.playerName);
            
            JsonObject res = new JsonObject();
            res.addProperty("ok", true);
            res.addProperty("code", code);
            res.addProperty("message", "重置申请已提交，请进入游戏完成验证");
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return error("重置申请失败: " + safeErrorMessage(e));
        }
    }

    public JsonObject handleCheckResetStatus(String username) {
        ResetManager.ResetSession session = resetManager.getSession(username);
        JsonObject res = new JsonObject();
        if (session == null) {
            res.addProperty("ok", false);
            res.addProperty("message", "重置会话已过期或不存在");
        } else {
            res.addProperty("ok", true);
            res.addProperty("verified", session.verified);
        }
        return res;
    }

    public JsonObject handleResetPassword(String body) {
        try {
            JsonObject request = parseBody(body);
            AuthRequest auth = decryptAndVerify(request);
            if (auth == null) {
                return error("鉴权失败，请刷新后重试");
            }
            
            ResetManager.ResetSession session = resetManager.getSession(auth.username);
            if (session == null || !session.verified) {
                return error("身份尚未验证或验证已过期");
            }
            
            if (!isValidPasswordHash(auth.passwordHash)) {
                return error("新密码格式不正确");
            }
            
            boolean updated = userStore.updatePassword(auth.username, auth.passwordHash);
            if (updated) {
                resetManager.removeSession(auth.username);
                JsonObject res = new JsonObject();
                res.addProperty("ok", true);
                res.addProperty("message", "密码重置成功，请使用新密码登录");
                return res;
            } else {
                return error("密码更新失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return error("重置失败: " + safeErrorMessage(e));
        }
    }

    public boolean verifyInGameReset(String accountName, String playerName, String code) {
        return resetManager.verifyCode(accountName, playerName, code);
    }

    public AuthUserStore.BanInfo getBanInfo(String username) {
        return userStore.getBanInfo(username);
    }

    public boolean accountExists(String username) {
        return userStore.exists(username);
    }

    public boolean bindAccountPlayerName(String username, String playerName) {
        return userStore.bindPlayerName(username, playerName);
    }

    public boolean unbindAccountPlayerName(String username) {
        return userStore.unbindPlayerName(username);
    }

    public String unbindAccountByPlayerName(String playerName) {
        return userStore.unbindByPlayerName(playerName);
    }

    public String resolveAccountByPlayerName(String playerName) {
        return userStore.getAccountByPlayerName(playerName);
    }

    public String getBoundPlayerName(String username) {
        return userStore.getBoundPlayerName(username);
    }

    public BanResult banUser(String username, long durationMillis, String reason, String bannedBy) {
        if (!userStore.exists(username)) {
            return BanResult.notFound();
        }
        boolean ok = userStore.ban(username, durationMillis, reason, bannedBy);
        if (!ok) {
            return BanResult.failed();
        }
        AuthUserStore.BanInfo info = userStore.getBanInfo(username);
        BanResult result = BanResult.success();
        result.accountName = userStore.getDisplayName(username);
        result.remainingMillis = info.remainingMillis;
        result.permanent = info.permanent;
        return result;
    }

    public BanResult unbanUser(String username) {
        if (!userStore.exists(username)) {
            return BanResult.notFound();
        }
        userStore.unban(username);
        BanResult result = BanResult.success();
        result.accountName = userStore.getDisplayName(username);
        return result;
    }

    public interface PlayerVerifier {
        boolean isBound(String accountName, String playerName);
    }

    public interface TokenDeleter {
        void deleteToken(String playerName);
    }

    private JsonObject parseBody(String body) {
        return JsonParser.parseString(body == null ? "{}" : body).getAsJsonObject();
    }

    private AuthRequest decryptAndVerify(JsonObject request) {
        String handshakeId = getString(request, "handshakeId");
        String payloadBase64 = getString(request, "payload");
        String payloadEncBase64 = getString(request, "payloadEnc");
        String keyEncBase64 = getString(request, "keyEnc");
        String ivBase64 = getString(request, "iv");
        String hmacBase64 = getString(request, "hmac");
        if (handshakeId.isEmpty() || hmacBase64.isEmpty()) {
            return null;
        }
        HandshakeManager.Handshake handshake = handshakeManager.getValidHandshake(handshakeId);
        if (handshake == null) {
            return null;
        }
        String hmacSource = payloadBase64;
        if (!payloadEncBase64.isEmpty() && !ivBase64.isEmpty()) {
            hmacSource = payloadEncBase64 + "." + ivBase64;
        }
        byte[] expectedHmac = CryptoUtil.hmacSha256(handshake.hmacKey, hmacSource.getBytes(StandardCharsets.UTF_8));
        byte[] providedHmac = Base64.getDecoder().decode(hmacBase64);
        if (!MessageDigestUtil.isEqual(expectedHmac, providedHmac)) {
            return null;
        }
        String json;
        if (!payloadEncBase64.isEmpty() && !keyEncBase64.isEmpty() && !ivBase64.isEmpty()) {
            byte[] encryptedKey = Base64.getDecoder().decode(keyEncBase64);
            byte[] aesKey = rsaDecryptWithFallback(encryptedKey);
            json = new String(CryptoUtil.aesGcmDecrypt(
                    Base64.getDecoder().decode(payloadEncBase64),
                    Base64.getDecoder().decode(ivBase64),
                    aesKey
            ), StandardCharsets.UTF_8);
        } else {
            byte[] encrypted = Base64.getDecoder().decode(payloadBase64);
            json = new String(rsaDecryptWithFallback(encrypted), StandardCharsets.UTF_8);
        }
        JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
        String nonce = getString(payload, "nonce");
        if (!handshake.nonce.equals(nonce)) {
            return null;
        }
        String passwordHash = getString(payload, "passwordHash");
        String saltedHash = getString(payload, "saltedHash");
        if (!CryptoUtil.sha256Hex(passwordHash + nonce).equalsIgnoreCase(saltedHash)) {
            return null;
        }
        handshakeManager.consume(handshakeId);
        AuthRequest auth = new AuthRequest();
        auth.username = getString(payload, "username");
        auth.passwordHash = passwordHash;
        auth.captchaId = getString(payload, "captchaId");
        auth.captchaText = getString(payload, "captchaText");
        auth.playerName = getString(payload, "playerName");
        return auth;
    }

    private boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        String trimmed = username.trim();
        if (trimmed.length() < USERNAME_MIN || trimmed.length() > USERNAME_MAX) {
            return false;
        }
        return trimmed.matches("[A-Za-z0-9_]+");
    }

    private boolean isValidPasswordHash(String passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        return passwordHash.matches("[A-Fa-f0-9]{64}");
    }

    private JsonObject error(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("ok", false);
        json.addProperty("message", message);
        return json;
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private byte[] rsaDecryptWithFallback(byte[] encrypted) {
        try {
            return CryptoUtil.rsaDecryptOaep(encrypted, keyPair.getPrivate(), rsaOaepHash);
        } catch (Exception first) {
            return CryptoUtil.rsaDecryptOaep(encrypted, keyPair.getPrivate(), rsaFallbackHash);
        }
    }

    private String safeErrorMessage(Exception e) {
        String name = e.getClass().getSimpleName();
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return name;
        }
        return name + " - " + message;
    }

    private static class AuthRequest {
        String username;
        String passwordHash;
        String captchaId;
        String captchaText;
        String playerName;
    }

    public static class BanResult {
        public boolean ok = false;
        public boolean notFound = false;
        public boolean failed = false;
        public boolean permanent = false;
        public long remainingMillis = 0L;
        public String accountName = "";

        static BanResult success() {
            BanResult res = new BanResult();
            res.ok = true;
            return res;
        }

        static BanResult notFound() {
            BanResult res = new BanResult();
            res.notFound = true;
            return res;
        }

        static BanResult failed() {
            BanResult res = new BanResult();
            res.failed = true;
            return res;
        }
    }

    public static long parseDurationMillis(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0L;
        }
        String text = raw.trim().toLowerCase();
        
        // 支持中文单位：秒、分、分钟、时、小时、天
        text = text.replace("秒钟", "s").replace("秒", "s")
                   .replace("分钟", "m").replace("分", "m")
                   .replace("小时", "h").replace("时", "h")
                   .replace("天", "d");
        
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)([smhd])?$").matcher(text);
        if (!matcher.matches()) {
            return 0L;
        }
        long value = 0L;
        try {
            value = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
        }
        if (value <= 0L) {
            return 0L;
        }
        String unit = matcher.group(2);
        if (unit == null || unit.isEmpty()) {
            return value * 1000L; // 默认秒
        }
        switch (unit) {
            case "s":
                return value * 1000L;
            case "m":
                return value * 60_000L;
            case "h":
                return value * 60_000L * 60L;
            case "d":
                return value * 60_000L * 60L * 24L;
            default:
                return 0L;
        }
    }

    public static String formatDuration(long millis) {
        if (millis < 0L) {
            return "永久";
        }
        if (millis <= 0L) {
            return "";
        }
        long seconds = millis / 1000L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        long secs = seconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分钟");
        if (days == 0 && hours == 0 && minutes == 0 && secs > 0) sb.append(secs).append("秒");
        return sb.toString();
    }

    private static class MessageDigestUtil {
        static boolean isEqual(byte[] a, byte[] b) {
            if (a == null || b == null) {
                return false;
            }
            if (a.length != b.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }
    }
}
