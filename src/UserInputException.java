public class UserInputException extends Exception {
    private String message;
    public UserInputException(String str) {
        message = str;
    }

    public String getMessage() {
        return message;
    }
}
