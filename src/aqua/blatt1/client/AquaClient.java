package aqua.blatt1.client;

import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.Token;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AquaClient extends Remote {
    void handleRegisterResponse(AquaClient sender, RegisterResponse registerResponse) throws RemoteException;
    void handleHandoffRequest(AquaClient sender) throws RemoteException;
    void handleNeighborUpdate(AquaClient sender, NeighborUpdate neighborUpdate) throws RemoteException;
    void handleToken(AquaClient sender, Token token) throws RemoteException;
    void handleSnapshotMarker(AquaClient sender) throws RemoteException;
    void handleSnapshotToken(AquaClient sender) throws RemoteException;
    void handleLocationRequest(AquaClient sender) throws RemoteException;
    void handleNameResolutionResponse(AquaClient sender, String requestId) throws RemoteException;
    void handleLocationUpdate(AquaClient sender) throws RemoteException;
}
