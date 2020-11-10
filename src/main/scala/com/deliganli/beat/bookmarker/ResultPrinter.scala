package com.deliganli.beat.bookmarker

import java.nio.file.Path

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Monad}
import com.deliganli.beat.bookmarker.Domain.{Segment, SegmentResponse}
import io.circe.Printer
import io.circe.syntax._

trait ResultPrinter[F[_]] {
  def print(segments: List[Segment]): F[Unit]
}

object ResultPrinter {

  def stdout[F[_]: Monad: Console]: ResultPrinter[F] =
    new ResultPrinter[F] {

      override def print(segments: List[Segment]): F[Unit] = {
        Applicative[F]
          .pure(SegmentResponse(segments).asJson.printWith(Printer.spaces2))
          .flatMap(Console[F].printLine(_))
      }
    }

  def filesystem[F[_]: Sync](output: Path): ResultPrinter[F] =
    new ResultPrinter[F] {
      override def print(segments: List[Segment]): F[Unit] = ???
    }
}
