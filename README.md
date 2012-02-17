# scala-async

scala-async is an attempt to implement something akin to C#'s async based on scala's delimited continuations and akka's Futures.

## Example
		
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
	  def downloadAsync(url: String): Future[String] = { ... }
	  
	  ...
	}
	
Check the full [ReadmeExample.scala](https://github.com/danielkroeni/scala-async/blob/master/src/main/scala/async/ReadmeExample.scala)


