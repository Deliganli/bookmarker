package com.deliganli.beat.bookmarker

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import com.deliganli.beat.bookmarker.Domain._

trait Program[F[_]] {
  def task: F[Unit]
}

object Program {

  def dsl[F[_]: Sync](args: Args): Program[F] = {
    val periodParser                 = PeriodParser.dsl[F](args.inputXmlFile)
    val chapterGenerator             = ChapterGenerator.dsl[F](args.threshold)
    implicit val console: Console[F] = Console.dsl[F]
    val printer                      = ResultPrinter.stdout[F]

    new Program[F] {
      def task: F[Unit] = {
        Applicative[F].unit
          .flatMap(_ => periodParser.parse)
          .flatMap(chapterGenerator.toChapters)
          .map(_.zipWithIndex.flatMap(Function.tupled(toSegments)))
          .flatTap(printer.print)
          .void
      }
    }
  }

}
