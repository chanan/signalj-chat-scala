package actors

import akka.actor.Actor
import hubs.{ChatPage, ChatHub}
import signalJ.GlobalHost
import signalJ.services.HubContext

class Robot extends Actor{
  def receive = {
    case _  => {
      val hub: HubContext[ChatPage] = GlobalHost.getHub(classOf[ChatHub])
      hub.clients.all.sendMessage("Robot", "I am alive!")
    }
  }
}