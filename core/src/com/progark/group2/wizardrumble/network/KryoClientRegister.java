package com.progark.group2.wizardrumble.network;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.progark.group2.wizardrumble.network.requests.PlayerDeadRequest;
import com.progark.group2.wizardrumble.network.requests.PlayerJoinedRequest;
import com.progark.group2.wizardrumble.network.responses.PlayerJoinedResponse;
import com.progark.group2.wizardrumble.network.requests.PlayerMovementRequest;
import com.progark.group2.wizardrumble.network.requests.PlayerNameRequest;
import com.progark.group2.wizardrumble.network.responses.PlayerNameResponse;
import com.progark.group2.wizardrumble.network.responses.PlayerStatisticsResponse;
import com.progark.group2.wizardrumble.network.requests.PlayersHealthStatusRequest;
import com.progark.group2.wizardrumble.network.responses.ServerErrorResponse;

import java.util.HashMap;

public class KryoClientRegister {

    /**
     * Registers all classes for kryo serializer
     * @param client    kryo client object
     */
    static void registerKryoClasses(Client client) {
        Kryo kryo = client.getKryo();
        kryo.register(PlayerDeadRequest.class);
        kryo.register(PlayerJoinedRequest.class);
        kryo.register(PlayerJoinedResponse.class);
        kryo.register(PlayerMovementRequest.class);
        kryo.register(PlayerNameRequest.class);
        kryo.register(PlayerNameResponse.class);
        kryo.register(PlayersHealthStatusRequest.class);
        kryo.register(PlayerStatisticsResponse.class);
        kryo.register(ServerErrorResponse.class);
        kryo.register(HashMap.class);
        kryo.register(Vector2.class);
    }
}
