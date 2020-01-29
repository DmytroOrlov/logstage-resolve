package myapp


import izumi.distage.plugins.load.PluginLoader.PluginConfig
import izumi.distage.roles.services.PluginSource
import izumi.distage.roles.{BootstrapConfig, RoleAppLauncher, RoleAppMain}
import izumi.fundamentals.platform.cli.model.raw.RawRoleParams
import zio.Task
import zio.interop.catz._

object Main extends RoleAppMain.Default(
  new RoleAppLauncher.LauncherF[Task] {
    override val pluginSource = PluginSource(
      BootstrapConfig(
        PluginConfig(
          debug = false,
          packagesEnabled = Seq("myapp.plugins"),
          packagesDisabled = Nil,
        )
      )
    )
  }
) {
  override val requiredRoles = Vector(RawRoleParams.empty(MyRole.id))
}
/*
import izumi.distage.plugins.PluginConfig
import izumi.distage.roles.{RoleAppLauncher, RoleAppMain}
import izumi.fundamentals.platform.cli.model.raw.RawRoleParams
import zio.IO

object Main extends RoleAppMain.Default(
  launcher = new RoleAppLauncher.LauncherBIO[IO] {
    val pluginConfig = PluginConfig.cached(
      packagesEnabled = Seq("myapp.plugins"),
    )
  }
) {
  override val requiredRoles = Vector(RawRoleParams(MyRole.id))
}
*/
