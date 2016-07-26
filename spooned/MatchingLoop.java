

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
        for (int i = 0; i < (list.length); i += 3) {
            java.lang.System.out.println(("[search method] checking position " + i));
            if (list[i].equals(target))
                return i;
            
        }
        return -1;
    }
}

