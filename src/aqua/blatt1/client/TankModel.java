package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.RecordingMode;
import aqua.blatt1.common.msgtypes.*;

public class TankModel extends Observable implements Iterable<FishModel> {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected volatile String id; //tank id
    protected final Set<FishModel> fishies;
    protected int fishCounter = 0;
    protected final ClientCommunicator.ClientForwarder forwarder;
    protected InetSocketAddress rightNeighbor;
    protected InetSocketAddress leftNeighbor;
    protected boolean token = false;
    protected Timer timer = new Timer();

    //SNAPSHOT
    protected RecordingMode recordingMode = RecordingMode.IDLE;
    protected int localState = 0;
    protected boolean isInitiator = false;
    protected boolean hasSnapshotToken = false;
    protected boolean localSnapshotDone = false;
    protected boolean globalSnapshotDone = false;
    protected SnapshotToken snapshotToken;
    protected int cntFishies = 0;

    //Namensdienste - Vorwärtsreferenzen
    Map<String, FishDirectionReference> fishDirectionReferenceMap = new HashMap<>();

    //Namensdienste 2 - Heimatgestützt
    //FishId - TankAddress
    Map<String, InetSocketAddress> homeAgent = new HashMap<>();

    public TankModel(ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
        this.forwarder = forwarder;
    }

    public void receiveToken() {
        token = true;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                token = false;
                forwarder.handOffToken(leftNeighbor);
            }
        }, 2000);
    }

    public boolean hasToken() {
        return token;
    }

    public void initiateSnapshot(boolean isInitiatior, RecordingMode mode) {
        this.isInitiator = isInitiatior;
        // Speichere lokalen Zustand
        localState = cntFishies;

        // Initiator startet Aufzeichungsmodus für alle Eingangskanäle (BOTH)
        // Rest startet Aufzeichungsmodus für alle anderen Eingangskanäle
        recordingMode = mode;

        // sende Markierungen in allen Ausgangskanäle
        forwarder.sendSnapshotMarker(leftNeighbor);
        forwarder.sendSnapshotMarker(rightNeighbor);
    }

    synchronized void onRegistration(String id) {
        this.id = id;
        newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
    }

    public synchronized void newFish(int x, int y) {
        if (fishies.size() < MAX_FISHIES) {
            x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
            y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

            cntFishies++;
            FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
                    rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

            //Vorwärtsreferenzen
            fishDirectionReferenceMap.put(fish.getId(), FishDirectionReference.HERE);

            //Heimatgestützt
            homeAgent.put(fish.getId(), null);

            fishies.add(fish);
        }
    }

    // Skript5 17-18
    public void receiveSnapshotMarker(InetSocketAddress sender) {
        // falls es sich nicht im Aufzeichungsmodus befinden
        if (recordingMode == RecordingMode.IDLE) {
            // starte Aufzeichnungsmodus für alle anderen Eingangskanäle
            if (sender.equals(leftNeighbor)) {
                initiateSnapshot(false, RecordingMode.RIGHT);
            } else {
                initiateSnapshot(false, RecordingMode.LEFT);
            }
        } else if (recordingMode == RecordingMode.LEFT || recordingMode == RecordingMode.RIGHT) {
            recordingMode = RecordingMode.IDLE;

            if (isInitiator) {
                SnapshotToken token = new SnapshotToken(localState);
                forwarder.sendSnapshotToken(leftNeighbor, token);
            } else {
                localSnapshotDone = true;
                // Hatte Token schon aber war nicht fertig mit dem Snapshot. Jetzt bin ich fertig mit Snapshot, dann schicke ich es weiter.
                if (hasSnapshotToken) {
                    snapshotToken.increaseFishies(localState);
                    forwarder.sendSnapshotToken(leftNeighbor, snapshotToken);
                    hasSnapshotToken = false;
                    localSnapshotDone = false;
                }
            }
        } else if (sender.equals(leftNeighbor) && recordingMode == RecordingMode.BOTH) {
            recordingMode = RecordingMode.RIGHT;
        } else if (sender.equals(rightNeighbor) && recordingMode == RecordingMode.BOTH) {
            recordingMode = RecordingMode.LEFT;
        }
    }

    synchronized void receiveFish(FishModel fish) {
        cntFishies++;
        if (recordingMode == RecordingMode.LEFT && fish.getDirection() == Direction.RIGHT) {
            localState++;
        } else if (recordingMode == RecordingMode.RIGHT && fish.getDirection() == Direction.LEFT) {
            localState++;
        } else if (recordingMode == RecordingMode.BOTH) {
            localState++;
        }

        fish.setToStart();

        //Vorwärtsreferenzen
        fishDirectionReferenceMap.put(fish.getId(), FishDirectionReference.HERE);

        //Heimatgesützt
        //wenn fisch wieder in den eigenen Tank schwimmt - Aufgabenblatt Fall a
        if (fish.getTankId().equals(id)) {
            homeAgent.put(fish.getId(), null);
        } else {//Fisch schwimmt in einen anderen Tank - Fall b
            forwarder.sendNameResolutionRequest(new NameResolutionRequest(fish.getTankId(), fish.getId()));
        }
        fishies.add(fish);
    }

    public String getId() {
        return id;
    }

    public synchronized int getFishCounter() {
        return fishCounter;
    }

    public synchronized Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private synchronized void updateFishies() {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();

            fish.update();

            if (fish.hitsEdge() && hasToken()) {
                cntFishies--;
                if (fish.getDirection().equals(Direction.LEFT)) {
                    //Vorwärtsreferenzen
                    fishDirectionReferenceMap.put(fish.getId(), FishDirectionReference.LEFT);

                    forwarder.handOff(fish, leftNeighbor);
                } else {
                    //Vorwärtsreferenzen
                    fishDirectionReferenceMap.put(fish.getId(), FishDirectionReference.RIGHT);

                    forwarder.handOff(fish, rightNeighbor);
                }
            } else if (fish.hitsEdge()) {
                fish.reverse();
            }

            if (fish.disappears())
                it.remove();
        }
    }

    private synchronized void update() {
        updateFishies();
        setChanged();
        notifyObservers();
    }

    protected void run() {
        forwarder.register();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                update();
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException consumed) {
            // allow method to terminate
        }
    }

    public synchronized void finish() {
        if (token) {
            forwarder.handOffToken(leftNeighbor);
        }
        forwarder.deregister(id);
    }

    public void receiveSnapshotToken(SnapshotToken token) {
        if (isInitiator) {
            // Token ist zurück gekommen. Zeige Anzahl an Fishies
            globalSnapshotDone = true;
            snapshotToken = token;
        } else {
            hasSnapshotToken = true;
            snapshotToken = token;
            // Hab Token und bin fertig mit dem Snapshot, dann gleich schicken
            if (localSnapshotDone) {
                token.increaseFishies(localState);
                forwarder.sendSnapshotToken(leftNeighbor, token);
                hasSnapshotToken = false;
                localSnapshotDone = false;
            }
        }
    }

    //Vorwärtsreferenzen
    public void locateFishGlobally(String fishId) {
        if (fishDirectionReferenceMap.get(fishId) == FishDirectionReference.HERE) {
            locateFishLocally(fishId);
        } else if (fishDirectionReferenceMap.get(fishId) == FishDirectionReference.LEFT) {
            forwarder.sendLocationRequest(leftNeighbor, fishId);
        } else {
            forwarder.sendLocationRequest(rightNeighbor, fishId);
        }
    }

    //Heimatgestützt
    public void locateFishGloballyHomeAgent(String fishId) {
        if (homeAgent.get(fishId) == null) { //fisch ist momentan im eigenen Tank
            System.out.println(homeAgent);
            locateFishLocally(fishId);
        } else {
            System.out.println(homeAgent);
            forwarder.sendLocationRequest(homeAgent.get(fishId), fishId);
        }
    }

    //Vorwärtsreferenzen & Heimatgestützt
    public void locateFishLocally(String fishId) {
        for (FishModel fish : fishies) {
            if (fishId.equals(fish.getId())) {
                fish.toggle();
            }
        }
    }

    //Heimatgestützt
    public void receiveNameResolutionResponse(NameResolutionResponse nrr) {
        forwarder.sendLocationUpdate(nrr.getAddress(), new LocationUpdate(nrr.getRequestId()));
    }

    //Heimatgestützt
    public void updateFishLocation(LocationUpdate lu, InetSocketAddress sender) {
        homeAgent.put(lu.getFishId(), sender);
    }
}

enum FishDirectionReference {
    HERE,
    LEFT,
    RIGHT
}