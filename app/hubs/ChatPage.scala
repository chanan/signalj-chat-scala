package hubs

trait ChatPage {
  def userJoined(username: String)
  def sendMessage(username: String, message: String)
  def userList(users: Array[String])
  def userLeftRoom(username: String)
  def userJoinedRoom(username: String)
  def roomList(rooms: Array[String])
}