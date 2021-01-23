package hr.identyum.model;

import java.util.List;

public class Image {

    private Long id;
    private String owner;
    private List<String> sharedWith;
    private String name;
    private String data;
    
    public Long getId()
    {
        return id;
    }
    public void setId(Long id)
    {
        this.id = id;
    }

    public String getOwner()
    {
        return owner;
    }
    public void setOwner(String owner)
    {
        this.owner = owner;
    }
    
    public List<String> getSharedWith()
    {
        return sharedWith;
    }
    public void setSharedWith(List<String> sharedWith)
    {
        this.sharedWith = sharedWith;
    }

    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public String getData()
    {
        return data;
    }
    public void setData(String data)
    {
        this.data = data;
    }
}
