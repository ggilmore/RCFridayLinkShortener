package controllers

import Util.Messages.CreateShortenedLink
import models.WordListHandler
import play.api.data.Forms._
import play.api.mvc._
import play.api.data.Form
import play.api.data._
import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import play.libs.Akka
import scala.concurrent.Future
import scala.concurrent.duration._

object Application extends Controller {

  implicit val timeout = Timeout(3.second)
  implicit val ec = Akka.system().dispatcher

  /**
   * Define a mapping that will handle the UI parameters
   */
  val linkShortenerForm = Form(
    mapping(
      "link" -> nonEmptyText,
      "duration" -> text)
      (CreateShortenedLink.apply)(CreateShortenedLink.unapply)
  )

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def createShortLink = Action {
    implicit request =>
      linkShortenerForm.bindFromRequest.fold({
        hasErrors => Redirect(routes.Application.index())
    },
        shortenedLink => Redirect(routes.Application.createShortLinkAsync(shortenedLink.link, shortenedLink.duration))
    }


def createShortLinkAsync(link: String, duration: String) = Action.async{
  val resFuture = WordListHandler.actorReference ? CreateShortenedLink(link, duration)
  val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Oops", 1.second)
  Future.firstCompletedOf(Seq(resFuture, timeoutFuture)).map {
    case result: Option[String] => Ok(routes.Application.index())
    case err: String  => Ok(routes.Application.index())
  }
}



  def getLink = {

  }

}