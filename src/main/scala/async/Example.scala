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

object Example extends App {
  def threadFactory(f: Thread => Thread ) = new ThreadFactory {
    def newThread(r: Runnable) = f(new Thread(r))
  }
  
  implicit val GUI = ExecutionContexts.fromExecutor(Executors.newSingleThreadExecutor(
      threadFactory { t => t.setName("UI"); t.setDaemon(true); t }
  ))
  
  val threadPool = ExecutionContexts.fromExecutor(Executors.newFixedThreadPool(2, 
      threadFactory { t => t.setName("Worker"); t.setDaemon(true); t }
  ))  

  def downloadAsync(url: String): Future[String] = Future { 
    m("downloading: " + url)
    Source.fromURL(new URL(url))(Codec.ISO8859).getLines.mkString
  } (threadPool)
  
  val totalLength = async { 
    m("1")
    val akka = await { downloadAsync("http://www.akka.io/") }
    m("2")
    val scala = await { downloadAsync("http://www.scala-lang.org/") }
    m("3")
    akka.length + scala.length 
  } 
  
  totalLength onSuccess { case r => m("result: " + r) }

  Await.ready(totalLength, 5 seconds)
  
  def m(msg: String) = println(Thread.currentThread().getName() + " '" + msg + "'")
}

