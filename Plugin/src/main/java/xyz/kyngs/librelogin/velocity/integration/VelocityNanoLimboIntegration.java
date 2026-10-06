/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import ua.nanit.limbo.NanoLimbo;
import ua.nanit.limbo.server.LimboServer;
import ua.nanit.limbo.server.data.InfoForwarding;
import xyz.kyngs.librelogin.common.integration.nanolimbo.NanoLimboIntegration;

public class VelocityNanoLimboIntegration extends NanoLimboIntegration<RegisteredServer> {

    private final ClassLoader classLoader;
    private final ProxyServer proxyServer;

    public VelocityNanoLimboIntegration(ProxyServer proxyServer, String portRange) {
        super(portRange);
        this.classLoader = NanoLimbo.class.getClassLoader();
        this.proxyServer = proxyServer;
    }

    @Override
    public RegisteredServer createLimbo(String serverName) {
        InetSocketAddress address = findLocalAvailableAddress().orElseThrow(() -> new IllegalStateException("Cannot find available port for limbo server!"));
        LimboServer server = createLimboServer(address);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proxyServer.registerServer(new ServerInfo(serverName, address));
    }

    /**
     * The player info forwarding mode and secret are read via reflection because PaperMC no longer
     * publishes the velocity-proxy artifact, and the public ProxyConfig API does not expose them.
     */
    @Override
    protected InfoForwarding createForwarding() {
        var configuration = proxyServer.getConfiguration();
        try {
            Method getMode = configuration.getClass().getMethod("getPlayerInfoForwardingMode");
            Method getSecret = configuration.getClass().getMethod("getForwardingSecret");
            Object mode = getMode.invoke(configuration);
            byte[] secret = (byte[]) getSecret.invoke(configuration);

            String name = mode instanceof Enum<?> enumMode ? enumMode.name() : mode.toString();
            return switch (name) {
                case "NONE" -> FORWARDING_FACTORY.none();
                case "LEGACY" -> FORWARDING_FACTORY.legacy();
                case "MODERN" -> FORWARDING_FACTORY.modern(secret);
                case "BUNGEEGUARD" ->
                        FORWARDING_FACTORY.bungeeGuard(Collections.singleton(new String(secret, StandardCharsets.UTF_8)));
                default -> throw new IllegalStateException("Unknown player info forwarding mode: " + name);
            };
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to determine the player info forwarding configuration", e);
        }
    }

    @Override
    protected ClassLoader classLoader() {
        return classLoader;
    }

}
