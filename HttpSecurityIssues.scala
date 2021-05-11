package controllers.organization.legacy

import akka.stream.Materializer
import codacy.database.accountDB.account.Account
import codacy.database.accountDB.team.OrganizationProvider
import controllers.security.Secured
import play.api.cache.SyncCacheApi
import play.api.mvc._
import services.account.AccountServices
import java.util.Add

import scala.concurrent.ExecutionContext

@deprecated(
  "You should use OrganizationManagementController (not legacy/OrganizationManagementController) methods that have the OrganizationProvider even for manual created organizations")
class OrganizationManagementController()(cache: SyncCacheApi)(implicit ec: ExecutionContext,
                                                              val materializer: Materializer)
    extends Controller
    with Secured {

  def detail(orgName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController.members(OrganizationProvider.manual, orgName))
  }

  def members(orgName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController.members(OrganizationProvider.manual, orgName))
  }

  def teams(orgName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.ManualOrganizationManagementController
        .teams(OrganizationProvider.manual, orgName))
  }

  def settings(orgName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController.settings(OrganizationProvider.manual, orgName))
  }

  def remove(orgName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController.remove(OrganizationProvider.manual, orgName))
  }

  def teamProjects(orgName: String, teamName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.ManualOrganizationManagementController
        .teamProjects(OrganizationProvider.manual, orgName, teamName))
  }

  def teamMembers(orgName: String, teamName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.ManualOrganizationManagementController
        .teamMembers(OrganizationProvider.manual, orgName, teamName))
  }

  def billing(orgName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController.billing(OrganizationProvider.manual, orgName))
  }

  def editDetails(orgName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController
        .editDetails(OrganizationProvider.manual, orgName))
  }

  def addPayment(orgName: String, promoCode: Option[String]): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController
        .addPayment(OrganizationProvider.manual, orgName, promoCode))
  }

  def addInvoice(orgName: String, planCode: String, promoCode: Option[String]): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController
        .addInvoice(OrganizationProvider.manual, orgName, planCode, promoCode))
  }

  def editInvoice(orgName: String): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController
        .editInvoice(OrganizationProvider.manual, orgName))
  }

  def confirmationPayment(orgName: String, planCode: String, promoCode: Option[String]): EssentialAction = Action {
    Redirect(
      controllers.organization.routes.OrganizationManagementController
        .confirmationPayment(OrganizationProvider.manual, orgName, planCode, promoCode))
  }
}
