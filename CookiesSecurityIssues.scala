class AuthController()(implicit eventBus: EventBus) extends Controller with Secured with RedirectUtils with Logger {
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

}
