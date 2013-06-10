package org.eventfully.gradle.plugins.svn

import org.junit.Before
import org.junit.BeforeClass
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.wc.ISVNOptions
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNUpdateClient
import org.tmatesoft.svn.core.wc.SVNWCUtil

import spock.lang.*
import wslite.soap.*

class SvnClientSpec extends Specification {

	String version = "1.0"

	def baseUrl = ""
	File exportDir = new File("C:/exports/INT001_Videos")
	File mqsiCreateBarExe = new File("C:/opt/IBM/WMBT800/mqsicreatebar.exe")
	String url
	SVNClientManager clientManager
	SVNURL svnUrlBase
	SVNURL svnUrlTrunk
	SVNURL svnUrlDeploy
	SVNURL svnUrlTags
	boolean uriEncoded = false

	@BeforeClass
	public static void initTestData(){
	}

	@Before
	public void setup() {
		if (exportDir){
			exportDir.deleteDir();
		}
		url = "https://MALL19927.lik.enfonet.fi:8443/svn/acme/integrations/INT001_Videos"
		String name="builder";
		String password="builder1";

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		ISVNAuthenticationManager authManager =
				SVNWCUtil.createDefaultAuthenticationManager(name, password);
		clientManager = SVNClientManager.newInstance(options, authManager)
		svnUrlBase = new SVNURL(url, uriEncoded)
		svnUrlTrunk = svnUrlBase.appendPath('trunk', uriEncoded)
		svnUrlDeploy = svnUrlBase.appendPath('deploy', uriEncoded)
		svnUrlDeploy = svnUrlBase.appendPath('tags', uriEncoded)
	}

	def "Check that mqsicreatebar exists and is executable"(){

		expect: "mqsicreatebar is executable"
		mqsiCreateBarExe.canExecute()
	}

	def "SVN list "() {

		given: "A logClient"
		SVNLogClient logClient = clientManager.getLogClient()
		def handler = new MyISVNDirEntryHandler()

		when: "listing the repo url"
		logClient.doList(svnUrlTrunk, SVNRevision.HEAD, SVNRevision.HEAD, false, SVNDepth.IMMEDIATES, 0, handler)

		then: "it looks like expected"
		assert handler.dirEntries*.name == ['', 'docs', 'src', 'test']
	}

	def "SVN list deploydir "() {

		given: "A logClient"
		SVNLogClient logClient = clientManager.getLogClient()
		def handler = new VerifyDeployDirExistISVNDirEntryHandler()

		when: "listing the repo url"
		logClient.doList(svnUrlBase, SVNRevision.HEAD, SVNRevision.HEAD, false, SVNDepth.IMMEDIATES, 0, handler)

		then: "It has a deploydir"
		assert handler.deployDir
	}

	def "SVN create dir"(){

		given: "A commitClient"
		SVNCommitClient commitClient = clientManager.getCommitClient()
		SVNURL newDir = svnUrlBase.appendPath('temp', uriEncoded)


		when: "I create a directory"
		SVNCommitInfo revision = commitClient.doMkDir([newDir] as SVNURL[], "A new dir")

		then: "The revision is greater than zero"
		revision.getNewRevision() > "0"
	}

	def "SVN delete dir"(){

		given: "A commitClient"
		SVNCommitClient commitClient = clientManager.getCommitClient()
		SVNURL newDir = svnUrlBase.appendPath('temp', uriEncoded)


		when: "I delete a directory"
		SVNCommitInfo revision = commitClient.doDelete([newDir] as SVNURL[], "Deleting")

		then: "The revision is greater than zero"
		revision.getNewRevision() > "0"
	}



	def "SVN export trunk to local dir"() {

		given: "An updateClient"

		SVNUpdateClient updateClient = clientManager.getUpdateClient()

		when: "The svn project is exported"
		def revision = updateClient.doExport(svnUrlTrunk, exportDir, SVNRevision.HEAD, SVNRevision.HEAD, "native", true, SVNDepth.INFINITY)

		then: "It gets exported"
		assert exportDir.exists()
	}
}

class MyISVNDirEntryHandler implements ISVNDirEntryHandler {

	def dirEntries = []

	@Override
	public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
		println dirEntry
		dirEntries << dirEntry
	}
}

class VerifyDeployDirExistISVNDirEntryHandler implements ISVNDirEntryHandler {

	def deployEntry
	boolean deployDir

	@Override
	public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {

		println "dep" + dirEntry.name
		if (dirEntry.name == 'deploy'){
			println "yes!"
			deployEntry = dirEntry
			deployDir = true
		}
	}
}
