package aqua.blatt1.broker;

import aqua.blatt1.client.ClientCommunicator;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Broker {
    private Endpoint endpoint;
    private ClientCollection<InetSocketAddress> clients;
    private int currentId;

    public Broker(int port) {
        endpoint = new Endpoint(port);
        clients = new ClientCollection<InetSocketAddress>();
        currentId = 0;
    }

    public static void main(String[] args) {
        Broker broker = new Broker(Properties.PORT);
        broker.broker();
    }

    public void broker() {
        while (true) {
            Message message = this.endpoint.blockingReceive();
            Serializable payload = message.getPayload();
            InetSocketAddress sender = message.getSender();

            if (payload instanceof RegisterRequest) {
               register(sender);
            } else if (payload instanceof DeregisterRequest) {
                deregister(sender);
            } else if (payload instanceof HandoffRequest) {
                handoffFish(sender, payload);
            } else {

            }
        }
    }

    private void register(InetSocketAddress sender) {
        String id = "tank " + currentId;
        currentId++;
        clients.add(id, sender);
        endpoint.send(sender, new RegisterResponse(id));
    }

    private void deregister(InetSocketAddress sender) {
        clients.remove(clients.indexOf(sender));
    }

    private void handoffFish(InetSocketAddress sender, Serializable payload) {
        HandoffRequest hor = (HandoffRequest)payload;
        FishModel fish = hor.getFish();
        InetSocketAddress target = null;
        if(fish.getDirection().equals(Direction.LEFT)){
            // If client is first
            if (clients.indexOf(sender) == 0) {
                target = clients.getClient(clients.size() - 1);
            } else {
                target = clients.getClient(clients.indexOf(sender) - 1);
            }
        } else {
            // If client is last
            if (clients.indexOf(sender) == clients.size() - 1) {
                target = clients.getClient(0);
            } else {
                target = clients.getClient(clients.indexOf(sender) + 1);
            }
        }
        endpoint.send(target, hor);
    }
}
