package com.example.myservice

import java.util.UUID

import play.api.libs.json.{Format, Json, Writes}
import play.api.mvc.{EssentialFilter, PathBindable}
import play.api.routing.Router
import play.api.routing.sird.PathBindableExtractor
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator}

import scala.collection.mutable
import scala.concurrent.Future

class AppLoader extends ApplicationLoader {
  override def load(context: ApplicationLoader.Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    new AppComponents(context).application
  }
}

class AppComponents(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context) {

  override lazy val httpFilters: Seq[EssentialFilter] = Seq.empty

  override lazy val router: Router = {

    def jsonResult[A: Writes](result: A) = Json.obj("result" -> result)

    import PathExtractors._
    import play.api.mvc.Results._
    import play.api.routing.sird._

    Router.from {
      case GET(p"/") => Action {
        Ok("Hello world!")
      }
      case GET(p"/persons") => Action.async {
        personRepository.all.map(p => Ok(jsonResult(p)))
      }
      case GET(p"/persons/${uuid(id)}") => Action.async {
        personRepository.get(id).map(p => Ok(jsonResult(p)))
      }
      case POST(p"/persons") => Action.async(parse.json[Person]) { request =>
        personRepository.save(request.body).map(p => Created(jsonResult(p)))
      }
      case DELETE(p"/persons/${uuid(id)}") => Action.async {
        personRepository.delete(id).map(r => Ok(jsonResult(r)))
      }
      case _ => Action(parse.empty) { _ =>
        NotFound("Action not found!")
      }
    }
  }

  lazy val personRepository: PersonRepository = new MockPersonRepository

}

object PathExtractors {
  val uuid = new PathBindableExtractor[UUID]()(PathBindable.bindableUUID)
}

trait PersonRepository {
  def all: Future[Seq[Person]]
  def get(uuid: UUID): Future[Option[Person]]
  def save(person: Person): Future[Person]
  def delete(uuid: UUID): Future[Option[Person]]
}

class MockPersonRepository extends PersonRepository {
  private val persons: mutable.Map[UUID, Person] = mutable.Map.empty

  def all = Future.successful {
    persons.values.toSeq
  }

  def get(uuid: UUID) = Future.successful {
    persons.get(uuid)
  }

  def save(person: Person) = Future.successful {
    val uuid = person.id getOrElse UUID.randomUUID
    val newPerson = person.copy(id = Some(uuid))
    persons.put(uuid, newPerson)
    newPerson
  }

  def delete(uuid: UUID) = Future.successful {
    persons.remove(uuid)
  }

}

case class Person(id: Option[UUID] = None, name: String)
object Person {
  implicit val format: Format[Person] = Json.format
}
