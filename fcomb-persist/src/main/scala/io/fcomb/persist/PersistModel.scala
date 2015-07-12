package io.fcomb.persist

import io.fcomb.models
import io.fcomb.Db._
import io.fcomb.validations
import slick.jdbc.TransactionIsolation
import scala.concurrent.{ ExecutionContext, Future, blocking }
import io.fcomb.RichPostgresDriver.api._
import io.fcomb.RichPostgresDriver.IntoInsertActionComposer
import scalaz._, syntax.validation._, syntax.applicative._
import java.util.UUID

trait PersistTypes[T] {
  type ValidationModel = validations.ValidationType[T]

  def recordNotFound(columnName: String, id: String): ValidationModel =
    validations.validationErrorsMap(columnName -> s"not found record with id: $id")

  def recordNotFoundAsFuture(field: String, id: String): Future[ValidationModel] =
    Future.successful(recordNotFound(field, id))
}

trait PersistModel[T, Q <: Table[T]] extends PersistTypes[T] {
  val table: TableQuery[Q]

  type ModelDBIO = DBIOAction[T, NoStream, Effect.All]
  type ModelDBIOOption = DBIOAction[Option[T], NoStream, Effect.All]

  val validationsOpt = Option.empty[T => validations.FutureValidationMapResult[_]]

  // @inline
  // private def recoverPersistExceptions(
  //   f: Future[ValidationModel]
  // )(implicit ec: ExecutionContext): Future[ValidationModel] =
  //   f.recover {
  //     case _: RecordNotFound => ("record", "not found").failureNel[T]
  //   }

  @inline
  private def runInTransaction[Q](
    q: DBIOAction[Q, NoStream, Effect.All]
  )(implicit ec: ExecutionContext): Future[Q] =
    db.run(q.transactionally.withTransactionIsolation(TransactionIsolation.ReadCommitted))

  protected def validateWith(item: T, vm: Future[ValidationModel])(f: => DBIOAction[T, NoStream, Effect.All])(implicit ec: ExecutionContext, m: Manifest[T]): Future[ValidationModel] = {
    vm.flatMap {
      case Success(_)     => runInTransaction(f).map(_ => Success(item))
      case e @ Failure(_) => Future.successful(e)
    }
  }

  protected def validateWith(item: T, chain: validations.ValidationContainerChain[_])(f: => DBIOAction[T, NoStream, Effect.All])(implicit ec: ExecutionContext, m: Manifest[T]): Future[ValidationModel] =
    validateWith(item, validations.validateChainAs(item, chain))(f)

  def validate(item: T)(implicit ec: ExecutionContext): Future[ValidationModel] =
    Future.successful(item.success[validations.ValidationMapResult])

  def createDBIO(item: T): ModelDBIO =
    table returning table.map(i => i) += item.asInstanceOf[Q#TableElementType]

  def createDBIO(items: Seq[T]) =
    table returning table.map(i => i) ++= items.asInstanceOf[Seq[Q#TableElementType]]

  def createDBIO(itemOpt: Option[T])(implicit ec: ExecutionContext): ModelDBIOOption =
    itemOpt match {
      case Some(item) => createDBIO(item).map(Some(_))
      case None       => DBIO.successful(Option.empty[T])
    }
}

trait PersistTableWithPk[T] { this: Table[_] =>
  def id: Rep[T]
}

trait PersistTableWithUuidPk extends PersistTableWithPk[UUID] { this: Table[_] =>
  def id = column[UUID]("id", O.PrimaryKey)
}

trait PersistTableWithAutoIntPk extends PersistTableWithPk[Int] { this: Table[_] =>
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
}

trait PersistModelWithPk[Id, T <: models.ModelWithPk[_, Id], Q <: Table[T] with PersistTableWithPk[Id]] extends PersistModel[T, Q] {
  val tableWithId: IntoInsertActionComposer[T, T]

  @inline
  def findByIdQuery(id: T#IdType): Query[Q, T, Seq]

  override def createDBIO(item: T) =
    tableWithId += item

  override def createDBIO(items: Seq[T]) =
    tableWithId ++= items

  @inline
  def mapModel(item: T): T = item

  def recordNotFound(id: T#IdType): ValidationModel =
    recordNotFound("id", id.toString)

  def recordNotFoundAsFuture(id: T#IdType): Future[ValidationModel] =
    Future.successful(recordNotFound(id))

  def create(item: T)(implicit ec: ExecutionContext, m: Manifest[T]): Future[ValidationModel] = {
    val mappedItem = mapModel(item)
    validateWith(mappedItem, validate(mappedItem))(createDBIO(mappedItem))
  }

  def findByIdDBIO(id: T#IdType) =
    findByIdQuery(id).take(1).result.headOption

  def findById(id: T#IdType): Future[Option[T]] =
    db.run(findByIdDBIO(id))

  // TODO: strict update - return error if record dosn't exists
  def update(item: T)(implicit ec: ExecutionContext, m: Manifest[T]): Future[ValidationModel] = {
    val mappedItem = mapModel(item)
    validate(mappedItem).map { res =>
      db.run(findByIdQuery(mappedItem.getId).update(mappedItem))
      res
    }
  }

  // TODO: strict update - return error if record dosn't exists
  def destroy(id: T#IdType)(implicit ec: ExecutionContext) =
    db
      .run(findByIdQuery(id).delete)
}

trait PersistModelWithUuid[T <: models.ModelWithUuid, Q <: Table[T] with PersistTableWithUuidPk] extends PersistModelWithPk[UUID, T, Q] {
  lazy val tableWithId = table returning table.map(_.id) into ((item, _) => item)

  def findByIdQuery(id: UUID): Query[Q, T, Seq] =
    table.filter(_.id === id)
}
