package myapp.plugins

import java.util.concurrent.ThreadPoolExecutor

import cats.effect.{Async, Blocker, Concurrent, ConcurrentEffect, ContextShift, Sync, Timer}
import distage.Id
import distage.plugins.PluginDef
import izumi.distage.effect.modules.ZIODIEffectModule
import logstage.LogBIO
import myapp.Bracket2
import org.http4s.metrics.prometheus.PrometheusExportService
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{IO, Task}

import scala.concurrent.ExecutionContext

object ZIOPlugin extends ZIODIEffectModule with PluginDef {
  addImplicit[Bracket2[IO]]
  addImplicit[Sync[Task]]
  addImplicit[Async[Task]]
  addImplicit[Concurrent[Task]]
  addImplicit[ContextShift[Task]]
  addImplicit[Timer[Task]]
  make[ConcurrentEffect[Task]].from((runtime: zio.Runtime[Any]) => taskEffectInstance(runtime))
  make[PrometheusExportService[Task]].fromResource(PrometheusExportService.build[Task])

  make[Blocker].from {
    pool: ThreadPoolExecutor@Id("zio.io") =>
      Blocker.liftExecutionContext(ExecutionContext.fromExecutorService(pool))
  }

  make[LogBIO[IO]].from(LogBIO.fromLogger[IO] _)
}
