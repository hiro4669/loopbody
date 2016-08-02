
public class ToyExample {
	private int val;
	private String name;
	public ToyExample (int x, String s) {
		val = x;
		name = s;
	}
	public int getVal() {
		return val;
	}
	public String getName() {
		return name;
	}
	public void setVal(int x) throws ArithmeticException {
		val = x;
	}
	
	@Override
	public String toString() {
		return "("+name+", "+val+")";
	}
}
