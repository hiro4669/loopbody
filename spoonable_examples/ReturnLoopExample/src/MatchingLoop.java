

public class MatchingLoop {
	public static void main(String argsp[]) {
		String[] romanNumerals = {"I", "II", "III", "IV", "V", 
				"VI", "VII", "VIII", "IX", "X"};
		for (String s : romanNumerals) {
			//testloop1
			System.out.println("looking for " + s);
			System.out.println("lucky search result: " + luckySearch(s, romanNumerals));
		}
		
	}
	private static int luckySearch(String target, String[] list) {
		// -------------- CODE TO TRANSFOR BEGINS HERE -----------
		for (int i = 0; i < list.length; i += 3) {
			System.out.println("[search method] checking position " + i);
			if (list[i].equals(target))
				return i;
		}
		// -------------- CODE TO TRANSFOR ENDS HERE -----------
		return -1;
	}
}
