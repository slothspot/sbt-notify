// Copyright 2016 Dmitry Melnichenko.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package name.dmitrym.sbtnotify

import sbt._
import Keys._

import scala.sys.process.Process

object SbtNotifyPlugin extends AutoPlugin {
  lazy val successPair: (Char, String) = ('\u2714', "Glass")
  lazy val failPair: (Char, String) = ('\u2717', "Glass")

  object autoImport {
    val nt = taskKey[Unit]("Compile or test with result notification")
  }
  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override lazy val projectSettings = inConfig(Compile)(
      Seq(
        nt := ntCompileTask.value
      )) ++ inConfig(Test)(
      Seq(
        nt := ntTestTask.value
      ))

  lazy val ntCompileTask = Def.task {
    val prjName = name.value
    val prjAction = "Compile"
    val (prjResultMark, prjResultSound) = compile.result.value match {
      case Inc(inc: Incomplete) => failPair
      case Value(v) => successPair
    }
    showNotification(prjName, prjAction, prjResultSound, prjResultMark)
  }

  lazy val ntTestTask = Def.task {
    val prjName = name.value
    compile.result.value match {
      case Inc(inc: Incomplete) =>
        showNotification(prjName, "Compile", failPair._2, failPair._1)
      case Value(v) =>
        test.result.value match {
          case Inc(inc: Incomplete) =>
            showNotification(prjName, "Test", failPair._2, failPair._1)
          case Value(v) =>
            showNotification(prjName, "Test", successPair._2, successPair._1)
        }
    }
  }

  private[this] def showNotification(prjName: String,
                                     prjAction: String,
                                     prjSound: String,
                                     prjResultMark: Char): Unit = {
    Process(
      Seq(
        "osascript",
        "-s",
        "o",
        "-e",
        "display notification \"" + prjAction + " completed!\" with title \"" + prjResultMark + " " + prjAction + "\" subtitle \"" + prjName + "\" sound name \"" + prjSound + "\"")
    ).!
  }
}
