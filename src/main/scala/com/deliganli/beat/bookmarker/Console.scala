package com.deliganli.beat.bookmarker

import cats.effect.Sync

trait Console[F[_]] {
  def printLine(line: String): F[Unit]
}

object Console {
  def apply[F[_]](implicit ev: Console[F]): Console[F] = ev

  def dsl[F[_]: Sync]: Console[F] = (line: String) => Sync[F].delay(println(line))
}
