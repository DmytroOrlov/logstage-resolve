package myapp

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import distage.config.AppConfig
import izumi.distage.framework.services.ConfigLoader
import izumi.distage.plugins.PluginConfig
import izumi.distage.roles.RoleAppLauncher.Options
import izumi.distage.roles.{RoleAppLauncher, RoleAppMain}
import izumi.fundamentals.platform.cli.model.raw.{RawAppArgs, RawRoleParams}
import izumi.fundamentals.platform.resources.IzResources
import izumi.logstage.api.IzLogger
import zio.IO

import scala.util.{Failure, Success, Try}

object Main
    extends RoleAppMain.Default(launcher = new RoleAppLauncher.LauncherBIO[IO] {
      val pluginConfig =
        PluginConfig.cached(packagesEnabled = Seq("myapp.plugins"))
      override protected def makeConfigLoader(
        logger: IzLogger,
        parameters: RawAppArgs
      ): ConfigLoader = {
        val maybeGlobalConfig =
          parameters.globalParameters.findValue(Options.configParam).asFile
        val roleConfigs = parameters.roles.map { roleParams =>
          roleParams.role -> roleParams.roleParameters
            .findValue(Options.configParam)
            .asFile
        }
        new LocalFSImpl(logger, maybeGlobalConfig, roleConfigs.toMap)
      }
    }) {
  override val requiredRoles = Vector(RawRoleParams(MyRole.id))
}
import izumi.fundamentals.platform.strings.IzString._

/**
  * default config locations:
  *   - common.conf
  *   - common-reference.conf
  *   - common-reference-dev.conf
  *   - $roleName.conf
  *   - $roleName-reference.conf
  *   - $roleName-reference-dev.conf
  */
class LocalFSImpl(logger: IzLogger,
                  primaryConfig: Option[File],
                  roleConfigs: Map[String, Option[File]],
) extends ConfigLoader {

  import ConfigLoader.LocalFSImpl._

  def buildConfig(): AppConfig = {
    val commonConfigFile = toConfig("common", primaryConfig)

    val roleConfigFiles = roleConfigs.flatMap {
      case (roleName, roleConfig) =>
        toConfig(roleName, roleConfig)
    }.toList

    val allConfigs = roleConfigFiles ++ commonConfigFile

    val cfgInfo = allConfigs.map {
      case r: ConfigSource.Resource =>
        IzResources.getPath(r.name) match {
          case Some(value) =>
            s"$r (exists: ${value.path})"
          case None =>
            s"$r (missing)"
        }

      case f: ConfigSource.File =>
        if (f.file.exists()) {
          s"$f (exists: ${f.file.getCanonicalPath})"
        } else {
          s"$f (missing)"
        }
    }

    logger.info(
      s"Using system properties with fallback ${cfgInfo.niceList() -> "config files"}"
    )

    val loaded = allConfigs.map {
      case s @ ConfigSource.File(file) =>
        s -> Try(ConfigFactory.parseFile(file))

      case s @ ConfigSource.Resource(name, _) =>
        s -> Try(ConfigFactory.parseResources(name))
    }

    val (good, bad) = loaded.partition(_._2.isSuccess)

    if (bad.nonEmpty) {
      val failures = bad.collect {
        case (s, Failure(f)) =>
          s"$s: $f"
      }

      logger.error(
        s"Failed to load configs: ${failures.niceList() -> "failures"}"
      )
    }

    val folded = foldConfigs(good.collect {
      case (src, Success(c)) => src -> c
    })

    val config = ConfigFactory
      .systemProperties()
      .withFallback(folded)
      .resolve()

    println(s"""
         |Config after resolution:
         |
         |$config
         |""".stripMargin)

    AppConfig(config)
  }

  protected def defaultConfigReferences(name: String): Seq[ConfigSource] = {
    Seq(
      ConfigSource.Resource(s"$name.conf", ResourceConfigKind.Primary),
      ConfigSource
        .Resource(s"$name-reference.conf", ResourceConfigKind.Primary),
      ConfigSource
        .Resource(s"$name-reference-dev.conf", ResourceConfigKind.Development),
    )
  }

  protected def foldConfigs(
    roleConfigs: Seq[(ConfigSource, Config)]
  ): Config = {
    roleConfigs.foldLeft(ConfigFactory.empty()) {
      case (acc, (src, cfg)) =>
        verifyConfigs(src, cfg, acc)
        acc.withFallback(cfg)
    }
  }

  protected def verifyConfigs(src: ConfigSource,
                              cfg: Config,
                              acc: Config): Unit = {
    import scala.jdk.CollectionConverters._
    val duplicateKeys = acc
      .entrySet()
      .asScala
      .map(_.getKey)
      .intersect(cfg.entrySet().asScala.map(_.getKey))
    if (duplicateKeys.nonEmpty) {
      src match {
        case ConfigSource.Resource(_, ResourceConfigKind.Development) =>
          logger.debug(
            s"Some keys in supplied ${src -> "development config"} duplicate already defined keys: ${duplicateKeys
              .niceList() -> "keys" -> null}"
          )

        case _ =>
          logger.warn(
            s"Some keys in supplied ${src -> "config"} duplicate already defined keys: ${duplicateKeys
              .niceList() -> "keys" -> null}"
          )
      }
    }
  }

  private def toConfig(name: String,
                       maybeConfigFile: Option[File]): Seq[ConfigSource] = {
    maybeConfigFile.fold(defaultConfigReferences(name)) { f =>
      Seq(ConfigSource.File(f))
    }
  }
}
