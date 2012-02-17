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

import Async.await
import Async.async
import akka.dispatch.ExecutionContext
import akka.dispatch.ExecutionContexts
import akka.dispatch.Future
import javax.swing.SwingUtilities

/** Painless asynchronous programming with Swing. */
object ReadmeExample extends SimpleSwingApplication {
  
  val dlButton = new Button("Download")
  val charCount = new TextField { text = "0"; columns = 5 }
  
  def top = new MainFrame {
    title = "Scala-Async Test"
    contents = new FlowPanel(dlButton, new Label("# Chars"), charCount)
  }

  listenTo(dlButton)
  reactions += {
    case ButtonClicked(`dlButton`) => 
      charCount.text = ""
      async {
        // No blocking is involved here!
        val akka = await { downloadAsync("http://www.akka.io/") }
        val scala = await { downloadAsync("http://www.scala-lang.org/") }
        // Access swing components directly
        charCount.text = (akka.length + scala.length).toString
      }
  }
  
  /** Downloads the content of the given url asynchronously. */
  def downloadAsync(url: String): Future[String] = Future { 
    Source.fromURL(new URL(url))(Codec.ISO8859).getLines.mkString
  } (threadPool)
  
  /** Swing execution context */
  implicit val swingCtx = new ExecutionContext {
    def execute(runnable: Runnable): Unit = SwingUtilities.invokeLater(runnable)
    def reportFailure(t: Throwable): Unit = sys.error(t.getMessage())
  }
  
  /** Worker thread pool */
  val threadPool = ExecutionContexts.fromExecutor(Executors.newFixedThreadPool(2, 
    new ThreadFactory {
      def newThread(r: Runnable) = { val t = new Thread(r); t.setDaemon(true); t }
    }
  ))
}
