package hr.identyum.model;

import java.util.List;

public class User {

    private String username;
    private List<String> phoneNumbers;
    
    public String getUsername()
    {
        return username;
    }
    public void setUsername(String username)
    {
        this.username = username;
    }

    public List<String> getPhoneNumbers()
    {
        return phoneNumbers;
    }
    public void setPhoneNumbers(List<String> phoneNumbers)
    {
        this.phoneNumbers = phoneNumbers;
    }
}
