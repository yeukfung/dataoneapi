import org.specs2.mutable._
import play.api.test.FakeApplication
import specs.TestGlobal

class MasterSpec extends Specification {

  "Master Spec" should {

    val additionalConfiguration: Map[String, String] = Map()
    Map("mongodb.db" -> "testNaajaaDB")
    def application = FakeApplication(additionalConfiguration = additionalConfiguration)

    step({
      play.api.Play.start(application)
      TestGlobal.started = true
    })

    include(new specs.actors.traffic.TrafficSpeedActorSpec)

    step(play.api.Play.stop())

  }

}