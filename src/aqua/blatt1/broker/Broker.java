package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.SecureEndpoint;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private final SecureEndpoint endpoint;
    private final ClientCollection<InetSocketAddress> clients;
    private volatile Integer currentId;
    private volatile static boolean stopRequested = false;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final int LEASE_TIME = 5000;
    private java.util.Timer timer = new Timer();

    //Heimatgestützt
    //Namespace - TankId zu InetSocketAddress
    Map<String, InetSocketAddress> namespace = new HashMap<>();

    public Broker(int port) {
        endpoint = new SecureEndpoint(port);
        clients = new ClientCollection<>();
        currentId = 0;
    }

    public class BrokerTask implements Runnable {
        private final Message message;

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
            if (payload instanceof DeregisterRequest dr) {
                deregister(sender);
            }
            if (payload instanceof HandoffRequest) {
                handoffFish(sender, payload);
            }
            if (payload instanceof NameResolutionRequest e) {
                handleNameResolutionRequest(sender, e);
            }
        }
    }

    private void handleNameResolutionRequest(InetSocketAddress sender, NameResolutionRequest nrr) {
        lock.readLock().lock();
        endpoint.send(sender, new NameResolutionResponse(namespace.get(nrr.getTankId()), nrr.getRequestId()));
        lock.readLock().unlock();
    }

    private synchronized void register(InetSocketAddress sender) {
        lock.writeLock().lock();
        int index = clients.indexOf(sender);
        if (index != -1) {
            String id = clients.getIdOf(index);
            clients.setInstant(id, Instant.now());
            lock.writeLock().unlock();
            return;
        }
        lock.writeLock().unlock();

        String id = "tank " + currentId;
        currentId++;

        lock.writeLock().lock();

        clients.add(id, sender, Instant.now());
        namespace.put(id, sender);

        lock.writeLock().unlock();

        lock.readLock().lock();
        InetSocketAddress leftNeighbor = clients.getLeftNeighorOf(clients.indexOf(id));
        InetSocketAddress rightNeighbor = clients.getRightNeighorOf(clients.indexOf(id));

        InetSocketAddress leftLeftNeighbor = clients.getLeftNeighorOf(clients.indexOf(leftNeighbor));
        InetSocketAddress rightRightNeighbor = clients.getRightNeighorOf(clients.indexOf(rightNeighbor));

        if (clients.size() == 1) {
            endpoint.send(sender, new Token());
        }
        lock.readLock().unlock();
        System.out.println("IS SENDER NULL?" + (sender == null));
        endpoint.send(sender, new NeighborUpdate(leftNeighbor, rightNeighbor));
        // TODO check if is better to make it relative to number of registered clients
        endpoint.send(sender, new RegisterResponse(id, LEASE_TIME));
        endpoint.send(leftNeighbor, new NeighborUpdate(leftLeftNeighbor, sender));
        endpoint.send(rightNeighbor, new NeighborUpdate(sender, rightRightNeighbor));
    }

    private void deregister(InetSocketAddress sender) {
        lock.readLock().lock();
        InetSocketAddress leftNeighbor = clients.getLeftNeighorOf(clients.indexOf(sender));
        InetSocketAddress rightNeighbor = clients.getRightNeighorOf(clients.indexOf(sender));

        InetSocketAddress leftLeftNeighbor = clients.getLeftNeighorOf(clients.indexOf(leftNeighbor));
        InetSocketAddress rightRightNeighbor = clients.getRightNeighorOf(clients.indexOf(rightNeighbor));
        lock.readLock().unlock();

        lock.writeLock().lock();

        String id = clients.getIdOf(clients.indexOf(sender));
        clients.remove(clients.indexOf(sender));
        namespace.remove(id);

        lock.writeLock().unlock();

        endpoint.send(leftNeighbor, new NeighborUpdate(leftLeftNeighbor, rightNeighbor));
        endpoint.send(rightNeighbor, new NeighborUpdate(leftNeighbor, rightRightNeighbor));
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

    public static void main(String[] args) {
        Thread guiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, "Press OK button to stop server.");
                stopRequested = true;
            }
        });
        guiThread.start();

        Broker broker = new Broker(Properties.PORT);
        broker.broker();
    }

    public void broker() {
        // TODO we also get a java.lang.IllegalArgumentException: unsupported address type error, when running normally
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clients.getClients().stream().filter(client -> client.instant.isBefore(Instant.now().minusMillis(LEASE_TIME * 3)))
                        .forEach(client -> deregister(client.client));
            }
        }, 5000, 15000);
        // POOL_SIZE = 4 / 0.7 = 6
        ExecutorService executor = Executors.newFixedThreadPool(6);

        while (!stopRequested) {
            Message message = this.endpoint.blockingReceive();
            if (message.getPayload() instanceof PoisonPill) {
                break;
            }
            executor.execute(new BrokerTask(message));
        }

        executor.shutdown();
    }
}
