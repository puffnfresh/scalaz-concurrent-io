package scalaz.concurrent.io

import scalaz.effect.IO
import java.util.concurrent.{ TimeUnit, ScheduledExecutorService }

case class Schedulable(schedule: (IO[Unit], Long, TimeUnit) => IO[Forked])

object Schedulable {
  def fromScheduledExecutorService(e: ScheduledExecutorService) =
    Schedulable { (io, t, tu) =>
      IO(e.schedule(unsafeRunnableIO(io), t, tu)).map { f =>
        Forked.fromFuture(f)
      }
    }
}
