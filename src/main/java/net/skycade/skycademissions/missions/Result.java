package net.skycade.skycademissions.missions;

public class Result {

    public Type type;

    private String message;

    public Result(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    boolean asBoolean() {
        return type == Type.SUCCESS;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum Type {
        SUCCESS,
        FAILURE
    }
}
