package rtc.pa.write.plain;

import java.io.IOException;
import java.net.URI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;

import rtc.model.Project;
import rtc.utils.Login;
import rtc.utils.ProgressMonitor;

public class Main {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		Project p = null;

		String url, proj, user, password;
		IPath file;
		try {
			url = new String(args[0]);
			proj = new String(args[1]);
			user = new String(args[2]);
			password = new String(args[3]);
			file = new Path(new String(args[4]));
		} catch (Exception e) {
			monitor.err("arguments: url user password destination_file");
			monitor.err(
					"example: https://my.clm.example.com/ccm \"UU | PPP\" jazz_admin iloveyou /home/issr/here/UU_PP.ser");
			monitor.err("bad args:");
			for (String arg : args) {
				monitor.err(' ' + arg);
			}
			monitor.err();
			return;
		}
		TeamPlatform.startup();
		try {
			ITeamRepository repo = Login.login(url, user, password, monitor);
			URI uri = URI.create(proj.replaceAll(" ", "%20").replaceAll("\\|", "%7C"));
			IProcessClientService processClient = (IProcessClientService) repo
					.getClientLibrary(IProcessClientService.class);
			IProcessArea pa0 = (IProcessArea) (processClient.findProcessArea(uri, IProcessItemService.ALL_PROPERTIES,
					monitor));
			IProjectArea pa = null;
			String message;
			if (null != pa0 && pa0 instanceof IProjectArea) {
				pa = (IProjectArea) pa0;
				p = Project.deserialize(file.toOSString());
				if (null == p) {
					message = "problem reading serialized project";
				} else {
					message = DoIt.execute(repo, pa, monitor, p);
				}
			} else {
				message = new String(uri + " is not a project area");
			}
			if (null == message) {
				monitor.out("OK, done.");
			} else {
				monitor.err("KO: " + message);
			}
		} catch (TeamRepositoryException e) {
			monitor.err("Unable to perform: " + e.getMessage());
		} catch (IOException e) {
			monitor.err("IO error: " + e.getMessage());
		} finally {
			TeamPlatform.shutdown();
		}
	}

}
