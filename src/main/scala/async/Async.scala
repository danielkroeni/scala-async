package async

import scala.util.continuations._
import akka.dispatch.ExecutionContext
import akka.dispatch.Future
import akka.dispatch.Promise

object Async {
  /** Holds the ExecutionContext captured by async for use in await. */
  private val localContext = new ThreadLocal[ExecutionContext]()
  implicit def toRunnable[T](f: => T): Runnable = new Runnable { def run = f }
  
  def async[A](body: => A @suspendable)(implicit ec: ExecutionContext): Future[A] = {
    val p = Promise[A]()
    val runnable = toRunnable { 
      localContext.set(ec)
      reset { 
        try { p.success(body); () }
        catch { case e: Throwable => { p.failure(e); () } }
      }  
    }
    run(runnable, ec)
    p.future
  }
  
  def await[A](block: => Future[A]): A@suspendable = {
    val ec = localContext get()
    shift { cont: (A => Unit) =>
      block onComplete { 
        case Left(e) => throw e
        case Right(r) => run(cont(r),ec)
      }
    }
  }
  
  private[this] def run(r: Runnable, ec: ExecutionContext) {
    if (ec != null) ec.execute(r)
    else r.run()
  }
}
