/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.persistence

import akka.testkit.{ ImplicitSender, EventFilter, TestEvent, AkkaSpec }
import java.io.{ IOException, File }
import akka.actor.{ ActorInitializationException, Props, ActorRef }

object SnapshotDirectoryFailureSpec {
  val inUseSnapshotPath = "target/inUseSnapshotPath"

  class TestProcessor(name: String, probe: ActorRef) extends Processor {

    override def persistenceId: String = name

    override def preStart(): Unit = ()

    def receive = {
      case s: String               ⇒ saveSnapshot(s)
      case SaveSnapshotSuccess(md) ⇒ probe ! md.sequenceNr
      case other                   ⇒ probe ! other
    }
  }
}

class SnapshotDirectoryFailureSpec extends AkkaSpec(PersistenceSpec.config("leveldb", "SnapshotDirectoryFailureSpec", extraConfig = Some(
  s"""
    |akka.persistence.snapshot-store.local.dir = "${SnapshotDirectoryFailureSpec.inUseSnapshotPath}"
  """.stripMargin))) with ImplicitSender {

  import SnapshotDirectoryFailureSpec._

  val file = new File(inUseSnapshotPath)

  override protected def atStartup() {
    if (!file.createNewFile()) throw new IOException(s"Failed to create test file [${file.getCanonicalFile}]")
  }

  override protected def afterTermination() {
    if (!file.delete()) throw new IOException(s"Failed to delete test file [${file.getCanonicalFile}]")
  }

  "A local snapshot store configured with an failing directory name " must {
    "throw an exception at startup" in {
      EventFilter[ActorInitializationException](occurrences = 1).intercept {
        val processor = system.actorOf(Props(classOf[TestProcessor], "SnapshotDirectoryFailureSpec-1", testActor))
        processor ! "blahonga"
      }
    }
  }
}
