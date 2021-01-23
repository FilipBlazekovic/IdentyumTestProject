package hr.identyum.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import hr.identyum.model.Authenticator;
import hr.identyum.utils.CryptoUtils;
import hr.identyum.utils.Utils;

@RestController
public class EndpointLogin {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(EndpointLogin.class);

    private static final String insertSessionSQL     = "INSERT INTO sessions(user_id, session_id) VALUES ((SELECT id FROM users WHERE username = ?), ?)";
    private static final String fetchPasswordHashSQL = "SELECT password, salt FROM users WHERE username = ?";
    
    /* -------------------------------------------------------------------------------------------------------- */

    @RequestMapping(value = "/login", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void processLoginRequest(@RequestBody Authenticator authenticator, HttpServletResponse response)
    {
        logger.info("=> POST /login");
      
        String username = null;
        char[] password = null;
      
        if (authenticator != null)
        {
            username = authenticator.getUsername();
            password = authenticator.getPassword();

            if (username != null)
                username = username.trim();
        }

        logger.info("Username: " + (username != null ? username : ""));
        if (password == null)
            logger.info("Password: ");
        else
        {
            StringBuilder maskedPassword = new StringBuilder();
            for (int i = 0; i < password.length; i++) maskedPassword.append("*");
            logger.info("Password: " + maskedPassword.toString());
        }


        logger.info("Performing validation...");
        if (username == null || username.length() == 0)
        {
            logger.info("Validation fail => Missing username!");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        else if (password == null || password.length < 8)
        {
            logger.info("Validation fail => Missing/invalid password!");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        else
        {
            try
            {
                List<Map<String,Object>> matchFound = jdbcTemplate.queryForList(fetchPasswordHashSQL, username);          
                if (matchFound == null || matchFound.size() == 0)
                {
                    logger.info("Validation fail => User not found!");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                else
                {
                    String dbPasswordHash         = (String) matchFound.get(0).get("password");
                    String dbSalt                 = (String) matchFound.get(0).get("salt");
                    String calculatedPasswordHash = CryptoUtils.generatePasswordHash(password, dbSalt);

                    if (!calculatedPasswordHash.equals(dbPasswordHash))
                    {
                        logger.info("Validation fail => Invalid password!");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                    else
                    {
                        logger.info("Performing login...");
                        String sessionID = CryptoUtils.generateSessionID();
                        jdbcTemplate.update(insertSessionSQL, username, sessionID);

                        Cookie sessionCookie = new Cookie("sessionID", sessionID);
//                      sessionCookie.setHttpOnly(true);
//                      sessionCookie.setSecure(true);
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.addCookie(sessionCookie);
                      
                        logger.info("Returning HTTP 200");
                    }
                }
            }
            catch (DataAccessException ex)
            {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                logger.error(Utils.generateErrorMessage(ex));
            }
        }
    }
}
