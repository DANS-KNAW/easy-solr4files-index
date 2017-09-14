import scala.util.{ Failure, Success }

val stream = Stream(Success(4), Success(5), Success(4), Success(5), Success(4), Failure(new Exception("bla")), Success(1))
stream
  .takeWhile(_.isSuccess)
  .partition(_.get == 4)._1.size
stream.take(1)