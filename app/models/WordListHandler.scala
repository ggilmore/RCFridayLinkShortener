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
  val filePath = WordListHandler.getClass.getResource("words.txt").toString
  private val originalWordList:mutable.Set[String] = mutable.Set() ++
    Source.fromFile(new File(filePath)).getLines.toSeq.filterNot(x => x.isEmpty).map(x=>x.trim)
  private val usedWords:mutable.Set[String] = Set()
  private val shortenedToOriginalMap:mutable.Map[String, String] = Map()
  val actorReference:ActorRef = Akka.system.actorOf(Props[WordListHandler], "Handler")


}

class WordListHandler extends Actor {

  def receive = {
    case CreateShortenedLink(link, duration) =>
    case GetLink(link) => {
      sender ! retrieveLink(link)
    }
  }

  def retrieveLink(link:String):Option[String] = WordListHandler.shortenedToOriginalMap.get(link)

  def createLink(link:String, duration:String):Unit = {
    if (WordListHandler.originalWordList.nonEmpty){
      val someWord = WordListHandler.originalWordList.toVector.head
      WordListHandler.originalWordList.remove(someWord)
      WordListHandler.usedWords.add(someWord)

      WordListHandler.shortenedToOriginalMap += (someWord -> link)


      val shouldRetire: Future[Boolean] = Future {
        Thread.sleep(duration.toLong)
        true
      }

      shouldRetire map {
        isExpired => if(isExpired) retireLink(someWord)
      }
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
