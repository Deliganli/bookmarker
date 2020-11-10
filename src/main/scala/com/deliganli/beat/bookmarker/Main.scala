package com.deliganli.beat.bookmarker

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    IO.unit
      .flatMap(_ => Args.parse[IO](args))
      .flatMap(Program.dsl[IO](_).task)
      .as(ExitCode.Success)
  }
}
