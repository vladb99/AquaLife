package aqua.blatt1.broker;

import aqua.blatt1.client.AquaClient;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AquaBroker extends Remote {
    void register(AquaClient sender) throws RemoteException;
    void deregister(AquaClient sender) throws RemoteException;
    void handleNameResolutionRequest(AquaClient sender, NameResolutionRequest nrr) throws RemoteException;
}
