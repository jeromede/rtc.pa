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
