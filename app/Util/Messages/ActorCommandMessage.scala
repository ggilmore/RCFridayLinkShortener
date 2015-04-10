package Util.Messages

/**
 * Created by gmgilmore on 4/10/15.
 */
sealed trait ActorCommandMessage

case class CreateShortenedLink(link:String) extends ActorCommandMessage

case class GetLink(link:String, duration:String = "5" ) extends ActorCommandMessage

case class RetireLink(link:String) extends ActorCommandMessage
