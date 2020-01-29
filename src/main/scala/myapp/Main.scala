package myapp

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
