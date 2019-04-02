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

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;

import rtc.pa.model.Administrator;
import rtc.pa.model.Member;
import rtc.pa.model.Project;
import rtc.pa.utils.ProgressMonitor;

public class UserHelper {

	static String readContributors(IContributorHandle[] contributors, ITeamRepository repo, IProjectArea pa,
			ProgressMonitor monitor, Project p, boolean isAdmin) {

		IContributor contrib;
		for (IContributorHandle contribHandle : contributors) {
			try {
				contrib = (IContributor) repo.itemManager().fetchCompleteItem(contribHandle, IItemManager.DEFAULT,
						monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error resolving IContributorHandle";
			}
			if (isAdmin) {
				p.putAdministrator(
						new Administrator(contrib.getItemId().getUuidValue(), contrib.getUserId(), contrib.getName()));
				monitor.out("\tjust added administrator " + contrib.getUserId());
			} else {
				p.putMember(new Member(contrib.getItemId().getUuidValue(), contrib.getUserId(), contrib.getName()));
				monitor.out("\tjust added member " + contrib.getUserId());
			}
		}
		return null;
	}

	static String readMembers(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p) {

		String result;
		monitor.out("Now reading administrators...");
		result = readContributors(pa.getAdministrators(), repo, pa, monitor, p, true);
		monitor.out("... administrators read.");
		if (null != result)
			return result;
		monitor.out("Now reading members...");
		result = readContributors(pa.getMembers(), repo, pa, monitor, p, false);
		monitor.out("... members read.");
		if (null != result)
			return result;
		return null;
	}

	static Member getM(Project p, IContributor c) {
		Member m = p.getMember(c.getItemId().getUuidValue());
		if (null == m) {
			m = new Member(c.getItemId().getUuidValue(), c.getUserId(), c.getName());
			p.putMember(m);
		}
		return m;
	}

}
