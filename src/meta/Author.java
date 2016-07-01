package meta;

/**
 * Created by buzhinsky on 7/1/16.
 */
public enum Author {
    IB("Igor Buzhinsky", "igor.buzhinsky@gmail.com"),
    VU("Vladimir Ulyantsev", "ulyantsev@rain.ifmo.ru");

    public String name;
    public String email;

    Author(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @Override
    public String toString() {
        return "Author: " + name + " (" + email + ")";
    }
}
