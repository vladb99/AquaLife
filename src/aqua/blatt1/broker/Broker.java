package aqua.blatt1.broker;

import aqua.blatt1.client.AquaClient;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.SecureEndpoint;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt1.common.Message;

import javax.swing.*;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker implements AquaBroker {
    private final SecureEndpoint endpoint;
    private final ClientCollection<AquaClient> clients;
    private volatile Integer currentId;
    private volatile static boolean stopRequested = false;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final int LEASE_TIME = 5000;
    private java.util.Timer timer = new Timer();

    //Heimatgestützt
    //Namespace - TankId zu InetSocketAddress
    Map<String, AquaClient> namespace = new HashMap<>();

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
            AquaClient sender = message.getSender();

            try {
                if (payload instanceof RegisterRequest) {
                    register(sender);
                }
                if (payload instanceof DeregisterRequest dr) {
                    deregister(sender);
                }
                if (payload instanceof NameResolutionRequest e) {
                    handleNameResolutionRequest(sender, e);
                }
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void handleNameResolutionRequest(AquaClient sender, NameResolutionRequest nrr) throws RemoteException {
        lock.readLock().lock();
        sender.handleNameResolutionResponse(namespace.get(nrr.getTankId()), nrr.getRequestId());
        //endpoint.send(sender, new NameResolutionResponse(namespace.get(nrr.getTankId()), nrr.getRequestId()));
        lock.readLock().unlock();
    }

    @Override
    public synchronized void register(AquaClient sender) throws RemoteException {
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
        AquaClient leftNeighbor = clients.getLeftNeighorOf(clients.indexOf(id));
        AquaClient rightNeighbor = clients.getRightNeighorOf(clients.indexOf(id));

        AquaClient leftLeftNeighbor = clients.getLeftNeighorOf(clients.indexOf(leftNeighbor));
        AquaClient rightRightNeighbor = clients.getRightNeighorOf(clients.indexOf(rightNeighbor));

        if (clients.size() == 1) {
            sender.handleToken(sender, new Token());
            //endpoint.send(sender, new Token());
        }
        lock.readLock().unlock();
        //System.out.println("IS SENDER NULL?" + (sender == null));
        sender.handleNeighborUpdate(sender, new NeighborUpdate(leftNeighbor, rightNeighbor));
        //endpoint.send(sender, new NeighborUpdate(leftNeighbor, rightNeighbor));
        // TODO check if is better to make it relative to number of registered clients
        sender.handleRegisterResponse(sender, new RegisterResponse(id, LEASE_TIME));
        //endpoint.send(sender, new RegisterResponse(id, LEASE_TIME));
        sender.handleNeighborUpdate(leftNeighbor, new NeighborUpdate(leftLeftNeighbor, sender));
        //endpoint.send(leftNeighbor, new NeighborUpdate(leftLeftNeighbor, sender));
        sender.handleNeighborUpdate(rightNeighbor, new NeighborUpdate(sender, rightRightNeighbor));
        //endpoint.send(rightNeighbor, new NeighborUpdate(sender, rightRightNeighbor));
    }

    @Override
    public void deregister(AquaClient sender) throws RemoteException {
        lock.readLock().lock();
        AquaClient leftNeighbor = clients.getLeftNeighorOf(clients.indexOf(sender));
        AquaClient rightNeighbor = clients.getRightNeighorOf(clients.indexOf(sender));

        AquaClient leftLeftNeighbor = clients.getLeftNeighorOf(clients.indexOf(leftNeighbor));
        AquaClient rightRightNeighbor = clients.getRightNeighorOf(clients.indexOf(rightNeighbor));
        lock.readLock().unlock();

        lock.writeLock().lock();

        String id = clients.getIdOf(clients.indexOf(sender));
        clients.remove(clients.indexOf(sender));
        namespace.remove(id);

        lock.writeLock().unlock();

        sender.handleNeighborUpdate(leftLeftNeighbor, new NeighborUpdate(leftLeftNeighbor, rightNeighbor));
        //endpoint.send(leftNeighbor, new NeighborUpdate(leftLeftNeighbor, rightNeighbor));
        sender.handleNeighborUpdate(rightNeighbor, new NeighborUpdate(leftNeighbor, rightRightNeighbor));
        //endpoint.send(rightNeighbor, new NeighborUpdate(leftNeighbor, rightRightNeighbor));
    }

    public static void main(String[] args) throws RemoteException {
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

        Registry registry = LocateRegistry.createRegistry(
                Registry.REGISTRY_PORT);
        // TODO check if Properties.PORT works
        AquaBroker stub = (AquaBroker)
                UnicastRemoteObject.exportObject(broker, 0);
        registry.rebind(Properties.BROKER_NAME, stub);
    }

    public void broker() {
        // TODO we also get a java.lang.IllegalArgumentException: unsupported address type error, when running normally
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clients.getClients().stream().filter(client -> client.instant.isBefore(Instant.now().minusMillis(LEASE_TIME * 3)))
                        .forEach(client -> {
                            try {
                                deregister(client.client);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        });
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
