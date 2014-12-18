package models.bot.tasks

import akka.actor._
import play.api.Logger
import scala.util.Random
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits._
import services._
import utils.tinder.TinderApi
import utils.tinder.model._


/**
 * This task is specifically meant for opening conversations with zero messages.
 */
class MessageIntroTask(val xAuthToken: String, val tinderBot: ActorRef, val m: Match) extends TaskActor {

  override def preStart() = {
    Logger.debug("[tinderbot] Starting message intro task.")
    self ! "tick"
  }

  private val funIntros: List[String] = List(
    "{name} are you a fan of avocados?",
    "Can you teach a guy to bake and all that?",
    "{name} are you a good rock skipper?",
    "Hello I am a Nigerian Prince and can make you rich beyond your dream! I just need your phone number.",
    "Are you a dog trainer?",
    "I can't wait to introduce you to my mom!",
    "I’m not saying I’m the type you can take home to your mom, but I’m definitely the type you can take home. Please do, actually, I’m homeless :(.",
    "How do you know if someone is gluten free?"
  )

  def receive = {
    case "tick" =>
      // choose the message
      val intro = funIntros(Random.nextInt(funIntros.size)).replace("{name}", m.person.map(_.name).getOrElse(""))
      // send the message
      new TinderApi(Some(xAuthToken)).sendMessage(m._id, intro).map { result =>
        result match {
          case Left(e) =>
            Logger.error("[tinderbot] Swipe task had an error on Tinder: " + e.error)

          case Right(message) =>
            Logger.info("[tinderbot] Opened conversation with match " + m._id)
            Logger.debug("[tinderbot] Message opener was: \"%s...\"" format intro.substring(0, 10))
        }
      }

      // make sure we properly shut down this actor
      self ! PoisonPill

    // someone is sending invalid messages
    case e: Any =>
      Logger.error("[tinderbot] Task received an unknown message")
      Logger.error("[tinderbot] Received: \n %s" format e.toString)

  }

}
