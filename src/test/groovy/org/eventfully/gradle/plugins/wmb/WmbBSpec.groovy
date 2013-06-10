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

	File mqsiCreateBarExe = new File("C:/opt/IBM/WMBT800/mqsicreatebar.exe")

	@BeforeClass
	public static void initTestData(){
	}

	@Before
	public void setup() {
	}

	def "Check that mqsicreatebar exists and is executable"(){

		expect: "mqsicreatebar is executable"
		mqsiCreateBarExe.canExecute()
	}

}
