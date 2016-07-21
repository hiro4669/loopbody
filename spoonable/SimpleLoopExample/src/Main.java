import java.util.concurrent.TimeUnit;

/** 
 * simple example of looping and some of its complexities
 * @author luke
 *
 */
public class Main {
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
		for (int i = 0; i < N; i++)
		{
			if (i == 0)
			{
				x = STARTING_VAL;
				globalX = STARTING_VAL;
			}
			if (i == 2)
			{
				System.out.println("skipping third iteration");
				continue;
			}
			if (i >= N/2)
			{
				System.out.println("breaking at i = " + i);
				break;
			}
			x += INC;
			globalX += INC;
			y.setVal(x);
			System.out.println("after iteration " + (i + 1) + " x is: " + x);
			TimeUnit.MILLISECONDS.sleep(300);
		}
		postPrinting(x, y);
	}
	
	
	private static void prePrinting(ToyExample y) {
		System.out.println("our example is initialized to " + y);
		System.out.println("running through a vanilla loop...");
	}
	
	private static void postPrinting(int x, ToyExample y) {
		System.out.println("after looping, we ended up with an x of: " + x);
		System.out.println("after looping, we ended up with a GLOBAL x of: " + globalX);
		System.out.println("our toy example is now " + y);
	}
}
