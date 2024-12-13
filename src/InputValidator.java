public class InputValidator {
    public static boolean isValid(String input, String errorMessage) {
        if (input == null || input.trim().isEmpty()) {
            System.out.println(errorMessage);
            return false;
        }
        return true;
    }
}
