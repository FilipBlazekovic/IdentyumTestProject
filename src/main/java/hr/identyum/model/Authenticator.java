package hr.identyum.model;

public class Authenticator {

    private String username;
    private char[] password;
    private String phoneNumber;
    private String verificationCode;

    public String getUsername()
    {
        return username;
    }
    public void setUsername(String username)
    {
        this.username = username;
    }

    public char[] getPassword()
    {
        return password;
    }
    public void setPassword(char[] password)
    {
        this.password = password;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getVerificationCode()
    {
        return verificationCode;
    }
    public void setVerificationCode(String verificationCode)
    {
        this.verificationCode = verificationCode;
    }
}
