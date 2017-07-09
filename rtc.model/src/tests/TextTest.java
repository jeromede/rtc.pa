/*
 * Copyright (c) 2017 jeromede@fr.ibm.com
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

package tests;

import rtc.model.Text;

public class TextTest {

	public static void main(String[] args) {
		Text t1;
		Text t2;
		Text t3;
		t1 = Text.get("abcdefghijklmnopqrstuvwxyz");
		t2 = Text.get("jerome");
		t3 = Text.get("abcdefghijklmnopqrstuvwxyz");
		System.out.println(t1.value());
		System.out.println(t2.value());
		System.out.println(t3.value());
		System.out.println(Text.same(t1, t2));
		System.out.println(Text.same(t1, t3));
	}

}
