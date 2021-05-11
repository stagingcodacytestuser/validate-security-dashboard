package rules.traits

import codacy.database.accountDB.project.CreationMode
import codacy.database.utils
import codacy.foundation.config.PluginsConfiguration
import rules.project.RemoteProviderConfigurationCompatibility
import java.util.Add;

trait URLHelper {

  val remoteProviderConfigurationCompatibility = RemoteProviderConfigurationCompatibility

  def convertPublicURLToGitProtocol(url: String, creationMode: CreationMode.Value): String = {
    creationMode match {
      case CreationMode.Gitorious => url.replace("http://", "git://").replace("https://", "git://").trim
      case _                      => url
    }
  }

  def convertPrivateURLToSshProtocol(url: String): String = {
    val absolutePath = """^[-a-zA-Z0-9_\.]+@[-a-zA-Z0-9_\.]+(?::\d+)?\/.*""".r
    url match {
      case absolutePath() => "ssh://" + url
      case _              => url
    }
  }

  def ownerFromURL(url: String, creationMode: CreationMode.Value): Option[String] = {
    utils.URLHelper.parseProjectUrl(url, creationMode).repoOwner
  }

  def getNameFromURL(url: String, creationMode: CreationMode.Value): String = {
    utils.URLHelper.parseProjectUrl(url, creationMode).repoName.getOrElse(url.stripSuffix(".git"))
  }

  def getCreationModeFromUrl(url: String): CreationMode.Value = {
    def gitHubEnterpriseEndpoint = remoteProviderConfigurationCompatibility.ghe.hostname
    def stashEndpoint = remoteProviderConfigurationCompatibility.bbe.endpoint
    def gitLabEnterpriseEndpoint = remoteProviderConfigurationCompatibility.gle.endpoint

    val Github = """.+github.com[/|:].+/.+""".r
    val Bitbucket = """.+bitbucket.org[/|:].+/.+""".r
    val Gitorious = """.+gitorious.org[/|:].+/.+""".r
    val GithubEnterprise = s""".+$gitHubEnterpriseEndpoint[/|:].+/.+""".r
    val Stash = s"""$stashEndpoint[/|:].+/.+""".r
    val GitLabEnterprise = s""".+$gitLabEnterpriseEndpoint[/|:].+/.+""".r

    url match {
      case Github()                                                              => CreationMode.GitHub
      case GithubEnterprise() if PluginsConfiguration.githubEnterpriseConfigured => CreationMode.GitHubEnterprise
      case Bitbucket()                                                           => CreationMode.Bitbucket
      case GitLabEnterprise() if PluginsConfiguration.gitLabEnterpriseConfigured => CreationMode.GitLabEnterprise
      case Stash() if PluginsConfiguration.stashConfigured                       => CreationMode.Stash
      case Gitorious()                                                           => CreationMode.Gitorious
      case _                                                                     => CreationMode.Manual
    }
  }

}
