package core

public class LoginTester1Spec extends LoginTesterSpec {

	public static String testerUsername = "tester1@streamr.com"
	public static String testerPassword = "tester1TESTER1"

	@Override
	String getTesterUsername() {
		return testerUsername
	}

	@Override
	public String getTesterPassword() {
		return testerPassword
	}
}