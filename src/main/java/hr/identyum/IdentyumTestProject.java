package hr.identyum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vonage.client.VonageClient;

@SpringBootApplication
public class IdentyumTestProject implements CommandLineRunner {

    @Value("${vonage_api_key}")
    private String VONAGE_API_KEY;

    @Value("${vonage_api_secret}")
    private String VONAGE_API_SECRET;

    public static VonageClient vonageClient = null;
    private static final Logger logger = LoggerFactory.getLogger(IdentyumTestProject.class);
   
    public static void main(String[] args)
    {   
        SpringApplication.run(IdentyumTestProject.class, args);
    }

    public void run(String... args)
    {
        logger.info("Initializing Vonage client...");
        vonageClient = VonageClient.builder().apiKey(VONAGE_API_KEY).apiSecret(VONAGE_API_SECRET).build();    
    }
}
