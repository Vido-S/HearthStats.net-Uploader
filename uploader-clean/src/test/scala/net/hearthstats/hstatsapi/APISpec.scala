package net.hearthstats.hstatsapi

import org.junit.runner.RunWith
import org.scalatest.{ Finders, FlatSpec, Matchers }
import com.softwaremill.macwire.MacwireMacros.wire
import net.hearthstats.config.{ TestConfig, TestEnvironment }
import net.hearthstats.core.{ ArenaRun, HearthstoneMatch, MatchOutcome, Rank }
import net.hearthstats.ui.Log
import org.scalatest.junit.JUnitRunner
import net.hearthstats.config.UserConfig
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class APISpec extends FlatSpec with Matchers with MockitoSugar {
  lazy val config: UserConfig = TestConfig
  lazy val uiLog = mock[Log]
  lazy val environment = TestEnvironment

  lazy val api = wire[API]
  lazy val cardUtils = wire[CardUtils]

  "The API" should "return some cards" in {
    val cards = cardUtils.cards
    cards.size should be > 400
  }

  "The API" should "create an Arena run" in {
    val arena = new ArenaRun
    arena.setUserClass("warlock")
    api.createArenaRun(arena).isDefined shouldBe true
    api.getLastArenaRun should not be null
    api.createMatch(new HearthstoneMatch("Arena",
      "warlock",
      "druid",
      false,
      Some(MatchOutcome.VICTORY),
      1,
      "unkownopp",
      Rank.RANK_1,
      1,
      1,
      ""))
    api.endCurrentArenaRun.isDefined shouldBe true
  }
}
