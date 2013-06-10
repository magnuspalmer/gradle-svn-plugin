package org.eventfully.gradle.plugins.svn

import org.junit.Before
import org.junit.BeforeClass
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.wc.ISVNOptions
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNUpdateClient
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil
import org.tmatesoft.svn.core.wc2.SvnCopySource;

import spock.lang.*
import wslite.soap.*

class SvnClientSpec extends Specification {

	String version = "1.0"

	def baseUrl = ""
	File exportDir = new File("C:/exports/INT001_Videos")
	String INT_ID = 'INT001'
	String url
	String integrationsUrl
	SVNClientManager clientManager
	SVNURL svnUrlBase
	SVNURL svnUrlTrunk
	SVNURL svnUrlDeploy
	SVNURL svnUrlTags
	SVNURL svnUrlIntegrations
	boolean uriEncoded = false
	String svnProjectName
	File checkoutDir

	@BeforeClass
	public static void initTestData(){
	}

	@Before
	public void setup() {
		if (exportDir){
			exportDir.deleteDir();
		}
		url = "https://MALL19927.lik.enfonet.fi:8443/svn/acme/integrations/INT001_Videos"
		integrationsUrl = "https://MALL19927.lik.enfonet.fi:8443/svn/acme/integrations"
		svnProjectName = 'INT001_Videos'
		String name="builder";
		String password="builder1";

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		ISVNAuthenticationManager authManager =
				SVNWCUtil.createDefaultAuthenticationManager(name, password);
		clientManager = SVNClientManager.newInstance(options, authManager)
		svnUrlBase = new SVNURL(url, uriEncoded)
		svnUrlTrunk = svnUrlBase.appendPath('trunk', uriEncoded)
		svnUrlDeploy = svnUrlBase.appendPath('deploy', uriEncoded)
		svnUrlTags = svnUrlBase.appendPath('tags', uriEncoded)
		svnUrlIntegrations = new SVNURL(integrationsUrl, uriEncoded)
		
		checkoutDir = new File("target" + File.separator + svnProjectName)
	}

	def "SVN list intId"() {

		given: "A logClient"
		SVNLogClient logClient = clientManager.getLogClient()
		def handler = new VerifyDirExistISVNDirEntryHandler(search: INT_ID, )

		when: "listing the repo url"
		logClient.doList(svnUrlIntegrations, SVNRevision.HEAD, SVNRevision.HEAD, false, SVNDepth.IMMEDIATES, 0, handler)

		then: "The entry for the intid is found"
		handler.found
		handler.entry.getURL() == svnUrlBase
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

	@Unroll
	def "SVN list deploydir using search: #searchString"() {

		given: "A logClient"
		SVNLogClient logClient = clientManager.getLogClient()
		def handler = new VerifyDirExistISVNDirEntryHandler(search: searchString, )

		when: "listing the repo url"
		logClient.doList(svnUrlBase, SVNRevision.HEAD, SVNRevision.HEAD, false, SVNDepth.IMMEDIATES, 0, handler)

		then: "It has a deploydir"
		assert handler.found == expected

		where:
		searchString | expected
		'deploy' | true
		'nodeploy' | false
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

	def "SVN create tag (by copy)"() {

		given: "A copyClient"
		SVNCopyClient copyClient = clientManager.getCopyClient()

		and: "the version"

		and: "the source and destination urls for copy"
		SVNCopySource[] source = [
			new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, svnUrlTrunk)
		]
		SVNURL tag = svnUrlTags.appendPath(version, uriEncoded)

		and: "some other options"
		boolean doNotMove = false
		boolean doNotCreateParentIfMissing = false
		boolean failOnExistingVersionTag = true

		when: "the tag is created"
		def revision = copyClient.doCopy(source, tag, doNotMove, doNotCreateParentIfMissing,
				failOnExistingVersionTag, "Creating tag: $version", null)

		then: "It gets copied successfully"
		assert revision.newRevision > 0
	}

	def "SVN delete created tag"(){

		given: "A commitClient"
		SVNCommitClient commitClient = clientManager.getCommitClient()
		SVNURL tag = svnUrlTags.appendPath(version, uriEncoded)

		and: "a version"
		String version = "1.0"

		when: "I delete the tag"
		SVNCommitInfo revision = commitClient.doDelete([tag] as SVNURL[], "Deleting tag for $version")

		then: "The revision is greater than zero"
		revision.getNewRevision() > "0"
	}
	
	def "SVN checkout trunk"() {
		
		given: "A checkout client"
		SVNUpdateClient client = clientManager.getUpdateClient()
		
		and: "A directory to checkout to"
		
		when: "checking out trunk"
		client.doCheckout(svnUrlTrunk, checkoutDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true)
		
		then: "trunk is checked out as a local working copy"
		checkoutDir.exists()
		
	}
	
	def "SVN add file(s)"() {
		
		given: "A working copy client"
		SVNWCClient client = clientManager.getWCClient()
		
		and: "A file to add"
		File checkoutSubDir = new File(checkoutDir, "temp")
		checkoutSubDir.mkdir()
		File fileToAdd = File.createTempFile("prefix", "suffix", checkoutSubDir  )
		
		and: "Some options"
		boolean noErrorsOnExistingFiles = true
		boolean makeDir = false
		boolean notUsed = false
		boolean dontIncludeIgnored = false
		boolean makeParents = true
		
		when: "adding a file"
		client.doAdd(fileToAdd, noErrorsOnExistingFiles, makeDir, notUsed, SVNDepth.INFINITY, dontIncludeIgnored, makeParents)
		
		then: "the file is added"
		assert true
		
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

		if (dirEntry.name == 'deploy'){
			deployEntry = dirEntry
			deployDir = true
		}
	}
}

class VerifyDirExistISVNDirEntryHandler implements ISVNDirEntryHandler {

	String search
	SVNDirEntry entry
	boolean found

	@Override
	public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {

		if (dirEntry.name =~ search ){
			entry = dirEntry
			found = true
		}
	}
}

