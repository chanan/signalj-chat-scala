import play.api.{Play, GlobalSettings}
import play.api.mvc.{Handler, RequestHeader}
import signalJ.TransportTransformer

object Global extends GlobalSettings {
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    val newRequest = TransportTransformer.transform(request)
    Option((Play.maybeApplication flatMap { app =>
      app.routes flatMap { router =>
        router.handlerFor(newRequest)
      }
    }).orNull)
  }
}