package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.RecordingMode;
import aqua.blatt1.common.msgtypes.SnapshotToken;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
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
		localState = fishCounter;

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

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	// Skript5 17-18
	public void receiveSnapshotMarker(InetSocketAddress sender){
		// falls es sich nicht im Aufzeichungsmodus befinden
		if(recordingMode == RecordingMode.IDLE) {
			// starte Aufzeichnungsmodus für alle anderen Eingangskanäle
			if(sender.equals(leftNeighbor)){
				initiateSnapshot(false, RecordingMode.RIGHT);
			} else {
				initiateSnapshot(false, RecordingMode.LEFT);
			}
		} else if(recordingMode == RecordingMode.LEFT || recordingMode == RecordingMode.RIGHT) {
			recordingMode = RecordingMode.IDLE;

			if(isInitiator) {
				SnapshotToken token = new SnapshotToken(localState);
				forwarder.sendSnapshotToken(leftNeighbor, token);
			} else {
				localSnapshotDone = true;
				if(hasSnapshotToken){


					System.out.println("Received snapshot marker - idle - hasSnapshotToken");

					snapshotToken.increaseFishies(localState);
					forwarder.sendSnapshotToken(leftNeighbor, snapshotToken);
					hasSnapshotToken = false;
					localSnapshotDone = false;
				}


			}
		}else if(sender.equals(leftNeighbor) && recordingMode == RecordingMode.BOTH) {
			System.out.println("Received snapshot marker - from left");
			recordingMode = RecordingMode.RIGHT;
		}else if(sender.equals(rightNeighbor) && recordingMode == RecordingMode.BOTH) {
			System.out.println("Received snapshot marker - from right");
			recordingMode = RecordingMode.LEFT;
		}else{
			System.out.println("THIS IS THE ELSE CASE");
		}
	}

	synchronized void receiveFish(FishModel fish) {
		if(recordingMode == RecordingMode.LEFT && fish.getDirection() == Direction.RIGHT){
			localState++;
		}else if(recordingMode == RecordingMode.RIGHT && fish.getDirection() == Direction.LEFT){
			localState++;
		}else if(recordingMode == RecordingMode.BOTH){
			localState++;
		}

		fish.setToStart();
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
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge() && hasToken()) {
				if (fish.getDirection().equals(Direction.LEFT)) {
					forwarder.handOff(fish, leftNeighbor);
				} else {
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
		if(token){
			forwarder.handOffToken(leftNeighbor);
		}
		forwarder.deregister(id);
	}

	public void receiveSnapshotToken(SnapshotToken token) {
		if (isInitiator) {
			System.out.println("Received snapshot token - global snapshot done");

			globalSnapshotDone = true;
			snapshotToken = token;
		} else {
			System.out.println("Received snapshot token");

			hasSnapshotToken = true;
			snapshotToken = token;
			if(localSnapshotDone) {
				System.out.println("Received snapshot token - local snapshot done");

				token.increaseFishies(localState);
				forwarder.sendSnapshotToken(leftNeighbor, token);
				hasSnapshotToken = false;
				localSnapshotDone = false;
			}
		}
	}
}