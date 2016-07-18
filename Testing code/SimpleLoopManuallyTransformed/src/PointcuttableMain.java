import java.util.concurrent.TimeUnit;

public class PointcuttableMain {
	static int globalX = 0;
	public static void main(String[] args) throws InterruptedException {
		// constant loop parameters
		final int STARTING_VAL = 5;
		final int INC = 2;
		final int N = 11;
		
		// local variables to be modified
		int x = 0; //basic variable
		ToyExample y = new ToyExample(1, "example object"); //example of an object (passed by reference)
		
		/* 
		 * -> start a local variable x at STARTING_VAL
		 * -> increment x by INC every loop, mirrors x's behavior to globalX as well
		 * -> skip body of third iteration via continue
		 * -> break before executing the (1/2)*Nth loop
		 * -> also has a pause every loop to demonstrate exception handling
		 */
		prePrinting(y);
		// ----------- CODE TRANSFORMATION BEGINS HERE ---------
		// move the pertinent local variables and body to a local class
		class $_LoopEnvironment1 {
			public int $_x;
			public ToyExample $_y;
			// boolean value represents whether a break was thrown or not
			public boolean $loop$for(int i) throws InterruptedException {
				if (i == 0)
				{
					$_x = STARTING_VAL;
					globalX = STARTING_VAL;
				}
				if (i == 2)
				{
					System.out.println("skipping third iteration");
					return false; //equivalent to "continue"
				}
				if (i >= N/2)
				{
					System.out.println("breaking at i = " + i);
					return true; //equivalent to "break"
				}
				$_x += INC;
				globalX += INC;
				$_y.setVal($_x);
				System.out.println("after iteration " + (i + 1) + " x is: " + $_x);
				TimeUnit.MILLISECONDS.sleep(300);
				return false;
			}
		}
		// instantiate and capture local variables
		$_LoopEnvironment1 $_env1 = new $_LoopEnvironment1();
		$_env1.$_x = x;
		$_env1.$_y = y;
		// run the loop
		boolean breakStatementIssued;
		for (int i = 0; i < N; i++)
		{
			breakStatementIssued = $_env1.$loop$for(i);
			if (breakStatementIssued)
				break;
		}
		// update local variables from captured copies
		x = $_env1.$_x;
		y = $_env1.$_y; //necessary if the *reference itself* is changed inside the loop body
		// ----------- CODE TRANSFORMATION ENDS HERE ---------
		postPrinting(x, y);
	}
	
	private static void prePrinting(ToyExample y) {
		System.out.println("our example is initialized to " + y);
		System.out.println("running through a body-pointncuttable loop...");
	}
	
	private static void postPrinting(int x, ToyExample y) {
		System.out.println("after looping, we ended up with an x of: " + x);
		System.out.println("after looping, we ended up with a GLOBAL x of: " + globalX);
		System.out.println("our toy example is now " + y);
	}
}
