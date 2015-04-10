package models

import java.io.File

import Util.Messages.{GetLink, CreateShortenedLink}

import scala.collection.mutable
import scala.concurrent.Future
import scala.io.Source
import akka.actor.{Props, ActorRef, Actor}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.{JsValue, Json}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.mutable.{Set, Map}

/**
 * Created by gmgilmore on 4/10/15.
 */
object WordListHandler {
//  val filePath = WordListHandler.getClass.getResource("words.txt").toString
//  require(filePath != null)
  println("WHAT ABOUT HERE")
  private val originalWordList:mutable.Set[String] = mutable.Set() ++
    Source.fromFile(new File("app/resources/words.txt")).getLines.toSeq.filterNot(x => x.isEmpty).map(x=>x.trim)
  private val usedWords:mutable.Set[String] = Set()
  private val shortenedToOriginalMap:mutable.Map[String, String] = Map()
  val actorReference:ActorRef = Akka.system.actorOf(Props[WordListHandler], "Handler")


}

class WordListHandler extends Actor {

  def receive = {
    case CreateShortenedLink(link, duration) => createLink(link, duration)
    case GetLink(link) => {
      retrieveLink(link) match {
        case Some(link) => {
          val target = if (link.startsWith("http://") || link.startsWith("https://")) link else "http://"+link
          sender ! Some(target)
        }
        case None => sender ! None

      }
    }
  }

  def retrieveLink(link:String):Option[String] = {
    println(WordListHandler.shortenedToOriginalMap)
    WordListHandler.shortenedToOriginalMap.get(link)}

  def createLink(link:String, duration:String):Unit = {
    if (WordListHandler.originalWordList.nonEmpty){
      val someWord = WordListHandler.originalWordList.toVector.head
      WordListHandler.originalWordList.remove(someWord)
      WordListHandler.usedWords.add(someWord)

      WordListHandler.shortenedToOriginalMap += (someWord -> link)


      val shouldRetire: Future[Boolean] = Future {
        Thread.sleep((duration.toLong)*1000*60)
        true
      }

      shouldRetire map {
        isExpired => if(isExpired) retireLink(someWord)
      }
      println(WordListHandler.shortenedToOriginalMap)
      sender ! Some(someWord)
    }
    else sender ! None
  }

  def retireLink(link:String):Unit = {
    WordListHandler.shortenedToOriginalMap.remove(link)
    WordListHandler.usedWords.remove(link)
    WordListHandler.originalWordList.add(link)
  }
}
