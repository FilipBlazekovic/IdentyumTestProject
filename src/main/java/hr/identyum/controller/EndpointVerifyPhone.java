package hr.identyum.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.vonage.client.verify.CheckResponse;
import com.vonage.client.verify.VerifyResponse;
import com.vonage.client.verify.VerifyStatus;

import hr.identyum.IdentyumTestProject;
import hr.identyum.model.Authenticator;
import hr.identyum.utils.CryptoUtils;
import hr.identyum.utils.Utils;

@RestController
public class EndpointVerifyPhone {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(EndpointVerifyPhone.class);
    
    private static final String deletePhoneIfExistsSQL = "DELETE FROM phones " +
                                                         "WHERE phone_number = ? " +
                                                         "AND user_id = (SELECT user_id FROM sessions WHERE session_id = ?)";

    private static final String insertPhoneVerificationRequestSQL = "INSERT INTO phones(user_id, phone_number, verification_request_id) " +
                                                                    "VALUES ((SELECT user_id FROM sessions WHERE session_id = ?),?,?)";

    private static final String readVerificationRequestIDSQL = "SELECT verification_request_id " +
                                                               "FROM phones " +
                                                               "WHERE user_id = (SELECT user_id FROM sessions WHERE session_id = ?) " +
                                                               "ORDER BY creation_tstamp DESC LIMIT 1";
    
    private static final String verifyPhoneSQL = "UPDATE phones " +
                                                 "SET verified = true " +
                                                 "WHERE verification_request_id = ? " +
                                                 "AND user_id = (SELECT user_id FROM sessions WHERE session_id = ?)";

    /* ---------------------------------------------------------------------------------------------------------------------------------- */
    
    @RequestMapping(value = "/verify", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void processVerifyRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID,
                                     @RequestBody Authenticator authenticator,
                                     HttpServletResponse response)
    {
        logger.info("=> POST /verify");

        String phoneNumber = null;      
        if (authenticator != null)
            phoneNumber = authenticator.getPhoneNumber();

        if (phoneNumber != null)
            phoneNumber = phoneNumber.trim().replaceAll("\\+", "").replaceAll("\\-", "").replaceAll("/", "").replaceAll(" ", "");

        logger.info("Phone Number: " + (phoneNumber != null ? phoneNumber : ""));       
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:            
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                logger.info("Validation fail => SessionID missing/invalid!");
                break;

            default:

                if (phoneNumber == null || !phoneNumber.matches("[0-9]{9,15}"))
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    logger.info("Validation fail => Missing/Invalid phoneNumber!");
                }
                else
                {
                    logger.info("Sending request to Vonage servers...");
                    try
                    {
                        VerifyResponse vonageResponse = IdentyumTestProject.vonageClient.getVerifyClient().verify(phoneNumber, "Vonage");
                        if (vonageResponse.getStatus() == VerifyStatus.OK)
                        {
                            String verificationRequestID = vonageResponse.getRequestId();                       
                            logger.info(String.format("VerificationRequestID: %s", verificationRequestID));

                            jdbcTemplate.update(deletePhoneIfExistsSQL, phoneNumber, sessionID);                            
                            jdbcTemplate.update(insertPhoneVerificationRequestSQL, sessionID, phoneNumber, verificationRequestID);

                            logger.info("Returning HTTP 200");
                            response.setStatus(HttpServletResponse.SC_OK);
                        }
                        else
                        {
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            logger.info(String.format("ERROR! %s: %s", vonageResponse.getStatus(), vonageResponse.getErrorText()));
                        }
                    }
                    catch (Exception ex)
                    {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        logger.error(Utils.generateErrorMessage(ex));
                    }
                }
        }
    }
    
    /* ---------------------------------------------------------------------------------------------------------------------------------- */
    
    @RequestMapping(value = "/submit-otp", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void processSubmitOTPRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID,
                                        @RequestBody Authenticator authenticator,
                                        HttpServletResponse response)
    {
        logger.info("=> POST /submit-otp");

        String verificationCode      = null;
        String verificationRequestID = null;
        if (authenticator != null)
            verificationCode = authenticator.getVerificationCode();

        logger.info("VerificationCode: " + (verificationCode != null ? verificationCode.replaceAll(".", "*") : ""));        
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                logger.info("Validation fail => SessionID missing/invalid!");
                break;

            default:

                if (verificationCode == null || verificationCode.length() == 0)
                {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    logger.info("Validation fail => Missing verificationCode!");
                }
                else
                {
                    try
                    {
                        List<Map<String,Object>> matchFound = jdbcTemplate.queryForList(readVerificationRequestIDSQL, sessionID);     
                        if (matchFound == null || matchFound.size() == 0)
                        {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            logger.info("Validation fail => VerificationRequestID not found!");
                        }
                        else
                        {
                            verificationRequestID = (String) matchFound.get(0).get("verification_request_id");                      
                            CheckResponse vonageResponse = IdentyumTestProject.vonageClient.getVerifyClient().check(verificationRequestID, verificationCode);
                            if (vonageResponse.getStatus() == VerifyStatus.OK)
                            {
                                logger.info("Mobile phone verified!");
                                logger.info("Updating database...");
                                jdbcTemplate.update(verifyPhoneSQL, verificationRequestID, sessionID);  

                                logger.info("Returning HTTP 200");
                                response.setStatus(HttpServletResponse.SC_OK);
                            }
                            else
                            {
                                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                logger.info(String.format("ERROR! %s: %s", vonageResponse.getStatus(), vonageResponse.getErrorText()));
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        logger.error(Utils.generateErrorMessage(ex));
                    }
                }
        }
    }
}
