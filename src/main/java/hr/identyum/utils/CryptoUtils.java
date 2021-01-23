package hr.identyum.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

public class CryptoUtils {

    public static final int SESSION_ID_VALID_PHONE_VERIFIED = 0;
    public static final int SESSION_ID_VALID_PHONE_UNVERIFIED = 1;
    public static final int SESSION_ID_INVALID = 3;

    private static final String validateSessionIDSQL            = "SELECT user_id FROM sessions WHERE session_id = ?";
    private static final String validateConfirmedPhoneNumberSQL = "SELECT id FROM phones WHERE user_id = ? AND verified = true";
    
    /* ---------------------------------------------------------------------------------------------------------------------- */
    
    public static String generateSalt()
    {
        SecureRandom generator = new SecureRandom();
        byte[] salt = new byte[4];
        generator.nextBytes(salt);      
        return convertByteArrayToHexString(salt);
    }
    
    /* ---------------------------------------------------------------------------------------------------------------------- */
    
    public static String generatePasswordHash(char[] password, String salt)
    {
        try
        {
            ByteBuffer buffer = Charset.forName("ISO-8859-1").encode(CharBuffer.wrap(password));
            byte[] passwordRaw = new byte[buffer.remaining()];
            buffer.get(passwordRaw);
            
            final MessageDigest digester = MessageDigest.getInstance("SHA-256");                        
            digester.update(passwordRaw);
            digester.update(salt.getBytes("ISO-8859-1"));           
            byte[] passwordHash = digester.digest();            

            Arrays.fill(password, '\0');
            Arrays.fill(passwordRaw, (byte) 0);
            Arrays.fill(buffer.array(), (byte) 0);
            
            return convertByteArrayToHexString(passwordHash);
        }
        catch (Exception ex) { return null; }
    }

    /* ---------------------------------------------------------------------------------------------------------------------- */
    
    public static String generateSessionID()
    {   
        String sessionID = null;    
        try
        {
            final String nanotimePrefix = String.valueOf(System.nanoTime());
            
            final SecureRandom randomGenerator = new SecureRandom();
            final byte[] randomBytes = new byte[16];
            randomGenerator.nextBytes(randomBytes);
            
            final MessageDigest sha256Digester = MessageDigest.getInstance("SHA-256");
            sha256Digester.update(nanotimePrefix.getBytes("ISO-8859-1"));
            sha256Digester.update(randomBytes);
            final byte[] rawResult = sha256Digester.digest();
            
            sessionID = convertByteArrayToHexString(rawResult);
        }
        catch (Exception ex) {}
        
        return sessionID;
    }
    
    /* ---------------------------------------------------------------------------------------------------------------------- */
    
    private static String convertByteArrayToHexString(byte[] raw)
    {
       final StringBuilder builder = new StringBuilder();
       for (int i = 0; i < raw.length; i++)
       {
           builder.append(String.format("%02X", raw[i]));
       }
       return builder.toString();
    }
    
    /* ---------------------------------------------------------------------------------------------------------------------- */

    public static int validateSessionID(JdbcTemplate jdbcTemplate, String sessionID)
    {   
        int status = SESSION_ID_INVALID;
        
        List<Map<String,Object>> matchFound = jdbcTemplate.queryForList(validateSessionIDSQL, sessionID);
        if (matchFound != null && matchFound.size() > 0)
        {
            Long userID = (Long) matchFound.get(0).get("user_id");          
            List<Map<String,Object>> verifiedPhoneMatch = jdbcTemplate.queryForList(validateConfirmedPhoneNumberSQL, userID);
        
            if (verifiedPhoneMatch == null || verifiedPhoneMatch.size() == 0)
                status = SESSION_ID_VALID_PHONE_UNVERIFIED;
            else
                status = SESSION_ID_VALID_PHONE_VERIFIED;           
        }
                
        return status;      
    }

    /* ---------------------------------------------------------------------------------------------------------------------- */

}
