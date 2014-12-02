package hubs

import java.util.UUID

import actors.Robot
import akka.actor.{ActorRef, Props}
import play.Logger
import play.libs.{Json, Akka}
import signalJ.services.Hub

import scala.concurrent.duration._

class ChatHub extends Hub[ChatPage] {
  private var users: Map[String, Set[String]] = Map.empty
  private var connectionsToUsernames: Map[UUID, String] = Map.empty
  private var robot: ActorRef = null

  override def getInterface: Class[ChatPage] = classOf[ChatPage]

  def login(username: String): Boolean = {
    if (containsValue(connectionsToUsernames, username) || (username == "Robot")) return false
    clients.callerState.put("username", username)
    joinRoom("Lobby")
    clients.caller.roomList(getRoomList)
    if(!connectionsToUsernames.contains(context.connectionId))
      connectionsToUsernames = connectionsToUsernames + (context.connectionId -> clients.callerState.get("username"))
    return true
  }

  private def containsValue(map: Map[UUID, String], value: String): Boolean = {
    if(map.isEmpty) return false;
    if(map.head._2 equalsIgnoreCase value) true
    else containsValue(map.tail, value)
  }

  def joinRoom(room: String) {
    if (users.contains(room)) joinRoom(room, false)
    else createRoom(room)
    clients.group(room).userList(getUserList(room))
  }

  def createRoom(room: String) {
    if(!users.contains(room)) {
      val set = Set.empty[String]
      users = users  + (room -> set)
    }
    joinRoom(room, true)
  }

  def sendMessage(room: String, message: String) {
    clients.othersInGroup(room).sendMessage(clients.callerState.get("username"), message)
  }

  private def joinRoom(room: String, fromCreate: Boolean) {
    val changed: Boolean = removeUserFromRoom(clients.callerState.get("username"))
    addUserToRoom(clients.callerState.get("username"), room)
    if (fromCreate || changed) clients.all.roomList(getRoomList)
  }

  private def addUserToRoom(username: String, room: String) {
    var set = users(room)
    set = set + username
    users = users - room
    users = users + (room -> set)
    groups.add(context.connectionId, room)
    clients.othersInGroup(room).userJoinedRoom(username)
  }

  private def removeUserFromRoom(username: String): Boolean = {
    var room: String = null
    var removekey: String = null
    for ((key, set) <- users) {
      if(set.contains(username)) {
        val removed = set - username
        users = users - key
        users = users + (key -> set)
        room = key
        clients.othersInGroup(key).userLeftRoom(username)
        clients.othersInGroup(key).userList(getUserList(key))
        if (users.get(key).size == 0 && !key.equalsIgnoreCase("Lobby")) removekey = key
      }
    }
    if (room != null) groups.remove(context.connectionId, room)
    if (removekey != null) {
      users = users - removekey
      return true
    }
    false
  }

  private def getUserList(room: String): Array[String] = {
    var userlist = users(room)
    userlist = userlist + "Robot"
    return userlist.toArray
  }

  private def getRoomList: Array[String] = {
    return users.keys.toArray
  }

  override def onDisconnected {
    val username: String = connectionsToUsernames(context.connectionId)
    connectionsToUsernames = connectionsToUsernames - context.connectionId
    Logger.debug("Disconnect: " + username)
    removeUserFromRoom(username)
  }

  override def onConnected {
    if (robot == null) {
      robot = Akka.system.actorOf(Props.create(classOf[Robot]), "robot")
      Akka.system.scheduler.schedule(5 seconds, 30 seconds, robot, "tick")(Akka.system().dispatcher)
    }
  }
}