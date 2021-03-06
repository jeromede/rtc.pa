/*
 * Copyright (c) 2017,2018,2019 jeromede@fr.ibm.com
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

package rtc.pa.model.utilities;

import rtc.pa.model.Project;

public abstract class Trace {

	public static void main(String[] args) {

		Project p = null;
		String ser;
		try {
			ser = new String(args[0]);
		} catch (Exception e) {
			System.err.println("arguments: file");
			System.err.println("example: pa123.ser");
			System.err.print("Bad arguments:");
			for (String arg : args) {
				System.err.println(arg);
			}
			System.err.println();
			return;
		}
		p = Project.deserialize(ser);
		p.dump(System.out);
	}
}
