package async

import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import scala.io.Codec.charset2codec
import scala.io.Codec
import scala.io.Source
import scala.swing.event.ButtonClicked
import scala.swing.Button
import scala.swing.FlowPanel
import scala.swing.Label
import scala.swing.MainFrame
import scala.swing.SimpleSwingApplication
import scala.swing.TextField

import Async.async
import Async.await
import akka.dispatch.ExecutionContext
import akka.dispatch.ExecutionContexts
import akka.dispatch.Future
import javax.swing.SwingUtilities

/** Simple Swing example showing how comfortable asynchronous programming can be. */
object SwingExample extends SimpleSwingApplication {
  
  /** Swing execution context */
  implicit val swingCtx = new ExecutionContext {
    def execute(runnable: Runnable): Unit = SwingUtilities.invokeLater(runnable)
    def reportFailure(t: Throwable): Unit = sys.error(t.getMessage())
  }
  
  val charCount = new TextField { text = "0"; columns = 5 }
  val seqDlButton = new Button { text = "Seq Download" }
  val parDlButton = new Button { text = "Par Download" }
  
  def top = new MainFrame {
    title = "Scala-Async Test"
    contents = new FlowPanel(seqDlButton, parDlButton, new Label("# Chars"), charCount)
  }

  listenTo(seqDlButton, parDlButton)
  reactions += {
    case ButtonClicked(`seqDlButton`) => 
      charCount.text = ""
      async {
        val akka = await { downloadAsync("http://www.akka.io/") }
        val scala = await { downloadAsync("http://www.scala-lang.org/") }
        charCount.text = (akka.length + scala.length).toString
      }
     case ButtonClicked(`parDlButton`) => 
       charCount.text = ""
       val akkaFuture = downloadAsync("http://www.akka.io/")
       val scalaFuture = downloadAsync("http://www.scala-lang.org/")
       async {
        val akka = await { akkaFuture }
        val scala = await { scalaFuture }
        charCount.text = (akka.length + scala.length).toString
      }  
  }
  
  def downloadAsync(url: String): Future[String] = Future { 
    Source.fromURL(new URL(url))(Codec.ISO8859).getLines.mkString
  } (threadPool)
  
  /** Worker thread pool */
  val threadPool = ExecutionContexts.fromExecutor(Executors.newFixedThreadPool(2, 
    new ThreadFactory {
      def newThread(r: Runnable) = { val t = new Thread(r); t.setDaemon(true); t }
    }
  ))
}
