class ResponseException extends Exception {
    private String errorMessage;
    ResponseException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    String getErrorMessage() {
        return errorMessage;
    }
}
