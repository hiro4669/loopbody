
public class CuttableMatchingLoop {
	public static void main(String argsp[]) {
		String[] romanNumerals = {"I", "II", "III", "IV", "V", 
				"VI", "VII", "VIII", "IX", "X"};
		for (String s : romanNumerals) {
			System.out.println("looking for " + s);
			System.out.println("lucky search result: " + luckySearch(s, romanNumerals));
		}
		
	}
	private static int luckySearch(String target, String[] list) {
		// -------------- CODE TRANSFORMATION BEGINS HERE -----------
		// NOTE: additional interface created
		class $_LoopEnvironment2 {
			public String $_target;
			public String[] $_list;
			// boolean value represents whether a break was thrown or not
			public $_LoopBodyOutcome $loop$for(int i) {
				System.out.println("[search method] checking position " + i);
				if ($_list[i].equals($_target)) {
					return new $_ReturnOutcome(i);
				}
				return new $_DefaultOutcome();
			}
			
		}
		$_LoopEnvironment2 env = new $_LoopEnvironment2();
		env.$_target = target;
		env.$_list = list;
		for (int i = 0; i < list.length; i += 3) {
			$_LoopBodyOutcome outcome = env.$loop$for(i);
			if (outcome.breakStatementIssued())
				break;
			if (outcome.returnStatementIssued())
				return (int) outcome.getReturnValue();
		}
		// -------------- CODE TRANSFORMATION ENDS HERE -----------
		return -1;
	}
}
