
public aspect LoopBodyAnnouncer {
	before(int i) : execution (* *.run$loop*(int) ) && args(i) {
		System.out.println("[ASPECT POWERED ANNOUNCER] now on iteration " + (i + 1));
	}
}
