import org.apache.commons.lang.builder.EqualsBuilder;

public class TestEquals {

	public static void main(String[] args) {

		String[] a = { "123" };
		String b = "123";

		System.out.println("a==b:" + a[0] == b);
		System.out.println("a.equals(b):" + a[0].equals(b));
		System.out.println("EqualsBuilder.reflectionEquals(a, b, true):"
				+ EqualsBuilder.reflectionEquals(a[0], b, true));
		System.out.println("EqualsBuilder.reflectionEquals(a, b, false):"
				+ EqualsBuilder.reflectionEquals(a[0], b, false));

	}

}
