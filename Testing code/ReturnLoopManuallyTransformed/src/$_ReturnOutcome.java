
public class $_ReturnOutcome implements $_LoopBodyOutcome {
	private Object retVal;
	public $_ReturnOutcome (Object retVal) {
		this.retVal = retVal;
	}
	public boolean returnStatementIssued() { return true; }
	public boolean breakStatementIssued() { return false; }
	public Object getReturnValue() { return retVal; }
}
