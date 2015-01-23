package amlibs.core

import play.api.libs.iteratee.Enumerator
import scala.concurrent._


/**
 * Abstract Datasource representation to be implemented for each DB.
 * The parameterized types means:
 * - T : the record type to be managed by datasource
 * - Id: the Id type of the record (please note it's separated from T but can be contained by T too)
 * - Query: the type to be used for all queries and selectors for batch operations
 * - Update: the type to be used for all partial or batch updates
 */
trait RESTAPI[T, Id, Query, Update, BatchReturn] {
  /**
   * Inserts a record and returns Id
   *
   * @param t the record to be stored
   * @param ctx the execution context used to execute the async action
   * @return the future generated Id (the generated Id can also be part of input data T
   * and managed by the underlying implementation).
   * if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def insert(t: T)(implicit ctx: ExecutionContext): Future[Id]

  /**
   * Fetches a record from its Id
   *
   * @param id the Id of the record to fetch
   * @param ctx the execution context used to execute the async action
   * @return if the id is found, the record is returned plus its Id (because the Id can be part of T but not necessarily).
   * if the id is not found, it returns None.
   * if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def get(id: Id)(implicit ctx: ExecutionContext): Future[Option[(T, Id)]]

  /**
   * Deletes a record by its Id
   *
   * @param id the Id of the record to delete
   * @param ctx the execution context used to execute the async action
   * @return if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def delete(id: Id)(implicit ctx: ExecutionContext): Future[Unit]

  /**
   * Updates a FULL record by its Id
   *
   * @param id the Id of the record to update
   * @param t the full record data
   * @param ctx the execution context used to execute the async action
   * @return if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def update(id: Id, t: T)(implicit ctx: ExecutionContext): Future[Unit]

  /**
   * Updates PARTIALLY a record by its Id
   *
   * @param id the Id of the record to update
   * @param update the partial descriptor of the data to be updated in this record
   * @param ctx the execution context used to execute the async action
   * @return if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def updatePartial(id: Id, upd: Update)(implicit ctx: ExecutionContext): Future[Unit]

  /**
   * Finds records using a Query selector
   *
   * @param sel the Query selector used to filter returned records
   * @param limit the max number of records to be fetched (could also be integrated in Query selector)
   * @param skip the number of records to skip (offset) before fetching (could also be integrated in Query selector)
   * @param ctx the execution context used to execute the async action
   * @return a sequence of (record plus Id) (the Id can be contained in record but not necessarily)
   * if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def find(sel: Query, limit: Int = 0, skip: Int = 0)(implicit ctx: ExecutionContext): Future[TraversableOnce[(T, Id)]]

  /**
   * Finds records using a Query selector and streams results page by page
   * (page streaming can allow better performances than streaming record by reocrd)
   *
   * @param sel the Query selector used to filter returned records
   * @param skip the number of records to skip (offset) before fetching (could also be integrated in Query selector)
   * @param pageSize the max number of records to be fetched by batch (could also be integrated in Query selector)
   * @param ctx the execution context used to execute the async action
   * @return an enumerator of iterator of (record plus Id) (the Id can be contained in record but not necessarily).
   * the enumerator represents the stream.
   * the iterator represents a page of max size pageSize.
   * if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
//  def findStream(sel: Query, skip: Int = 0, pageSize: Int = 0)(implicit ctx: ExecutionContext): Enumerator[TraversableOnce[(T, Id)]]

  /**
   * Inserts a batch of records and returns the number of inserted records
   *
   * @param t an enumerator of records to be stored (generated Id can be included in T)
   * @param ctx the execution context used to execute the async action
   * @return the future number of inserted records Id.
   * if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def batchInsert(elems: Enumerator[T])(implicit ctx: ExecutionContext): Future[BatchReturn]

  /**
   * Deletes a batch of records using a query Selector
   *
   * @param sel the Query selector used to filter returned records
   * @param ctx the execution context used to execute the async action
   * @return if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def batchDelete(sel: Query)(implicit ctx: ExecutionContext): Future[BatchReturn]

  /**
   * Updates a batch of records using a query Selector and an update Descriptor
   *
   * @param sel the Query selector used to filter returned records
   * @param update the partial descriptor of the data to be updated in this record
   * @param ctx the execution context used to execute the async action
   * @return if there are any DB error, it should be encapsulated in a Future.failed(Exception).
   */
  def batchUpdate(sel: Query, upd: Update)(implicit ctx: ExecutionContext): Future[BatchReturn]
}