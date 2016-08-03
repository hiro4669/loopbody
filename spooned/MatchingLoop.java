

// default package (CtPackage.TOP_LEVEL_PACKAGE_NAME in Spoon= unnamed package)



public class MatchingLoop {
    public static void main(java.lang.String[] argsp) {
        java.lang.String[] romanNumerals = new java.lang.String[]{ "I" , "II" , "III" , "IV" , "V" , "VI" , "VII" , "VIII" , "IX" , "X" };
        MatchingLoop.LoopBodyEnvironment$EXPOSED_JAVA$2 $envObject$$EXPOSED_JAVA$ = new MatchingLoop.LoopBodyEnvironment$EXPOSED_JAVA$2(romanNumerals, s);
        for (java.lang.String s : romanNumerals) {
            java.lang.System.out.println(("looking for " + s));
            java.lang.System.out.println(("lucky search result: " + (MatchingLoop.luckySearch(s, romanNumerals))));
        }
    }

    private static int luckySearch(java.lang.String target, java.lang.String[] list) {
        MatchingLoop.LoopBodyEnvironment$EXPOSED_JAVA$1 $envObject$$EXPOSED_JAVA$ = new MatchingLoop.LoopBodyEnvironment$EXPOSED_JAVA$1(i);
        for (int i = 0; i < (list.length); i += 3) {
            java.lang.System.out.println(("[search method] checking position " + i));
            if (list[i].equals(target))
                return i;
            
        }
        return -1;
    }

    class LoopBodyEnvironment$EXPOSED_JAVA$1 {
        public LoopBodyEnvironment$EXPOSED_JAVA$1(int i$EXPOSED_JAVA$) {
            this.i$EXPOSED_JAVA$ = i$EXPOSED_JAVA$;
        }

        public int i$EXPOSED_JAVA$;

        public int retValue$EXPOSED_JAVA$;

        public java.lang.Boolean loopBody$EXPOSED_JAVA$(java.lang.Integer $loopIterator$$EXPOSED_JAVA$) {
            {
                java.lang.System.out.println(("[search method] checking position " + i));
                if (list[i].equals(target)) {
                    retValue$EXPOSED_JAVA$ = i;
                    return true;
                } 
            }
        }
    }

    class LoopBodyEnvironment$EXPOSED_JAVA$2 {
        public LoopBodyEnvironment$EXPOSED_JAVA$2(java.lang.String[] romanNumerals$EXPOSED_JAVA$, java.lang.String s$EXPOSED_JAVA$) {
            this.romanNumerals$EXPOSED_JAVA$ = romanNumerals$EXPOSED_JAVA$;
            this.s$EXPOSED_JAVA$ = s$EXPOSED_JAVA$;
        }

        public java.lang.String[] romanNumerals$EXPOSED_JAVA$;

        public java.lang.String s$EXPOSED_JAVA$;

        public java.lang.Boolean loopBody$EXPOSED_JAVA$(java.lang.Integer $loopIterator$$EXPOSED_JAVA$) {
            {
                java.lang.System.out.println(("looking for " + s));
                java.lang.System.out.println(("lucky search result: " + (MatchingLoop.luckySearch(s, romanNumerals))));
            }
        }
    }
}

