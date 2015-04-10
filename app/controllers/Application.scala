package controllers

import Util.Messages.{GetLink, CreateShortenedLink}
import models.WordListHandler
import play.api.data.Forms._
import play.api.mvc._
import play.api.data.Form
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
      verifying("Duration must be a digit", shortenedLink => isNumber(shortenedLink.duration))
  )

  def index = Action {
    Ok(views.html.index(linkShortenerForm.fill(CreateShortenedLink("",""))))
  }

  def createShortLink = Action {
    implicit request =>
      linkShortenerForm.bindFromRequest.fold({
        hasErrors => Redirect(routes.Application.index())
    },
        originalLink =>
        {
          Redirect(routes.Application.createShortLinkAsync(originalLink.link, originalLink.duration, getFullUrl(request)))}
      )
    }


def createShortLinkAsync(originalLink: String, duration: String, fullUrl: String) = Action.async{
  val resFuture = WordListHandler.actorReference ? CreateShortenedLink(originalLink, duration)
  val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Oops", 3.second)
  Future.firstCompletedOf(Seq(resFuture, timeoutFuture)).map {
    case result: Option[String] => result match {
      case Some(word) => Ok(views.html.showlink(  fullUrl + "/" + word))
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

  private def getFullUrl(request: Request[AnyContent]) = "http://" + request.host + request.path

  private def isNumber(text: String)= {
    try{
      text.toLong
      true
    }catch{
      case e: NumberFormatException => false
    }
  }

}