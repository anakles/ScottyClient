public class Command
{
	String cmd;
	Double value;
	public Command(String cmd, String value)
	{
		this.cmd = cmd;
		this.value = Double.valueOf(value);
	}
}
	