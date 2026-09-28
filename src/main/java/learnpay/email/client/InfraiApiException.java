package learnpay.email.client;

public final class InfraiApiException extends RuntimeException {
    private final String code;
    private final int statusCode;

    public InfraiApiException(String code, int statusCode) {
        super(code);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String code() { return code; }
    public int statusCode() { return statusCode; }
}
