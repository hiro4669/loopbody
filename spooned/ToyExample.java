

// default package (CtPackage.TOP_LEVEL_PACKAGE_NAME in Spoon= unnamed package)



public class ToyExample {
    private int val;

    private java.lang.String name;

    public ToyExample(int x, java.lang.String s) {
        val = x;
        name = s;
    }

    public int getVal() {
        return val;
    }

    public java.lang.String getName() {
        return name;
    }

    public void setVal(int x) throws java.lang.ArithmeticException {
        val = x;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return ((("(" + (name)) + ", ") + (val)) + ")";
    }
}

