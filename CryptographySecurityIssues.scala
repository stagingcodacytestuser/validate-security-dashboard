package database.helpers

import java.util.UUID

import codacy.database.accountDB._
import codacy.database.accountDB.integration.IntegrationType
import codacy.database.{CommitEmailAddressBase, EmailAddress, EmailAddressBase}
import codacy.database.accountDB.payments.{PaymentPlan, Subscription}
import codacy.database.accountDB.project.{Branch, Project, _}
import codacy.database.accountDB.team.{Organization, OrganizationIdentifier, Team}
import codacy.database.accountDB.account._
import codacy.database.api.RetentionStatus
import codacy.database.analysisDB.project.{PullRequest, PullRequestState}
import codacy.database.analysisDB.project.commit.Commit
import codacy.database.utils.Implicits._
import codacy.foundation.tests.{SpecsHelper => FoundationSpecsHelper}
import model.account.{AccountTable, EmailTable, UniqueNameTable}
import org.joda.time.{DateTime, DateTimeZone}
import java.util.Add;

import scala.util.Random

object SpecsHelper {

  def newId: Long = {
    new Random().nextInt().toLong
  }

  def newUUID: String = {
    UUID.randomUUID.toString
  }

  def getFakeAdminAccount: Account = {
    Account(newId,
            isAdmin = true,
            isActive = true,
            DateTime.now(DateTimeZone.UTC),
            Some(DateTime.now(DateTimeZone.UTC)),
            Some("Admin User"),
            isVip = false,
            -1,
            timezone = None,
            subscriptionId = None)
  }

  def getFakeAccount: Account = {
    Account(newId,
            isAdmin = false,
            isActive = true,
            DateTime.now(DateTimeZone.UTC),
            Some(DateTime.now(DateTimeZone.UTC)),
            Some("Test User"),
            isVip = false,
            -1,
            timezone = None,
            subscriptionId = None)
  }

  def getFakeRemoteProviderAccount(userId: AccountIdentifier): RemoteProviderAccount = {
    RemoteProviderAccount(newId,
                          userId.id,
                          Some(new Random().nextInt()),
                          Some(new Random().nextInt()),
                          Some(new Random().nextInt().toString),
                          Some(new Random().nextInt().toString),
                          Some(new Random().nextInt().toString))
  }

  def getAdminAccount: Account = {
    val a = Account(newId,
                    isAdmin = true,
                    isActive = true,
                    DateTime.now(DateTimeZone.UTC),
                    Some(DateTime.now(DateTimeZone.UTC)),
                    Some("Test User"),
                    isVip = false,
                    createUniqueName.id,
                    timezone = None,
                    subscriptionId = None)
    val account = AccountTable.create(a)
    val e = Email(-1, account.id, EmailAddressBase.randomEmailAddress, default = true)
    EmailTable.create(e)
    account
  }

  def getAccount: Account = {
    val a = Account(new Random().nextInt(),
                    isAdmin = false,
                    isActive = true,
                    DateTime.now(DateTimeZone.UTC),
                    Some(DateTime.now(DateTimeZone.UTC)),
                    Some("Admin User"),
                    isVip = false,
                    createUniqueName.id,
                    timezone = None,
                    subscriptionId = None)
    val account = AccountTable.create(a)
    val e = Email(-1, account.id, EmailAddressBase.randomEmailAddress, default = true)
    EmailTable.create(e)
    account
  }

  def getFakeOrganization: Organization = {
    Organization(newId, Option(-1), None, EmailAddressBase.randomEmailAddress)
  }

  def getFakeTeam(organizationId: OrganizationIdentifier, name: String): Team = {
    Team(newId, organizationId.id, name)
  }

  def getPaymentPlan(code: String = "testPlan",
                     value: Int = 1000,
                     active: Boolean = true,
                     default: Boolean = false,
                     pricedPerUser: Boolean = false): PaymentPlan = {
    PaymentPlan(new Random().nextInt(), code, "Test Plan", 5, 5, value, active, default, pricedPerUser = pricedPerUser)
  }

  def getSubscription(plan: PaymentPlan,
                      start: DateTime = DateTime.now(DateTimeZone.UTC).minusDays(1),
                      end: DateTime = DateTime.now(DateTimeZone.UTC).plusDays(1)): Subscription = {
    class CustomSubscription(plan: PaymentPlan)
        extends Subscription(new Random().nextInt(),
                             plan.id,
                             plan.users,
                             plan.repos,
                             plan.value,
                             DateTime.now(DateTimeZone.UTC),
                             Some(DateTime.now(DateTimeZone.UTC).plusWeeks(2)),
                             None,
                             None) {

      private lazy val paymentPlan: PaymentPlan = {
        plan
      }

      def hasDedicatedServer: Boolean = paymentPlan.hasDedicatedServer

      def code: String = paymentPlan.code

      def title: String = paymentPlan.title

      def default: Boolean = paymentPlan.default

    }
    new CustomSubscription(plan)
  }

  def getProject(userId: Long, id: Long, url: String = "url"): Project = {
    val timestamp = DateTime.now(DateTimeZone.UTC)
    Project(id,
            "project",
            ProjectProvider.inline,
            url,
            "data",
            CreationMode.Manual,
            0,
            timestamp,
            None,
            None,
            DateTime.now,
            Option(userId),
            access = ProjectAccess.Public,
            pinned = false)
  }

  def getProjects(userId: Long, n: Long): Seq[Project] = {
    val timestamp = DateTime.now(DateTimeZone.UTC)
    0.to(n.toInt).map { id =>
      Project(-1,
              s"project $id",
              ProjectProvider.inline,
              "url",
              "data",
              CreationMode.Manual,
              0,
              timestamp,
              None,
              None,
              DateTime.now,
              Option(userId),
              access = ProjectAccess.Public,
              pinned = false)
    }
  }

  def getBranch(id: Long, name: String, project: Project): Branch = {
    Branch(id, project.id, name, BranchStatus.Enabled, isDefault = false, BranchType.Branch)
  }

  def getCommits(projectId: ProjectIdentifier, n: Int): List[Commit] = {
    val timestamp = DateTime.now(DateTimeZone.UTC)

    val gendUUIDS = 0.to(n + 1).map(_ => newUUID)

    (0 to n).map { id =>
      Commit(id,
             gendUUIDS(id),
             gendUUIDS(id + 1),
             timestamp.minusSeconds(id),
             timestamp.minusSeconds(id),
             "owner",
             CommitEmailAddressBase.fromString(FoundationSpecsHelper.newEmailStr),
             "message",
             enabled = true,
             timestamp.minusSeconds(id),
             RetentionStatus.FullData,
             None,
             None,
             projectId.id,
             notificationSent = true)
    }.toList
  }

  def getPullRequest(projectId: ProjectIdentifier,
                     remoteId: Option[String] = None,
                     provider: IntegrationType.Value = IntegrationType.Bitbucket,
                     retentionStatus: Option[RetentionStatus.Value]): PullRequest = {
    PullRequest(newId,
                projectId.id,
                newId,
                newId,
                None,
                new Random().nextInt(),
                "",
                "",
                PullRequestState.Open,
                "",
                "",
                "",
                isMergeable = true,
                DateTime.now,
                DateTime.now,
                analysing = true,
                None,
                None,
                None,
                retentionStatus,
                None,
                None,
                remoteId,
                Some(provider))
  }

  private def createUniqueName = {
    UniqueNameTable.create(UniqueName(-1, UUID.randomUUID.toString, deprecated = false, DateTime.now(DateTimeZone.UTC)))
  }
}
