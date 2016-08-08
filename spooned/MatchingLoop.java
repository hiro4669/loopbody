

// default package (CtPackage.TOP_LEVEL_PACKAGE_NAME in Spoon= unnamed package)



public class MatchingLoop {
    public static void main(java.lang.String[] argsp) {
        java.lang.String[] romanNumerals = new java.lang.String[]{ "I" , "II" , "III" , "IV" , "V" , "VI" , "VII" , "VIII" , "IX" , "X" };
        for (java.lang.String s : romanNumerals) {
            java.lang.System.out.println(("looking for " + s));
            java.lang.System.out.println(("lucky search result: " + (MatchingLoop.luckySearch(s, romanNumerals))));
        }
    }

    private static int luckySearch(java.lang.String target, java.lang.String[] list) {
        class LoopBodyEnvironment1$EXPOSED_JAVA$ {
            public LoopBodyEnvironment1$EXPOSED_JAVA$() {
            }

            public int retValue$EXPOSED_JAVA$;

            public java.lang.Boolean hasRetVal$EXPOSED_JAVA$;

            public java.lang.Boolean loopBody$EXPOSED_JAVA$(java.lang.Integer $counter_of$LoopBodyEnvironment1$EXPOSED_JAVA$, int i) {
                {
                    java.lang.System.out.println(("[search method] checking position " + i));
                    if (list[i].equals(target)) {
                        retValue$EXPOSED_JAVA$ = i;
                        hasRetVal$EXPOSED_JAVA$ = true;
                        return true;
                    } 
                }
                return false;
            }
        }
        java.lang.Integer $counter_of$LoopBodyEnvironment1$EXPOSED_JAVA$ = 0;
        LoopBodyEnvironment1$EXPOSED_JAVA$ $initialized$LoopBodyEnvironment1$EXPOSED_JAVA$ = new LoopBodyEnvironment1$EXPOSED_JAVA$();
        for (int i = 0; i < (list.length); i += 3) {
            $counter_of$LoopBodyEnvironment1$EXPOSED_JAVA$++;
            if ($initialized$LoopBodyEnvironment1$EXPOSED_JAVA$.loopBody$EXPOSED_JAVA$($counter_of$LoopBodyEnvironment1$EXPOSED_JAVA$, i))
                if ($initialized$LoopBodyEnvironment1$EXPOSED_JAVA$.hasRetVal$EXPOSED_JAVA$) {
                    return $initialized$LoopBodyEnvironment1$EXPOSED_JAVA$.retValue$EXPOSED_JAVA$;
                } else
                    break;
                
            
        }
        return -1;
    }
}

