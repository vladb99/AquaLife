package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private final Endpoint endpoint;
    private volatile ClientCollection<InetSocketAddress> clients;
    private volatile Integer currentId;
    private volatile static boolean stopRequested = false;

    public Broker(int port) {
        endpoint = new Endpoint(port);
        clients = new ClientCollection<InetSocketAddress>();
        currentId = 0;
    }

    public class BrokerTask implements Runnable {
        private final Message message;
        ReadWriteLock lock = new ReentrantReadWriteLock();

        public BrokerTask(Message message) {
            this.message = message;
        }

        @Override
        public void run() {
            Serializable payload = message.getPayload();
            InetSocketAddress sender = message.getSender();

            if (payload instanceof RegisterRequest) {
                register(sender);
            }
            if (payload instanceof DeregisterRequest) {
                deregister(sender);
            }
            if (payload instanceof HandoffRequest) {
                handoffFish(sender, payload);
            }
        }

        private synchronized void register(InetSocketAddress sender) {
            String id = "tank " + currentId;
            currentId++;
            clients.add(id, sender);
            endpoint.send(sender, new RegisterResponse(id));
        }

        private synchronized void deregister(InetSocketAddress sender) {
            clients.remove(clients.indexOf(sender));
        }

        private void handoffFish(InetSocketAddress sender, Serializable payload) {
            HandoffRequest hor = (HandoffRequest) payload;
            FishModel fish = hor.getFish();
            InetSocketAddress target = null;

            lock.readLock().lock();

            if (fish.getDirection().equals(Direction.LEFT)) {
                target = clients.getLeftNeighorOf(clients.indexOf(sender));
            } else {
                target = clients.getRightNeighorOf(clients.indexOf(sender));
            }

            lock.readLock().unlock();

            endpoint.send(target, hor);
        }
    }

    public static void main(String[] args) {
        Thread guiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int input = JOptionPane.showConfirmDialog(null,
                        "Press OK button to stop server", "Message", JOptionPane.DEFAULT_OPTION);

                if(input == 0){
                    stopRequested = true;
                }
            }
        });
        guiThread.start();

        Broker broker = new Broker(Properties.PORT);
        broker.broker();
    }

    public void broker() {
        ExecutorService executor = Executors.newCachedThreadPool();

        while (!stopRequested) {
            Message message = this.endpoint.blockingReceive();
            executor.execute(new BrokerTask(message));
        }

        executor.shutdown();
    }
}
