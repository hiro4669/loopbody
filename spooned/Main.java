

// default package (CtPackage.TOP_LEVEL_PACKAGE_NAME in Spoon= unnamed package)



public class Main {
    static int globalX = 0;

    public static void main(java.lang.String[] args) throws java.lang.InterruptedException {
        final int STARTING_VAL = 5;
        final int INC = 2;
        final int N = 11;
        int x = 0;
        ToyExample y = new ToyExample(1, "example object");
        Main.prePrinting(y);
        for (int i = 0; i < N; i++) {
            if (i == 0) {
                x = STARTING_VAL;
                Main.globalX = STARTING_VAL;
            } 
            if (i == 2) {
                java.lang.System.out.println("skipping third iteration");
                continue;
            } 
            if (i >= (N / 2)) {
                java.lang.System.out.println(("breaking at i = " + i));
                break;
            } 
            x += INC;
            Main.globalX += INC;
            y.setVal(x);
            java.lang.System.out.println(((("after iteration " + (i + 1)) + " x is: ") + x));
            java.util.concurrent.TimeUnit.MILLISECONDS.sleep(300);
        }
        Main.postPrinting(x, y);
    }

    private static void prePrinting(ToyExample y) {
        java.lang.System.out.println(("our example is initialized to " + y));
        java.lang.System.out.println("running through a vanilla loop...");
    }

    private static void postPrinting(int x, ToyExample y) {
        java.lang.System.out.println(("after looping, we ended up with an x of: " + x));
        java.lang.System.out.println(("after looping, we ended up with a GLOBAL x of: " + (Main.globalX)));
        java.lang.System.out.println(("our toy example is now " + y));
    }
}

