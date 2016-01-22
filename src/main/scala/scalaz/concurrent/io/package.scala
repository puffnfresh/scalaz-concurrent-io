package scalaz.concurrent

import java.util.concurrent.{ Executors, TimeUnit }
import scalaz.effect.IO
import scalaz.{ \/, \/-, -\/, NonEmptyList, Maybe, Traverse }

package object io {
  def unsafeRunnableIO(io: IO[Unit]): Runnable =
    new Runnable { def run() { io.unsafePerformIO } }

  def fixedThreadPool(i: Int): IO[Forkable] =
    IO(Executors.newFixedThreadPool(i)).map(Forkable.fromExecutorService)

  def scheduledThreadPool(i: Int): IO[Schedulable] =
    IO(Executors.newScheduledThreadPool(i)).map(Schedulable.fromScheduledExecutorService)

  val singleForkable: IO[Forkable] =
    IO(Executors.newSingleThreadExecutor).map(Forkable.fromExecutorService)

  val singleSchedulable: IO[Schedulable] =
    IO(Executors.newSingleThreadScheduledExecutor).map(Schedulable.fromScheduledExecutorService)

  def timeoutOn(e: Schedulable, t: Long, tu: TimeUnit) =
    for {
      x <- MVar.newEmptyMVar[Unit]
      _ <- e.schedule(x.put(()), t, tu)
      _ <- x.take
    } yield ()

  def timeout(t: Long, tu: TimeUnit) =
    for {
      e <- singleSchedulable
      _ <- timeoutOn(e, t, tu)
    } yield ()

  def forkIO[A](io: IO[A]): IO[Forked] =
    for {
      e <- singleForkable
      s <- e.fork(io.map(_ => ()))
    } yield s

  def submitToMVar[A](io: IO[A], e: Forkable): IO[(MVar[A], Forked)] =
    for {
      m <- MVar.newEmptyMVar[A]
      f <- e.fork(io.flatMap(m.put(_)))
    } yield (m, f)

  def waitAny[A](ios: NonEmptyList[IO[A]], e: Forkable): IO[A] =
    for {
      m <- MVar.newEmptyMVar[A]
      fs <- Traverse[NonEmptyList].traverse(ios) { io => e.fork(io.flatMap(m.put(_))) }
      a <- m.take
      _ <- Traverse[NonEmptyList].traverse_(fs)(_.cancel)
    } yield a

  def waitBoth[A, B](ioa: IO[A], iob: IO[B], e: Forkable): IO[(A, B)] =
    for {
      af <- submitToMVar(ioa, e)
      bf <- submitToMVar(iob, e)
      a <- af._1.take
      b <- bf._1.take
    } yield (a, b)

  def waitEither[A, B](ioa: IO[A], iob: IO[B], e: Forkable): IO[A \/ B] =
    waitAny(NonEmptyList(ioa.map(-\/(_)), iob.map(\/-(_))), e)
}
