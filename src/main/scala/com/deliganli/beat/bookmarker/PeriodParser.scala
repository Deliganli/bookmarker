package com.deliganli.beat.bookmarker

import java.io.File

import cats.effect.Sync
import cats.implicits._
import com.deliganli.beat.bookmarker.Domain.Period

import scala.jdk.DurationConverters._
import scala.xml.{Elem, Node, XML}

trait PeriodParser[F[_]] {
  def parse: F[List[Period]]
}

object PeriodParser {

  def dsl[F[_]: Sync](input: File): PeriodParser[F] =
    new PeriodParser[F] {

      def parseInput(xml: Elem): List[Period] = {
        // can have error handling, assumed no error
        (xml \\ "silence")
          .map(n => Period(extractDuration(n, "from"), extractDuration(n, "until")))
          .toList
      }

      // TODO: make it a typeclass
      private def extractDuration(n: Node, tag: String) = {
        java.time.Duration.parse(n \@ tag).toScala
      }

      override def parse: F[List[Period]] = Sync[F].delay(XML.loadFile(input)).map(parseInput)
    }
}
