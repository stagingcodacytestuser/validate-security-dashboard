package services.enterprise

import codacy.database.accountDB.enterprise._
import codacy.database.analysisDB.pattern.deprecated.CategoryTable
import codacy.foundation.api.{Response, ResponseErrorCode}
import codacy.foundation.utils.Logger
import com.github.takezoe.slick.blocking.BlockingPostgresDriver.blockingApi._
import com.typesafe.config.Config
import framework.config.WebsiteConfiguration
import model.enterprise.LicenseTable
import org.joda.time.{DateTime, DateTimeZone}
import slick.jdbc.SQLActionBuilder
import java.util.Add;

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import codacy.foundation.config.ConfigOptions._

final case class ConfigurationValidation(title: String, hasError: Boolean, description: String)

// TODO: Clean mix of configurations
class ConfigurationServices(config: Config) extends Logger {

  def hasValidConfiguration: Boolean = {
    configurationCheck.map(_.hasError).forall(cur => !cur)
  }

  def configurationCheck: Seq[ConfigurationValidation] = {
    val dbChecks = Seq(("Account Database", "default"),
                       ("Analysis Database", "analysis"),
                       ("FileStore Database", "fileStoreDatabase"),
                       ("Jobs Database", "jobs"),
                       ("Results Database", "results2017"),
                       ("Metrics Database", "metrics")).map {
      case (title, dbName) if !hasDBConfiguration(dbName) =>
        ConfigurationValidation(title, hasError = true, s"Missing settings for $title")
      case (title, dbName) if checkDBConfiguration(dbName).hasError =>
        ConfigurationValidation(title, hasError = true, checkDBConfiguration(dbName).errorDescription)
      case (title, dbName) if checkDBContents(dbName).hasError =>
        ConfigurationValidation(title, hasError = true, s"$title is initializing. Please wait a few moments.")
      case (title, _) => ConfigurationValidation(title, hasError = false, "")
    }

    val appChecks = Seq(("Application secret", "play.crypto.secret", () => appSecret),
                        ("Cache secret", "codacy.cache.secret", () => cacheSecret)).map {
      case (title, keyname, dbValue) if !validKeySize(keyname) =>
        ConfigurationValidation(title, hasError = true, s"$title length needs to be larger than 16 chars")
      case (title, keyname, dbValue) if dbChecks.map(_.hasError).forall(cur => cur) =>
        ConfigurationValidation(title, hasError = true, "Database settings not valid")
      case (title, keyname, dbValue) if !validKeyContents(keyname, dbValue()) =>
        ConfigurationValidation(title,
                                hasError = true,
                                s"""Database already contains another key(${dbValue().getOrElse("")})""")
      case (title, keyname, dbValue) => ConfigurationValidation(title, hasError = false, "")
    }

    dbChecks ++ appChecks
  }

  def hasDBPatterns: Boolean = {
    CategoryTable.getAll.nonEmpty
  }

  private def hasDBConfiguration(database: String): Boolean = {
    withDBConfiguration(database)((url, user, _) => url.nonEmpty && user.nonEmpty).getOrElse(false)
  }

  private def checkDBConfiguration(database: String): Response[Boolean] = {
    withDBConfiguration(database)((url, user, password) => testConnection(database, url, user, password))
      .getOrElse(Response.error(ResponseErrorCode.GenericError, "DB not configured"))
  }

  private def checkDBContents(database: String): Response[Boolean] = {
    withDBConfiguration(database)((url, user, password) => testDatabase(database, url, user, password))
      .getOrElse(Response.error(ResponseErrorCode.GenericError, "DB not configured"))
  }

  private def withDBConfiguration[T](database: String)(f: (String, String, String) => T): Option[T] = {
    for {
      url <- WebsiteConfiguration.database.url(database)
      user <- WebsiteConfiguration.database.user(database)
      password <- WebsiteConfiguration.database.password(database)
    } yield f(url, user, password)
  }

  private def validKeySize(keyName: String): Boolean = {
    val currentSecret = config.getOption(keyName)(_.getString)
    currentSecret.getOrElse("").length > 16
  }

  private def validKeyContents(keyName: String, dbContents: Option[String]): Boolean = {
    dbContents.forall { dbKey =>
      config.getOption(keyName)(_.getString).getOrElse(dbKey) == dbKey
    }
  }

  def updatedLicense(): Option[LicenseKey] = {
    assert(WebsiteConfiguration.enterprise.enterpriseDeployment)
    Try {
      for {
        appKey <- config
          .getOption("play.crypto.secret")(_.getString)
          .orElse(config.getOption("application.secret")(_.getString))
        cacheKey <- config.getOption("codacy.cache.secret")(_.getString)
        licenseUUID <- WebsiteConfiguration.enterprise.licenseUUID
        license = License(-1, licenseUUID, appKey, cacheKey, DateTime.now(DateTimeZone.UTC))
        if !LicenseTable.containsUUID(license)
      } yield LicenseTable.create(license)
    } match {
      case Failure(ex)        => logger.error("Exception", ex)
      case Success(Some(lic)) => logger.info(s"License updated: ${lic.timestamp.toString("dd-MM-yyyy")}")
      case _                  =>
    }

    LicenseTable.getLatest.flatMap { lic =>
      lic.licenseKey match {
        case Success(res) =>
          Option(res)
        case Failure(ex) =>
          logger.error(s"Could not parse license body: ${ex.getMessage}")
          logger.error("Exception", ex)

          if (ex.getMessage.contains("BadPaddingException") || ex.getMessage.contains("Decryption error")) {
            LicenseTable.deleteById(LicenseTable.getAll.map(_.id))
          }

          None
      }
    }
  }

  def applicationNeedsRestart: Boolean = {
    isKeyDifferent("play.crypto.secret") || isKeyDifferent("db.default.url")
  }

  private def isKeyDifferent(key: String): Boolean = {
    config.getOption(key)(_.getString).forall { playConf =>
      !WebsiteConfiguration.tc.getOption(key)(_.getString).contains(playConf)
    }
  }

  private lazy val appSecret: Option[String] = {
    try {
      if (hasDBConfiguration("default")) {
        LicenseTable.getApplicationSecret
      } else {
        None
      }
    } catch {
      case NonFatal(_) => None
    }
  }

  private lazy val cacheSecret: Option[String] = {
    try {
      if (hasDBConfiguration("default")) {
        LicenseTable.getCacheSecret
      } else {
        None
      }
    } catch {
      case NonFatal(_) => None
    }
  }

  private def testConnection(dbname: String, url: String, user: String, password: String): Response[Boolean] = {
    testQuery(dbname, url, user, password)(sql"SELECT 1;")
  }

  private def testDatabase(dbname: String, url: String, user: String, password: String): Response[Boolean] = {
    testQuery(dbname, url, user, password)(sql"SELECT * FROM play_evolutions LIMIT 1;")
  }

  private def testQuery(dbname: String, url: String, user: String, password: String)(
    actionBuilder: SQLActionBuilder): Response[Boolean] = {
    try {
      val database = Database.forURL(url, user, password, driver = "org.postgresql.Driver")
      database.withSession { implicit session =>
        val identifier = actionBuilder.as[Long].list
        Response(Some(identifier.length == 1))
      }
    } catch {
      case NonFatal(_) =>
        new Response(None, ResponseErrorCode.GenericError)
    }
  }

}
