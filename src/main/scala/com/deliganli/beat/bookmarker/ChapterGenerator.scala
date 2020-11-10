package com.deliganli.beat.bookmarker

import cats.Applicative
import cats.implicits._
import com.deliganli.beat.bookmarker.Args.Threshold
import com.deliganli.beat.bookmarker.Domain.{Chapter, Period, State}

import scala.concurrent.duration.Duration
import scala.util.chaining._

trait ChapterGenerator[F[_]] {
  def toChapters(silences: List[Period]): F[List[Chapter]]
}

object ChapterGenerator {

  def dsl[F[_]: Applicative](threshold: Threshold): ChapterGenerator[F] =
    new ChapterGenerator[F] {
      private def diff(p: Period) = p.until - p.from

      /**
        * Either generates a chapter or accumulates intermediate values (multipart dividers) for given silence period
        * @param state chapter start and potential multipart dividers in case of long chapter
        * @param period silence period
        * @return
        */
      def stepOver(state: State, period: Period): Either[List[Period], Chapter] = {
        if (period.from - state.start > threshold.maxChapter) {
          // passed max chapter limit
          if (state.shortSilences.isEmpty) {
            // passed max chapter limit but there is not even a short break eligible for multi parts, either
            // - we can accept even shorter breaks (in that case filtering needs to be lazy on shortSilences)
            // - we ignore the max chapter limit, opted for the second approach here
            val chapter = Chapter.Single(state.start)

            Right(chapter)
          } else {
            val silences = state.start +: state.shortSilences.map(_.until)
            val chapter  = silences.map(Chapter.Single).pipe(Chapter.Multi)

            Right(chapter)
          }
        } else if (diff(period) < threshold.minChapterSilence) {
          // found silence that is below chapter threshold
          val shortSilence = if (diff(period) < threshold.minMultipartSilence) None else Some(period)

          Left(state.shortSilences ++ shortSilence)
        } else {
          // found silence indicating new chapter
          val chapter = Chapter.Single(state.start)

          Right(chapter)
        }
      }

      override def toChapters(silences: List[Period]): F[List[Chapter]] =
        Applicative[F].pure {
          (silences :+ Period(silences.lastOption.map(_.until).getOrElse(Duration.Zero), Duration.Inf))
            .foldLeft(State(Duration.Zero, Nil), List.empty[Chapter]) {
              case ((state, acc), x) =>
                val step     = stepOver(state, x)
                val newState = State(step.as(x.until).getOrElse(state.start), step.left.getOrElse(Nil))

                (newState, acc ++ step.toOption)
            }
            ._2
        }
    }
}
