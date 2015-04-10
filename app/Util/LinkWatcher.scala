package Util

import _root_.Util.Messages.RetireLink
import models.WordListHandler

/**
 * Created by gmgilmore on 4/10/15.
 */
case class LinkWatcher(link:String, duration:Long) extends Thread(new Runnable {
  override def run(): Unit = {
    Thread.sleep(duration)
    WordListHandler.actorReference ! RetireLink(link)
  }
})