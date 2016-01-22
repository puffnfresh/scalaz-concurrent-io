package scalaz.concurrent.io

import scalaz.effect.IO
import java.util.concurrent.{ Future => JFuture }

case class Forked(cancel: IO[Unit], block: IO[Unit])

object Forked {
  def fromFuture[A](f: JFuture[A]) =
    Forked(IO(f.cancel(true)), IO(f.get))
}
