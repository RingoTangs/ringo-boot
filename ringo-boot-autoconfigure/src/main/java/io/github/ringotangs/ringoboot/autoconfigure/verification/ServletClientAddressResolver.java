package io.github.ringotangs.ringoboot.autoconfigure.verification;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;

/** 使用当前 Servlet 请求的 {@link HttpServletRequest#getRemoteAddr()} 解析客户端来源地址。 */
public final class ServletClientAddressResolver implements ClientAddressResolver {

    private final ObjectProvider<HttpServletRequest> requestProvider;

    /**
     * 使用请求对象提供器创建解析器。
     *
     * @param requestProvider 当前 Servlet 请求提供器
     */
    public ServletClientAddressResolver(ObjectProvider<HttpServletRequest> requestProvider) {
        this.requestProvider = Objects.requireNonNull(requestProvider, "requestProvider must not be null");
    }

    @Override
    public String resolve() throws ClientAddressResolutionException {
        HttpServletRequest request;
        try {
            request = requestProvider.getIfAvailable();
        } catch (RuntimeException exception) {
            throw new ClientAddressResolutionException("current servlet request is unavailable", exception);
        }
        if (request == null) {
            throw new ClientAddressResolutionException("current servlet request is unavailable");
        }
        return normalize(request.getRemoteAddr());
    }

    static String normalize(String address) {
        if (address == null || address.isBlank()) {
            throw new ClientAddressResolutionException("client address must not be blank");
        }
        String candidate = address.strip();
        if (candidate.indexOf(':') >= 0) {
            return normalizeIpv6(candidate);
        }
        String[] octets = candidate.split("\\.", -1);
        if (octets.length != 4) {
            throw invalidAddress();
        }
        StringBuilder normalized = new StringBuilder();
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3 || !octet.chars().allMatch(Character::isDigit)) {
                throw invalidAddress();
            }
            int value;
            try {
                value = Integer.parseInt(octet);
            } catch (NumberFormatException exception) {
                throw invalidAddress(exception);
            }
            if (value > 255) {
                throw invalidAddress();
            }
            if (!normalized.isEmpty()) {
                normalized.append('.');
            }
            normalized.append(value);
        }
        return normalized.toString();
    }

    private static String normalizeIpv6(String address) {
        if (!address.matches("[0-9A-Fa-f:.]+")) {
            throw invalidAddress();
        }
        try {
            InetAddress parsed = InetAddress.getByName(address);
            if (!(parsed instanceof Inet6Address)) {
                throw invalidAddress();
            }
            return parsed.getHostAddress();
        } catch (UnknownHostException exception) {
            throw invalidAddress(exception);
        }
    }

    private static ClientAddressResolutionException invalidAddress() {
        return new ClientAddressResolutionException("client address is not a valid IP literal");
    }

    private static ClientAddressResolutionException invalidAddress(Throwable cause) {
        return new ClientAddressResolutionException("client address is not a valid IP literal", cause);
    }
}
