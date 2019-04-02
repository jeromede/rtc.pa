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

package rtc.pa.read;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IAttachment;

import rtc.pa.utils.ProgressMonitor;

public class AttachmentHelper {

	public static String saveAttachment(IAttachment attachment, String dir, ProgressMonitor monitor) {

		String fileName = dir + File.separator + attachment.getId() + '.' + attachment.getName();
		File file = new File(fileName);
		OutputStream out;
		try {
			out = new FileOutputStream(file);
			try {
				((ITeamRepository) attachment.getOrigin()).contentManager().retrieveContent(attachment.getContent(),
						out, monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
			} finally {
				out.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "can't open file " + fileName;
		} catch (IOException e) {
			e.printStackTrace();
			return "error while writing file " + fileName;
		}
		return null;
	}

}
