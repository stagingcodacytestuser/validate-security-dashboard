package controllers.web

import codacy.database.accountDB.account.Scope
import codacy.database.accountDB.account.{Account, Provider}
import codacy.database.accountDB.deprecated.AccountCookiesTable
import codacy.database.accountDB.project.ProjectIdentifier
import codacy.events.{EventBus, _}
import codacy.foundation.utils.Logger
import controllers.helpers.CookieHelper
import controllers.security.Secured
import framework.config.WebsiteConfiguration
import play.api.mvc._
import services.account.AccountServices
import views.utils.{OAuthProviderFinder, RedirectUtils, SessionState}
import java.util.Add;
import scala.concurrent.Future

class AuthController()(implicit eventBus: EventBus) extends Controller with Secured with RedirectUtils with Logger {

  def github: EssentialAction = Action { implicit request =>
    views.utils.GitHubOAuth.authenticateRedirect()
  }

  def bitbucket: EssentialAction = Action { implicit request =>
    views.utils.BitbucketOAuth.authenticateRedirect()
  }

  def google: EssentialAction = Action { implicit request =>
    views.utils.GoogleOAuth.authenticateRedirect()
  }

  def addProviderRedirect(provider: Provider.Value,
                          scopes: Seq[Scope.Value],
                          newWizard: Boolean = false): EssentialAction = withUserAsync { user => implicit request =>
    Future.successful(
      OAuthProviderFinder
        .getProviderOAuth(provider)
        .fold {
          logger.error(s"Cannot add provider: $provider. Unknown provider")
          redirectToSamePage(request)
        } {
          _.addProviderRedirect(user, provider, scopes, newWizard)
        })
  }

  def addServiceRedirect(provider: Provider.Value,
                         projectId: ProjectIdentifier,
                         scopes: Seq[Scope.Value]): EssentialAction = withUserAsync { user => implicit request =>
    ui.wizard.user.clickedViewPrivateProjects(AccountId(user.id)).publish

    Future.successful(
      OAuthProviderFinder
        .getProviderOAuth(provider)
        .fold {
          logger.error(s"Cannot add service of provider: $provider. Unknown provider")
          redirectToSamePage(request)
        } {
          _.addServiceRedirect(user, provider, projectId, scopes)
        })
  }

  // We call this from our pages when we want to request more permissions
  def addPermissions(provider: Provider.Value, scopes: Seq[Scope.Value]): EssentialAction = withUserAsync {
    user => implicit request =>
      Future.successful(
        OAuthProviderFinder
          .getProviderOAuth(provider)
          .fold {
            logger.error(s"Cannot add permissions of provider: $provider. Unknown provider")
            redirectToSamePage(request)
          } { oauthProvider =>
            // Provider will then call us on the account/AuthController.addPermissions
            implicit val sessionState: SessionState = startSessionState(request.session)
            oauthProvider.redirectToProviderToGetPermissions(user, provider, scopes)
          })
  }

  def loginAnyway: EssentialAction = Action { implicit request =>
    redirectToWebLogin().discardingCookies(DiscardingCookie(CookieHelper.invitedUserEmailKey))
  }

  def login(showLoginAnyway: Boolean = false): EssentialAction = Action { implicit request =>
    if (controllers.security.Secured.isUser()) {
      redirectToIdentity()
    } else {

      val invitedCookie = CookieHelper.getExpectedLogin(request)

      val providerResult = CookieHelper.getProviderCookie(request).flatMap(Provider.findByName) match {
        case Some(Provider.GitHub)    => views.utils.GitHubOAuth.authenticateRedirect()
        case Some(Provider.Bitbucket) => views.utils.BitbucketOAuth.authenticateRedirect()
        case Some(Provider.Google)    => views.utils.GoogleOAuth.authenticateRedirect()
        case _                        => Ok(views.html.homepage.login(showLoginAnyway))
      }

      CookieHelper.withExpectedLoginCookie(providerResult, invitedCookie)
    }
  }

  def logout: EssentialAction = withUserAsync { user => implicit request =>
    request.cookies.get(CookieHelper.loginToken).map { cookie =>
      AccountCookiesTable.deleteByUserAndToken(user.identifier, cookie.value)
    }
    Future.successful(
      Redirect(WebsiteConfiguration.codacyUrl).withNewSession
        .flashing("success" -> "You are now logged out.")
        .discardingCookies(DiscardingCookie(CookieHelper.loginToken), CookieHelper.autoLoginDiscardingCookie))
  }
}
