package com.deliganli.beat.bookmarker

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.jdk.DurationConverters._

object Domain {
  case class Period(from: Duration, until: Duration)

  sealed trait Chapter

  object Chapter {
    case class Single(offset: Duration)   extends Chapter
    case class Multi(parts: List[Single]) extends Chapter
  }

  case class State(start: Duration, shortSilences: List[Period])

  case class Segment(title: String, offset: Duration)

  object Segment {

    implicit val durationEncoder: Encoder[Duration] = Encoder.encodeString.contramap[Duration] { d =>
      if (d.isFinite) FiniteDuration(d._1, d._2).toJava.toString
      else "infinite"
    }

    implicit val encoder: Encoder[Segment] = deriveEncoder
  }

  case class SegmentResponse(segments: List[Segment])

  object SegmentResponse {
    implicit val encoder: Encoder[SegmentResponse] = deriveEncoder
  }

  def toSegments(chapter: Chapter, index: Int): List[Segment] =
    chapter match {
      case Chapter.Single(offset) => List(Segment(s"Chapter ${index + 1}", offset))
      case Chapter.Multi(parts)   => parts.zipWithIndex.map { case (c, i) => Segment(s"Chapter ${index + 1}, part ${i + 1}", c.offset) }
    }
}
