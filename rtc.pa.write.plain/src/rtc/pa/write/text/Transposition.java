package rtc.pa.write.text;

import java.util.Map;

public class Transposition {

	public static String transpose(String content, Map<String, String> tasks) {
		String result = "";
		String id = null;
		String newId;
		boolean inId = false;
		for (char c : content.toCharArray()) {
			if ('0' <= c && c <= '9') {
				if (inId) {
					id = id.concat("" + c);
					// System.out.println("2) c: '" + c + "' id: \"" + id + "\"
					// result: \"" + result + "\"");
				} else {
					inId = true;
					id = "" + c;
				}
			} else if (inId) {
				inId = false;
				newId = tasks.get(id);
				if (null == newId) {
					result = result.concat(id);
				} else {
					result = result.concat(newId + ' ' + '{' + id + '}');
				}
				result = result.concat("" + c);
			} else {
				result = result.concat("" + c);
			}
		}
		if (inId) {
			newId = tasks.get(id);
			if (null == newId) {
				result = result.concat(id);
			} else {
				result = result.concat(newId + ' ' + '{' + id + '}');
			}
		}
		return result;
	}

}
