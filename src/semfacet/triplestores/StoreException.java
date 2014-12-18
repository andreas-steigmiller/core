package semfacet.triplestores;

public class StoreException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public StoreException() {}

    public StoreException(String message)
    {
       super(message);
    }

}
