package com.progark.group2.wizardrumble.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NetworkController {

    // IP address to MasterServer
    private final static int TIMEOUT = 5000;
    private final static String MASTER_SERVER_HOST = "localhost";
    private final static int MASTER_SERVER_TCP_PORT = 54555;
    private final static int MASTER_SERVER_UDP_PORT = 54777;

    // Client for handling communication with given game server
    private static Client client = new Client();

    public void init() throws IOException {

        // Client for handling communication with master server
        Client masterServerClient = new Client();
        masterServerClient.start();
        masterServerClient.connect(
                TIMEOUT,
                MASTER_SERVER_HOST,
                MASTER_SERVER_TCP_PORT,
                MASTER_SERVER_UDP_PORT
        );

        Kryo kryo = masterServerClient.getKryo();
        kryo.register(PlayerJoinedRequest.class);
        kryo.register(PlayerDeadRequest.class);
        kryo.register(PlayerStatisticsResponse.class);
        kryo.register(ServerErrorResponse.class);
        kryo.register(CreateGameRequest.class);
        kryo.register(CreateGameResponse.class);
        kryo.register(HashMap.class);

        final CreateGameRequest request = new CreateGameRequest();
        request.playerID = 0; // TODO: ID is generated through name registering
        masterServerClient.sendTCP(request);

        masterServerClient.addListener(new Listener() {
            public void received (Connection connection, Object object) {
                if (object instanceof CreateGameResponse) {
                    CreateGameResponse response = (CreateGameResponse)object;
                    try {
                        // Client tries to connect to the given GameServer
                        client.close();
                        client.start();
                        client.connect(
                                TIMEOUT,
                                MASTER_SERVER_HOST,
                                response.map.get("tcpPort"),
                                response.map.get("udpPort")
                        );


                        Kryo kryo = client.getKryo();
                        kryo.register(PlayerJoinedRequest.class);
                        kryo.register(PlayerDeadRequest.class);
                        kryo.register(PlayerStatisticsResponse.class);
                        kryo.register(ServerErrorResponse.class);
                        kryo.register(CreateGameRequest.class);
                        kryo.register(CreateGameResponse.class);
                        kryo.register(HashMap.class);

                        // Let the client join the game server (lobby)
                        PlayerJoinedRequest requestToJoin = new PlayerJoinedRequest();
                        client.sendTCP(requestToJoin);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (object instanceof ServerIsFullResponse) {
                    // If all servers are full
                    ServerIsFullResponse response = (ServerIsFullResponse) object;
                    System.out.println("Client got that the server is full: " + response.isFull);
                    // TODO: Handle server is full - display message on interface
                } else if (object instanceof ServerErrorResponse) {
                    // If there occures a server error
                    ServerErrorResponse response = (ServerErrorResponse) object;
                    System.out.println("Client got this error message: " + response.errorMsg);
                    // TODO: Handle server error on client side. Give error message to interface
                }
            }
        });
    }

    public Map getStats(){
        //TODO
        return new HashMap();
    }

    public Map updateStats(Map map){
        //TODO
        return new HashMap();
    }

    public Map updateGameState(Map map){
        //TODO
        return new HashMap();
    }

    public void handleUpdate(){

    }
}
