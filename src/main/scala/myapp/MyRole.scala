package myapp

import izumi.distage.model.definition.DIResource
import izumi.distage.model.definition.DIResource.DIResourceBase
import izumi.distage.roles.model.{RoleDescriptor, RoleService}
import izumi.fundamentals.platform.cli.model.raw.RawEntrypointParams
import logstage.LogBIO
import zio._
import zio.duration._

final class MyRole(
    log: LogBIO[IO],
    clock: zio.clock.Clock,
) extends RoleService[Task] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResourceBase[Task, Unit] = {
    DIResource.fromZIO {
      (log.info(".") *> ZIO.sleep(1.second))
        .unit
        .toManaged_
        .provide(clock)
    }
  }
}

object MyRole extends RoleDescriptor {
  val id = "myapp"
}
