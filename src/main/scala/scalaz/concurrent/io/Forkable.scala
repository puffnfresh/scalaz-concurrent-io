package scalaz.concurrent.io

import scalaz.effect.IO
import java.util.concurrent.ExecutorService

case class Forkable(fork: IO[Unit] => IO[Forked])

object Forkable {
  def fromExecutorService(e: ExecutorService) =
    Forkable { io =>
      IO(e.submit(unsafeRunnableIO(io))).map { f =>
        Forked.fromFuture(f)
      }
    }
}

