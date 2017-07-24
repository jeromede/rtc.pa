package rtc.pa.write.text;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Test {

	@SuppressWarnings("serial")
	private static final Map<String, String> tasks = Collections.unmodifiableMap(new HashMap<String, String>() {
		{
			put("123", "102030");
			put("456", "405060");
			put("789", "708090");
		}
	});

	public static void main(String[] args) {
		String before = "Task 456";
		System.out.println("BEFORE:\n" + before);
		System.out.println("AFTER:\n" + Transposition.transpose(before, tasks));
		before = "Task 456 Story 78 Story 789 blabla";
		System.out.println("BEFORE:\n" + before);
		System.out.println("AFTER:\n" + Transposition.transpose(before, tasks));
	}

}
