package async

import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import scala.io.Codec.charset2codec
import scala.io.Codec
import scala.io.Source

import Async.async
import Async.await

import akka.dispatch.Await
import akka.dispatch.ExecutionContexts
import akka.dispatch.Future
import akka.util.duration.intToDurationInt

object Example {
  
  /**
   * Prints:
   * {{{
   * main '4'
   * UI '1'
   * Worker 'downloading: http://www.akka.io/'
   * UI '2'
   * Worker 'downloading: http://www.scala-lang.org/'
   * UI '3'
   * UI 'result: 60959'
   * }}}
   */
  def main(args: Array[String]) {
    val totalLength = async { 
      m("1")
      val akka = await { downloadAsync("http://www.akka.io/") }
      m("2")
      val scala = await { downloadAsync("http://www.scala-lang.org/") }
      m("3")
      akka.length + scala.length 
    }
    totalLength onSuccess { case r => m("result: " + r) }
    m("4")
    Await.ready(totalLength, 5 seconds)
  }
  
  /** Downloads the content of the given url asynchronously. */
  def downloadAsync(url: String): Future[String] = Future { 
    m("downloading: " + url); Source.fromURL(new URL(url))(Codec.ISO8859).getLines.mkString
  } (threadPool)
  
  /** Worker thread pool */
  val threadPool = ExecutionContexts.fromExecutor(Executors.newFixedThreadPool(2, 
    new ThreadFactory {
      def newThread(r: Runnable) = { val t = new Thread(r); t.setName("Worker"); t.setDaemon(true); t }
    }
  ))
  
  /** Implicit ExecutionContext to 'simulate' the UI thread. Check the Swing example
   *  for a more realistic example. */
  implicit val UI = ExecutionContexts.fromExecutor(Executors.newSingleThreadExecutor(
      new ThreadFactory {
      def newThread(r: Runnable) = { val t = new Thread(r); t.setName("UI"); t.setDaemon(true); t }
    }
  ))
  
  /** Prints msg prefixed with the name of the current thread. */
  def m(msg: String) = println(Thread.currentThread().getName() + " '" + msg + "'")
}

