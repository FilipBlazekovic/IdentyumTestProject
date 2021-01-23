package hr.identyum.utils;

public class Utils {

    public static String generateErrorMessage(Exception ex)
    {
        StringBuilder errorMessageBuilder = new StringBuilder();
        errorMessageBuilder.append("*** EXCEPTION ***\n");
        errorMessageBuilder.append("Message: " + ex.getMessage() + "\n");       
        StackTraceElement[] stackTrace = ex.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++)
        {
            errorMessageBuilder.append(stackTrace[i].toString() + "\n");
        }
        return errorMessageBuilder.toString();
    }

    public static boolean checkIfImageBasedOnMagicBytes(byte[] rawImageBytes)
    {
//      JPG
//      ---
//      FF D8 FF DB
//      FF D8 FF E0 00 10 4A 46 49 46 00 01
//      FF D8 FF EE
//      FF D8 FF E1 ?? ?? 45 78 69 66 00 00
//
//      PNG
//      ---
//      89 50 4E 47 0D 0A 1A 0A
//
//      GIF
//      ---
//      47 49 46 38 37 61
//      47 49 46 38 39 61
        
        return true;
    }   
}
