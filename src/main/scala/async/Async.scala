package async

import scala.util.continuations._
import akka.dispatch.ExecutionContext
import akka.dispatch.Future
import akka.dispatch.Promise
import akka.dispatch.ExecutionContexts
import scala.concurrent.forkjoin.ForkJoinPool

object Async {
  /** Holds the ExecutionContext captured by async for use in await. */
  private val localContext = new ThreadLocal[ExecutionContext]()
  /** By name to runnable. */
  implicit def toRunnable[T](f: => T): Runnable = new Runnable { def run = f }
  /** Default ExecutionContext. */
  val defaultContext = new ExecutionContext {
    val pool = new ForkJoinPool
    def execute(r: Runnable) { pool.execute(r) }
    def reportFailure(t: Throwable) { sys.error(t.toString()) }
  }
  
  
  def async[A](body: => A @suspendable)(implicit ec: ExecutionContext = defaultContext): Future[A] = {
    val p = Promise[A]()(ec)
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
  
  private def run(r: Runnable, ec: ExecutionContext) {
    if (ec != null) ec.execute(r)
    else r.run()
  }
}
