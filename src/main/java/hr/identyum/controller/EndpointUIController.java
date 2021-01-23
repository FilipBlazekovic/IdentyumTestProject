package hr.identyum.controller;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import hr.identyum.model.Image;
import hr.identyum.model.SharePermission;
import hr.identyum.model.User;
import hr.identyum.utils.CryptoUtils;
import hr.identyum.utils.Utils;

@RestController
public class EndpointUIController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final Logger logger = LoggerFactory.getLogger(EndpointUIController.class);

    private static final String getUserSQL         = "SELECT id, username FROM users WHERE id = (SELECT user_id FROM sessions WHERE session_id = ?)";
    private static final String getPhoneNumbersSQL = "SELECT phone_number FROM phones WHERE verified = true AND user_id = ?";
     
    private static final String getSharedWithUsersForImageSQL = "SELECT n.username AS username " +
                                                                "FROM permissions m " +
                                                                "INNER JOIN users n ON n.id = m.permitted_user_id " +
                                                                "WHERE m.image_id = ?";

    private static final String getMyImagesSQL = "SELECT " +
                                                 "x.id AS image_id, " +
                                                 "y.username AS image_owner, " +
                                                 "x.image_name AS image_name, " +
                                                 "x.image_data AS image_data " +
                                                 "FROM images x " +
                                                 "INNER JOIN users y ON y.id = x.owner_id " +
                                                 "WHERE x.owner_id = (SELECT user_id FROM sessions WHERE session_id = ?) ";
    
    private static final String getSharedImagesSQL = "SELECT " +
                                                     "x.id AS image_id, " +
                                                     "y.username AS image_owner, " +
                                                     "x.image_name AS image_name, " +
                                                     "x.image_data AS image_data " +
                                                     "FROM images x " +
                                                     "INNER JOIN users y ON y.id = x.owner_id " +
                                                     "WHERE x.id IN (SELECT image_id FROM permissions WHERE permitted_user_id = (SELECT user_id FROM sessions WHERE session_id = ?))";

    private static final String deleteImageSQL = "DELETE FROM images WHERE id = ? AND owner_id = (SELECT user_id FROM sessions WHERE session_id = ?)";

    private static final String insertImageSQL = "INSERT INTO images(owner_id, image_name, image_data) " +
                                                 "VALUES ((SELECT user_id FROM sessions WHERE session_id = ?), ?, ?)";
    
    private static final String shareImageSQL = "INSERT INTO permissions(image_id, permitted_user_id) VALUES (?, (SELECT id FROM users WHERE username = ?))";

    private static final String unshareImageSQL = "DELETE FROM permissions " +
                                                  "WHERE image_id = ? " +
                                                  "AND permitted_user_id = (SELECT id FROM users WHERE username = ?) " +
                                                  "AND image_id IN (SELECT id FROM images WHERE id = ? AND owner_id = (SELECT user_id FROM sessions WHERE session_id = ?))";

    /* ------------------------------------------------------------------------------------------------------------------------ */

    @RequestMapping(value = "/details", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> processGetDetailsRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID)
    {
        logger.info("=> GET /details");
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:
                logger.info("alidation fail => SessionID missing/invalid!");
                return new ResponseEntity<User>(HttpStatus.UNAUTHORIZED);

            case CryptoUtils.SESSION_ID_VALID_PHONE_UNVERIFIED:
                logger.info("Validation fail => Phone number not verified!");
                return new ResponseEntity<User>(HttpStatus.FORBIDDEN);
            
//          case CryptoUtils.SESSION_ID_VALID_PHONE_VERIFIED:
            default:

                try
                {
                    logger.info("Retrieving values from the database...");
                    List<Map<String,Object>> userDetails = jdbcTemplate.queryForList(getUserSQL, sessionID);
                    Long userID     = (Long) userDetails.get(0).get("id");
                    String username = (String) userDetails.get(0).get("username");
                    
                    List<String> phoneNumbers = new ArrayList<String>();
                    List<Map<String,Object>> retrievedPhoneNumbers = jdbcTemplate.queryForList(getPhoneNumbersSQL, userID);
                    for (Map<String,Object> currentPhoneNumber : retrievedPhoneNumbers)
                    {
                        phoneNumbers.add((String)currentPhoneNumber.get("phone_number"));
                    }

                    User user = new User();
                    user.setUsername(username);
                    user.setPhoneNumbers(phoneNumbers);

                    logger.info("Returning HTTP 200");
                    return new ResponseEntity<User>(user, HttpStatus.OK);
                }
                catch (DataAccessException ex)
                {
                    logger.error(Utils.generateErrorMessage(ex));
                    return new ResponseEntity<User>(HttpStatus.INTERNAL_SERVER_ERROR);

                }
        }
    }
    
    /* -------------------------------------------------------------------------------------------------------------------- */

    @RequestMapping(value = "/my-images", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Image>> processGetMyImagesRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID)
    {
        logger.info("=> GET /my-images");
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:
                logger.info("Validation fail => SessionID missing/invalid!");
                return new ResponseEntity<List<Image>>(HttpStatus.UNAUTHORIZED);

            case CryptoUtils.SESSION_ID_VALID_PHONE_UNVERIFIED:
                logger.info("Validation fail => Phone number not verified!");
                return new ResponseEntity<List<Image>>(HttpStatus.FORBIDDEN);

//          case CryptoUtils.SESSION_ID_VALID_PHONE_VERIFIED:
            default:

                try
                {
                    logger.info("Retrieving values from the database...");
                    List<Image> sharedImages = new ArrayList<Image>();
                    List<Map<String,Object>> queryResult = jdbcTemplate.queryForList(getMyImagesSQL, sessionID);
                    for (Map<String,Object> temp : queryResult)
                    {

                        Long currentImageID      = (Long) temp.get("image_id");
                        String currentImageOwner = (String) temp.get("image_owner");
                        String currentImageName  = (String) temp.get("image_name");
                        String currentImageData  = (String) temp.get("image_data");
                        List<String> sharedUsers = new ArrayList<String>();
                        
                        List<Map<String,Object>> sharedUsersQueryResult = jdbcTemplate.queryForList(getSharedWithUsersForImageSQL, currentImageID);
                        if (sharedUsersQueryResult != null && sharedUsersQueryResult.size() > 0)
                        {
                            for (Map<String,Object> currentSharedUser : sharedUsersQueryResult)
                            {
                                sharedUsers.add((String)currentSharedUser.get("username"));
                            }
                        }
                        
                        Image image = new Image();
                        image.setId(currentImageID);
                        image.setOwner(currentImageOwner);
                        image.setSharedWith(sharedUsers);
                        image.setName(currentImageName);
                        image.setData(currentImageData);
                        sharedImages.add(image);
                    }

                    logger.info("Returning HTTP 200");
                    return new ResponseEntity<List<Image>>(sharedImages, HttpStatus.OK);
                }
                catch (DataAccessException ex)
                {
                    logger.error(Utils.generateErrorMessage(ex));
                    return new ResponseEntity<List<Image>>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
        }
    }

    /* -------------------------------------------------------------------------------------------------------------------- */
    
    @RequestMapping(value = "/shared-images", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Image>> processGetSharedImagesRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID)
    {
        logger.info("=> GET /shared-images");
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:
                logger.info("Validation fail => SessionID missing/invalid!");
                return new ResponseEntity<List<Image>>(HttpStatus.UNAUTHORIZED);

            case CryptoUtils.SESSION_ID_VALID_PHONE_UNVERIFIED:
                logger.info("Validation fail => Phone number not verified!");
                return new ResponseEntity<List<Image>>(HttpStatus.FORBIDDEN);

//          case CryptoUtils.SESSION_ID_VALID_PHONE_VERIFIED:
            default:

                try
                {
                    logger.info("Retrieving values from the database...");
                    List<Image> sharedImages = new ArrayList<Image>();
                    List<Map<String,Object>> queryResult = jdbcTemplate.queryForList(getSharedImagesSQL, sessionID);
                    for (Map<String,Object> temp : queryResult)
                    {
                        
                        Long currentImageID      = (Long) temp.get("image_id");
                        String currentImageOwner = (String) temp.get("image_owner");
                        String currentImageName  = (String) temp.get("image_name");
                        String currentImageData  = (String) temp.get("image_data");

                        Image image = new Image();
                        image.setId(currentImageID);
                        image.setOwner(currentImageOwner);
                        image.setName(currentImageName);
                        image.setData(currentImageData);
                        sharedImages.add(image);
                    }
                    logger.info("Returning HTTP 200");
                    return new ResponseEntity<List<Image>>(sharedImages, HttpStatus.OK);    
                }
                catch (DataAccessException ex)
                {
                    logger.error(Utils.generateErrorMessage(ex));
                    return new ResponseEntity<List<Image>>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
        }
    }

    /* -------------------------------------------------------------------------------------------------------------------- */

    @RequestMapping(value = "/image/{id}", method = RequestMethod.DELETE)
    public void processDeleteImageRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID,
                                          @PathVariable String id,
                                          HttpServletResponse response)
    {
        logger.info("=> DELETE /image/{id}");
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                logger.info("Validation fail => SessionID missing/invalid!");
                break;

            case CryptoUtils.SESSION_ID_VALID_PHONE_UNVERIFIED:
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                logger.info("Validation fail => Phone number not verified!");
                break;

//          case CryptoUtils.SESSION_ID_VALID_PHONE_VERIFIED:
            default:
                try
                {
                    logger.info("Deleting image from the database...");
                    jdbcTemplate.update(deleteImageSQL, id, sessionID);

                    logger.info("Returning HTTP 200");
                    response.setStatus(HttpServletResponse.SC_OK);
                }
                catch (DataAccessException ex)
                {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    logger.error(Utils.generateErrorMessage(ex));
                }
        }
    }
    
    /* -------------------------------------------------------------------------------------------------------------------- */

    @RequestMapping(value = "/image", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void processImageUploadRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID,
                                                    @RequestParam MultipartFile file,
                                                    HttpServletResponse response)
    {
        logger.info("=> POST /image");
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                logger.info("Validation fail => SessionID missing/invalid!");
                break;

            case CryptoUtils.SESSION_ID_VALID_PHONE_UNVERIFIED:
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                logger.info("Validation fail => Phone number not verified!");
                break;

//          case CryptoUtils.SESSION_ID_VALID_PHONE_VERIFIED:
            default:
                
                try
                {
                    String imageName = file.getOriginalFilename();
                    byte[] imageBytesRaw = file.getBytes();
                    
                    if (imageName == null || imageName.trim().length() == 0)
                    {
                        logger.info("Validation fail => Missing image name!");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                    else if (imageBytesRaw == null || imageBytesRaw.length == 0)
                    {
                        logger.info("Validation fail => Missing image!");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                    else if (!Utils.checkIfImageBasedOnMagicBytes(imageBytesRaw))
                    {
                        logger.info("Validation fail => Unsupported image format!");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                    else
                    {
                        logger.info("Inserting image into the database...");
                        String base64ImageData = Base64.getEncoder().encodeToString(imageBytesRaw);         
                        jdbcTemplate.update(insertImageSQL, sessionID, imageName.trim(), base64ImageData); 

                        logger.info("Returning HTTP 200");
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                }
                catch (Exception ex)
                {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    logger.error(Utils.generateErrorMessage(ex));
                }
        }       
    }
    
    /* -------------------------------------------------------------------------------------------------------------------- */

    @RequestMapping(value = "/share", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void processShareImageRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID,
                                         @RequestBody SharePermission sharePermission,
                                         HttpServletResponse response)
    {
        logger.info("=> POST /share");
        
        Long imageId             = null;
        String permittedUsername = null;

        if (sharePermission != null)
        {
            imageId              = sharePermission.getImageId();
            permittedUsername    = sharePermission.getUsername();
            if (permittedUsername != null)
                permittedUsername = permittedUsername.trim();
        }
        
        logger.info("ImageID: "  + (imageId != null ? imageId : ""));
        logger.info("Username: " + (permittedUsername != null ? permittedUsername : ""));
        
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                logger.info("Validation fail => SessionID missing/invalid!");
                break;

            case CryptoUtils.SESSION_ID_VALID_PHONE_UNVERIFIED:
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                logger.info("Validation fail => Phone number not verified!");
                break;

//          case CryptoUtils.SESSION_ID_VALID_PHONE_VERIFIED:
            default:

                if (imageId == null)
                {
                    logger.info("Validation fail => Missing imageId!");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }               
                else if (permittedUsername == null || permittedUsername.length() == 0)
                {
                    logger.info("Validation fail => Missing username!");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                else
                {
                    try
                    {
                        List<Map<String,Object>> userDetails = jdbcTemplate.queryForList(getUserSQL, sessionID);
                        String myUsername = (String) userDetails.get(0).get("username");
                        if (myUsername.equals(permittedUsername))
                        {
                            logger.info("Validation fail => Can't share the image with image owner!");
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        }
                        else
                        {
                            logger.info("Inserting permission into the database...");
                            jdbcTemplate.update(shareImageSQL, imageId, permittedUsername);
                            logger.info("Returning HTTP 200");
                            response.setStatus(HttpServletResponse.SC_OK);
                        }
                    }
                    catch (DataAccessException ex)
                    {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        logger.error(Utils.generateErrorMessage(ex));
                    }
                }
        }
    }

    /* -------------------------------------------------------------------------------------------------------------------- */

    @RequestMapping(value = "/unshare", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void processUnshareImageRequest(@CookieValue(value = "sessionID", defaultValue = "") String sessionID,
                                           @RequestBody SharePermission sharePermission,
                                           HttpServletResponse response)
    {
        logger.info("=> POST /unshare");

        Long imageId             = null;
        String permittedUsername = null;

        if (sharePermission != null)
        {
            imageId              = sharePermission.getImageId();
            permittedUsername    = sharePermission.getUsername();
            if (permittedUsername != null)
                permittedUsername = permittedUsername.trim();
        }
        
        logger.info("ImageID: "  + (imageId != null ? imageId : ""));
        logger.info("Username: " + (permittedUsername != null ? permittedUsername : ""));
        
        logger.info("Performing validation...");
        int status = CryptoUtils.validateSessionID(jdbcTemplate, sessionID);
        switch (status)
        {
            case CryptoUtils.SESSION_ID_INVALID:
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                logger.info("Validation fail => SessionID missing/invalid!");
                break;

            case CryptoUtils.SESSION_ID_VALID_PHONE_UNVERIFIED:
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                logger.info("Validation fail => Phone number not verified!");
                break;

//          case CryptoUtils.SESSION_ID_VALID_PHONE_VERIFIED:
            default:

                if (imageId == null)
                {
                    logger.info("Validation fail => Missing imageId!");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }               
                else if (permittedUsername == null || permittedUsername.length() == 0)
                {
                    logger.info("Validation fail => Missing username!");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                else
                {
                    try
                    {
                        logger.info("Deleting permission from the database...");
                        jdbcTemplate.update(unshareImageSQL, imageId, permittedUsername, imageId, sessionID);
                        logger.info("Returning HTTP 200");
                        response.setStatus(HttpServletResponse.SC_OK);                      
                    }
                    catch (DataAccessException ex)
                    {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        logger.error(Utils.generateErrorMessage(ex));
                    }
                }   
        }
    }
}
