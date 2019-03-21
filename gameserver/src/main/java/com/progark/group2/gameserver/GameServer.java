package com.progark.group2.gameserver;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.progark.group2.gameserver.resources.Player;
import com.progark.group2.gameserver.resources.PortStatus;
import com.progark.group2.wizardrumble.entities.Wizard;
import com.progark.group2.wizardrumble.network.requests.PlayerDeadRequest;
import com.progark.group2.wizardrumble.network.requests.PlayerJoinedRequest;
import com.progark.group2.wizardrumble.network.requests.PlayerNamesRequest;
import com.progark.group2.wizardrumble.network.requests.PlayersHealthStatusRequest;
import com.progark.group2.wizardrumble.network.requests.Request;
import com.progark.group2.wizardrumble.network.responses.Response;
import com.progark.group2.wizardrumble.network.responses.ServerErrorResponse;
import com.progark.group2.wizardrumble.network.responses.ServerSuccessResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GameServer {

    private List<Server> servers = new ArrayList<Server>();
    //TODO: Consider creating a PortPair object, because the TCP port and UDP port are often used together
    private HashMap<Integer, PortStatus> TCP_PORTS = new HashMap<Integer, PortStatus>();
    private HashMap<Integer, PortStatus> UDP_PORTS = new HashMap<Integer, PortStatus>();
    // List of all players that has joined the game with their stats for this game
    private HashMap<Integer, Player> players = new HashMap<Integer, Player>();

    // This is the master server
    GameServer(List<Integer> tcpPorts, List<Integer> udpPorts) throws IOException {

        if (tcpPorts.size() != udpPorts.size()) {
            throw new IllegalArgumentException("Gameserver: tcpports length must be equal to udpports provided");
        }

        // Add ports for reference
        // Create one kryo server object per tcp port and add to list of servers
        for (int i = 0; i < tcpPorts.size(); i++) {
            TCP_PORTS.put(tcpPorts.get(i), PortStatus.OPEN);
            UDP_PORTS.put(udpPorts.get(i), PortStatus.OPEN);
            servers.add(createNewServer(tcpPorts.get(i), udpPorts.get(i)));
        }
    }

    /**
     * Used by master server to determine which tcp port this server used
     * @return  tcp port used by server
     */
    HashMap<Integer, PortStatus> getTCPPorts() {
        return TCP_PORTS;
    }

    /**
     * Used by master server to determine which udp ports this server used
     * @return  udp ports used by server
     */
    HashMap<Integer, PortStatus> getUDPPorts() {
        return UDP_PORTS;
    }

    /**
     * Add a new player to list when joining or creating a new game.
     * @param playerId  (int) player id
     */
    protected void addPlayer(int playerId) throws IOException {
        String playerName = MasterServer.getInstance().getPlayerName(playerId);
        Player player = new Player(
                playerId,
                playerName, // name
                0, // Kills
                Wizard.DEFAULT_HEALTH, // Health
                -1, // Position or rank according to time of death
                0// Time alive, milliseconds
        );
        // Add playerstats to the list of joined players
        if(players.keySet().size() < MasterServer.getMaximumPlayers()){
            players.put(playerId, player);

        }
        else{
            //TODO: Don't add allow more players
        }
    }

    /**
     * Returns available udp port on gameserver
     * @return  udp port if any, else null
     */
    // TODO: Set port as busy
    public Integer getAvailableUDPPort() {
        for (int port : UDP_PORTS.keySet()){
            if (UDP_PORTS.get(port) == null) {
                return port;
            }
        }
        return null;
    }

    /**
     * Returns available tcp port on gameserver
     * @return  tcp port if any, else null
     */
    // TODO: Set port as busy
    public Integer getAvailableTCPPort() {
        for (int port : TCP_PORTS.keySet()){
            if (TCP_PORTS.get(port) == null) {
                return port;
            }
        }
        return null;
    }

    /**
     * Return whether all players are dead and the game has ended.
     * @return  Boolean     True if all players have died
     */
    private boolean hasGameEnded() {
        int playersAlive = 0;
        for (int playerID : players.keySet()) {
            // Count every player alive
            if (players.get(playerID).getHealth() > 0) {
                playersAlive++;
            }
        }
        return playersAlive <= 1;
    }

    /**
     * Creates a new Kryo Server instance and registers request and
     * response classes.
     * @return      Kryo Server object
     */
    private Server createNewServer(final int tcpPort, final int udpPort) throws IOException {
        Server server = createNewServer(tcpPort, udpPort);

        // Register response and request classes for kryo serializer
        KryoServerRegister.registerKryoClasses(server);

        // Add a receiver listener to server
        server.addListener(new Listener() {
            public void received (Connection connection, Object object) {
                if (object instanceof PlayerJoinedRequest) {
                    // If a player has joined
                    PlayerJoinedRequest request = (PlayerJoinedRequest) object;
                    // Add player to list of joined players
                    try {
                        addPlayer(request.getPlayerID());
                        ServerSuccessResponse response = new ServerSuccessResponse();
                        response.setSuccessMessage("Game joined!");
                        connection.sendTCP(response);
                        TCP_PORTS.put(tcpPort, PortStatus.CLOSED);
                        UDP_PORTS.put(udpPort, PortStatus.CLOSED);
                    } catch (IOException e) {
                        ServerErrorResponse errorResponse = new ServerErrorResponse();
                        errorResponse.setErrorMsg(
                                "Something is wrong with the server. Please try again later.");
                        connection.sendTCP(errorResponse);
                        e.printStackTrace();
                    } finally {
                        PlayerNamesRequest playerNamesRequest = new PlayerNamesRequest();
                        List<String> playerNames = new ArrayList<String>();
                        for(Player player : players.values()){
                            playerNames.add(player.getName());
                        }
                        playerNamesRequest.setPlayersInLobby(playerNames);
                        broadcastRequest(playerNamesRequest);
                    }
                }
            }
        });
        return server;
    }

    public void broadcastRequest(Request request){
        for(Server server : servers){
            server.getConnections()[0].sendTCP(request);
        }
    }


    public void broadcastReponse(Response response){
        for(Server server : servers){
            server.getConnections()[0].sendTCP(response);
        }
    }

    /**
     * Subtracts the player's health equal to the damage taken
     * and updates the game data correspondingly.
     * @param playerID  (int) the id of the player taken damage
     * @param damage    (int) damage taken
     */
    public void takeDamage(int playerID, int damage) {
        // The player's previous health
        int previousHealth = players.get(playerID).getHealth();

        // Update player's health
        players.get(playerID).setHealth(previousHealth - damage);

        if (previousHealth - damage <= 0) {
            // TODO: send player is dead request to all clients
            PlayerDeadRequest request = new PlayerDeadRequest();
            request.setPlayerID(playerID);
        }
    }

    /**
     * Sends all players health status to all clients.
     * This should be called between a set time interval
     * to update all clients about the health to all players.
     */
    // TODO: Call this function between a set time interval.
    public void sendPlayersHealthStatusRequest() {
        PlayersHealthStatusRequest request = new PlayersHealthStatusRequest();
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

        // Add each players health status to the map
        for (int playerID : players.keySet()) {
            map.put(playerID, players.get(playerID).getHealth());
        }

        // Add the health status in the request
        request.setMap(map);

        // Send health status update to all clients
        for (Server server : servers) {
            // TODO: Go through all clients joined and send this request through connection.sendTCP(request).
        }
    }

    /**
     * Tell the master server that this server no longer is on standby,
     * but in progress. Start game after 30 seconds from when two players joined or
     * when the game server is full.
     */
    private void startGame() {

        if (players.keySet().size() == MasterServer.getMaximumPlayers()) {
            // set gameserver status to inprogress
        }

        if (players.keySet().size() >= 2) {
            // Start countdown from 30 sec.
            // After this countdown, set gameserver status to inprogress

        }
    }

    /**
     * When the game has ended and all joinedPlayerIDs has left the game,
     * the server stops and removes itself from the MasterServer.
     */
    private void endGame(Connection connection) {
        if (!hasGameEnded()) return;

        // TODO: Create a timeout for when the server should shutdown anyway

        // TODO: send request with metadata (scoreboard data) to all clients
        //PlayerStatisticsResponse response = new PlayerStatisticsResponse();

        // TODO: FIll response with players metadata

        // Send the response back to client
        //connection.sendTCP(response);

        System.out.println("ALL PLAYERS ARE DEAD. STOPPING GAMESERVER: GOODBYE WORLD");
        // Stop the server connection for all servers
        for (Server server : servers) {
            server.stop();
        }

        try {
            // Try removing this from the master server
            // This should open the used ports in master server
            MasterServer.getInstance().removeGameServer(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    public static void main(String[] args) throws IOException {
        GameServer gs = new GameServer(54556, 544557);
        gs.hasGameEnded();
    }*/
}
