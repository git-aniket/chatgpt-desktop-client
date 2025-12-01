package nl.vu.psy.ams.suite.tools;
/*
 * Put anything you want to test here. It will be run if you push the 
 * Test Function button (which will only show up on beta versions)
 */
public class TestFunction {
	public static void testFunction() {
		Timer timer = new Timer();
		timer.start();
		timer.stop();
		System.out.println("TESTFUNCTION END (" + timer.getTime() / 1000. + " sec)!");
	}
}
