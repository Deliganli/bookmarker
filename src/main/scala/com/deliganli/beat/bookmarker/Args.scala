package com.deliganli.beat.bookmarker

import java.io.File
import java.nio.file.Paths

import cats.ApplicativeError
import com.deliganli.beat.bookmarker.Args.Threshold
import scopt.{OParserBuilder, Read}

import scala.concurrent.duration.Duration
import scala.jdk.DurationConverters._

case class Args private (
  inputXmlFile: File,
  threshold: Threshold)

object Args {

  case class Threshold(
    minChapterSilence: Duration,
    minMultipartSilence: Duration,
    maxChapter: Duration)

  implicit val fileRead: Read[File]         = Read.reads(Paths.get(_).toFile)
  implicit val durationRead: Read[Duration] = Read.reads(s => java.time.Duration.parse(s).toScala)

  import scopt.OParser
  val builder: OParserBuilder[Args] = OParser.builder[Args]

  val parser: OParser[Unit, Args] = {
    import builder._
    OParser.sequence(
      programName("bookmarker"),
      head(
        "bookmarker",
        "0.x",
        "\nExample usage: sbt \"run src/test/resources/silence.xml PT30S PT15M PT3S\"",
        "\nRequires openjdk 11 or above"
      ),
      arg[File]("<file>")
        .action((x, c) => c.copy(inputXmlFile = x))
        .text("The path to an XML file with silence intervals"),
      arg[Duration]("<min ch silence>")
        .action((x, c) => c.copy(threshold = c.threshold.copy(minChapterSilence = x)))
        .text("The silence duration which reliably indicates a chapter transition"),
      arg[Duration]("<max ch length>")
        .action((x, c) => c.copy(threshold = c.threshold.copy(maxChapter = x)))
        .text("The maximum duration of a segment, after which the chapter will be broken up into multiple segments"),
      arg[Duration]("<min long ch silence>")
        .action((x, c) => c.copy(threshold = c.threshold.copy(minMultipartSilence = x)))
        .text(
          "A silence duration which can be used to split a long chapter (always shorter than the silence duration used to split chapters)"
        ),
      checkConfig(c =>
        Either.cond(
          c.threshold.minMultipartSilence < c.threshold.minChapterSilence,
          (),
          "min multipart silence should be less than min chapter silence"
        )
      )
    )
  }

  /**
    * scopt requires an instance to begin with since each parser generates new instance from previous. Therefore
    * unfortunately we must initialize an instance full of meaningless values
    * @return Seed for scopt
    */
  private def initializer = Args(new File(""), Threshold(Duration.Zero, Duration.Zero, Duration.Zero))

  def parse[F[_]: ApplicativeError[*[_], Throwable]](args: List[String]): F[Args] =
    ApplicativeError[F, Throwable].fromOption(
      OParser.parse(parser, args, initializer),
      new IllegalArgumentException("")
    )
}
