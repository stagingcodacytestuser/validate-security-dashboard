trait StateVerification extends Logger {
  def verifySessionState(provider: Provider.Value, request: Request[AnyContent]): Boolean = {
    provider match {
      case Provider.Bitbucket | Provider.Stash => true
      case _ =>
        if (WebsiteConfiguration.isOAuthStateEnable) {
          request.session.get("sessionId").exists { sessionId =>
            val stateMatched = request.getQueryString("state").exists { state =>
              accountDB.deprecated.LoginSessionsTable
                .getBySession(sessionId, WebsiteConfiguration.loginStateDuration.toMillis)
                .contains(state)
            }
            accountDB.deprecated.LoginSessionsTable.deleteSession(sessionId)
            stateMatched
          }
        } else {
          true
        }
    }
  }
  
  redirectToWebLogin().withNewSession
        .flashing("success" ->
          "Thanks for signing up. We're currently under private beta but we'll notify you as soon as we go live.")
        .discardingCookies(DiscardingCookie(CookieHelper.providerCookieKey), CookieHelper.autoLoginDiscardingCookie)

lazy val autoLoginCookie = Cookie(name = autoLoginKey,
                                    value = "true",
                                    maxAge = Option(autoLoginExpiration),
                                    httpOnly = false,
                                    domain = autoLoginDomain)

  lazy val autoLoginDiscardingCookie = DiscardingCookie(name = autoLoginKey, domain = autoLoginDomain)

  def withProviderCookie(result: Result, provider: Provider.Value): Result = {
    val providerCookie =
      Cookie(providerCookieKey, provider.toString, Option(providerCookieExpiration), httpOnly = false)

    result.withCookies(autoLoginCookie, providerCookie)
  }
