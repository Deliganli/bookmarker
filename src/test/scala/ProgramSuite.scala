import java.io.File

import cats.Id
import cats.effect.IO
import cats.effect.concurrent.Ref
import com.deliganli.beat.bookmarker.Args.Threshold
import com.deliganli.beat.bookmarker.Domain.Chapter.{Multi, Single}
import com.deliganli.beat.bookmarker.Domain._
import com.deliganli.beat.bookmarker.{ChapterGenerator, Console, PeriodParser, ResultPrinter}
import io.circe.parser._

import scala.concurrent.duration._

class ProgramSuite extends UnitTest {
  behavior of "PeriodParser"
  it should "parse input correctly" in {
    def check(res: String, size: Int) = {
      val periodParser = PeriodParser.dsl[IO](new File(s"src/test/resources/$res.xml"))
      val result       = periodParser.parse.unsafeRunSync()

      result.size shouldEqual size
    }

    check("silence", 470)
    check("silence2", 386)
  }

  behavior of "ChapterGenerator"
  it should "correctly divide chapters" in {
    def check(threshold: Threshold, silences: List[Period], chapters: List[Chapter]) = {
      val CG = ChapterGenerator.dsl[Id](threshold)
      CG.toChapters(silences) shouldEqual chapters
    }

    def period(start: FiniteDuration, offset: Duration) = Period(start, start.plus(offset))

    check(
      Threshold(1.minute, 6.seconds, 10.minutes),
      List(
        period(4.minutes, 5.seconds),                // very short pause
        period(6.minutes, 7.seconds),                // short pause
        period(7.minutes, 1.minutes.plus(3.second)), // pause
        period(10.minutes, 7.seconds),               // short pause
        period(12.minutes, 9.seconds),               // short pause
        period(20.minutes, 2.minutes)                // pause
      ),
      List(
        Single(Duration.Zero),
        Multi(
          List(
            Single(8.minutes.plus(3.seconds)),
            Single(10.minutes.plus(7.seconds)),
            Single(12.minutes.plus(9.seconds))
          )
        ),
        Single(22.minutes)
      )
    )
  }

  behavior of "ResultPrinter"
  it should "format correctly" in {
    def check(segments: List[Segment], expected: String) = {
      (for {
        ref <- Ref.of[IO, String]("")
        _ <- {
          implicit val C: Console[IO] = (line: String) => ref.set(line)
          val RP                      = ResultPrinter.stdout[IO]

          RP.print(segments)
        }
        printed <- ref.get
      } yield parse(printed) shouldEqual parse(expected)).unsafeRunSync()
    }

    check(
      List(
        Segment("Chapter 1", Duration.Zero),
        Segment("Chapter 2", 3.minutes),
        Segment("Chapter 3, part 1", 5.minutes),
        Segment("Chapter 3, part 2", 8.minutes),
        Segment("Chapter 4", 10.minutes)
      ),
      """{
        |  "segments" : [
        |    {
        |      "title" : "Chapter 1",
        |      "offset" : "PT0S"
        |    },
        |    {
        |      "title" : "Chapter 2",
        |      "offset" : "PT3M"
        |    },
        |    {
        |      "title" : "Chapter 3, part 1",
        |      "offset" : "PT5M"
        |    },
        |    {
        |      "title" : "Chapter 3, part 2",
        |      "offset" : "PT8M"
        |    },
        |    {
        |      "title" : "Chapter 4",
        |      "offset" : "PT10M"
        |    }
        |  ]
        }""".stripMargin
    )
  }
}
