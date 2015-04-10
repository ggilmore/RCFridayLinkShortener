package controllers

import Util.Messages.{GetLink, CreateShortenedLink}
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
    Ok(views.html.index(linkShortenerForm.fill(CreateShortenedLink("",""))))
  }

  def createShortLink = Action {
    implicit request =>
      linkShortenerForm.bindFromRequest.fold({
        hasErrors => Redirect(routes.Application.index())
    },
        shortenedLink => Redirect(routes.Application.createShortLinkAsync(shortenedLink.link, shortenedLink.duration)))
    }


def createShortLinkAsync(link: String, duration: String) = Action.async{
  val resFuture = WordListHandler.actorReference ? CreateShortenedLink(link, duration)
  val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Oops", 3.second)
  Future.firstCompletedOf(Seq(resFuture, timeoutFuture)).map {
    case result: Option[String] => result match {
      case Some(word) => Ok(views.html.showlink(word))
      case None =>  Ok(views.html.showlink("No more words available to use"))
    }
    case err: String  =>Ok(views.html.showlink("Internal Server Error"))
  }
}



  def getLink(link:String) = Action.async{
    val resFuture = WordListHandler.actorReference ? GetLink(link)
    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Oops", 3.second)
    Future.firstCompletedOf(Seq(resFuture, timeoutFuture)).map {
      case result: Option[String] => result match {
        case Some(link) => {
          println("LINK: " + link)
          Redirect(link)}
        case None =>  Ok(views.html.showlink("Link not found"))
      }
      case err: String  => Ok(views.html.showlink("Internal Server Error"))
    }
  }

}