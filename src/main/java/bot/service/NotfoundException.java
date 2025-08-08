package bot.service;

public class NotfoundException extends Exception {

	public NotfoundException() {
		super();
	}

	public NotfoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotfoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotfoundException(String message) {
		super(message);
	}

	public NotfoundException(Throwable cause) {
		super(cause);
	}

}
