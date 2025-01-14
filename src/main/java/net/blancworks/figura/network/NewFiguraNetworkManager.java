package net.blancworks.figura.network;

import com.neovisionaries.ws.client.*;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.network.messages.avatar.AvatarUploadMessageSender;
import net.blancworks.figura.network.messages.user.UserDeleteCurrentAvatarMessageSender;
import net.blancworks.figura.network.messages.user.UserGetCurrentAvatarHashMessageSender;
import net.blancworks.figura.network.messages.user.UserGetCurrentAvatarMessageSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Level;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


public class NewFiguraNetworkManager implements IFiguraNetwork {

    public static CompletableFuture networkTasks;

    //The protocol version for this version of the mod.
    public static final int PROTOCOL_VERSION = 0;

    //----- WEBSOCKETS -----

    //The factory that creates all sockets
    public static WebSocketFactory socketFactory;
    //The last socket we were using
    public static WebSocket currWebSocket;

    public static Object authWaitObject = new Object();
    public static ClientConnection authConnection;

    //The JWT token handed to us by the server.
    public static String jwtToken;

    //The time the JWT token was gotten at
    public static Date tokenReceivedTime;

    public static int tokenReauthCooldown = 0;

    //The time tokens last for. (default is 20 minutes)
    public static final int TOKEN_LIFETIME = 1000 * 60 * 20;
    //The time we wait to automatically re-auth once a token has expired. (default is 1 minute)
    public static final int TOKEN_REAUTH_WAIT_TIME = 200;

    //Timeout before a connection with a socket is considered dead.
    public static final int TIMEOUT_SECONDS = 10;

    private static boolean hasInited = false;

    private static CompletableFuture doTask(Runnable toRun) {
        if (networkTasks == null || networkTasks.isDone()) {
            networkTasks = CompletableFuture.runAsync(toRun);
        } else {
            networkTasks.thenRun(toRun);
        }

        return networkTasks;
    }

    private static <T> CompletableFuture doTaskSupply(Supplier<T> toRun) {
        if (networkTasks == null || networkTasks.isDone()) {
            networkTasks = CompletableFuture.supplyAsync(toRun);
        } else {
            CompletableFuture.supplyAsync(toRun);
        }

        return networkTasks;
    }

    @Override
    public void tickNetwork() {

        if(authConnection != null) {
            authConnection.handleDisconnection();
        }
        
        //If the old token we had is old enough, re-auth us.
        Date currTime = new Date();

        if (tokenReauthCooldown > 0) {
            tokenReauthCooldown--;
        } else {
            if (tokenReceivedTime != null && currTime.getTime() - tokenReceivedTime.getTime() > TOKEN_LIFETIME) {
                tokenReauthCooldown = TOKEN_REAUTH_WAIT_TIME; //Wait

                //Auth user ASAP
                doTask(() -> {
                    authUser(true);
                });
            }
        }
    }

    @Override
    public CompletableFuture getAvatarData(UUID id) {
        return doTask(() -> {
            try {
                ensureConnection();

                new UserGetCurrentAvatarMessageSender(id).sendMessage(NewFiguraNetworkManager.currWebSocket);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        });
    }

    @Override
    public CompletableFuture postAvatar() {
        return doTask(() -> {
            try {
                ensureConnection();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            //Get NBT tag for local player avatar
            PlayerData data = PlayerDataManager.localPlayer;
            CompoundTag infoNbt = new CompoundTag();
            data.writeNbt(infoNbt);

            try {
                //Set up streams.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream nbtDataStream = new DataOutputStream(baos);

                NbtIo.writeCompressed(infoNbt, nbtDataStream);

                byte[] result = baos.toByteArray();

                new AvatarUploadMessageSender(result).sendMessage(currWebSocket);

                nbtDataStream.close();
                baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture setCurrentUserAvatar(UUID avatarID) {
        return null;
    }

    @Override
    public CompletableFuture deleteAvatar() {
        return doTask(() -> {
            try {
                ensureConnection();

                new UserDeleteCurrentAvatarMessageSender().sendMessage(currWebSocket);

                PlayerDataManager.clearLocalPlayer();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        });
    }

    @Override
    public CompletableFuture checkAvatarHash(UUID playerID, String lastHash) {
        return doTask(() -> {
            try {
                ensureConnection();
                new UserGetCurrentAvatarHashMessageSender(playerID).sendMessage(currWebSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void parseKickAuthMessage(Text reason) {
        if (reason.asString().equals("This is the Figura Auth Server V2.0!\n")) {

            Text tokenText = reason.getSiblings().get(1);

            jwtToken = tokenText.asString();
            tokenReceivedTime = new Date();
        }
    }

    @Override
    public void onClose() {
        if (currWebSocket != null && currWebSocket.isOpen()) {
            currWebSocket.sendClose();
            currWebSocket.disconnect();
        }
    }

    //Minecraft authentication server URL
    public String authServerURL() {
        if ((boolean) Config.entries.get("useLocalServer").value)
            return "localhost";
        return "figuranew.blancworks.org";
    }

    //Main server for distributing files URL
    public String mainServerURL() {
        if ((boolean) Config.entries.get("useLocalServer").value)
            return "http://localhost:6050";
        return "https://figuranew.blancworks.org";
    }

    private static boolean localLastCheck = false;

    //Ensures there is a connection open with the server, if there was not before.
    public void ensureConnection() throws Exception {

        if (localLastCheck != (boolean) Config.entries.get("useLocalServer").value || socketFactory == null) {
            localLastCheck = (boolean) Config.entries.get("useLocalServer").value;

            socketFactory = new WebSocketFactory();

            //Don't verify hostname for local servers.
            if (localLastCheck) {
                SSLContext ctx = NaiveSSLContext.getInstance("TLS");

                socketFactory.setSSLContext(ctx);
                socketFactory.setVerifyHostname(false);
            }

            socketFactory.setServerName("figuranew.blancworks.org");
        }

        if (currWebSocket == null || currWebSocket.isOpen() == false) {
            currWebSocket = openNewConnection();
        }
    }

    //Opens a connection
    public WebSocket openNewConnection() throws Exception {

        //Ensure user is authed, we need the JWT to verify this user.
        authUser();

        closeSocketConnection();
        String connectionString = String.format("%s/connect/", mainServerURL());

        FiguraMod.LOGGER.error("Connecting to websocket server " + connectionString);

        WebSocket newSocket = socketFactory.createSocket(connectionString, TIMEOUT_SECONDS * 1000);
        newSocket.addListener(new FiguraNetworkMessageHandler(this));

        newSocket.connect();

        newSocket.sendText(jwtToken);

        newSocket.sendText(String.format("{\"protocol\":%d}", PROTOCOL_VERSION));

        return newSocket;
    }

    private void closeSocketConnection() {
        if (currWebSocket == null)
            return;

        if (currWebSocket.isOpen() == false) {
            currWebSocket = null;
            return;
        }

        currWebSocket.sendClose(0);
        currWebSocket = null;
    }

    public void authUser() {
        authUser(false);
    }

    public void authUser(boolean force) {

        if (!force && jwtToken != null)
            return;

        if(authConnection != null) {
            authConnection.handleDisconnection();
            
            if(authConnection != null)
                return;
        }
        
        try {
            String address = authServerURL();
            InetAddress inetAddress = InetAddress.getByName(address);
            
            //Create new connection
            ClientConnection connection = ClientConnection.connect(inetAddress, 25565, true);
            
            //Set listener/handler
            connection.setPacketListener(
                    new ClientLoginNetworkHandler(connection, MinecraftClient.getInstance(), null, (text) -> {
                        FiguraMod.LOGGER.log(Level.ERROR, text.toString());
                    }) {
                        
                        //Handle disconnect message
                        @Override
                        public void onDisconnected(Text reason) {
                            try {
                                Text dcReason = connection.getDisconnectReason();

                                if (dcReason != null) {
                                    Text tc = dcReason;
                                    parseKickAuthMessage(tc);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            
                            //Once connection is closed, yeet this connection so we can make new ones.
                            authConnection = null;
                            
                            synchronized (authWaitObject) {
                                authWaitObject.notifyAll();
                            }
                        }
                    });
            
            //Send packets.
            connection.send(new HandshakeC2SPacket(address, 25565, NetworkState.LOGIN));
            connection.send(new LoginHelloC2SPacket(MinecraftClient.getInstance().getSession().getProfile()));
            
            synchronized (authWaitObject) {
                //Wait for authentication to be done.
                authConnection = connection;
                authWaitObject.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
