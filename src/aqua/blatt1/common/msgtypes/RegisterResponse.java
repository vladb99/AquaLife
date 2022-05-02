package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final int lease;

	public RegisterResponse(String id, int lease) {
		this.id = id;
		this.lease = lease;
	}

	public String getId() {
		return id;
	}

    public int getLease() {
        return lease;
    }
}
