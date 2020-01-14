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
