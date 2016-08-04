

// default package (CtPackage.TOP_LEVEL_PACKAGE_NAME in Spoon= unnamed package)



public class Main {
    static int globalX = 0;

    public static void main(java.lang.String[] args) throws java.lang.ArithmeticException, java.lang.InterruptedException {
        final int STARTING_VAL = 5;
        final int INC = 2;
        final int N = 11;
        int x = 0;
        ToyExample y = new ToyExample(1, "example object");
        Main.prePrinting(y);
        java.lang.Integer $counter_of$LoopBodyEnvironment1$EXPOSED_JAVA$ = 0;
        Main.LoopBodyEnvironment1$EXPOSED_JAVA$ $initialized$LoopBodyEnvironment1$EXPOSED_JAVA$ = new Main.LoopBodyEnvironment1$EXPOSED_JAVA$();
        $initialized$LoopBodyEnvironment1$EXPOSED_JAVA$.x = x;
        $initialized$LoopBodyEnvironment1$EXPOSED_JAVA$.y = y;
        for (int i = 0; i < N; i++) {
            $counter_of$LoopBodyEnvironment1$EXPOSED_JAVA$++;
            if ($initialized$LoopBodyEnvironment1$EXPOSED_JAVA$.loopBody$EXPOSED_JAVA$($counter_of$LoopBodyEnvironment1$EXPOSED_JAVA$, i))
                break;
            
        }
        y = $initialized$LoopBodyEnvironment1$EXPOSED_JAVA$.y;
        x = $initialized$LoopBodyEnvironment1$EXPOSED_JAVA$.x;
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

    class LoopBodyEnvironment1$EXPOSED_JAVA$ {
        public LoopBodyEnvironment1$EXPOSED_JAVA$() {
        }

        public int x;

        public ToyExample y;

        public java.lang.Boolean loopBody$EXPOSED_JAVA$(java.lang.Integer $counter_of$LoopBodyEnvironment1$EXPOSED_JAVA$, int i) throws java.lang.ArithmeticException, java.lang.InterruptedException {
            {
                if (i == 0) {
                    x = STARTING_VAL;
                    Main.globalX = STARTING_VAL;
                } 
                if (i == 2) {
                    java.lang.System.out.println("skipping third iteration");
                    return false;
                } 
                if (i >= (N / 2)) {
                    java.lang.System.out.println(("breaking at i = " + i));
                    return true;
                } 
                x += INC;
                Main.globalX += INC;
                y.setVal(x);
                java.lang.System.out.println(((("after iteration " + (i + 1)) + " x is: ") + x));
                java.util.concurrent.TimeUnit.MILLISECONDS.sleep(300);
            }
        }
    }
}

